/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

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

    @Override
    public V get(K key)
    {
        return get(key, null);
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
        public Cache<K, V> synchronizedCache()
        {
            synchronized(lock)
            {
                return this;
            }
        }
    }
    
    @Override
    public Cache<K, V> synchronizedCache()
    {
        return new SynchronizedCache<>(this);
    }
}
