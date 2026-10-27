/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.cache.persistent;

import io.github.francitoshi.nut.base.commons.cache.AbstractCache;
import io.github.francitoshi.nut.base.commons.cache.Cache;
import io.github.francitoshi.nut.base.commons.cache.CacheFactory;
import io.github.francitoshi.nut.base.commons.cache.CacheType;
import io.github.francitoshi.nut.base.commons.serializer.Serializer;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.crypto.SecretKey;

/**
 * A persistent (write-through) {@link Cache} that combines an in-memory cache
 * with a durable {@link CacheStore}, without modifying the existing cache
 * framework.
 * <p>
 * The in-memory tier is any implementation created with
 * {@link CacheFactory} (bounded, subject to its eviction policy) and acts as
 * a hot accelerator. The store tier is the permanent source of truth and
 * survives JVM restarts.
 * <ul>
 * <li>{@link #put(Object, Object)} writes to the store first (durability),
 * then to memory.</li>
 * <li>{@link #get(Object)} looks up memory first, then the store, repopulating
 * memory on a store hit.</li>
 * <li>{@link #get(Object, java.util.function.Function)} only invokes the
 * creator when both tiers miss, then writes the result through.</li>
 * <li>{@link #size()} and {@link #isEmpty()} report the store contents (the
 * authoritative tier).</li>
 * </ul>
 * Expiration uses a single logical TTL. On the store it is kept as wall-clock
 * epoch millis (surviving restarts); on the memory tier the existing
 * implementations apply it via {@link System#nanoTime()}. When memory repopulates
 * itself from the store, the memory entry receives a fresh TTL starting at
 * repopulation time, so the memory tier may serve an entry for up to the TTL
 * after the store considers it expired; the store itself always expires on
 * time. Call {@link #purgeExpired()} periodically to bound this drift.
 * <p>
 * Following the rest of the package, this class is <strong>not
 * thread-safe</strong>; wrap it with {@link #synchronizedCache()} for
 * concurrent access. A single instance (and thus a single process) should own
 * a given store file at a time.
 * <p>
 * As with the {@code HashMapCache} implementation, {@code null} values are
 * allowed but {@code get(key, creator)} cannot distinguish a stored
 * {@code null} from a miss and will invoke the creator again.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class PersistentCache<K, V> extends AbstractCache<K, V> implements AutoCloseable
{
    private final Cache<K, V> memory;
    private final CacheStore<K, V> store;
    private final long ttlNanos;

    /**
     * Creates a new persistent cache from an existing in-memory cache and a
     * durable store.
     *
     * @param memory the in-memory tier, e.g. from
     * {@link CacheFactory#getInstance(io.github.francitoshi.nut.base.commons.cache.CacheType, int, long, java.util.concurrent.TimeUnit)}
     * @param store the durable tier
     * @param ttlNanos logical time-to-live of entries in nanoseconds
     * ({@link Long#MAX_VALUE} for no expiration)
     */
    public PersistentCache(Cache<K, V> memory, CacheStore<K, V> store, long ttlNanos)
    {
        this.memory = Objects.requireNonNull(memory, "memory");
        this.store = Objects.requireNonNull(store, "store");
        this.ttlNanos = ttlNanos;
    }

    /**
     * Opens (or creates) a persistent cache backed by a single file, wiring a
     * {@link FileCacheStore} with the given serializers and an in-memory
     * cache of the given {@link CacheType}.
     *
     * @param file the store file
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
    public static <K, V> PersistentCache<K, V> open(Path file, CacheType type, int capacity, long ttl, TimeUnit timeUnit, Serializer<K> keySerializer, Serializer<V> valueSerializer)
    {
        return open(file, type, capacity, timeUnit.toNanos(ttl), keySerializer, valueSerializer);
    }

    /**
     * Opens (or creates) a persistent cache backed by a single file, wiring a
     * {@link FileCacheStore} with the given serializers and an in-memory
     * cache of the given {@link CacheType}.
     *
     * @param file the store file
     * @param type the in-memory cache implementation
     * @param capacity the in-memory cache capacity
     * @param ttl time-to-live of entries
     * @param timeUnit the unit of {@code ttl}
     * @param keySerializer serializer for keys of type {@code K}
     * @param valueSerializer serializer for values of type {@code V}
     * @param key the AES key used to encrypt the store file; {@code null}
     *            keeps the content in clear
     * @param <K> the type of keys
     * @param <V> the type of values
     * @return a new persistent cache
     */
    public static <K, V> PersistentCache<K, V> open(Path file, CacheType type, int capacity, long ttl, TimeUnit timeUnit, Serializer<K> keySerializer, Serializer<V> valueSerializer, SecretKey key)
    {
        return open(file, type, capacity, timeUnit.toNanos(ttl), keySerializer, valueSerializer, key);
    }

    /**
     * Opens (or creates) a persistent cache backed by a single file, wiring a
     * {@link FileCacheStore} with the given serializers and an in-memory
     * cache of the given {@link CacheType}.
     *
     * @param file the store file
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
    public static <K, V> PersistentCache<K, V> open(Path file, CacheType type, int capacity, long ttlNanos, Serializer<K> keySerializer, Serializer<V> valueSerializer)
    {
        return open(file, type, capacity, ttlNanos, keySerializer, valueSerializer, null);
    }

    /**
     * Opens (or creates) a persistent cache backed by a single file, wiring a
     * {@link FileCacheStore} with the given serializers and an in-memory
     * cache of the given {@link CacheType}.
     * <p>
     * When a key is given, the store file is written in the encrypted format
     * (version 2) and its content is encrypted and authenticated with
     * AES-256-GCM. The key is never written to the file.
     *
     * @param file the store file
     * @param type the in-memory cache implementation
     * @param capacity the in-memory cache capacity
     * @param ttlNanos time-to-live of entries in nanoseconds
     * ({@link Long#MAX_VALUE} for no expiration)
     * @param keySerializer serializer for keys of type {@code K}
     * @param valueSerializer serializer for values of type {@code V}
     * @param key the AES key used to encrypt the store file; {@code null}
     *            keeps the content in clear
     * @param <K> the type of keys
     * @param <V> the type of values
     * @return a new persistent cache
     */
    public static <K, V> PersistentCache<K, V> open(Path file, CacheType type, int capacity, long ttlNanos, Serializer<K> keySerializer, Serializer<V> valueSerializer, SecretKey key)
    {
        FileCacheStore<K, V> store = new FileCacheStore<>(file, keySerializer, valueSerializer, key);
        Cache<K, V> memory = CacheFactory.getInstance(type, capacity, ttlNanos);
        return new PersistentCache<>(memory, store, ttlNanos);
    }

    @Override
    public V get(K key, Function<? super K, ? extends V> create)
    {
        V value = memory.get(key);
        if (value != null)
        {
            return value;
        }
        value = store.get(key);
        if (value != null)
        {
            memory.put(key, value);
            return value;
        }
        if (create == null)
        {
            return null;
        }
        value = create.apply(key);
        put(key, value);
        return value;
    }

    @Override
    public void put(K key, V value)
    {
        store.put(key, value, calculateExpirationEpoch());
        memory.put(key, value);
    }

    /**
     * Removes the mapping for the given key from both tiers.
     *
     * @param key the key whose mapping is to be removed
     */
    @Override
    public void remove(K key)
    {
        store.remove(key);
        memory.remove(key);
    }

    @Override
    public boolean containsKey(K key)
    {
        return memory.containsKey(key) || store.containsKey(key);
    }

    @Override
    public int size()
    {
        long size = store.size();
        return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
    }

    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }

    @Override
    public void clear()
    {
        store.clear();
        memory.clear();
    }

    @Override
    public void purgeExpired()
    {
        store.purgeExpired();
        memory.purgeExpired();
    }

    /**
     * Forces the durable tier to write any pending changes to disk.
     */
    public void flush()
    {
        store.flush();
    }

    /**
     * Flushes the store and releases its resources. The in-memory tier is
     * cleared. Calling this method more than once has no additional effect.
     */
    public void close()
    {
        memory.clear();
        store.close();
    }

    /**
     * Returns the durable tier of this cache.
     *
     * @return the store
     */
    public CacheStore<K, V> getStore()
    {
        return store;
    }

    /**
     * Returns the in-memory tier of this cache.
     *
     * @return the memory cache
     */
    public Cache<K, V> getMemory()
    {
        return memory;
    }

    private long calculateExpirationEpoch()
    {
        if (ttlNanos == Long.MAX_VALUE)
        {
            return Long.MAX_VALUE;
        }
        long now = System.currentTimeMillis();
        long ttlMillis;
        if (ttlNanos <= 0)
        {
            ttlMillis = 0;
        }
        else
        {
            ttlMillis = (ttlNanos + 999_999L) / 1_000_000L;
        }
        long exp = now + ttlMillis;
        if (exp < now)
        {
            return Long.MAX_VALUE;
        }
        return exp;
    }
}
