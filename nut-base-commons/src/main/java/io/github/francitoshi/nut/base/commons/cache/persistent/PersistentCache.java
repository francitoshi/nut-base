/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.cache.persistent;

import io.github.francitoshi.nut.base.commons.cache.AbstractCache;
import io.github.francitoshi.nut.base.commons.cache.Cache;
import io.github.francitoshi.nut.base.commons.cache.CacheFactory;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

/**
 * A persistent (write-through) {@link Cache} that combines an in-memory cache
 * with a durable {@link CacheStore}, without modifying the existing cache
 * framework.
 * <p>
 * The in-memory tier is any implementation created with
 * {@link CacheFactory} (bounded, subject to its eviction policy) and acts as
 * a hot accelerator. The store tier is the permanent source of truth and
 * survives JVM restarts. The concrete stores provide static {@code open}
 * helpers that wire the memory tier, the store and the time-to-live for
 * their own layout ({@code FileCacheStore}, {@code Sha256FileCacheStore});
 * this class itself only pairs an existing memory tier with an existing
 * store.
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
 * implementations apply it via {@link System#nanoTime()}. When memory
 * repopulates itself from the store, the memory entry keeps the time-to-live
 * already burned by the store entry (see
 * {@link CacheStore#expiresAtEpochMillis}), so the memory tier expires in
 * step with the store; the store itself always expires on time. A store that
 * does not report the expiration of its entries restores a fresh TTL, in
 * which case call {@link #purgeExpired()} periodically to bound the drift.
 * <p>
 * Following the rest of the package, this class is <strong>not
 * thread-safe</strong>; wrap it with {@link #synchronizedCache()} for
 * concurrent access. A single instance (and thus a single process) should own
 * a given store file at a time.
 * <p>
 * Following the {@code HashMapCache} implementation, {@code null} values are
 * allowed: {@code get(key, creator)} distinguishes a stored {@code null}
 * from a miss and does not invoke the creator again for keys mapped to
 * {@code null}.
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
            restoreFromStore(key, value, store.expiresAtEpochMillis(key));
            return value;
        }
        if (containsKey(key))
        {
            // a {@code null} value is explicitly stored in the cache or the
            // store: it is a hit, the creator must not be invoked again
            return null;
        }
        if (create == null)
        {
            return null;
        }
        value = create.apply(key);
        put(key, value);
        return value;
    }

    /**
     * Populates the memory tier from a store hit, keeping the time-to-live
     * already burned by the store entry instead of starting a fresh one.
     * <p>
     * When the store reports an expiration (see
     * {@link CacheStore#expiresAtEpochMillis}), the restored entry expires in
     * memory exactly when the store copies expire, so the memory tier cannot
     * serve an entry after the store has considered it expired. When the
     * store does not report it ({@code Long.MAX_VALUE}), the entry is
     * restored with a fresh time-to-live, as before.
     */
    private void restoreFromStore(K key, V value, long expiresAtEpochMillis)
    {
        long remainingNanos;
        if (expiresAtEpochMillis == Long.MAX_VALUE)
        {
            remainingNanos = ttlNanos;
        }
        else
        {
            long remainingMillis = expiresAtEpochMillis - System.currentTimeMillis();
            if (remainingMillis <= 0)
            {
                // unreachable when the store expires on time, but the
                // wall-clock expiration cannot be trusted beyond itself
                return;
            }
            long remaining;
            if (remainingMillis > Long.MAX_VALUE / 1_000_000L)
            {
                remaining = Long.MAX_VALUE;
            }
            else
            {
                remaining = remainingMillis * 1_000_000L;
            }
            remainingNanos = ttlNanos == Long.MAX_VALUE ? remaining : Math.min(remaining, ttlNanos);
        }

        long expirationNanoTime;
        if (remainingNanos == Long.MAX_VALUE)
        {
            expirationNanoTime = Long.MAX_VALUE;
        }
        else
        {
            long now = System.nanoTime();
            long exp = now + remainingNanos;
            expirationNanoTime = remainingNanos > 0 && exp < now ? Long.MAX_VALUE : exp;
        }

        if (memory instanceof AbstractCache)
        {
            @SuppressWarnings("unchecked")
            AbstractCache<K, V> abstractMemory = (AbstractCache<K, V>) memory;
            abstractMemory.restore(key, expirationNanoTime, value);
        }
        else
        {
            memory.put(key, value);
        }
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
