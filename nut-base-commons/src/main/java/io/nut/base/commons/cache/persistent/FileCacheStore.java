/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.cache.persistent;

import io.nut.base.commons.serializer.Serializer;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.zip.CRC32;

/**
 * A {@link CacheStore} backed by a single binary file.
 * <p>
 * The file is an append-only journal: every mutation appends a CRC-protected
 * record ({@code PUT}, {@code PUT_NULL} or {@code REMOVE}). On open, the
 * journal is replayed to rebuild an in-memory key index; a truncated or
 * corrupt tail (e.g. after a crash) is detected via CRC and discarded.
 * <p>
 * Space is reclaimed by compaction: live records are copied to a temporary
 * file which then atomically replaces the original. Compaction runs
 * automatically when more than half of the journal is dead and it exceeds a
 * minimum size, and on {@link #close()} when any dead bytes remain.
 * <p>
 * Keys and values are encoded with the {@link Serializer}s given at
 * construction time. Serializers are never invoked with {@code null}:
 * {@code null} keys and values are encoded with dedicated sentinel lengths.
 * <p>
 * Expiration is stored as wall-clock epoch millis so it survives restarts;
 * expired entries are ignored on read, discarded lazily and reclaimed by
 * compaction.
 * <p>
 * This class is <strong>not thread-safe</strong> and supports a single
 * process per store file.
 *
 * @param <K> the type of keys maintained by the store
 * @param <V> the type of mapped values
 */
public class FileCacheStore<K, V> implements CacheStore<K, V>
{
    private static final byte[] HEADER = new byte[]{'N', 'U', 'T', 'C', 1};
    private static final int HEADER_SIZE = HEADER.length;
    private static final int VERSION = HEADER[HEADER_SIZE - 1];

    private static final byte TYPE_PUT = 1;
    private static final byte TYPE_PUT_NULL = 2;
    private static final byte TYPE_REMOVE = 3;

    private static final int MIN_COMPACT_BYTES = 16 * 1024;
    private static final int COPY_BUFFER_SIZE = 8 * 1024;

    private final Path file;
    private final Serializer<K> keySerializer;
    private final Serializer<V> valueSerializer;
    private final Map<K, Entry> index = new HashMap<>();

    private RandomAccessFile raf;
    private long liveBytes;
    private long totalBytes;
    private boolean closed;

    public FileCacheStore(Path file, Serializer<K> keySerializer, Serializer<V> valueSerializer)
    {
        this.file = Objects.requireNonNull(file, "file");
        this.keySerializer = Objects.requireNonNull(keySerializer, "keySerializer");
        this.valueSerializer = Objects.requireNonNull(valueSerializer, "valueSerializer");
        RandomAccessFile opened = null;
        try
        {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null)
            {
                Files.createDirectories(parent);
            }
            opened = new RandomAccessFile(file.toFile(), "rw");
            this.raf = opened;
            replay();
        }
        catch (IOException e)
        {
            if (opened != null)
            {
                try
                {
                    opened.close();
                }
                catch (IOException ignored)
                {
                    // best effort on the error path
                }
            }
            throw new UncheckedIOException("cannot open cache store file " + file, e);
        }
    }

    @Override
    public V get(K key)
    {
        checkOpen();
        Entry e = index.get(key);
        if (e == null)
        {
            return null;
        }
        if (isExpired(e.expiresAtMillis, System.currentTimeMillis()))
        {
            index.remove(key);
            return null;
        }
        if (e.valueLen < 0)
        {
            return null;
        }
        byte[] data = new byte[e.valueLen];
        try
        {
            raf.seek(e.valueOffset);
            raf.readFully(data);
        }
        catch (IOException e1)
        {
            throw new UncheckedIOException("cannot read value at offset " + e.valueOffset + " from " + file, e1);
        }
        return valueSerializer.deserialize(data);
    }

    @Override
    public void put(K key, V value, long expiresAtEpochMillis)
    {
        checkOpen();
        byte[] keyBytes = key == null ? null : keySerializer.serialize(key);
        byte[] valueBytes = value == null ? null : valueSerializer.serialize(value);
        byte type = valueBytes == null ? TYPE_PUT_NULL : TYPE_PUT;
        int keyLen = keyBytes == null ? -1 : keyBytes.length;
        int valueLen = valueBytes == null ? -1 : valueBytes.length;

        int recordLen = 1 + 4 + Math.max(keyLen, 0) + 8 + (type == TYPE_PUT ? 4 + valueLen : 0) + 4;
        byte[] record = new byte[recordLen];
        ByteBuffer bb = ByteBuffer.wrap(record);
        bb.put(type);
        bb.putInt(keyLen);
        if (keyBytes != null)
        {
            bb.put(keyBytes);
        }
        bb.putLong(expiresAtEpochMillis);
        int valueOffsetInRecord = -1;
        if (type == TYPE_PUT)
        {
            valueOffsetInRecord = bb.position() + 4;
            bb.putInt(valueLen);
            bb.put(valueBytes);
        }
        CRC32 crc = new CRC32();
        crc.update(record, 0, bb.position());
        bb.putInt((int) crc.getValue());

        try
        {
            long recordOffset = raf.length();
            raf.seek(recordOffset);
            raf.write(record);
            Entry old = index.put(key, new Entry(recordOffset, recordLen,
                    valueOffsetInRecord < 0 ? -1 : recordOffset + valueOffsetInRecord,
                    valueLen, expiresAtEpochMillis));
            if (old != null)
            {
                liveBytes -= old.recordLength;
            }
            liveBytes += recordLen;
            totalBytes = raf.length() - HEADER_SIZE;
            maybeCompact();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot append record to " + file, e);
        }
    }

    @Override
    public boolean containsKey(K key)
    {
        checkOpen();
        Entry e = index.get(key);
        return e != null && !isExpired(e.expiresAtMillis, System.currentTimeMillis());
    }

    @Override
    public void clear()
    {
        checkOpen();
        index.clear();
        liveBytes = 0;
        totalBytes = 0;
        try
        {
            raf.setLength(0);
            raf.seek(0);
            raf.write(HEADER);
            raf.getFD().sync();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot truncate " + file, e);
        }
    }

    @Override
    public long size()
    {
        checkOpen();
        long now = System.currentTimeMillis();
        long count = 0;
        for (Entry e : index.values())
        {
            if (!isExpired(e.expiresAtMillis, now))
            {
                count++;
            }
        }
        return count;
    }

    @Override
    public void purgeExpired()
    {
        checkOpen();
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<K, Entry>> it = index.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<K, Entry> me = it.next();
            if (isExpired(me.getValue().expiresAtMillis, now))
            {
                try
                {
                    appendRemove(me.getKey());
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException("cannot append remove record to " + file, e);
                }
                liveBytes -= me.getValue().recordLength;
                it.remove();
            }
        }
        maybeCompact();
    }

    @Override
    public void close()
    {
        if (closed)
        {
            return;
        }
        try
        {
            if (totalBytes > liveBytes)
            {
                compact();
            }
            else
            {
                raf.getFD().sync();
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot flush and close " + file, e);
        }
        finally
        {
            try
            {
                raf.close();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("cannot close " + file, e);
            }
            closed = true;
        }
    }

    /**
     * Returns the file backing this store.
     *
     * @return the store file
     */
    public Path getFile()
    {
        return file;
    }

    private void appendRemove(K key) throws IOException
    {
        byte[] keyBytes = key == null ? null : keySerializer.serialize(key);
        int keyLen = keyBytes == null ? -1 : keyBytes.length;
        int recordLen = 1 + 4 + Math.max(keyLen, 0) + 4;
        byte[] record = new byte[recordLen];
        ByteBuffer bb = ByteBuffer.wrap(record);
        bb.put(TYPE_REMOVE);
        bb.putInt(keyLen);
        if (keyBytes != null)
        {
            bb.put(keyBytes);
        }
        CRC32 crc = new CRC32();
        crc.update(record, 0, bb.position());
        bb.putInt((int) crc.getValue());
        long recordOffset = raf.length();
        raf.seek(recordOffset);
        raf.write(record);
        totalBytes = raf.length() - HEADER_SIZE;
    }

    private void maybeCompact()
    {
        if (totalBytes > MIN_COMPACT_BYTES && (totalBytes - liveBytes) * 2 > totalBytes)
        {
            try
            {
                compact();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("cannot compact " + file, e);
            }
        }
    }

    private void compact() throws IOException
    {
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        long newTotal;
        try (RandomAccessFile out = new RandomAccessFile(tmp.toFile(), "rw"))
        {
            out.setLength(0);
            out.write(HEADER);
            newTotal = 0;
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            Iterator<Map.Entry<K, Entry>> it = index.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry<K, Entry> me = it.next();
                Entry e = me.getValue();
                if (isExpired(e.expiresAtMillis, System.currentTimeMillis()))
                {
                    it.remove();
                    continue;
                }
                long newRecordOffset = out.getFilePointer();
                int remaining = e.recordLength;
                raf.seek(e.recordOffset);
                while (remaining > 0)
                {
                    int n = raf.read(buffer, 0, Math.min(remaining, buffer.length));
                    if (n < 0)
                    {
                        throw new EOFException("unexpected end of journal in " + file);
                    }
                    out.write(buffer, 0, n);
                    remaining -= n;
                }
                long shift = newRecordOffset - e.recordOffset;
                e.recordOffset = newRecordOffset;
                e.valueOffset = e.valueOffset < 0 ? -1 : e.valueOffset + shift;
                newTotal += e.recordLength;
            }
            out.getFD().sync();
        }
        raf.close();
        try
        {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        raf = new RandomAccessFile(file.toFile(), "rw");
        liveBytes = newTotal;
        totalBytes = newTotal;
    }

    private void replay() throws IOException
    {
        long length = raf.length();
        if (length < HEADER_SIZE)
        {
            if (length == 0)
            {
                raf.write(HEADER);
                raf.getFD().sync();
                return;
            }
            throw new IOException("not a nut cache store file (truncated header): " + file);
        }
        byte[] header = new byte[HEADER_SIZE];
        raf.seek(0);
        raf.readFully(header);
        for (int i = 0; i < HEADER_SIZE - 1; i++)
        {
            if (header[i] != HEADER[i])
            {
                throw new IOException("not a nut cache store file (bad magic): " + file);
            }
        }
        if ((header[HEADER_SIZE - 1] & 0xFF) != VERSION)
        {
            throw new IOException("unsupported cache store file version "
                    + (header[HEADER_SIZE - 1] & 0xFF) + ": " + file);
        }

        index.clear();
        liveBytes = 0;
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        long pos = HEADER_SIZE;
        while (pos < length)
        {
            long recordStart = pos;
            raf.seek(pos);
            try
            {
                int type = raf.read();
                if (type < 0)
                {
                    break;
                }
                CRC32 crc = new CRC32();
                crc.update(type);
                int keyLen = readInt(raf, crc);
                byte[] keyBytes = null;
                if (keyLen >= 0)
                {
                    keyBytes = new byte[keyLen];
                    raf.readFully(keyBytes);
                    crc.update(keyBytes, 0, keyLen);
                }
                long expiresAt = 0;
                if (type == TYPE_PUT || type == TYPE_PUT_NULL)
                {
                    expiresAt = readLong(raf, crc);
                }
                int valueLen = -1;
                long valueOffset = -1;
                if (type == TYPE_PUT)
                {
                    valueLen = readInt(raf, crc);
                    valueOffset = raf.getFilePointer();
                    int remaining = valueLen;
                    while (remaining > 0)
                    {
                        int n = raf.read(buffer, 0, Math.min(remaining, buffer.length));
                        if (n < 0)
                        {
                            throw new EOFException();
                        }
                        crc.update(buffer, 0, n);
                        remaining -= n;
                    }
                }
                int storedCrc = readInt(raf, null);
                if (storedCrc != (int) crc.getValue())
                {
                    break;
                }
                long recordEnd = raf.getFilePointer();
                K key = keyLen < 0 ? null : keySerializer.deserialize(keyBytes);
                switch (type)
                {
                    case TYPE_PUT:
                    case TYPE_PUT_NULL:
                    {
                        Entry old = index.put(key, new Entry(recordStart, (int) (recordEnd - recordStart),
                                valueOffset, valueLen, expiresAt));
                        if (old != null)
                        {
                            liveBytes -= old.recordLength;
                        }
                        liveBytes += recordEnd - recordStart;
                        break;
                    }
                    case TYPE_REMOVE:
                    {
                        Entry old = index.remove(key);
                        if (old != null)
                        {
                            liveBytes -= old.recordLength;
                        }
                        break;
                    }
                    default:
                        break;
                }
                pos = recordEnd;
            }
            catch (EOFException eof)
            {
                break;
            }
        }
        if (pos < length)
        {
            raf.setLength(pos);
        }
        totalBytes = pos - HEADER_SIZE;
    }

    private static int readInt(RandomAccessFile raf, CRC32 crc) throws IOException
    {
        byte[] b = new byte[4];
        raf.readFully(b);
        if (crc != null)
        {
            crc.update(b, 0, 4);
        }
        return ByteBuffer.wrap(b).getInt();
    }

    private static long readLong(RandomAccessFile raf, CRC32 crc) throws IOException
    {
        byte[] b = new byte[8];
        raf.readFully(b);
        if (crc != null)
        {
            crc.update(b, 0, 8);
        }
        return ByteBuffer.wrap(b).getLong();
    }

    private static boolean isExpired(long expiresAtEpochMillis, long now)
    {
        return expiresAtEpochMillis != Long.MAX_VALUE && now - expiresAtEpochMillis >= 0;
    }

    private void checkOpen()
    {
        if (closed)
        {
            throw new IllegalStateException("cache store is closed: " + file);
        }
    }

    private static final class Entry
    {
        long recordOffset;
        long valueOffset;
        final int recordLength;
        final int valueLen;
        final long expiresAtMillis;

        Entry(long recordOffset, int recordLength, long valueOffset, int valueLen, long expiresAtMillis)
        {
            this.recordOffset = recordOffset;
            this.recordLength = recordLength;
            this.valueOffset = valueOffset;
            this.valueLen = valueLen;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
