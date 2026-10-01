/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import java.util.concurrent.TimeUnit;

/**
 * Factory class for instantiating Cache implementations.
 */
public final class CacheFactory
{
    private CacheFactory()
    {
        // Prevent instantiation
    }

    /**
     * Instantiates a Cache implementation based on the specified {@link CacheType} and capacity.
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param type the type of cache to instantiate
     * @param capacity the capacity parameter of the cache
     * @return a new Cache instance of the requested type
     */
    public static <K, V> Cache<K, V> getInstance(CacheType type, int capacity)
    {
        return getInstance(type, capacity, Long.MAX_VALUE);
    }

    /**
     * Instantiates a Cache implementation based on the specified {@link CacheType}, capacity, and time-to-live.
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param type the type of cache to instantiate
     * @param capacity the capacity parameter of the cache
     * @param ttlNanos time-to-live in nanoseconds
     * @return a new Cache instance of the requested type
     */
    public static <K, V> Cache<K, V> getInstance(CacheType type, int capacity, long ttlNanos)
    {
        if (type == null)
        {
            throw new IllegalArgumentException("Cache type must not be null");
        }
        switch (type)
        {
            case HASH_MAP:
                return new HashMapCache<>(capacity, ttlNanos);
            case ARC:
                return new ARCCache<>(capacity, ttlNanos);
            case LRU_LFU:
                return new LRULFUCache<>(capacity, ttlNanos);
            case TINY_LFU:
                return new TinyLFUCache<>(capacity, ttlNanos);
            default:
                throw new IllegalArgumentException("Unsupported cache type: " + type);
        }
    }
    /**
     * Instantiates a Cache implementation based on the specified {@link CacheType}, capacity, and time-to-live.
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param type the type of cache to instantiate
     * @param capacity the capacity parameter of the cache
     * @param ttl time-to-live
     * @param timeUnit units of ttl
     * @return a new Cache instance of the requested type
     */
    public static <K, V> Cache<K, V> getInstance(CacheType type, int capacity, long ttl, TimeUnit timeUnit)
    {
        if (type == null)
        {
            throw new IllegalArgumentException("Cache type must not be null");
        }
        switch (type)
        {
            case HASH_MAP:
                return new HashMapCache<>(capacity, ttl, timeUnit);
            case ARC:
                return new ARCCache<>(capacity, ttl, timeUnit);
            case LRU_LFU:
                return new LRULFUCache<>(capacity, ttl, timeUnit);
            case TINY_LFU:
                return new TinyLFUCache<>(capacity, ttl, timeUnit);
            default:
                throw new IllegalArgumentException("Unsupported cache type: " + type);
        }
    }
}
