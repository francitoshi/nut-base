/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.cache.persistent;

/**
 * Durable storage backend for a {@link PersistentCache}.
 * <p>
 * A store is the permanent tier of a two-tier cache: the in-memory tier
 * (an existing {@link io.github.francitoshi.nut.base.commons.cache.Cache} implementation) acts
 * as a hot accelerator while this store keeps entries across restarts.
 * <p>
 * Implementations of this interface are <strong>not thread-safe</strong>.
 * A {@code PersistentCache} instance owns its store exclusively; concurrent
 * access must be external (e.g. via
 * {@link io.github.francitoshi.nut.base.commons.cache.Cache#synchronizedCache()}).
 * <p>
 * Expiration uses wall-clock time ({@link System#currentTimeMillis()} epoch
 * millis), not {@link System#nanoTime()}, so that it survives JVM restarts.
 * {@link Long#MAX_VALUE} as {@code expiresAtEpochMillis} means "never
 * expires".
 *
 * @param <K> the type of keys maintained by the store
 * @param <V> the type of mapped values
 */
public interface CacheStore<K, V> extends AutoCloseable
{
    /**
     * Returns the value for the given key, or {@code null} if the key is
     * absent or its entry has expired. Expired entries may be discarded
     * lazily by this operation.
     *
     * @param key the key whose value is to be returned
     * @return the value, or {@code null} if absent or expired
     */
    V get(K key);

    /**
     * Associates the given value with the given key, replacing any previous
     * mapping.
     *
     * @param key the key
     * @param value the value to associate; {@code null} is allowed
     * @param expiresAtEpochMillis wall-clock expiration instant in epoch
     * milliseconds ({@link Long#MAX_VALUE} for no expiration)
     */
    void put(K key, V value, long expiresAtEpochMillis);

    /**
     * Removes the mapping for the given key, if present.
     * <p>
     * Removing a key that is not present has no effect.
     *
     * @param key the key whose mapping is to be removed
     */
    void remove(K key);

    /**
     * Returns {@code true} if a non-expired mapping exists for the key.
     *
     * @param key the key
     * @return {@code true} if present and not expired
     */
    boolean containsKey(K key);

    /**
     * Removes all entries.
     */
    void clear();

    /**
     * Returns the number of non-expired entries.
     *
     * @return the number of live entries
     */
    long size();

    /**
     * Returns {@code true} if the store holds no non-expired entries.
     *
     * @return {@code true} if there are no live entries
     */
    default boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * Removes all expired entries.
     */
    void purgeExpired();

    /**
     * Forces any pending writes to be written to the durable storage.
     * <p>
     * Implementations that write through to the storage on every operation
     * may implement this as a no-op.
     */
    void flush();

    /**
     * Flushes and releases the underlying resources. Implementations should
     * leave the store in a consistent, durable state. Calling this method
     * more than once has no additional effect.
     */
    @Override
    void close();
}
