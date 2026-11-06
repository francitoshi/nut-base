/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The encrypted profile of the Bitcask spec (appendix A): the same disk
 * layout and API as {@link Bitcask}, but every data file carries a header
 * with its own salt and every entry is AES-256-GCM encrypted with a key
 * derived per file, so keys and values are opaque at rest.
 * <p>
 * Off-disk layout per profile: a {@code bitcask.meta} file holds the PBKDF2
 * parameters and a GCM probe for the password (created once, wrong password
 * fails); each {@code <id>.bitcask.data} file starts {@link
 * #DATA_HEADER_SIZE} bytes of header followed by encrypted entries. The
 * header fields of the spec add up to 4+1+8+16 = 29 bytes, not the 28 stated
 * (an arithmetic slip in the appendix); this implementation uses 29.
 * <p>
 * Each entry is stored as {@code len(4) nonce(12) ciphertext+tag(len)}, the
 * GCM tag (16 bytes) replaces the plain-profile CRC, the associated data is
 * fileId and entry offset (so entries cannot be relocated or duplicated),
 * and the nonce counter restarts per file. A per-file key
 * {@code HMAC-SHA256(masterKey, "bitcask-file" || fileId || fileSalt)} means
 * a nonce reuse in one file cannot endanger another file.
 * <p>
 * Hint files are never generated here (they would reveal keys); opening
 * scans and decrypts everything. The merge re-encrypts the entries into
 * fresh files with a fresh salt and counter, tombstones vanish, and the
 * rename-and-delete safety sequence is the same as the plain profile.
 * <p>
 * One instance owns one directory (the shared {@code bitcask.write.lock})
 * and, like the plain store, serializes writers with a read-write lock.
 */
public class EncryptedBitcask implements Closeable
{
    /** Name of the metadata file, created once when the store is initialized. */
    public static final String META_FILE = "bitcask.meta";

    /**
     * Size in bytes of the per-file header: magic BCKE (4) + version (1) +
     * file id (8) + file salt (16).
     */
    public static final int DATA_HEADER_SIZE = 29;

    /** Minimum PBKDF2 iterations allowed by the spec. */
    public static final int DEFAULT_ITERATIONS = 600000;

    /** Lowest iteration count accepted by the builder (for tests only). */
    public static final int MIN_ITERATIONS_ALLOWED = 1000;

    private static final byte[] META_MAGIC = {'B', 'C', 'K', 'M'};
    private static final byte[] DATA_MAGIC = {'B', 'C', 'K', 'E'};
    private static final byte META_VERSION = 0x01;
    private static final byte[] CHECK_PLAINTEXT = "bitcask-check".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static final byte[] FILE_KEY_INFO = "bitcask-file".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static final int TAG_BITS = 128;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final KeyDir keydir = new KeyDir();
    private final HashMap<Long, FileChannel> channels = new HashMap<>();
    private final HashMap<Long, SecretKey> fileKeys = new HashMap<>();
    private final Path directory;
    private final long maxFileSize;
    private final SyncMode syncMode;
    private final long syncIntervalMillis;

    private SecretKey masterKey;
    private FileChannel lockChannel;
    private FileLock fileLock;

    private FileChannel activeChannel;
    private SecretKey activeKey;
    private long activeId;
    private long activeSize;
    private long activeCounter;
    private long lastSyncMillis;
    private boolean closed;

    private EncryptedBitcask(Builder builder) throws IOException
    {
        this.directory = builder.directory;
        this.maxFileSize = builder.maxFileSize;
        this.syncMode = builder.syncMode;
        this.syncIntervalMillis = builder.syncIntervalMillis;

        try
        {
            openStore(builder.password, builder.iterations);
        }
        catch (IOException | RuntimeException failure)
        {
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
     * Opens (or initializes) an encrypted store with the default settings.
     *
     * @param directory the store directory, created if missing
     * @param password the password; it is copied and the copy zeroed after key derivation
     * @return the open store
     * @throws IOException if the password is wrong or the store cannot be opened
     */
    public static EncryptedBitcask open(Path directory, char[] password) throws IOException
    {
        return new Builder(directory, password).build();
    }

    /**
     * Opens (or initializes) an encrypted store with the default settings.
     *
     * @param directory the store directory, created if missing
     * @param password the password
     * @return the open store
     * @throws IOException if the password is wrong or the store cannot be opened
     */
    public static EncryptedBitcask open(String directory, char[] password) throws IOException
    {
        return new Builder(java.nio.file.Paths.get(directory), password).build();
    }

    /**
     * Returns a builder for an encrypted store in the given directory.
     *
     * @param directory the store directory, created by build() if missing
     * @param password the password
     * @return the builder
     */
    public static Builder builder(Path directory, char[] password)
    {
        return new Builder(directory, password);
    }

    /**
     * Configuration of an encrypted store to build.
     */
    public static class Builder
    {
        private final Path directory;
        private final char[] password;
        private long maxFileSize = Bitcask.DEFAULT_MAX_FILE_SIZE;
        private SyncMode syncMode = SyncMode.EVERY_WRITE;
        private long syncIntervalMillis = 1000;
        private int iterations = DEFAULT_ITERATIONS;

        private Builder(Path directory, char[] password)
        {
            if (password == null || password.length == 0)
            {
                throw new IllegalArgumentException("a non-empty password is required");
            }

            this.directory = directory;
            this.password = password;
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
         * Sets the PBKDF2 iteration count. The default and the spec minimum
         * is 600000; a lower value must only be used in tests, never with
         * real data, and still no less than 1000.
         *
         * @param iterations the PBKDF2 iteration count
         * @return the builder
         */
        public Builder iterations(int iterations)
        {
            if (iterations < MIN_ITERATIONS_ALLOWED)
            {
                throw new IllegalArgumentException("iterations must be at least " + MIN_ITERATIONS_ALLOWED);
            }
            this.iterations = iterations;
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
         * Opens the encrypted store: acquires the lock, validates or creates
         * bitcask.meta and decrypts every data file into the keydir.
         *
         * @return the open store
         * @throws IOException on file errors or a wrong password
         */
        public EncryptedBitcask build() throws IOException
        {
            return new EncryptedBitcask(this);
        }
    }

    // ------------------------------------------------------------------ API

    /**
     * Stores a value under a key. Value bytes are copied into the disk entry
     * during the write; the key bytes are copied into the keydir.
     *
     * @param key the key, 1 to 65535 bytes long
     * @param value the value, 0 to maximum bytes; empty values allowed
     * @throws IOException if the entry cannot be written
     */
    public void put(byte[] key, byte[] value) throws IOException
    {
        BitcaskFormat.validateSizes(key, value);

        int tstamp = now();
        byte[] plaintext = plaintext(key, value, tstamp);

        lock.writeLock().lock();
        try
        {
            checkOpen();

            int recordSize = plaintext.length + 32;  // len(4) nonce(12) ct(pt+16)

            if (activeSize > 0 && activeSize + recordSize > maxFileSize)
            {
                rotate();
            }

            writeRecord(plaintext, recordSize);
            keydir.put(key, new KeyDirEntry(activeId, activeSize - recordSize, recordSize, tstamp));
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the decrypted value stored under the given key, or null if the
     * key is absent.
     *
     * @param key the key bytes
     * @return the decrypted value, or null if the key is absent
     * @throws CorruptEntryException if the stored entry fails GCM authentication
     * @throws IOException if the entry cannot be read back
     */
    public byte[] get(byte[] key) throws IOException
    {
        BitcaskFormat.validateKeySize(key);

        lock.readLock().lock();
        try
        {
            checkOpen();

            KeyDirEntry location = keydir.get(key);

            if (location == null)
            {
                return null;
            }

            FileChannel channel = channels.get(location.fileId);
            SecretKey fileKey = fileKeys.get(location.fileId);

            if (channel == null || fileKey == null)
            {
                throw new IOException("no channel or key open for data file " + location.fileId);
            }

            ByteBuffer record = ByteBuffer.allocate(location.entrySize);
            readFully(channel, record, location.entryOffset);

            byte[] plaintext = decrypt(fileKey, record.array(), 4, 12, // nonce at 4, ciphertext at 16
                location.fileId, location.entryOffset, 16, location.entrySize - 16);

            // plaintext: tstamp(4) ksz(2) vsz(4) key value
            int ksz = ByteBuffer.wrap(plaintext, 4, 2).getShort() & 0xFFFF;

            if (10 + ksz > plaintext.length || plaintext.length < 10)
            {
                throw new CorruptEntryException("decrypted entry is malformed at " + location.fileId + "@" + location.entryOffset);
            }

            if (!Arrays.equals(key, Arrays.copyOfRange(plaintext, 10, 10 + ksz)))
            {
                throw new CorruptEntryException("decrypted key does not match the requested key");
            }

            return Arrays.copyOfRange(plaintext, 10 + ksz, plaintext.length);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * Marks a key as deleted with a tombstone entry.
     *
     * @param key the key bytes
     * @return true if a tombstone was written, false if the key was already absent
     * @throws IOException if the tombstone cannot be written
     */
    public boolean delete(byte[] key) throws IOException
    {
        BitcaskFormat.validateKeySize(key);

        int tstamp = now();
        byte[] plaintext = plaintext(key, null, tstamp);
        int recordSize = plaintext.length + 32;

        lock.writeLock().lock();
        try
        {
            checkOpen();

            if (!keydir.containsKey(key))
            {
                return false;
            }

            if (activeSize > 0 && activeSize + recordSize > maxFileSize)
            {
                rotate();
            }

            writeRecord(plaintext, recordSize);
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
     * The blocking merge, adapted to the encrypted profile (appendix A.4):
     * every output file starts with its own header, salt and counter, the
     * entries are decrypted and re-encrypted freshly, no hint files are
     * written, and the rename/delete safety sequence of section 6.7 applies
     * unchanged.
     *
     * @throws IOException on any file error; the store stays usable
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
     * Closes the store, releasing the channels and the directory lock.
     * Idempotent.
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
            catch (IOException ignored)
            {
                // closing proceeds even if this force fails
            }

            closeChannels();

            keydir.clear();
            masterKey = null;

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

    // ------------------------------------------------------------ internals

    private int now()
    {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    private void checkOpen()
    {
        if (closed)
        {
            throw new IllegalStateException("encrypted bitcask store is closed");
        }
    }

    /**
     * Open sequence (appendix A): lock, validate or create the meta file,
     * then decrypt every data file into the keydir and open a new active
     * file. There are no hint files to consult.
     */
    private void openStore(char[] password, int iterations) throws IOException
    {
        Files.createDirectories(directory);

        acquireLock();
        deleteTemporaries();

        char[] work = password.clone();
        try
        {
            masterKey = openOrCreateMeta(work, iterations);
        }
        finally
        {
            Arrays.fill(work, '\0'); // the derived key lives on; the copy dies here
        }

        List<Long> ids = listDataFileIds();
        activeId = ids.isEmpty() ? 0 : ids.get(ids.size() - 1);

        for (int i = 0; i < ids.size(); i++)
        {
            long id = ids.get(i);
            boolean newest = i == ids.size() - 1;

            FileChannel channel = FileChannel.open(dataPath(id),
                StandardOpenOption.READ, StandardOpenOption.WRITE);
            SecretKey fileKey = readFileHeader(channel, id);

            channels.put(id, channel);
            fileKeys.put(id, fileKey);

            recover(channel, fileKey, id, keydir, newest);
        }

        createActiveFile();
    }

    private void acquireLock() throws IOException
    {
        lockChannel = FileChannel.open(directory.resolve(Bitcask.LOCK_FILE),
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
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + Bitcask.TMP_SUFFIX))
        {
            for (Path path : stream)
            {
                Files.deleteIfExists(path);
            }
        }
    }

    /**
     * Validates or creates the meta file: without a meta file but with data
     * files present, the store is in a mixed/unknown state and the open is
     * rejected. A wrong password fails while decrypting the check probe.
     */
    private SecretKey openOrCreateMeta(char[] password, int iterations) throws IOException
    {
        Path meta = directory.resolve(META_FILE);
        SecureRandom random = new SecureRandom();

        if (!Files.exists(meta))
        {
            long dataFiles = countDataFiles();

            if (dataFiles > 0)
            {
                throw new IOException("data files exist without " + META_FILE + ": refusing to open an unknown directory");
            }

            byte[] salt = new byte[16];
            byte[] nonce = new byte[12];
            random.nextBytes(salt);
            random.nextBytes(nonce);

            SecretKey key = pbkdf2(password, salt, iterations);
            byte[] check = encryptRaw(key, nonce, CHECK_PLAINTEXT); // 16 bytes text + 16 tag = 32

            ByteBuffer metaBuf = ByteBuffer.allocate(4 + 1 + 4 + 16 + 12 + check.length);
            metaBuf.put(META_MAGIC);
            metaBuf.put(META_VERSION);
            metaBuf.putInt(iterations);
            metaBuf.put(salt);
            metaBuf.put(nonce);
            metaBuf.put(check);

            Files.write(meta, metaBuf.array());
            syncDirectory();

            return key;
        }

        byte[] metaBytes = Files.readAllBytes(meta);

        // magic(4) version(1) iterations(4) salt(16) nonce(12) check(...); the
        // check is the GCM ciphertext of the 13-byte probe plus a 16-byte tag
        int checkStart = 4 + 1 + 4 + 16 + 12;

        if (metaBytes.length < checkStart + 1 + 16
            || !Arrays.equals(META_MAGIC, Arrays.copyOfRange(metaBytes, 0, 4)))
        {
            throw new IOException(META_FILE + " is not a Bitcask encrypted meta file");
        }

        ByteBuffer buf = ByteBuffer.wrap(metaBytes);
        buf.position(4);
        byte version = buf.get();

        if (version != META_VERSION)
        {
            throw new IOException("unsupported meta version: " + version);
        }

        int stored = buf.getInt(); // iterations used at creation
        byte[] salt = new byte[16];
        byte[] nonce = new byte[12];
        byte[] check = new byte[metaBytes.length - checkStart];
        buf.get(salt);
        buf.get(nonce);
        buf.get(check);

        SecretKey key = pbkdf2(password, salt, stored);
        byte[] expected = decryptRaw(key, nonce, check); // wrong password => bad tag

        if (!Arrays.equals(CHECK_PLAINTEXT, expected))
        {
            throw new IOException("wrong password: the check probe does not match");
        }

        return key;
    }

    private long countDataFiles() throws IOException
    {
        long count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + Bitcask.DATA_SUFFIX))
        {
            for (Path ignored : stream)
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the ids of every data file, sorted ascending.
     */
    private List<Long> listDataFileIds() throws IOException
    {
        List<Long> ids = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + Bitcask.DATA_SUFFIX))
        {
            for (Path path : stream)
            {
                String name = path.getFileName().toString();
                String id = name.substring(0, name.length() - Bitcask.DATA_SUFFIX.length());

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
     * Reads and validates the BCKE header of a data file, returning its
     * derived file key.
     */
    private SecretKey readFileHeader(FileChannel channel, long fileId) throws IOException
    {
        if (channel.size() < DATA_HEADER_SIZE)
        {
            throw new CorruptEntryException("truncated BCKE header in file " + fileId);
        }

        ByteBuffer header = ByteBuffer.allocate(DATA_HEADER_SIZE);
        readFully(channel, header, 0);

        byte[] bytes = header.array();

        if (!Arrays.equals(DATA_MAGIC, Arrays.copyOfRange(bytes, 0, 4)))
        {
            throw new CorruptEntryException("file " + fileId + " lacks the BCKE magic");
        }

        if (bytes[4] != META_VERSION)
        {
            throw new CorruptEntryException("file " + fileId + " has the unsupported version " + bytes[4]);
        }

        long declared = ByteBuffer.wrap(bytes, 5, 8).getLong();

        if (declared != fileId)
        {
            throw new CorruptEntryException("file id mismatch: name says " + fileId + ", header says " + declared);
        }

        byte[] salt = Arrays.copyOfRange(bytes, 13, 29);
        return fileKey(fileId, salt);
    }

    /**
     * Recovers one encrypted data file into the keydir (appendix A.4). A
     * truncated tail or an invalid tag in the last file truncates the file
     * at the failing record; the same failure in any other file aborts.
     */
    private void recover(FileChannel channel, SecretKey fileKey, long fileId, KeyDir keydir, boolean last) throws IOException
    {
        long size = channel.size();
        long offset = DATA_HEADER_SIZE;

        while (offset < size)
        {
            try
            {
                offset = scanRecord(channel, fileKey, fileId, keydir, offset, size);
            }
            catch (CorruptEntryException e)
            {
                // note: the structural part of the records is authenticated
                // by the tag, so only a cut tail or a bad tag land here
                if (!last)
                {
                    throw e;
                }

                channel.truncate(offset);
                return;
            }
        }
    }

    private long scanRecord(FileChannel channel, SecretKey fileKey, long fileId, KeyDir keydir,
        long offset, long size) throws IOException
    {
        long remaining = size - offset;

        if (remaining < 4)
        {
            throw new TruncatedEntryException("truncated record length at offset " + offset);
        }

        ByteBuffer lengthBuf = ByteBuffer.allocate(4);
        readFully(channel, lengthBuf, offset);
        int len = lengthBuf.getInt(0);

        if (len < 27 || len < 0)
        {
            // minimum: tstamp(4) ksz(2) vsz(4) key(1) + tag(16)
            throw new CorruptEntryException("record length " + len + " is impossible at offset " + offset);
        }

        if (remaining < 16L + len)
        {
            throw new TruncatedEntryException("truncated record at offset " + offset + ": declared " + len + " bytes");
        }

        ByteBuffer record = ByteBuffer.allocate(16 + len);
        readFully(channel, record, offset);

        byte[] plaintext = decrypt(fileKey, record.array(), 4, 12, fileId, offset, 16, len);

        applyPlaintext(fileId, keydir, offset, 16 + len, plaintext);

        return offset + 16L + len;
    }

    /**
     * Applies a decrypted entry to the keydir: tstamp(4) ksz(2) vsz(4) key
     * [value]. Structural validation failures mean the bytes authenticated
     * but are not an entry: manipulation, so corruption in any file.
     */
    private void applyPlaintext(long fileId, KeyDir keydir, long offset, long recordSize, byte[] plaintext) throws CorruptEntryException
    {
        if (plaintext.length < 10)
        {
            throw new CorruptEntryException("decrypted record shorter than the entry header at offset " + offset);
        }

        ByteBuffer buf = ByteBuffer.wrap(plaintext);
        int tstamp = buf.getInt();
        int ksz = buf.getShort() & 0xFFFF;
        int vsz = buf.getInt();

        if (ksz == 0)
        {
            throw new CorruptEntryException("decrypted entry has ksz 0 at offset " + offset);
        }

        boolean tombstone = vsz == BitcaskFormat.TOMBSTONE_VSZ;

        if (!tombstone && vsz > BitcaskFormat.MAX_VALUE_SIZE)
        {
            throw new CorruptEntryException("decrypted entry has invalid vsz at offset " + offset);
        }

        int expected = 10 + ksz + (tombstone ? 0 : vsz);

        if (plaintext.length != expected)
        {
            throw new CorruptEntryException("decrypted entry length mismatch at offset " + offset
                + ": expected " + expected + ", got " + plaintext.length);
        }

        byte[] key = Arrays.copyOfRange(plaintext, 10, 10 + ksz);

        if (tombstone)
        {
            keydir.remove(key);
        }
        else
        {
            keydir.put(key, new KeyDirEntry(fileId, offset, (int) recordSize, tstamp));
        }
    }

    /**
     * Forces the active file as immutable and installs a new one.
     */
    private void rotate() throws IOException
    {
        activeChannel.force(true);
        createActiveFile();
    }

    /**
     * Creates the next BCKE file with a fresh salt and a zero counter as the
     * new active file.
     */
    private void createActiveFile() throws IOException
    {
        Path path = dataPath(activeId + 1);

        if (Files.exists(path))
        {
            throw new IOException("unexpected data file with id " + (activeId + 1) + ": " + path);
        }

        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        activeId = activeId + 1;
        activeChannel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);

        ByteBuffer header = ByteBuffer.allocate(DATA_HEADER_SIZE);
        header.put(DATA_MAGIC);
        header.put(META_VERSION);
        header.putLong(activeId);
        header.put(salt);
        header.flip();
        writeFully(activeChannel, header, 0);

        activeKey = fileKey(activeId, salt);
        activeSize = DATA_HEADER_SIZE;
        activeCounter = 0;
        channels.put(activeId, activeChannel);
        fileKeys.put(activeId, activeKey);
        syncDirectory();
    }

    /**
     * Encrypts, appends and records one plaintext entry in the active file.
     * The keydir bookkeeping is left to the caller, which needs the offset.
     */
    private void writeRecord(byte[] plaintext, int recordSize) throws IOException
    {
        long offset = activeSize;
        byte[] record = encryptRecord(activeKey, activeId, activeCounter, offset, recordSize - 16, plaintext);

        ByteBuffer buffer = ByteBuffer.wrap(record);
        writeFully(activeChannel, buffer, offset);
        activeSize += recordSize;
        activeCounter++;

        applySyncPolicy();
    }

    /**
     * Builds the on-disk record: len(4) nonce(12) ciphertext+tag, with the
     * nonce of 4 zero bytes plus the u64 counter and AAD of fileId+offset.
     */
    private byte[] encryptRecord(SecretKey key, long fileId, long counter, long offset, int ctLength, byte[] plaintext)
        throws IOException
    {
        byte[] nonce = nonce(counter);
        byte[] ciphertext;

        try
        {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(fileId, offset));
            ciphertext = cipher.doFinal(plaintext);
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException("encryption failed", e);
        }

        if (ctLength != ciphertext.length)
        {
            throw new IOException("ciphertext length mismatch: expected " + ctLength + ", got " + ciphertext.length);
        }

        ByteBuffer record = ByteBuffer.allocate(16 + ciphertext.length);
        record.putInt(ciphertext.length);
        record.put(nonce);
        record.put(ciphertext);
        return record.array();
    }

    /**
     * Decrypts the ciphertext of one record with the GCM tag as its only
     * integrity check; the associated data (fileId+offset) is verified too.
     */
    private byte[] decrypt(SecretKey key, byte[] record, int noncePos, int nonceLen, long fileId, long offset, int ctPos, int ctLength)
        throws CorruptEntryException
    {
        byte[] nonce = new byte[12];
        System.arraycopy(record, noncePos, nonce, 0, nonceLen);
        byte[] ciphertext = Arrays.copyOfRange(record, ctPos, ctPos + ctLength);

        try
        {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(aad(fileId, offset));
            return cipher.doFinal(ciphertext);
        }
        catch (javax.crypto.AEADBadTagException e)
        {
            throw new CorruptEntryException("GCM tag failed at " + fileId + "@" + offset + ": corrupted or manipulated", e);
        }
        catch (GeneralSecurityException e)
        {
            throw new CorruptEntryException("decryption failed at " + fileId + "@" + offset, e);
        }
    }

    /**
     * One live key queued for the merge, with its location.
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

    /**
     * One merge output under construction.
     */
    private static final class Output
    {
        FileChannel channel;
        SecretKey key;
        long id;
        long size;
        long counter;
    }

    private void doMerge() throws IOException
    {
        activeChannel.force(true);
        activeChannel = null;

        long maxId = activeId;

        List<CopyItem> items = new ArrayList<>();
        keydir.forEach((key, entry) -> items.add(new CopyItem(key, entry)));

        if (items.isEmpty())
        {
            deleteOldFiles(maxId);
            createActiveFile();
            return;
        }

        items.sort((a, b) -> a.entry.compareTo(b.entry));

        FileChannel out = null;
        long outId = maxId;
        long outSize = 0;
        long outCounter = 0;
        SecretKey outKey = null;
        Map<byte[], KeyDirEntry> newPositions = new HashMap<>();

        try
        {
            for (CopyItem item : items)
            {
                int recordSize = item.entry.entrySize;

                if (out == null || outSize + recordSize > maxFileSize)
                {
                    if (out != null)
                    {
                        out.force(true);
                        channels.put(outId, out);
                        fileKeys.put(outId, outKey);
                    }

                    outId++;
                    Output fresh = newOutputFile(outId);
                    out = fresh.channel;
                    outKey = fresh.key;
                    outSize = DATA_HEADER_SIZE;
                    outCounter = 0;
                }

                long entryOffset = outSize;

                // decrypt with the source file key, re-encrypt with the output's
                int sourceRecordSize = item.entry.entrySize;
                FileChannel source = channels.get(item.entry.fileId);
                SecretKey sourceKey = fileKeys.get(item.entry.fileId);

                if (source == null || sourceKey == null)
                {
                    throw new IOException("no channel or key for data file " + item.entry.fileId + " during merge");
                }

                ByteBuffer record = ByteBuffer.allocate(sourceRecordSize);
                readFully(source, record, item.entry.entryOffset);

                byte[] plaintext = decrypt(sourceKey, record.array(), 4, 12,
                    item.entry.fileId, item.entry.entryOffset, 16, sourceRecordSize - 16);

                byte[] encrypted = encryptRecord(outKey, outId, outCounter, entryOffset, sourceRecordSize - 16, plaintext);
                writeFully(out, ByteBuffer.wrap(encrypted), entryOffset);
                outSize += recordSize;
                outCounter++;

                newPositions.put(item.key, new KeyDirEntry(outId, entryOffset, recordSize, item.entry.tstamp));
            }

            out.force(true);
            channels.put(outId, out);
            fileKeys.put(outId, outKey);
            long lastOutputId = outId;

            renameOutputs(maxId, lastOutputId);
            deleteOldFiles(maxId);

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
                out.close();
            }
        }
    }

    /**
     * Opens a fresh encrypted output file with the BCKE header.
     */
    private Output newOutputFile(long id) throws IOException
    {
        Output output = new Output();
        output.id = id;

        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        output.channel = FileChannel.open(tmpDataPath(id), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);

        ByteBuffer header = ByteBuffer.allocate(DATA_HEADER_SIZE);
        header.put(DATA_MAGIC);
        header.put(META_VERSION);
        header.putLong(id);
        header.put(salt);
        header.flip();
        writeFully(output.channel, header, 0);

        output.key = fileKey(id, salt);
        return output;
    }

    /**
     * Renames the outputs in ascending id order.
     */
    private void renameOutputs(long maxId, long lastOutputId) throws IOException
    {
        for (long id = maxId + 1; id <= lastOutputId; id++)
        {
            Files.move(tmpDataPath(id), dataPath(id), StandardCopyOption.ATOMIC_MOVE);
        }

        syncDirectory();
    }

    /**
     * Deletes the old files from oldest to newest.
     */
    private void deleteOldFiles(long maxId) throws IOException
    {
        for (long id = 1; id <= maxId; id++)
        {
            FileChannel channel = channels.remove(id);
            SecretKey ignored = fileKeys.remove(id);

            if (channel != null && channel.isOpen())
            {
                channel.close();
            }

            Files.deleteIfExists(dataPath(id));
        }

        syncDirectory();
    }

    /**
     * Applies the sync policy to the active channel.
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

    private void readFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException
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
        fileKeys.clear();
        activeChannel = null;
        activeKey = null;

        if (failure != null)
        {
            throw failure;
        }
    }

    private Path dataPath(long id)
    {
        return directory.resolve(id + Bitcask.DATA_SUFFIX);
    }

    private Path tmpDataPath(long id)
    {
        return directory.resolve(id + Bitcask.DATA_SUFFIX + Bitcask.TMP_SUFFIX);
    }

    /**
     * The 12-byte nonce: 4 zero bytes plus the u64 counter, big-endian.
     */
    private static byte[] nonce(long counter)
    {
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.putLong(4, counter); // first 4 bytes stay zero
        return buf.array();
    }

    /**
     * The associated data: fileId(8) and entry offset(8), big-endian, so
     * entries cannot be relocated, duplicated or reordered undetected.
     */
    private static byte[] aad(long fileId, long offset)
    {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(fileId);
        buf.putLong(offset);
        return buf.array();
    }

    /**
     * The transparent plaintext of one entry: tstamp(4) ksz(2) vsz(4) key
     * [value]; a null value encodes a tombstone with vsz 0xFFFFFFFF.
     */
    private static byte[] plaintext(byte[] key, byte[] value, int tstamp)
    {
        ByteBuffer buf = ByteBuffer.allocate(10 + key.length + (value == null ? 0 : value.length));
        buf.putInt(tstamp);
        buf.putShort((short) key.length);
        buf.putInt(value == null ? BitcaskFormat.TOMBSTONE_VSZ : value.length);
        buf.put(key);

        if (value != null)
        {
            buf.put(value);
        }

        return buf.array();
    }

    /**
     * PBKDF2WithHmacSHA256 to 256 bits over the password char array.
     */
    private static SecretKey pbkdf2(char[] password, byte[] salt, int iterations) throws IOException
    {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        try
        {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException("PBKDF2 is not available in this JVM", e);
        }
        finally
        {
            spec.clearPassword();
        }
    }

    /**
     * The per-file key: HMAC-SHA256(masterKey, "bitcask-file" || fileId ||
     * fileSalt), used as an AES-256 key.
     */
    private SecretKey fileKey(long fileId, byte[] fileSalt) throws IOException
    {
        try
        {
            ByteBuffer fileIdBuf = ByteBuffer.allocate(8);
            fileIdBuf.putLong(fileId);

            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(masterKey);
            mac.update(FILE_KEY_INFO);
            mac.update(fileIdBuf.array());
            mac.update(fileSalt);
            return new SecretKeySpec(mac.doFinal(), "AES");
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException("HmacSHA256 failed to derive the file key", e);
        }
    }

    /**
     * AES-256-GCM encrypt with a given nonce; used for the meta check probe.
     */
    private static byte[] encryptRaw(SecretKey key, byte[] nonce, byte[] plaintext) throws IOException
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            return cipher.doFinal(plaintext);
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException("AES-GCM is not available in this JVM", e);
        }
    }

    /**
     * AES-256-GCM decrypt; AEADBadTagException surfaces as an IOException
     * (for the meta probe: the wrong password).
     */
    private static byte[] decryptRaw(SecretKey key, byte[] nonce, byte[] ciphertext) throws IOException
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            return cipher.doFinal(ciphertext);
        }
        catch (javax.crypto.AEADBadTagException e)
        {
            throw new IOException("wrong password: the check probe failed authentication", e);
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException("AES-GCM is not available in this JVM", e);
        }
    }

    /**
     * Syncs the directory entry itself after creates, renames or deletes;
     * silently skipped on platforms where a directory cannot be opened.
     */
    private void syncDirectory()
    {
        FileChannel channel = null;
        try
        {
            channel = FileChannel.open(directory, StandardOpenOption.READ);
            channel.force(true);
        }
        catch (IOException ignored)
        {
            // unsupported on this platform
        }
        finally
        {
            if (channel != null)
            {
                try
                {
                    channel.close();
                }
                catch (IOException ignored)
                {
                    // ignored
                }
            }
        }
    }
}
