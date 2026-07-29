/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import java.util.HashMap;
import java.util.Map;

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
    private final Map<K, V> map;

    /**
     * Creates a new, empty cache backed by a default-constructed
     * {@link HashMap}.
     */
    public HashMapCache()
    {
        this.map = new HashMap<>();
    }

    /**
     * Creates a new, empty cache backed by a {@link HashMap} with the given
     * initial capacity.
     *
     * @param initialCapacity the initial capacity of the underlying
     * {@link HashMap}
     */
    public HashMapCache(int initialCapacity)
    {
        this.map = new HashMap<>(initialCapacity);
    }

    /**
     * Creates a new cache backed by a {@link HashMap}, pre-populated with the
     * entries of the given map.
     *
     * @param initial the entries to seed this cache with; a defensive copy is
     * made, so subsequent changes to {@code initial} are not reflected here
     */
    public HashMapCache(Map<? extends K, ? extends V> initial)
    {
        this.map = new HashMap<>(initial);
    }

    @Override
    public V get(K key)
    {
        return map.get(key);
    }

    @Override
    public void put(K key, V value)
    {
        map.put(key, value);
    }

    @Override
    public int size()
    {
        return map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    @Override
    public void clear()
    {
        map.clear();
    }
}
