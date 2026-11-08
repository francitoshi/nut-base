/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import io.github.francitoshi.nut.base.commons.cache.Cache;
import io.github.francitoshi.nut.base.commons.cache.CacheFactory;
import io.github.francitoshi.nut.base.commons.cache.CacheType;
import io.github.francitoshi.nut.base.commons.cache.persistent.CacheStore;
import io.github.francitoshi.nut.base.commons.cache.persistent.PersistentCache;
import io.github.francitoshi.nut.base.commons.serializer.Serializer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * The encrypted counterpart of {@link BitcaskCacheStore}: a {@link CacheStore}
 * for {@link io.github.francitoshi.nut.base.commons.cache.persistent.PersistentCache}
 * whose durable tier is an {@link EncryptedBitcask} directory. Every value
 * and every key reaches the disk encrypted and authenticated (AES-256-GCM
 * with a per-file key derived from the password), no plaintext of keys or
 * values ever lands in a data file.
 * <p>
 * The store semantics are shared with the plain store through
 * {@link AbstractBitcaskCacheStore}: expiration encoded in the entry
 * timestamp (seconds, rounded up, 0 = never), null keys and values with a
 * one-byte sentinel, the same compaction thresholds and the same accounting.
 * <p>
 * There are no hint files in the encrypted profile (they would reveal the
 * keys), so a reopen scans and decrypts every data file; what the encrypted
 * store pays in open latency it returns in byte-level authentication of the
 * whole history at open time. Like its base store, this class is
 * thread-safe: the underlying encrypted Bitcask serializes writers and
 * serves concurrent readers, and the directory lock rejects a second open
 * (also with a different password).
 *
 * @param <K> the type of keys maintained by the store
 * @param <V> the type of mapped values
 */
public class EncryptedBitcaskCacheStore<K,V> extends AbstractBitcaskCacheStore<K,V>
{
    private final Path directory;
    private final EncryptedBitcask store;

    /**
     * Creates an encrypted store in the given directory with the default
     * Bitcask settings (1 GiB rotation threshold, 600000 PBKDF2 iterations).
     * The password array is copied and the copy zeroed after key
     * derivation; zeroing the caller's array is its own job.
     *
     * @param directory the cache directory, created if missing
     * @param password the password; empty passwords are rejected
     * @param keySerializer the serializer for keys
     * @param valueSerializer the serializer for values
     */
    public EncryptedBitcaskCacheStore(Path directory, char[] password, Serializer<K> keySerializer, Serializer<V> valueSerializer)
    {
        this(directory, password, keySerializer, valueSerializer,
            EncryptedBitcask.DEFAULT_ITERATIONS, Bitcask.DEFAULT_MAX_FILE_SIZE);
    }

    /**
     * Creates an encrypted store in the given directory with explicit
     * PBKDF2 and rotation settings.
     *
     * @param directory the cache directory, created if missing
     * @param password the password
     * @param keySerializer the serializer for keys
     * @param valueSerializer the serializer for values
     * @param iterations the PBKDF2 iteration count; lower than the default
     *                   only for tests (never below 1000)
     * @param maxFileSize the Bitcask rotation threshold in bytes
     */
    public EncryptedBitcaskCacheStore(Path directory, char[] password, Serializer<K> keySerializer,
        Serializer<V> valueSerializer, int iterations, long maxFileSize)
    {
        super(keySerializer, valueSerializer);
        this.directory = directory;

        try
        {
            this.store = EncryptedBitcask.builder(directory, password)
                .iterations(iterations)
                .maxFileSize(maxFileSize)
                .sync(SyncMode.NONE)
                .build();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(
                "cannot open the encrypted cache store (wrong password or locked directory?): " + directory, e);
        }

        rebuildAccounting();
    }

    /**
     * Opens (or creates) an encrypted persistent cache backed by a store of
     * this class, with an in-memory cache of the given {@link CacheType}.
     * <p>
     * Every key and value reaches the disk encrypted and authenticated
     * (AES-256-GCM with a per-file key derived from the password). The
     * password array is copied and the copy zeroed after key derivation;
     * zeroing the caller's array is its own job.
     *
     * @param directory the cache directory, created if missing
     * @param password the password; empty passwords are rejected
     * @param type the in-memory cache implementation
     * @param capacity the in-memory cache capacity
     * @param ttlNanos time-to-live of entries in nanoseconds
     * ({@link Long#MAX_VALUE} for no expiration)
     * @param keySerializer serializer for keys of type {@code K}
     * @param valueSerializer serializer for values of type {@code V}
     * @param <K> the type of keys
     * @param <V> the type of values
     * @return a new persistent cache
     */
    public static <K, V> PersistentCache<K, V> open(Path directory, char[] password, CacheType type, int capacity, long ttlNanos, Serializer<K> keySerializer, Serializer<V> valueSerializer)
    {
        Cache<K, V> memory = CacheFactory.getInstance(type, capacity, ttlNanos);
        CacheStore<K, V> store = new EncryptedBitcaskCacheStore<>(directory, password, keySerializer, valueSerializer);
        return new PersistentCache<>(memory, store, ttlNanos);
    }

    /**
     * Opens (or creates) an encrypted persistent cache backed by a store of
     * this class, with an in-memory cache of the given {@link CacheType}.
     * <p>
     * Every key and value reaches the disk encrypted and authenticated
     * (AES-256-GCM with a per-file key derived from the password). The
     * password array is copied and the copy zeroed after key derivation;
     * zeroing the caller's array is its own job.
     *
     * @param directory the cache directory, created if missing
     * @param password the password; empty passwords are rejected
     * @param type the in-memory cache implementation
     * @param capacity the in-memory cache capacity
     * @param ttl time-to-live of entries
     * @param timeUnit the unit of {@code ttl}
     * @param keySerializer serializer for keys of type {@code K}
     * @param valueSerializer serializer for values of type {@code V}
     * @param <K> the type of keys
     * @param <V> the type of values
     * @return a new persistent cache
     */
    public static <K, V> PersistentCache<K, V> open(Path directory, char[] password, CacheType type, int capacity, long ttl, TimeUnit timeUnit, Serializer<K> keySerializer, Serializer<V> valueSerializer)
    {
        return open(directory, password, type, capacity, timeUnit.toNanos(ttl), keySerializer, valueSerializer);
    }

    /**
     * Returns the directory of the underlying encrypted store.
     *
     * @return the cache directory
     */
    public Path getDirectory()
    {
        return directory;
    }

    @Override
    protected KeyDirEntry storeLocate(byte[] key)
    {
        return store.locate(key);
    }

    @Override
    protected void storePut(byte[] key, byte[] value, int tstamp) throws IOException
    {
        store.put(key, value, tstamp);
    }

    @Override
    protected byte[] storeGet(byte[] key) throws IOException
    {
        return store.get(key);
    }

    @Override
    protected boolean storeRemove(byte[] key) throws IOException
    {
        return store.delete(key);
    }

    @Override
    protected void storeForEachEntry(Bitcask.EntryVisitor visitor)
    {
        store.forEachEntry(visitor);
    }

    /**
     * The encrypted tombstone record: len(4) + nonce(12) + the plaintext
     * header (10 bytes: tstamp, ksz, vsz) + key + the 16-byte GCM tag.
     */
    @Override
    protected long tombstoneRecordSize(byte[] key)
    {
        return 32 + 10 + key.length;
    }

    @Override
    protected long storeTotalBytes() throws IOException
    {
        return store.totalDataBytes();
    }

    @Override
    protected void storeFlush() throws IOException
    {
        store.flush();
    }

    @Override
    protected void storeMerge() throws IOException
    {
        store.merge();
    }

    @Override
    protected void storeClear() throws IOException
    {
        store.clear();
    }

    @Override
    protected void storeClose() throws IOException
    {
        store.close();
    }

    @Override
    protected byte[] encodedKey(K key)
    {
        // serialized null keys become empty payloads in this store; the
        // sentinel keeps the shape (see the plain store)
        byte[] encoded = super.encodedKey(key);
        if (encoded.length == 0)
        {
            return new byte[]{AbstractBitcaskCacheStore.NULL_SENTINEL};
        }
        return encoded;
    }

    @Override
    protected byte[] encodedValue(V value)
    {
        byte[] encoded = super.encodedValue(value);
        if (encoded.length == 0)
        {
            return new byte[]{AbstractBitcaskCacheStore.NULL_SENTINEL};
        }
        return encoded;
    }

    /**
     * Access to the underlying store for tests, not for casual use.
     *
     * @return the encrypted store
     */
    EncryptedBitcask underlying()
    {
        return store;
    }
}
