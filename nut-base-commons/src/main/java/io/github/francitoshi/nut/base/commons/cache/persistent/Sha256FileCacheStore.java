/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.cache.persistent;

import io.github.francitoshi.nut.base.commons.encoding.Hex;
import io.github.francitoshi.nut.base.commons.io.IO;
import io.github.francitoshi.nut.base.commons.jca.Kr;
import io.github.francitoshi.nut.base.commons.serializer.Serializer;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.CRC32;
import javax.crypto.SecretKey;

/**
 * A {@link CacheStore} backed by a directory with <strong>one file per
 * entry</strong>, named after the SHA-256 digest of the key.
 * <p>
 * The name of a file is the SHA-256 of the serialized key, encoded in lower
 * case hexadecimal: 64 characters, always a valid file name, unguessable in
 * content and private in length for any key type. This makes the digest of
 * the key <em>the</em> index of the store: there is no in-memory key index,
 * no journal and no compaction. A {@code get} is resolved by opening the file
 * named after the digest of the key, which makes this store the natural fit
 * for large values (images by URL, documents, reports) and for very large
 * numbers of entries.
 * <p>
 * Every file is a single record. The expiration lives at a fixed position
 * right after the five-byte header, in the clear on every format, so deciding
 * whether an entry has expired only costs reading the first 13 bytes; the
 * value itself is never read for an expiration check.
 *
 * <h2>File format</h2>
 * <p>
 * Two on-disk formats exist. Version 1 ({@code NUTE1}) is plaintext and
 * version 2 ({@code NUTE2}) encrypts and authenticates the value with
 * AES-256-GCM. The version is the last byte of the five-byte header and it is
 * chosen by the presence of the encryption key: a store created with a key
 * writes and reads version 2, a store created without one writes and reads
 * version 1. An entry whose version does not match the key it was given is a
 * <strong>hard error</strong>, so a plaintext file can never be silently read
 * as an encrypted one, nor the other way around.
 * <p>
 * In both formats the multi-byte integers are big-endian, as produced by
 * {@link ByteBuffer}, and the trailing {@code crc32} is a CRC-32 (IEEE 802.3)
 * of all the preceding bytes of the record. The CRC is a cheap framing check
 * that detects corrupted bytes; it is <strong>not</strong> a security
 * control, the authentication tag of the encrypted format is.
 * <p>
 * <b>Version 1, plaintext</b>
 * <pre>
 * header   : 'N' 'U' 'T' 'E' 1
 * record   : [expiresAt:8][valueLen:4][value][crc32:4]
 * </pre>
 * <b>Version 2, encrypted</b>
 * <pre>
 * header   : 'N' 'U' 'T' 'E' 2
 * record   : [expiresAt:8][cipherLen:4][iv:12 + AES-256-GCM(value)][crc32:4]
 * aad      : [type:1][version:1]
 * </pre>
 * The expiration and the length fields stay in the clear on both formats, so
 * an expiration check never reaches the encrypted bytes; what is protected is
 * the content of the value, which is authenticated and can only be read with
 * the key. {@code valueLen}/{@code cipherLen} is {@code -1} for a {@code null}
 * value, in which case no value bytes are stored at all.
 * <p>
 * A record that is corrupted or truncated (a CRC mismatch, a bad magic or an
 * unexpectedly short file) is treated as a cache miss: the file is removed
 * and {@code null} is returned. A record whose authentication tag does not
 * match (a wrong key or tampered bytes) is a <strong>hard error</strong>: the
 * entry is kept and the store refuses to read it, so a wrong key is never
 * silently mistaken for an empty cache.
 * <p>
 * Entries are written to a temporary file that then atomically replaces the
 * final name, so a crash can never leave a truncated file under an entry
 * name; leftover temporary files are removed the next time the directory is
 * opened. Expired entries are discarded lazily on {@code get} and
 * {@code containsKey}, and reclaimed in bulk by {@link #purgeExpired()}, which
 * {@link #size()} and {@link #clear()} are directory scans: with no key index
 * they must walk the entry files, reading only their first 13 bytes each.
 * <p>
 * Keys and values are encoded with the {@link Serializer}s given at
 * construction time. Keys must not be {@code null}; {@code null} values are
 * allowed and stored as entries of negative length. When keys and values
 * share the same type, a single serializer can be given: see
 * {@link #Sha256FileCacheStore(Path, Serializer)} and {@link #of(Path, Serializer)}.
 * <p>
 * Expiration is stored as wall-clock epoch millis so it survives restarts;
 * {@link Long#MAX_VALUE} as {@code expiresAtEpochMillis} means "never
 * expires".
 * <p>
 * The directory and its files are created with owner-only permissions
 * ({@code 0700} and {@code 0600}) on POSIX filesystems.
 * <p>
 * This class is <strong>not thread-safe</strong> and supports a single
 * process per store directory.
 *
 * @param <K> the type of keys maintained by the store
 * @param <V> the type of mapped values
 */
public class Sha256FileCacheStore<K, V> implements CacheStore<K, V>
{
    private static final byte[] HEADER_V1 = new byte[]{'N', 'U', 'T', 'E', 1};
    private static final byte[] HEADER_V2 = new byte[]{'N', 'U', 'T', 'E', 2};

    /** Length of the header: 'N' 'U' 'T' 'E' + version. */
    private static final int HEADER_SIZE = 5;

    /** Length of the expiration field, right after the header. */
    private static final int EXPIRES_AT_BYTES = 8;

    /** Length of the value or ciphertext length field. */
    private static final int LENGTH_BYTES = 4;

    /** Bytes to read to decide whether an entry has expired. */
    private static final int HEAD_SIZE = HEADER_SIZE + EXPIRES_AT_BYTES;

    /** Sentinel length of a stored {@code null} value. */
    private static final int NULL_LENGTH = -1;

    /** Record type bound to the ciphertext of the encrypted format as AAD. */
    private static final byte TYPE_VALUE = 1;

    private static final int VERSION_V1 = 1;
    private static final int VERSION_V2 = 2;

    private static final String TMP_SUFFIX = ".tmp";
    private static final int NAME_LENGTH = 64;

    private static final Kr KRIPTO = Kr.getInstance();

    private final Path dir;
    private final Serializer<K> keySerializer;
    private final Serializer<V> valueSerializer;
    private final SecretKey key;
    private final AeadRecordCipher cipher;

    private boolean closed;

    /**
     * Creates a new store backed by the given directory, one file per entry.
     *
     * @param dir the cache directory
     * @param keySerializer the serializer for keys
     * @param valueSerializer the serializer for values
     */
    public Sha256FileCacheStore(Path dir, Serializer<K> keySerializer, Serializer<V> valueSerializer)
    {
        this(dir, keySerializer, valueSerializer, null);
    }

    /**
     * Creates a new store backed by the given directory, one file per entry,
     * encrypting each value with AES-256-GCM.
     * <p>
     * Entries are written in the encrypted format (version 2): the value is
     * encrypted and authenticated, and only the expiration and the length of
     * the ciphertext stay in clear. The key is never written to the entries;
     * the caller keeps its ownership of it.
     *
     * @param dir the cache directory
     * @param keySerializer the serializer for keys
     * @param valueSerializer the serializer for values
     * @param key the AES key used to encrypt the values; {@code null} creates
     *            a plaintext store (version 1)
     */
    public Sha256FileCacheStore(Path dir, Serializer<K> keySerializer, Serializer<V> valueSerializer, SecretKey key)
    {
        this.dir = Objects.requireNonNull(dir, "dir");
        this.keySerializer = Objects.requireNonNull(keySerializer, "keySerializer");
        this.valueSerializer = Objects.requireNonNull(valueSerializer, "valueSerializer");
        this.key = key;
        this.cipher = key == null ? null : new AeadRecordCipher(key);
        try
        {
            open();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot open cache store directory " + dir, e);
        }
    }

    /**
     * Creates a new store backed by the given directory, using the same
     * serializer for keys and values.
     * <p>
     * This constructor is meant for stores where keys and values share the
     * same type, that is {@code Sha256FileCacheStore<T, T>}; the given
     * serializer is used for keys and values alike.
     *
     * @param dir the cache directory
     * @param serializer the serializer used for both keys and values
     * @param <T> the common type of keys and values
     */
    @SuppressWarnings("unchecked")
    public <T> Sha256FileCacheStore(Path dir, Serializer<T> serializer)
    {
        this(dir, (Serializer<K>) serializer, (Serializer<V>) serializer);
    }

    /**
     * Creates a new store backed by the given directory, using the same
     * serializer for keys and values, encrypting each value with AES-256-GCM.
     * <p>
     * See {@link #Sha256FileCacheStore(Path, Serializer, Serializer, SecretKey)}
     * for the details of the encrypted format.
     *
     * @param dir the cache directory
     * @param serializer the serializer used for both keys and values
     * @param key the AES key used to encrypt the values; {@code null} creates
     *            a plaintext store (version 1)
     * @param <T> the common type of keys and values
     */
    @SuppressWarnings("unchecked")
    public <T> Sha256FileCacheStore(Path dir, Serializer<T> serializer, SecretKey key)
    {
        this(dir, (Serializer<K>) serializer, (Serializer<V>) serializer, key);
    }

    /**
     * Creates a new store backed by the given directory, using the same
     * serializer for keys and values.
     * <p>
     * Unlike the constructor, this factory enforces at compile time that keys
     * and values share the same type, that is {@code Sha256FileCacheStore<T, T>}.
     *
     * @param dir the cache directory
     * @param serializer the serializer used for both keys and values
     * @param <T> the common type of keys and values
     * @return a new store
     */
    public static <T> Sha256FileCacheStore<T, T> of(Path dir, Serializer<T> serializer)
    {
        return new Sha256FileCacheStore<>(dir, serializer);
    }

    /**
     * Creates a new store backed by the given directory, using the same
     * serializer for keys and values, encrypting each value with AES-256-GCM.
     * <p>
     * See {@link #Sha256FileCacheStore(Path, Serializer, Serializer, SecretKey)}
     * for the details of the encrypted format.
     *
     * @param dir the cache directory
     * @param serializer the serializer used for both keys and values
     * @param key the AES key used to encrypt the values
     * @param <T> the common type of keys and values
     * @return a new store
     */
    public static <T> Sha256FileCacheStore<T, T> of(Path dir, Serializer<T> serializer, SecretKey key)
    {
        return new Sha256FileCacheStore<>(dir, serializer, key);
    }

    @Override
    public V get(K key)
    {
        checkOpen();
        Path entry = entry(key);
        boolean expired = false;
        byte[] storedBytes = null;
        try (SeekableByteChannel ch = Files.newByteChannel(entry, StandardOpenOption.READ))
        {
            CRC32 crc = new CRC32();
            byte[] head = readFully(ch, new byte[HEAD_SIZE], crc);
            validateVersion(head, entry);
            long expiresAt = headExpiresAt(head);
            if (isExpired(expiresAt, System.currentTimeMillis()))
            {
                expired = true;
            }
            else
            {
                int length = readLength(ch, crc);
                if (length >= 0)
                {
                    long available = ch.size() - ch.position();
                    if (length > available)
                    {
                        throw new CorruptEntryException("corrupted cache entry (length beyond file): " + entry);
                    }
                    storedBytes = readFully(ch, new byte[length], crc);
                    int storedCrc = readStoredCrc(ch);
                    if (storedCrc != (int) crc.getValue())
                    {
                        throw new CorruptEntryException("corrupted cache entry (crc mismatch): " + entry);
                    }
                }
            }
        }
        catch (NoSuchFileException e)
        {
            return null;
        }
        catch (CorruptEntryException | EOFException e)
        {
            // a truncated or corrupted entry is the cache-miss analogue of an
            // absent one: it is lazily removed so the space is reclaimed
            deleteQuietly(entry);
            return null;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot read cache entry " + entry, e);
        }
        if (expired)
        {
            deleteQuietly(entry);
            return null;
        }
        if (storedBytes == null)
        {
            return null;
        }
        if (cipher != null)
        {
            return valueSerializer.deserialize(decrypt(storedBytes, entry));
        }
        return valueSerializer.deserialize(storedBytes);
    }

    @Override
    public void put(K key, V value, long expiresAtEpochMillis)
    {
        checkOpen();
        String name = name(key);
        byte[] storedBytes = storedBytes(value);
        int length = storedBytes == null ? NULL_LENGTH : storedBytes.length;
        Path target = dir.resolve(name);
        Path tmp = dir.resolve(name + TMP_SUFFIX);
        CRC32 crc = new CRC32();
        try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))
        {
            // restrict the permissions right after creation, before any data
            // is written to the file
            IO.setPrivate(tmp);
            write(ch, header(), crc);
            write(ch, longBytes(expiresAtEpochMillis), crc);
            write(ch, intBytes(length), crc);
            if (storedBytes != null)
            {
                write(ch, storedBytes, crc);
            }
            write(ch, intBytes((int) crc.getValue()), null);
            ch.force(false);
        }
        catch (IOException e)
        {
            deleteQuietly(tmp);
            throw new UncheckedIOException("cannot write cache entry to " + tmp, e);
        }
        try
        {
            IO.replaceAtomically(tmp, target);
        }
        catch (IOException e)
        {
            deleteQuietly(tmp);
            throw new UncheckedIOException("cannot replace cache entry " + target, e);
        }
    }

    @Override
    public void remove(K key)
    {
        checkOpen();
        try
        {
            Files.deleteIfExists(entry(key));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot remove cache entry of " + key + " in " + dir, e);
        }
    }

    @Override
    public boolean containsKey(K key)
    {
        checkOpen();
        Path entry = entry(key);
        boolean expired;
        try (SeekableByteChannel ch = Files.newByteChannel(entry, StandardOpenOption.READ))
        {
            byte[] head = readFully(ch, new byte[HEAD_SIZE], null);
            validateVersion(head, entry);
            expired = isExpired(headExpiresAt(head), System.currentTimeMillis());
        }
        catch (NoSuchFileException e)
        {
            return false;
        }
        catch (CorruptEntryException | EOFException e)
        {
            deleteQuietly(entry);
            return false;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot read cache entry " + entry, e);
        }
        if (expired)
        {
            deleteQuietly(entry);
        }
        return !expired;
    }

    @Override
    public void clear()
    {
        checkOpen();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir))
        {
            for (Path file : files)
            {
                String name = file.getFileName().toString();
                // the iterator is weakly consistent, so deleting while
                // iterating is safe
                if (isEntryName(name) || name.endsWith(TMP_SUFFIX))
                {
                    deleteQuietly(file);
                }
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot clear cache store directory " + dir, e);
        }
    }

    /**
     * Returns the number of non-expired entries.
     * <p>
     * Unlike {@code FileCacheStore}, which reads its in-memory key index, this
     * operation is a directory scan: every entry file costs one open of its
     * first 13 bytes. Its cost grows with the number of entries, so prefer
     * {@link #isEmpty()} only when the size information matters and call it
     * sparingly.
     *
     * @return the number of live entries
     */
    @Override
    public long size()
    {
        checkOpen();
        long count = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir))
        {
            for (Path file : files)
            {
                if (!isEntryName(file.getFileName().toString()))
                {
                    continue;
                }
                Long expiresAt = headExpires(file);
                if (expiresAt != null && !isExpired(expiresAt, System.currentTimeMillis()))
                {
                    count++;
                }
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot scan cache store directory " + dir, e);
        }
        return count;
    }

    /**
     * Removes all expired entries, and also leftover temporary files and
     * entries damaged beyond recovery.
     * <p>
     * This operation is a directory scan: every entry file costs one open of
     * its first 13 bytes. It never opens the values themselves.
     */
    @Override
    public void purgeExpired()
    {
        checkOpen();
        long now = System.currentTimeMillis();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir))
        {
            // collect the names first so the modification of the directory
            // cannot disturb the iteration itself
            List<Path> scanned = new ArrayList<>();
            for (Path file : files)
            {
                scanned.add(file);
            }
            for (Path file : scanned)
            {
                String name = file.getFileName().toString();
                if (name.endsWith(TMP_SUFFIX))
                {
                    deleteQuietly(file);
                    continue;
                }
                if (!isEntryName(name))
                {
                    continue;
                }
                Long expiresAt = headExpires(file);
                if (expiresAt == null || isExpired(expiresAt, now))
                {
                    deleteQuietly(file);
                }
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot scan cache store directory " + dir, e);
        }
    }

    /**
     * A no-op: every {@code put} writes through and replaces its entry file
     * atomically, so there is nothing to flush to durable storage.
     */
    @Override
    public void flush()
    {
        checkOpen();
    }

    /**
     * Marks the store closed. There are no resources to release: every
     * operation opens its own entry file. Calling this method more than once
     * has no additional effect.
     */
    @Override
    public void close()
    {
        closed = true;
    }

    /**
     * Returns the directory backing this store.
     *
     * @return the cache directory
     */
    public Path getDirectory()
    {
        return dir;
    }

    /**
     * Returns the key used to encrypt the values, or {@code null} when the
     * store is not encrypted.
     *
     * @return the encryption key
     */
    public SecretKey getKey()
    {
        return key;
    }

    private void open() throws IOException
    {
        Files.createDirectories(dir);
        setDirPrivate(dir);
        removeLeftoverTmpFiles();
    }

    private void removeLeftoverTmpFiles() throws IOException
    {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir))
        {
            for (Path file : files)
            {
                if (file.getFileName().toString().endsWith(TMP_SUFFIX))
                {
                    deleteQuietly(file);
                }
            }
        }
    }

    private void checkOpen()
    {
        if (closed)
        {
            throw new IllegalStateException("cache store is closed: " + dir);
        }
    }

    private Path entry(K key)
    {
        return dir.resolve(name(key));
    }

    /**
     * Returns the entry name of the key: the SHA-256 of its serialized form,
     * in lower case hexadecimal.
     */
    private String name(K key)
    {
        Objects.requireNonNull(key, "key");
        byte[] keyBytes = keySerializer.serialize(key);
        return Hex.encode(KRIPTO.sha256.digest(keyBytes));
    }

    /**
     * Returns the bytes to store for the value: the serialized value itself,
     * or its ciphertext, or {@code null} for a {@code null} value.
     */
    private byte[] storedBytes(V value)
    {
        if (value == null)
        {
            return null;
        }
        byte[] valueBytes = valueSerializer.serialize(value);
        return cipher == null ? valueBytes : cipher.encrypt(aad(), valueBytes);
    }

    private byte[] decrypt(byte[] storedBytes, Path entry)
    {
        try
        {
            return cipher.decrypt(aad(), storedBytes);
        }
        catch (GeneralSecurityException e)
        {
            // wrong key or tampered bytes: a hard error, the entry is kept so
            // that the problem is not mistaken for an empty cache
            throw new UncheckedIOException("cannot authenticate the cache entry " + entry, new IOException(e));
        }
    }

    private byte[] header()
    {
        return cipher == null ? HEADER_V1 : HEADER_V2;
    }

    /**
     * Validates the header of an entry file. A bad magic means the file was
     * not written by this store or was damaged: a {@link CorruptEntryException}
     * the caller treats as a removable cache miss. A version that does not
     * match the key of this store is a hard {@link IOException}: the mode of
     * this store must not be silently inverted.
     */
    private void validateVersion(byte[] head, Path entry) throws IOException
    {
        for (int i = 0; i < HEADER_SIZE - 1; i++)
        {
            if (head[i] != HEADER_V1[i])
            {
                throw new CorruptEntryException("not a nut cache entry file (bad magic): " + entry);
            }
        }
        int version = head[HEADER_SIZE - 1] & 0xFF;
        int expected = cipher == null ? VERSION_V1 : VERSION_V2;
        if (version != expected)
        {
            if (version == VERSION_V1)
            {
                throw new IOException("cache entry file is not encrypted but a key was provided: " + entry);
            }
            if (version == VERSION_V2)
            {
                throw new IOException("cache entry file is encrypted but no key was provided: " + entry);
            }
            throw new IOException("unsupported cache entry file version " + version + ": " + entry);
        }
    }

    private static long headExpiresAt(byte[] head)
    {
        return ByteBuffer.wrap(head, HEADER_SIZE, EXPIRES_AT_BYTES).getLong();
    }

    /**
     * Reads the head (magic, version and expiration) of an entry file, using
     * only its first 13 bytes. The value is never opened.
     *
     * @return the expiration of the entry, or {@code null} if the file is
     *         absent, corrupt or truncated
     * @throws IOException if the entry is not of the format of this store
     */
    private Long headExpires(Path entry) throws IOException
    {
        byte[] head;
        try (SeekableByteChannel ch = Files.newByteChannel(entry, StandardOpenOption.READ))
        {
            head = readFully(ch, new byte[HEAD_SIZE], null);
        }
        catch (NoSuchFileException | CorruptEntryException | EOFException e)
        {
            return null;
        }
        validateVersion(head, entry);
        return headExpiresAt(head);
    }

    private static int readLength(SeekableByteChannel ch, CRC32 crc) throws IOException
    {
        return ByteBuffer.wrap(readFully(ch, new byte[LENGTH_BYTES], crc)).getInt();
    }

    private static int readStoredCrc(SeekableByteChannel ch) throws IOException
    {
        return ByteBuffer.wrap(readFully(ch, new byte[LENGTH_BYTES], null)).getInt();
    }

    private static byte[] readFully(SeekableByteChannel ch, byte[] data, CRC32 crc) throws IOException
    {
        ByteBuffer bb = ByteBuffer.wrap(data);
        while (bb.hasRemaining())
        {
            int n = ch.read(bb);
            if (n < 0)
            {
                throw new EOFException("truncated cache entry");
            }
        }
        if (crc != null)
        {
            crc.update(data, 0, data.length);
        }
        return data;
    }

    private static void write(SeekableByteChannel ch, byte[] data, CRC32 crc) throws IOException
    {
        ByteBuffer bb = ByteBuffer.wrap(data);
        while (bb.hasRemaining())
        {
            int n = ch.write(bb);
            if (n < 0)
            {
                throw new IOException("cannot write cache entry");
            }
        }
        if (crc != null)
        {
            crc.update(data, 0, data.length);
        }
    }

    private static byte[] intBytes(int v)
    {
        ByteBuffer bb = ByteBuffer.allocate(LENGTH_BYTES);
        bb.putInt(v);
        return bb.array();
    }

    private static byte[] longBytes(long v)
    {
        ByteBuffer bb = ByteBuffer.allocate(EXPIRES_AT_BYTES);
        bb.putLong(v);
        return bb.array();
    }

    /**
     * The associated data of an encrypted record: it binds the ciphertext to
     * the record type and to the format version.
     */
    private static byte[] aad()
    {
        return new byte[]{TYPE_VALUE, (byte) VERSION_V2};
    }

    private static void deleteQuietly(Path file)
    {
        try
        {
            Files.deleteIfExists(file);
        }
        catch (IOException e)
        {
            // best effort: a file that cannot be removed now will be removed
            // on the next purge
        }
    }

    /**
     * Best-effort restriction of the cache directory to owner-only
     * permissions ({@code 0700}): reading the entries requires listing and
     * traversing the directory, which is why the execute bit is kept.
     */
    private static void setDirPrivate(Path directory)
    {
        try
        {
            Set<PosixFilePermission> permissions = EnumSet.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(directory, permissions);
        }
        catch (IOException | UnsupportedOperationException e)
        {
            // best effort: not every filesystem supports POSIX permissions
        }
    }

    private static boolean isEntryName(String name)
    {
        return name.length() == NAME_LENGTH && Hex.isValid(name);
    }

    private static boolean isExpired(long expiresAtEpochMillis, long now)
    {
        return expiresAtEpochMillis != Long.MAX_VALUE && now - expiresAtEpochMillis >= 0;
    }

    /**
     * A record damaged beyond what an entry file could legitimately contain
     * (bad magic, truncated framing, CRC mismatch). It is the removable,
     * cache-miss class of error.
     */
    private static final class CorruptEntryException extends IOException
    {
        CorruptEntryException(String message)
        {
            super(message);
        }
    }
}
