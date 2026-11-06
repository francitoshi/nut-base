/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A persistent append-only key-value store of {@code byte[]} keys and values,
 * one Bitcask directory per instance, following the bitcask-format.md
 * design: an in-memory keydir (section 5) over append-only data files
 * (section 4), optional hint files (section 7), rotation by size (6.6) and a
 * blocking merge (6.7, implemented in a later step).
 * <p>
 * A directory is opened by at most one process: the exclusive
 * {@code bitcask.write.lock} file must be acquirable with
 * {@link FileChannel#tryLock}, otherwise the open fails. Between threads a
 * {@link ReentrantReadWriteLock} allows concurrent gets and iterates while
 * puts, deletes, rotation and merges are serialized.
 * <p>
 * A store that is never opened with {@link #open} or {@link Builder#build}
 * and never closed again is not usable: every operation on a closed store
 * throws {@link IllegalStateException}.
 */
public class Bitcask implements Closeable
{
    /** Name of the exclusive lock file inside the store directory. */
    public static final String LOCK_FILE = "bitcask.write.lock";

    /** Suffix of every data file, preceded by its numeric id. */
    public static final String DATA_SUFFIX = ".bitcask.data";

    /** Suffix of every hint file, preceded by its numeric id. */
    public static final String HINT_SUFFIX = ".bitcask.hint";

    /** Suffix of merge temporaries; these are deleted at open time. */
    public static final String TMP_SUFFIX = ".tmp";

    /** Default rotation threshold for the active data file: 1 GiB. */
    public static final long DEFAULT_MAX_FILE_SIZE = 1L << 30;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final KeyDir keydir = new KeyDir();
    private final HashMap<Long, FileChannel> channels = new HashMap<>();
    private final Path directory;
    private final long maxFileSize;
    private final SyncMode syncMode;
    private final long syncIntervalMillis;
    private final boolean forceRecovery;

    private FileChannel lockChannel;
    private FileLock fileLock;

    private FileChannel activeChannel;
    private long activeId;
    private long activeSize;

    private long lastSyncMillis;
    private boolean closed;

    private Bitcask(Builder builder) throws IOException
    {
        this.directory = builder.directory;
        this.maxFileSize = builder.maxFileSize;
        this.syncMode = builder.syncMode;
        this.syncIntervalMillis = builder.syncIntervalMillis;
        this.forceRecovery = builder.forceRecovery;

        try
        {
            openStore();
        }
        catch (IOException | RuntimeException failure)
        {
            // a failed open must not keep the directory locked
            try
            {
                close();
            }
            catch (IOException ignored)
            {
                // the original failure matters more
            }
            throw failure;
        }
    }

    /**
     * Opens a store with the default configuration: a 1 GiB rotation
     * threshold and {@link SyncMode#EVERY_WRITE}.
     *
     * @param directory the store directory, created if missing
     * @return the open store
     * @throws IOException if the lock cannot be acquired or the recovery fails
     */
    public static Bitcask open(String directory) throws IOException
    {
        return open(java.nio.file.Paths.get(directory));
    }

    /**
     * Opens a store with the default configuration: a 1 GiB rotation
     * threshold and {@link SyncMode#EVERY_WRITE}.
     *
     * @param directory the store directory, created if missing
     * @return the open store
     * @throws IOException if the lock cannot be acquired or the recovery fails
     */
    public static Bitcask open(Path directory) throws IOException
    {
        return new Builder(directory).build();
    }

    /**
     * Returns a builder for a store in the given directory.
     *
     * @param directory the store directory, created by build() if missing
     * @return the builder
     */
    public static Builder builder(String directory)
    {
        return new Builder(java.nio.file.Paths.get(directory));
    }

    /**
     * Returns a builder for a store in the given directory.
     *
     * @param directory the store directory, created by build() if missing
     * @return the builder
     */
    public static Builder builder(Path directory)
    {
        return new Builder(directory);
    }

    /**
     * Configuration of a store to build. All methods return the builder for
     * chaining.
     */
    public static class Builder
    {
        private final Path directory;
        private long maxFileSize = DEFAULT_MAX_FILE_SIZE;
        private SyncMode syncMode = SyncMode.EVERY_WRITE;
        private long syncIntervalMillis = 1000;
        private boolean forceRecovery = false;

        private Builder(Path directory)
        {
            this.directory = directory;
        }

        /**
         * Sets the rotation threshold of the active data file.
         *
         * @param bytes the maximum size of the active file before rotation
         * @return the builder
         */
        public Builder maxFileSize(long bytes)
        {
            this.maxFileSize = bytes;
            return this;
        }

        /**
         * Sets the durability policy with no interval (NONE, EVERY_WRITE).
         *
         * @param syncMode the policy; must not be INTERVAL
         * @return the builder
         */
        public Builder sync(SyncMode syncMode)
        {
            if (syncMode == SyncMode.INTERVAL)
            {
                throw new IllegalArgumentException("use sync(SyncMode.INTERVAL, millis) for interval mode");
            }
            this.syncMode = syncMode;
            return this;
        }

        /**
         * Sets the durability policy with an interval for INTERVAL mode.
         *
         * @param syncMode the policy; must be INTERVAL
         * @param syncIntervalMillis the minimum milliseconds between forces
         * @return the builder
         */
        public Builder sync(SyncMode syncMode, long syncIntervalMillis)
        {
            if (syncMode != SyncMode.INTERVAL)
            {
                throw new IllegalArgumentException("syncIntervalMillis applies only to SyncMode.INTERVAL");
            }
            this.syncMode = syncMode;
            this.syncIntervalMillis = syncIntervalMillis;
            return this;
        }

        /**
         * Enables forced recovery: any corruption found while recovering also
         * truncates immutable files instead of aborting the open.
         *
         * @param forceRecovery true to recover even from immutable file corruption
         * @return the builder
         */
        public Builder forceRecovery(boolean forceRecovery)
        {
            this.forceRecovery = forceRecovery;
            return this;
        }

        /**
         * Opens the store: acquires the lock, recovers the keydir from the
         * data and hint files and creates a new active data file.
         *
         * @return the open store
         * @throws IOException if the lock cannot be acquired or the recovery fails
         */
        public Bitcask build() throws IOException
        {
            return new Bitcask(this);
        }
    }

    // ------------------------------------------------------------------ API

    /**
     * Stores a value under a key, appending the entry to the active data
     * file and possibly rotating first.
     *
     * @param key the key, 1 to 65535 bytes long
     * @param value the value, 0 to MAX_VALUE_SIZE bytes long; empty values are allowed
     * @throws IOException if the entry cannot be written
     * @throws IllegalStateException if the store is closed
     */
    public void put(byte[] key, byte[] value) throws IOException
    {
        BitcaskFormat.validateSizes(key, value);

        int tstamp = now();
        byte[] entry = BitcaskFormat.encode(key, value, tstamp);

        lock.writeLock().lock();
        try
        {
            checkOpen();

            if (activeSize > 0 && activeSize + entry.length > maxFileSize)
            {
                rotate();
            }

            long offset = writeEntry(entry);
            keydir.put(key, new KeyDirEntry(activeId, offset, entry.length, tstamp));
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the value stored under the given key, or null if the store
     * holds no such live key.
     *
     * @param key the key bytes
     * @return the value, or null if the key is absent
     * @throws IOException if the entry cannot be read back
     * @throws CorruptEntryException if the stored entry fails the CRC check
     * @throws IllegalStateException if the store is closed
     */
    public byte[] get(byte[] key) throws IOException
    {
        BitcaskFormat.validateKeySize(key);

        lock.readLock().lock();
        try
        {
            checkOpen();
            return readValue(keydir.get(key), key);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * Marks a key as deleted by appending a tombstone entry to the active
     * data file and removing the key from the keydir.
     *
     * @param key the key bytes
     * @return true if a tombstone was written, false if the key was already absent
     * @throws IOException if the tombstone cannot be written
     * @throws IllegalStateException if the store is closed
     */
    public boolean delete(byte[] key) throws IOException
    {
        BitcaskFormat.validateKeySize(key);

        int tstamp = now();
        byte[] tombstone = BitcaskFormat.encodeTombstone(key, tstamp);

        lock.writeLock().lock();
        try
        {
            checkOpen();

            if (!keydir.containsKey(key))
            {
                return false; // no tombstone for unknown keys (section 6.5)
            }

            if (activeSize > 0 && activeSize + tombstone.length > maxFileSize)
            {
                rotate();
            }

            long offset = writeEntry(tombstone);
            keydir.remove(key);
            return true;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns a defensive-copy snapshot of every live key, in no particular
     * order.
     *
     * @return the live keys
     * @throws IllegalStateException if the store is closed
     */
    public byte[][] keys()
    {
        lock.readLock().lock();
        try
        {
            checkOpen();
            return keydir.keys();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns true if the store holds a live value for the given key.
     *
     * @param key the key bytes
     * @return true if the key is present
     * @throws IllegalStateException if the store is closed
     */
    public boolean containsKey(byte[] key)
    {
        BitcaskFormat.validateKeySize(key);

        lock.readLock().lock();
        try
        {
            checkOpen();
            return keydir.containsKey(key);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the number of live keys.
     *
     * @return the size of the keydir
     * @throws IllegalStateException if the store is closed
     */
    public int size()
    {
        lock.readLock().lock();
        try
        {
            checkOpen();
            return keydir.size();
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * Closes the store: forces the active file, releases every channel and
     * the directory lock. Idempotent: closing again does nothing.
     *
     * @throws IOException if a channel cannot be closed
     */
    @Override
    public void close() throws IOException
    {
        lock.writeLock().lock();
        try
        {
            if (closed)
            {
                return;
            }
            closed = true;

            try
            {
                if (activeChannel != null && activeChannel.isOpen())
                {
                    activeChannel.force(true);
                }
            }
            catch (IOException e)
            {
                // closing proceeds even if the last force fails
            }

            closeChannels();

            keydir.clear();

            if (fileLock != null)
            {
                fileLock.release();
            }
            if (lockChannel != null)
            {
                lockChannel.close();
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    // -------------------------------------------------------------- internals

    /**
     * The blocking merge of section 6.7: copies every live key to fresh data
     * files with new ids (tombstones vanish, dead values vanish), writes one
     * hint file per output, renames the temporaries and deletes the old
     * files from oldest to newest, so a tombstone can never be lost before
     * the entry it annulled.
     * <p>
     * The store holds its write lock for the whole merge, which blocks every
     * other operation in this process: reads, writes and rotates. Concurrent
     * processes cannot interfere either: the whole merge runs under the
     * directory lock, and a crashed merge loses at most the temporaries,
     * deleted at the next open.
     *
     * @throws IOException on any file error; the store stays usable and the
     *                     temporaries are removed at the next open or merge
     * @throws IllegalStateException if the store is closed
     */
    public void merge() throws IOException
    {
        lock.writeLock().lock();
        try
        {
            checkOpen();
            doMerge();
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * One live key and the location of its entry, queued for copying.
     */
    private static final class CopyItem
    {
        final byte[] key;
        final KeyDirEntry entry;

        CopyItem(byte[] key, KeyDirEntry entry)
        {
            this.key = key;
            this.entry = entry;
        }
    }

    private void doMerge() throws IOException
    {
        // the preconditions: freeze the active file as immutable; its channel
        // stays open because it is the read channel for its id in this store
        activeChannel.force(true);
        activeChannel = null;   // recreateActiveFile installs the next one

        long maxId = activeId;  // every id <= maxId is deleted afterwards

        List<CopyItem> items = new ArrayList<>();
        keydir.forEach((key, entry) -> items.add(new CopyItem(key, entry)));

        if (items.isEmpty())
        {
            deleteOldFiles(maxId);
            createActiveFile();
            return;
        }

        sortByLocation(items);

        // sorted by file, then offset: an output file is contiguous reads,
        // and offsets from previous files stay grouped
        FileChannel out = null;
        ByteArrayOutputStream hintBody = new ByteArrayOutputStream();
        long outId = maxId; // outputs take the ids maxId+1, maxId+2, ...
        long outSize = 0;
        Map<byte[], KeyDirEntry> newPositions = new HashMap<>();

        try
        {
            for (CopyItem item : items)
            {
                int entrySize = item.entry.entrySize;

                if (out == null || outSize + entrySize > maxFileSize)
                {
                    if (out != null)
                    {
                        closeOutput(out, hintBody, outId, outSize);
                        channels.put(outId, out); // survives the rename
                    }

                    outId++; // outputs are the ids maxId+1, maxId+2, ...
                    out = openChannel(tmpDataPath(outId));
                    hintBody.reset();
                    outSize = 0;
                }

                long entryOffset = outSize;
                copyEntry(item, out, entryOffset);
                outSize += entrySize;

                byte[] hintEntry = BitcaskHint.encode(item.key, item.entry.tstamp, entrySize, entryOffset);
                hintBody.write(hintEntry);

                newPositions.put(item.key, new KeyDirEntry(outId, entryOffset, entrySize, item.entry.tstamp));
            }

            closeOutput(out, hintBody, outId, outSize);
            channels.put(outId, out);
            long lastOutputId = outId;

            renameOutputs(maxId, outId);
            deleteOldFiles(maxId); // never deletes > maxId: outputs stay

            keydir.clear();
            for (Map.Entry<byte[], KeyDirEntry> position : newPositions.entrySet())
            {
                keydir.put(position.getKey(), position.getValue());
            }

            activeId = lastOutputId;
            createActiveFile();
        }
        finally
        {
            if (out != null && out.isOpen() && !channels.containsValue(out))
            {
                // on failure the temporary is not adopted: close what is open
                out.close();
            }
        }
    }

    /**
     * Copies one entry verbatim from its source data file to the output.
     * The bytes are already valid and stay untouched (the original tstamp is
     * kept by copying raw bytes), and the CRC is preserved.
     */
    private void copyEntry(CopyItem item, FileChannel out, long entryOffset) throws IOException
    {
        FileChannel source = channels.get(item.entry.fileId);

        if (source == null)
        {
            throw new IOException("no channel open for data file " + item.entry.fileId + " during merge");
        }

        ByteBuffer buffer = ByteBuffer.allocate(item.entry.entrySize);
        readFully(source, buffer, item.entry.entryOffset);
        buffer.flip();

        writeFully(out, buffer, entryOffset);
    }

    /**
     * Forces the data temporary and writes, forces and closes its hint
     * temporary (section 7): one hint entry per copied key and the closing
     * trailer with the CRC over the hint body and the data file length.
     */
    private void closeOutput(FileChannel out, ByteArrayOutputStream hintBody, long outId, long outSize) throws IOException
    {
        out.force(true);

        byte[] body = hintBody.toByteArray();
        byte[] trailer = BitcaskHint.encodeTrailer(body, outSize);

        FileChannel hint = openChannel(tmpHintPath(outId));
        try
        {
            writeFully(hint, ByteBuffer.wrap(body), 0);
            writeFully(hint, ByteBuffer.wrap(trailer), body.length);
            hint.force(true);
        }
        finally
        {
            hint.close();
        }
    }

    /**
     * Renames every merge output to its final name: all data files first,
     * then all hint files, in ascending id order, with atomic moves.
     */
    private void renameOutputs(long maxId, long lastOutputId) throws IOException
    {
        for (long id = maxId + 1; id <= lastOutputId; id++)
        {
            Files.move(tmpDataPath(id), dataPath(id), StandardCopyOption.ATOMIC_MOVE);
        }

        for (long id = maxId + 1; id <= lastOutputId; id++)
        {
            Files.move(tmpHintPath(id), hintPath(id), StandardCopyOption.ATOMIC_MOVE);
        }

        syncDirectory();
    }

    /**
     * Deletes every file of every id up to maxId, oldest first, so a
     * tombstone is never deleted before the entry it annulled (section 6.7
     * step 7). Every data file goes with its hint.
     */
    private void deleteOldFiles(long maxId) throws IOException
    {
        for (long id = 1; id <= maxId; id++)
        {
            FileChannel channel = channels.remove(id);
            if (channel != null && channel.isOpen())
            {
                channel.close();
            }

            Files.deleteIfExists(dataPath(id));
            Files.deleteIfExists(hintPath(id));
        }

        syncDirectory();
    }

    /**
     * Orders the copy items by file id and entry offset, keeping the data
     * reads sequential.
     */
    private static void sortByLocation(List<CopyItem> items)
    {
        Collections.sort(items, (a, b) -> a.entry.compareTo(b.entry));
    }

    private int now()
    {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    private void checkOpen()
    {
        if (closed)
        {
            throw new IllegalStateException("bitcask store is closed");
        }
    }

    /**
     * Open sequence of section 6.1: lock, delete temporaries, recover the
     * keydir from data (or hint) files, and create a brand-new active file.
     */
    private void openStore() throws IOException
    {
        Files.createDirectories(directory);

        acquireLock();
        deleteTemporaries();

        List<Long> ids = listDataFileIds();
        activeId = ids.isEmpty() ? 0 : ids.get(ids.size() - 1); // the next active file gets max + 1

        for (int i = 0; i < ids.size(); i++)
        {
            long id = ids.get(i);
            Path dataPath = dataPath(id);
            Path hintPath = hintPath(id);
            boolean newest = i == ids.size() - 1;

            if (tryLoadFromHint(id, dataPath, hintPath))
            {
                channels.put(id, openChannel(dataPath));
                continue;
            }

            boolean recovery = newest || forceRecovery;
            try (FileChannel channel = FileChannel.open(dataPath,
                StandardOpenOption.READ, recovery ? StandardOpenOption.WRITE : StandardOpenOption.READ))
            {
                DataFileScanner.scan(channel, id, keydir, recovery);
            }
            channels.put(id, openChannel(dataPath));
        }

        createActiveFile();
    }

    private void acquireLock() throws IOException
    {
        lockChannel = FileChannel.open(directory.resolve(LOCK_FILE),
            StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);

        try
        {
            fileLock = lockChannel.tryLock();
        }
        catch (java.nio.channels.OverlappingFileLockException e)
        {
            throw new IOException("directory is already locked within this JVM: " + directory, e);
        }

        if (fileLock == null)
        {
            lockChannel.close();
            lockChannel = null;
            throw new IOException("directory is already locked by another process: " + directory);
        }
    }

    private void deleteTemporaries() throws IOException
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + TMP_SUFFIX))
        {
            for (Path path : stream)
            {
                Files.deleteIfExists(path);
            }
        }
    }

    /**
     * Returns the ids of every data file, sorted numerically ascending. The
     * active file is always the highest one.
     */
    private List<Long> listDataFileIds() throws IOException
    {
        List<Long> ids = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + DATA_SUFFIX))
        {
            for (Path path : stream)
            {
                String name = path.getFileName().toString();
                String id = name.substring(0, name.length() - DATA_SUFFIX.length());

                if (id.matches("[1-9][0-9]*"))
                {
                    ids.add(Long.parseLong(id));
                }
            }
        }

        Collections.sort(ids);
        return ids;
    }

    /**
     * Attempts to load a data file's entries from its hint file (section
     * 7.3): the hint must end with a valid trailer and the data file length
     * must equal the trailer offset. Returns false to ask for a full scan.
     */
    private boolean tryLoadFromHint(long fileId, Path dataPath, Path hintPath) throws IOException
    {
        if (!Files.exists(hintPath) || !Files.exists(dataPath) || Files.size(dataPath) < BitcaskFormat.HEADER_SIZE)
        {
            return false;
        }

        byte[] hint = Files.readAllBytes(hintPath);
        BitcaskHint.Trailer trailer = BitcaskHint.decodeTrailer(hint);

        if (trailer == null || Files.size(dataPath) != trailer.dataFileLength)
        {
            return false;
        }

        int offset = 0;

        while (offset < hint.length)
        {
            BitcaskHint.HintEntry entry = BitcaskHint.decode(hint, offset, hint.length - offset);

            if (entry == null)
            {
                break; // reached the trailer
            }

            byte[] key = new byte[entry.keySize];
            System.arraycopy(hint, offset + BitcaskHint.HEADER_SIZE, key, 0, entry.keySize);
            keydir.put(key, new KeyDirEntry(fileId, entry.offset, entry.totalSize, entry.tstamp));

            offset += BitcaskHint.HEADER_SIZE + entry.keySize;
        }

        return true;
    }

    /**
     * Closes the current active channel as immutable and opens a fresh data
     * file with the next id as the new active file (section 6.6).
     */
    private void rotate() throws IOException
    {
        activeChannel.force(true); // always, regardless of the sync policy
        createActiveFile();
    }

    /**
     * Creates the next data file as the new active one. Writing never
     * resumes in a previous file, so an interrupted tail is never mixed with
     * new data (section 6.1).
     */
    private void createActiveFile() throws IOException
    {
        Path path = dataPath(activeId + 1);

        if (Files.exists(path))
        {
            throw new IOException("unexpected data file with id " + (activeId + 1) + ": " + path);
        }

        activeChannel = openChannel(path);
        activeId = activeId + 1;
        activeSize = 0;
        channels.put(activeId, activeChannel);
        syncDirectory();
    }

    /**
     * Appends the entry bytes at the end of the active file and returns the
     * offset where it starts. A single write is not guaranteed to consume
     * the buffer, so it is looped.
     */
    private long writeEntry(byte[] entry) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.wrap(entry);
        long offset = activeSize;

        writeFully(activeChannel, buffer, offset);
        activeSize += entry.length;

        applySyncPolicy();

        return offset;
    }

    /**
     * Applies the configured durability policy of section 9 to the active
     * channel after a put or delete.
     */
    private void applySyncPolicy() throws IOException
    {
        long now = System.currentTimeMillis();

        switch (syncMode)
        {
            case EVERY_WRITE:
                activeChannel.force(false);
                lastSyncMillis = now;
                break;
            case INTERVAL:
                if (now - lastSyncMillis >= syncIntervalMillis)
                {
                    activeChannel.force(false);
                    lastSyncMillis = now;
                }
                break;
            case NONE:
            default:
                break;
        }
    }

    /**
     * Read a value back through one keydir position: the entry is read in
     * full, the CRC checked and the key re-verified.
     */
    private byte[] readValue(KeyDirEntry location, byte[] key) throws IOException
    {
        if (location == null)
        {
            return null;
        }

        FileChannel channel = channels.get(location.fileId);

        if (channel == null)
        {
            throw new IOException("no channel open for data file " + location.fileId);
        }

        ByteBuffer buffer = ByteBuffer.allocate(location.entrySize);
        readFully(channel, buffer, location.entryOffset);

        BitcaskFormat.Entry record = BitcaskFormat.decode(buffer.array(), 0, location.entrySize);

        if (!java.util.Arrays.equals(record.key, key))
        {
            throw new CorruptEntryException("stored key does not match the requested key at " + location.fileId + "@" + location.entryOffset);
        }

        return record.value;
    }

    private void writeFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException
    {
        while (buffer.hasRemaining())
        {
            int written = channel.write(buffer, position);

            if (written < 0)
            {
                throw new IOException("write failed at position " + position);
            }

            position += written;
        }
    }

    private static void readFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException
    {
        while (buffer.hasRemaining())
        {
            int read = channel.read(buffer, position);

            if (read < 0)
            {
                throw new IOException("unexpected end of file at position " + position);
            }

            position += read;
        }
    }

    private FileChannel openChannel(Path path) throws IOException
    {
        return FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    }

    private Path dataPath(long id)
    {
        return directory.resolve(id + DATA_SUFFIX);
    }

    private Path hintPath(long id)
    {
        return directory.resolve(id + HINT_SUFFIX);
    }

    private Path tmpDataPath(long id)
    {
        return directory.resolve(id + DATA_SUFFIX + TMP_SUFFIX);
    }

    private Path tmpHintPath(long id)
    {
        return directory.resolve(id + HINT_SUFFIX + TMP_SUFFIX);
    }

    private void closeChannels() throws IOException
    {
        IOException failure = null;

        for (FileChannel channel : channels.values())
        {
            try
            {
                if (channel.isOpen())
                {
                    channel.close();
                }
            }
            catch (IOException e)
            {
                if (failure == null)
                {
                    failure = e;
                }
            }
        }

        channels.clear();
        activeChannel = null;

        if (failure != null)
        {
            throw failure;
        }
    }

    /**
     * Syncs the directory after create, rename or delete operations so the
     * directory entry itself is durable (section 9). Not possible on every
     * platform (Windows refuses to open a directory), and silently skipped
     * there.
     */
    private void syncDirectory()
    {
        FileChannel channel = null;
        try
        {
            channel = FileChannel.open(directory, StandardOpenOption.READ);
            channel.force(true);
        }
        catch (IOException e)
        {
            // unsupported on this platform: nothing durable to gain
        }
        finally
        {
            if (channel != null)
            {
                try
                {
                    channel.close();
                }
                catch (IOException e)
                {
                    // ignored
                }
            }
        }
    }
}
