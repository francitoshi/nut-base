/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.cache;

import java.util.function.Function;

/**
 * A skeletal implementation of the {@link Cache} interface to minimize the
 * effort required to implement this interface. This class provides a default
 * implementation for the "get-or-create" logic.
 * <p>
 * To implement a cache, the programmer needs only to extend this class and
 * provide implementations for the core {@link #get(Object)} and
 * {@link #put(Object, Object)} methods, and any other methods from the
 * {@code Cache} interface (e.g., {@code invalidate}, {@code size}, etc.).
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public abstract class AbstractCache<K, V> implements Cache<K, V>
{
    public static class Item<V>
    {
        public volatile long expirationNanoTime;
        public volatile V v;

        public Item(V v, long expirationNanoTime)
        {
            this.v = v;
            this.expirationNanoTime = expirationNanoTime;
        }

        public boolean isExpired(long now)
        {
            if (expirationNanoTime == Long.MAX_VALUE)
            {
                return false;
            }
            return now - expirationNanoTime >= 0;
        }

        /**
         * Returns whether this item has expired as of now. Eternal items
         * ({@code expirationNanoTime == Long.MAX_VALUE}) never read the
         * clock, so a cache without time-to-live pays no {@code nanoTime}
         * on its hot path; only items with a real deadline do.
         *
         * @return {@code true} if the item has a deadline and it has passed
         */
        public boolean isExpiredNow()
        {
            long exp = expirationNanoTime;
            return exp != Long.MAX_VALUE && System.nanoTime() - exp >= 0;
        }
    }

    protected static long calculateExpiration(long now, long ttlNanos)
    {
        if (ttlNanos == Long.MAX_VALUE)
        {
            return Long.MAX_VALUE;
        }
        long exp = now + ttlNanos;
        if (ttlNanos > 0 && exp < now)
        {
            return Long.MAX_VALUE;
        }
        return exp;
    }

    /**
     * Calculates the expiration for a new item from the cache's
     * time-to-live, reading the clock only when the time-to-live is real:
     * an eternal cache ({@code ttlNanos == Long.MAX_VALUE}) pays no
     * {@code nanoTime} on its write path.
     *
     * @param ttlNanos the time-to-live in nanos
     * @return the expiration timestamp in {@link System#nanoTime()} space
     */
    protected static long calculateExpiration(long ttlNanos)
    {
        if (ttlNanos == Long.MAX_VALUE)
        {
            return Long.MAX_VALUE;
        }
        return calculateExpiration(System.nanoTime(), ttlNanos);
    }

    @Override
    public V get(K key)
    {
        return get(key, null);
    }

    /**
     * Inserts an entry with an exact expiration time, bypassing the
     * time-to-live of the cache. It is the hook used to repopulate a cache
     * tier from a durable copy without re-basing the age of the entry.
     * <p>
     * The default implementation delegates to {@link #put(Object, Object)},
     * which applies a fresh time-to-live; implementations that keep their
     * entries in a private {@code Item} override it so the given expiration
     * is honored exactly.
     * <p>
     * This method is an implementation hook of the caching framework and not
     * part of the {@link Cache} contract: it exists so that persistence code
     * can restore an entry with the expiration it had when it was stored.
     *
     * @param key the key to restore
     * @param expirationNanoTime the absolute {@link System#nanoTime()}
     *        expiration instant; {@link Long#MAX_VALUE} restores an entry
     *        that never expires
     * @param value the value to restore
     */
    public void restore(K key, long expirationNanoTime, V value)
    {
        put(key, value);
    }

    private static class SynchronizedCache<K,V> implements Cache<K,V>
    {
        private final Object lock = new Object();
        private final Cache<K,V> cache;

        public SynchronizedCache(Cache<K, V> cache)
        {
            this.cache = cache;
        }

        @Override
        public V get(K key)
        {
            synchronized(lock)
            {
                return cache.get(key);
            }
        }

        @Override
        public V get(K key, Function<? super K, ? extends V> create)
        {
            synchronized(lock)
            {
                return cache.get(key, create);
            }
        }

        @Override
        public void put(K key, V value)
        {
            synchronized(lock)
            {
                cache.put(key, value);
            }
        }

        @Override
        public void remove(K key)
        {
            synchronized(lock)
            {
                cache.remove(key);
            }
        }

        @Override
        public boolean containsKey(K key)
        {
            synchronized(lock)
            {
                return cache.containsKey(key);
            }
        }

        @Override
        public int size()
        {
            synchronized(lock)
            {
                return cache.size();
            }
        }

        @Override
        public boolean isEmpty()
        {
            synchronized(lock)
            {
                return cache.isEmpty();
            }
        }

        @Override
        public void clear()
        {
            synchronized(lock)
            {
                cache.clear();
            }
        }

        @Override
        public void purgeExpired()
        {
            synchronized(lock)
            {
                cache.purgeExpired();
            }
        }

        @Override
        public int shrink(int target)
        {
            synchronized(lock)
            {
                return cache.shrink(target);
            }
        }

        @Override
        public Cache<K, V> synchronizedCache()
        {
            return this;
        }
    }
    
    @Override
    public Cache<K, V> synchronizedCache()
    {
        return new SynchronizedCache<>(this);
    }
}
