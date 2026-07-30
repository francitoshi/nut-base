/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

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
        if (type == null)
        {
            throw new IllegalArgumentException("Cache type must not be null");
        }
        switch (type)
        {
            case HASH_MAP:
                return new HashMapCache<>(capacity);
            case ARC:
                return new ARCCache<>(capacity);
            case LRU_LFU:
                return new LRULFUCache<>(capacity);
            case TINY_LFU:
                return new TinyLFUCache<>(capacity);
            default:
                throw new IllegalArgumentException("Unsupported cache type: " + type);
        }
    }
}
