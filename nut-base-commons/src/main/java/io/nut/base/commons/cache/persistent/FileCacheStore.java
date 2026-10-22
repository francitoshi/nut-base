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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.CRC32;
import javax.crypto.SecretKey;

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
 * When keys and values share the same type, a single serializer can be given:
 * see {@link #FileCacheStore(Path, Serializer)} and {@link #of(Path, Serializer)}.
 * <p>
 * Expiration is stored as wall-clock epoch millis so it survives restarts;
 * expired entries are ignored on read, discarded lazily and reclaimed by
 * compaction.
 * <p>
 * This class is <strong>not thread-safe</strong> and supports a single
 * process per store file.
 *
 * <h2>File format</h2>
 * <p>
 * Two on-disk formats exist. Version 1 ({@code NUTC1}) is plaintext and
 * version 2 ({@code NUTC2}) is encrypted with AES-256-GCM. The version is the
 * last byte of the five-byte header and it is chosen by the presence of the
 * encryption key: a store created with a key writes and reads version 2, a
 * store created without one writes and reads version 1. The store refuses to
 * open a file whose version does not match the key it was given, so a
 * plaintext file can never be silently read as an encrypted one, nor the
 * other way around.
 * <p>
 * In both formats the multi-byte integers are big-endian, as produced by
 * {@link ByteBuffer}, and the trailing {@code crc32} of every record is a
 * CRC-32 (IEEE 802.3) of all the preceding bytes of that same record. The
 * CRC is a cheap framing check that detects truncated or corrupted bytes; it
 * is <strong>not</strong> a security control, the authentication tag of the
 * encrypted format is.
 * <p>
 * <b>Version 1, plaintext</b>
 * <pre>
 * header   : 'N' 'U' 'T' 'C' 1
 * PUT      : [type=1][keyLen:4][key][expiresAt:8][valueLen:4][value][crc32:4]
 * PUT_NULL : [type=2][keyLen:4][key][expiresAt:8][crc32:4]
 * REMOVE   : [type=3][keyLen:4][key][crc32:4]
 * </pre>
 * {@code keyLen} is {@code -1} for a {@code null} key, {@code valueLen} is
 * {@code -1} for a {@code null} value. The expiration is a wall-clock
 * timestamp in epoch milliseconds, absent from the {@code REMOVE} records.
 * <p>
 * <b>Version 2, encrypted</b>
 * <pre>
 * header   : 'N' 'U' 'T' 'C' 2
 * record   : [type:1][cipherLen:4][ciphertext:cipherLen][crc32:4]
 * ciphertext = [iv:12] + AES-256-GCM(key, iv, aad, plaintext)
 * aad      : [type:1][version:1]
 * plaintext: [keyLen:4][key][expiresAt:8][valueLen:4][value]   (PUT)
 *            [keyLen:4][key][expiresAt:8]                       (PUT_NULL)
 *            [keyLen:4][key]                                    (REMOVE)
 * </pre>
 * The plaintext is exactly the body of the equivalent version 1 record, so
 * keys, values and expirations are all encrypted and authenticated. Only the
 * record type and the length of the ciphertext stay in clear. Every record
 * uses a fresh random 96-bit initialization vector, which travels with the
 * record when the journal is compacted, so no initialization vector is ever
 * reused. The associated data binds every ciphertext to its record type, so
 * a record cannot be replayed as a record of another type.
 * <p>
 * A record of the encrypted format that fails the authentication check (a
 * wrong key or tampered bytes) is a <strong>hard error</strong>: the store
 * refuses to open instead of silently discarding the journal. Only a
 * truncated tail or corrupted bytes (CRC mismatch) are discarded, as they
 * are the normal consequence of a crash.
 * <p>
 * The store file is created with {@code 0600} permissions on POSIX
 * filesystems, so it is readable only by its owner.
 *
 * @param <K> the type of keys maintained by the store
 * @param <V> the type of mapped values
 */
public class FileCacheStore<K,V> implements CacheStore<K,V>
{
    private static final byte[] HEADER_V1 = new byte[]{'N', 'U', 'T', 'C', 1};
    private static final byte[] HEADER_V2 = new byte[]{'N', 'U', 'T', 'C', 2};
    private static final int HEADER_SIZE = 5;
    private static final int VERSION_V1 = 1;
    private static final int VERSION_V2 = 2;

    private static final byte TYPE_PUT = 1;
    private static final byte TYPE_PUT_NULL = 2;
    private static final byte TYPE_REMOVE = 3;

    private static final int MIN_COMPACT_BYTES = 16 * 1024;
    private static final int COPY_BUFFER_SIZE = 8 * 1024;

    private final Path file;
    private final Serializer<K> keySerializer;
    private final Serializer<V> valueSerializer;
    private final Map<K,Entry> index = new HashMap<>();

    private final SecretKey key;
    private final AeadRecordCipher cipher;

    private RandomAccessFile raf;
    private long liveBytes;
    private long totalBytes;
    private boolean closed;

    public FileCacheStore(Path file, Serializer<K> keySerializer, Serializer<V> valueSerializer)
    {
        this(file, keySerializer, valueSerializer, null);
    }

    /**
     * Creates a new store backed by the given file, encrypting its content
     * with AES-256-GCM.
     * <p>
     * The file is written in the encrypted format (version 2): the keys, the
     * values and the expirations are encrypted and authenticated, and only
     * the record types and the ciphertext lengths remain in clear. The key is
     * never written to the file; the caller keeps its ownership of it.
     *
     * @param file the store file
     * @param keySerializer the serializer for keys
     * @param valueSerializer the serializer for values
     * @param key the AES key used to encrypt the records; {@code null}
     *            creates a plaintext store (version 1)
     */
    public FileCacheStore(Path file, Serializer<K> keySerializer, Serializer<V> valueSerializer, SecretKey key)
    {
        this.file = Objects.requireNonNull(file, "file");
        this.keySerializer = Objects.requireNonNull(keySerializer, "keySerializer");
        this.valueSerializer = Objects.requireNonNull(valueSerializer, "valueSerializer");
        this.key = key;
        this.cipher = key == null ? null : new AeadRecordCipher(key);
        open();
    }

    /**
     * Creates a new store backed by the given file, using the same serializer
     * for keys and values.
     * <p>
     * This constructor is meant for stores where keys and values share the
     * same type, that is {@code FileCacheStore<T, T>}; the given serializer is
     * used for keys and values alike.
     *
     * @param file the store file
     * @param serializer the serializer used for both keys and values
     * @param <T> the common type of keys and values
     */
    @SuppressWarnings("unchecked")
    public <T> FileCacheStore(Path file, Serializer<T> serializer)
    {
        this(file, serializer, (SecretKey) null);
    }

    /**
     * Creates a new store backed by the given file, using the same serializer
     * for keys and values and encrypting its content with AES-256-GCM.
     * <p>
     * See {@link #FileCacheStore(Path, Serializer, Serializer, SecretKey)} for
     * the details of the encrypted format.
     *
     * @param file the store file
     * @param serializer the serializer used for both keys and values
     * @param key the AES key used to encrypt the records; {@code null}
     *            creates a plaintext store (version 1)
     * @param <T> the common type of keys and values
     */
    @SuppressWarnings("unchecked")
    public <T> FileCacheStore(Path file, Serializer<T> serializer, SecretKey key)
    {
        this(file, (Serializer<K>) serializer, (Serializer<V>) serializer, key);
    }

    /**
     * Creates a new store backed by the given file, using the same serializer
     * for keys and values.
     * <p>
     * Unlike the constructor, this factory enforces at compile time that keys
     * and values share the same type, that is {@code FileCacheStore<T, T>}.
     *
     * @param file the store file
     * @param serializer the serializer used for both keys and values
     * @param <T> the common type of keys and values
     * @return a new store
     */
    public static <T> FileCacheStore<T, T> of(Path file, Serializer<T> serializer)
    {
        return new FileCacheStore<T, T>(file, serializer);
    }

    /**
     * Creates a new encrypted store backed by the given file, using the same
     * serializer for keys and values.
     * <p>
     * See {@link #FileCacheStore(Path, Serializer, Serializer, SecretKey)} for
     * the details of the encrypted format.
     *
     * @param file the store file
     * @param serializer the serializer used for both keys and values
     * @param key the AES key used to encrypt the records
     * @param <T> the common type of keys and values
     * @return a new store
     */
    public static <T> FileCacheStore<T, T> of(Path file, Serializer<T> serializer, SecretKey key)
    {
        return new FileCacheStore<T, T>(file, serializer, key);
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
        if (cipher != null)
        {
            return getEncrypted(e);
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
        byte[] body = buildBody(type, keyBytes, expiresAtEpochMillis, valueBytes);
        byte[] record;
        int cipherLen = -1;
        if (cipher != null)
        {
            byte[] blob = cipher.encrypt(aad(type), body);
            cipherLen = blob.length;
            record = encryptedRecord(type, blob);
        }
        else
        {
            record = plainRecord(type, body);
        }
        int recordLen = record.length;
        int valueLen = valueBytes == null ? -1 : valueBytes.length;

        try
        {
            long recordOffset = raf.length();
            raf.seek(recordOffset);
            raf.write(record);
            long valueOffset = -1;
            if (cipher == null && type == TYPE_PUT)
            {
                valueOffset = recordOffset + 1 + (body.length - valueLen);
            }
            Entry old = index.put(key, new Entry(type, recordOffset, recordLen, valueOffset, cipher == null ? valueLen : -1, cipherLen, expiresAtEpochMillis));
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
            raf.write(header());
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
        Iterator<Map.Entry<K,Entry>> it = index.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<K,Entry> me = it.next();
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

    /**
     * Returns the key used to encrypt the records, or {@code null} when the
     * store is not encrypted.
     *
     * @return the encryption key
     */
    public SecretKey getKey()
    {
        return key;
    }

    private void open()
    {
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
            restrictPermissions();
            validateHeader();
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

    private void validateHeader() throws IOException
    {
        long length = raf.length();
        if (length < HEADER_SIZE)
        {
            if (length == 0)
            {
                raf.write(header());
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
            if (header[i] != HEADER_V1[i])
            {
                throw new IOException("not a nut cache store file (bad magic): " + file);
            }
        }
        int version = header[HEADER_SIZE - 1] & 0xFF;
        int expected = cipher == null ? VERSION_V1 : VERSION_V2;
        if (version != expected)
        {
            if (version == VERSION_V1)
            {
                throw new IOException("cache store file is not encrypted but a key was provided: " + file);
            }
            if (version == VERSION_V2)
            {
                throw new IOException("cache store file is encrypted but no key was provided: " + file);
            }
            throw new IOException("unsupported cache store file version " + version + ": " + file);
        }
    }

    private void restrictPermissions()
    {
        try
        {
            Set<PosixFilePermission> permissions = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(file, permissions);
        }
        catch (IOException | UnsupportedOperationException e)
        {
            // best effort: not every filesystem supports POSIX permissions
        }
    }

    private byte[] header()
    {
        return cipher == null ? HEADER_V1 : HEADER_V2;
    }

    private void appendRemove(K key) throws IOException
    {
        byte[] keyBytes = key == null ? null : keySerializer.serialize(key);
        byte[] body = buildBody(TYPE_REMOVE, keyBytes, 0, null);
        byte[] record;
        if (cipher != null)
        {
            byte[] blob = cipher.encrypt(aad(TYPE_REMOVE), body);
            record = encryptedRecord(TYPE_REMOVE, blob);
        }
        else
        {
            record = plainRecord(TYPE_REMOVE, body);
        }
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
            out.write(header());
            newTotal = 0;
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            Iterator<Map.Entry<K,Entry>> it = index.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry<K,Entry> me = it.next();
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
        index.clear();
        liveBytes = 0;
        if (cipher == null)
        {
            replayV1();
        }
        else
        {
            replayV2();
        }
    }

    /**
     * Replays a plaintext journal (version 1).
     */
    private void replayV1() throws IOException
    {
        long length = raf.length();
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
                        Entry old = index.put(key, new Entry((byte) type, recordStart, (int) (recordEnd - recordStart),
                                valueOffset, valueLen, -1, expiresAt));
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

    /**
     * Replays an encrypted journal (version 2).
     * <p>
     * A truncated or corrupted tail is discarded, as in the plaintext
     * format, but a record whose authentication tag does not match is a hard
     * error: the key is wrong or the file has been tampered with, and the
     * journal must not be silently discarded.
     */
    private void replayV2() throws IOException
    {
        long length = raf.length();
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
                int cipherLen = readInt(raf, crc);
                long available = length - pos - 9;
                if (cipherLen < AeadRecordCipher.MIN_BLOB_BYTES || cipherLen > available)
                {
                    break;
                }
                byte[] blob = new byte[cipherLen];
                raf.readFully(blob);
                crc.update(blob, 0, cipherLen);
                int storedCrc = readInt(raf, null);
                if (storedCrc != (int) crc.getValue())
                {
                    break;
                }
                byte[] body;
                try
                {
                    body = cipher.decrypt(aad((byte) type), blob);
                }
                catch (GeneralSecurityException e)
                {
                    throw new IOException("cache store file authentication failed (wrong key or tampered data): " + file, e);
                }
                long recordEnd = raf.getFilePointer();
                Body decoded;
                try
                {
                    decoded = parseBody((byte) type, body);
                }
                catch (BufferUnderflowException | IllegalArgumentException e)
                {
                    break;
                }
                K key = decoded.key == null ? null : keySerializer.deserialize(decoded.key);
                switch (type)
                {
                    case TYPE_PUT:
                    case TYPE_PUT_NULL:
                    {
                        Entry old = index.put(key, new Entry((byte) type, recordStart, (int) (recordEnd - recordStart),
                                -1, -1, cipherLen, decoded.expiresAt));
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

    private V getEncrypted(Entry e)
    {
        byte[] blob = new byte[e.cipherLen];
        try
        {
            raf.seek(e.recordOffset + 5);
            raf.readFully(blob);
        }
        catch (IOException e1)
        {
            throw new UncheckedIOException("cannot read value at offset " + e.recordOffset + " from " + file, e1);
        }
        byte[] body;
        try
        {
            body = cipher.decrypt(aad(e.type), blob);
        }
        catch (GeneralSecurityException e1)
        {
            throw new UncheckedIOException("cannot authenticate the record at offset " + e.recordOffset
                    + " of " + file, new IOException(e1));
        }
        Body decoded;
        try
        {
            decoded = parseBody(e.type, body);
        }
        catch (BufferUnderflowException | IllegalArgumentException e1)
        {
            throw new IllegalStateException("malformed record at offset " + e.recordOffset + " of " + file, e1);
        }
        if (decoded.value == null)
        {
            return null;
        }
        return valueSerializer.deserialize(decoded.value);
    }

    /**
     * Builds the body of a record, that is, everything but the record type
     * and the trailing CRC. The same body is written in clear in the
     * plaintext format and encrypted in the encrypted one.
     */
    private static byte[] buildBody(byte type, byte[] keyBytes, long expiresAt, byte[] valueBytes)
    {
        int keyLen = keyBytes == null ? -1 : keyBytes.length;
        int valueLen = valueBytes == null ? -1 : valueBytes.length;
        int length = 4 + Math.max(keyLen, 0)
                + (type == TYPE_REMOVE ? 0 : 8)
                + (type == TYPE_PUT ? 4 + valueLen : 0);
        ByteBuffer bb = ByteBuffer.allocate(length);
        bb.putInt(keyLen);
        if (keyBytes != null)
        {
            bb.put(keyBytes);
        }
        if (type != TYPE_REMOVE)
        {
            bb.putLong(expiresAt);
        }
        if (type == TYPE_PUT)
        {
            bb.putInt(valueLen);
            bb.put(valueBytes);
        }
        return bb.array();
    }

    /**
     * Builds a plaintext record (version 1).
     */
    private static byte[] plainRecord(byte type, byte[] body)
    {
        byte[] record = new byte[1 + body.length + 4];
        ByteBuffer bb = ByteBuffer.wrap(record);
        bb.put(type);
        bb.put(body);
        CRC32 crc = new CRC32();
        crc.update(record, 0, bb.position());
        bb.putInt((int) crc.getValue());
        return record;
    }

    /**
     * Builds an encrypted record (version 2).
     */
    private static byte[] encryptedRecord(byte type, byte[] blob)
    {
        byte[] record = new byte[1 + 4 + blob.length + 4];
        ByteBuffer bb = ByteBuffer.wrap(record);
        bb.put(type);
        bb.putInt(blob.length);
        bb.put(blob);
        CRC32 crc = new CRC32();
        crc.update(record, 0, bb.position());
        bb.putInt((int) crc.getValue());
        return record;
    }

    /**
     * Parses the body of an encrypted record.
     */
    private static Body parseBody(byte type, byte[] body)
    {
        ByteBuffer bb = ByteBuffer.wrap(body);
        int keyLen = bb.getInt();
        byte[] keyBytes = null;
        if (keyLen >= 0)
        {
            keyBytes = new byte[keyLen];
            bb.get(keyBytes);
        }
        Body decoded = new Body();
        decoded.key = keyBytes;
        if (type == TYPE_PUT || type == TYPE_PUT_NULL)
        {
            decoded.expiresAt = bb.getLong();
        }
        if (type == TYPE_PUT)
        {
            int valueLen = bb.getInt();
            if (valueLen < 0 || valueLen > bb.remaining())
            {
                throw new IllegalArgumentException("invalid value length: " + valueLen);
            }
            decoded.value = new byte[valueLen];
            bb.get(decoded.value);
        }
        return decoded;
    }

    /**
     * The associated data of a record: it binds the ciphertext to its record
     * type and to the format version.
     */
    private static byte[] aad(byte type)
    {
        return new byte[]{type, (byte) VERSION_V2};
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

    /**
     * The decoded body of an encrypted record.
     */
    private static final class Body
    {
        byte[] key;
        long expiresAt;
        byte[] value;
    }

    private static final class Entry
    {
        final byte type;
        long recordOffset;
        long valueOffset;
        final int recordLength;
        final int valueLen;
        final int cipherLen;
        final long expiresAtMillis;

        Entry(byte type, long recordOffset, int recordLength, long valueOffset, int valueLen, int cipherLen,
                long expiresAtMillis)
        {
            this.type = type;
            this.recordOffset = recordOffset;
            this.recordLength = recordLength;
            this.valueOffset = valueOffset;
            this.valueLen = valueLen;
            this.cipherLen = cipherLen;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
