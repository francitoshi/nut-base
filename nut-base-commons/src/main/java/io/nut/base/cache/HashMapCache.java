/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Cache} implementation backed by a plain {@link HashMap}.
 * <p>
 * This implementation applies <strong>no eviction policy</strong> whatsoever:
 * entries are kept indefinitely until explicitly removed via {@link #clear()}
 * or overwritten via {@link #put(Object, Object)}. It is therefore best suited
 * for scenarios where the key space is bounded, or where the caller manages
 * the cache lifecycle explicitly.
 * <p>
 * This class is <strong>not thread-safe</strong>. If concurrent access is
 * required, wrap an instance using {@link #synchronizedCache()} (inherited
 * from {@link AbstractCache}), or use an external synchronization mechanism.
 * <p>
 * Following {@link HashMap}'s own contract, this cache permits {@code null}
 * keys and {@code null} values. Note, however, that because
 * {@link #get(Object)} returns {@code null} both when the key is absent and
 * when the key is explicitly mapped to {@code null}, the
 * {@link #get(Object, java.util.function.Function)} convenience method
 * (inherited from {@link AbstractCache}) cannot distinguish between the two
 * cases and will invoke the creator function again for keys mapped to
 * {@code null}.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class HashMapCache<K, V> extends AbstractCache<K, V>
{
    private final Map<K, AbstractCache.Item<V>> map;
    private final long ttlNanos;

    /**
     * Creates a new, empty cache backed by a {@link HashMap} with the given
     * initial capacity and time-to-live.
     *
     * @param initialCapacity the initial capacity of the underlying
     * {@link HashMap}
     * @param ttlNanos time-to-live of elements in nanoseconds
     */
    public HashMapCache(int initialCapacity, long ttlNanos)
    {
        this.map = new HashMap<>(Math.min(initialCapacity, 1024 * 1024));
        this.ttlNanos = ttlNanos;
    }

    /**
     * Creates a new, empty cache backed by a default-constructed
     * {@link HashMap}.
     */
    public HashMapCache()
    {
        this(16, Long.MAX_VALUE);
    }

    public HashMapCache(int initialCapacity, long ttl, TimeUnit timeUnit)
    {
        this(initialCapacity, timeUnit.toNanos(ttl));
    }

    @Override
    public V get(K key, java.util.function.Function<? super K, ? extends V> creator)
    {
        AbstractCache.Item<V> item = map.get(key);
        if (item != null)
        {
            if (item.isExpired(System.nanoTime()))
            {
                map.remove(key);
            }
            else
            {
                return item.v;
            }
        }

        if (creator == null)
        {
            return null;
        }

        V value = creator.apply(key);
        long now = System.nanoTime();
        long exp = calculateExpiration(now, ttlNanos);
        map.put(key, new AbstractCache.Item<>(value, exp));
        return value;
    }

    @Override
    public boolean containsKey(K key)
    {
        AbstractCache.Item<V> item = map.get(key);
        if (item == null)
        {
            return false;
        }
        if (item.isExpired(System.nanoTime()))
        {
            map.remove(key);
            return false;
        }
        return true;
    }

    @Override
    public void put(K key, V value)
    {
        long now = System.nanoTime();
        long exp = calculateExpiration(now, ttlNanos);
        map.put(key, new AbstractCache.Item<>(value, exp));
    }

    @Override
    public int size()
    {
        // Clean up expired items lazily to make size as accurate as possible
        long now = System.nanoTime();
        map.values().removeIf(item -> item.isExpired(now));
        return map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }

    @Override
    public void clear()
    {
        map.clear();
    }

    @Override
    public void purgeExpired()
    {
        long now = System.nanoTime();
        map.values().removeIf(item -> item.isExpired(now));
    }
}
