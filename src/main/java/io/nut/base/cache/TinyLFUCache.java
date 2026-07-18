/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simplified W-TinyLFU implementation inspired by Caffeine cache. Uses a window
 * cache (LRU) for recent items and a main cache (LFU) for frequently used
 * items.
 *
 * This provides better admission policy than pure LRU or LFU.
 */
public class TinyLFUCache<K, V> extends AbstractCache<K,V> implements Cache<K,V>
{

    private final int capacity;
    private final int windowSize;
    private final int mainSize;

    // Window cache (1% of capacity) - protects against bursts
    private final LRUCache<K, V> windowCache;

    // Main cache (99% of capacity) - for frequent items
    private final SegmentedLFUCache<K, V> mainCache;

    // Frequency sketch - probabilistic counter
    private final CountMinSketch<K> sketch;
    
    private final boolean statistics;
    private final AtomicInteger countWindowHits = new AtomicInteger();
    private final AtomicInteger countMainHits = new AtomicInteger();
    private final AtomicInteger countAttempts = new AtomicInteger();

    public TinyLFUCache(int capacity, boolean statistics)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.windowSize = Math.max(1, capacity / 100); // 1% for window
        this.mainSize = capacity - windowSize;

        this.windowCache = new LRUCache<>(windowSize);
        this.mainCache = new SegmentedLFUCache<>(mainSize);
        this.sketch = new CountMinSketch<>(capacity * 10);
        this.statistics = statistics;
    }
    public TinyLFUCache(int capacity)
    {
        this(capacity, false);
    }

    @Override
    public V get(K key)
    {
        // Record access
        sketch.increment(key);
        if(statistics) countAttempts.incrementAndGet();

        // Try window cache first
        V value = windowCache.get(key);
        if (value != null)
        {
            if(statistics) countWindowHits.incrementAndGet();
            return value;
        }

        // Try main cache
        value = mainCache.get(key);
        if(statistics && value!=null)
        {
            countMainHits.incrementAndGet();
        }
        return value;
    }

    @Override
    public void put(K key, V value)
    {
        sketch.increment(key);

        // Check if already exists
        if (windowCache.contains(key))
        {
            windowCache.put(key, value);
            return;
        }

        if (mainCache.contains(key))
        {
            mainCache.put(key, value);
            return;
        }

        // New item - try admission to window
        if (windowCache.size() < windowSize)
        {
            windowCache.put(key, value);
        }
        else
        {
            // Window is full - evict from window and try admission to main
            Map.Entry<K, V> evicted = windowCache.evict();
            windowCache.put(key, value);

            // Admit to main if frequency is good
            tryAdmitToMain(evicted.getKey(), evicted.getValue());
        }
    }

    private void tryAdmitToMain(K key, V value)
    {
        if (mainSize <= 0)
        {
            // Main cache has no room at all (can happen with very small
            // overall capacities), so the evicted item is simply discarded.
            return;
        }

        if (mainCache.size() < mainSize)
        {
            mainCache.put(key, value);
            return;
        }

        // Compare frequencies - admit only if better than victim
        if (mainCache.isEmpty())
        {
            // Nothing to compare against; just admit the candidate.
            mainCache.put(key, value);
            return;
        }

        K victim = mainCache.peekVictim();
        int candidateFreq = sketch.estimate(key);
        int victimFreq = sketch.estimate(victim);

        if (candidateFreq > victimFreq)
        {
            mainCache.evictVictim();
            mainCache.put(key, value);
        }
    }

    @Override
    public int size()
    {
        return windowCache.size() + mainCache.size();
    }

    @Override
    public boolean isEmpty()
    {
        return windowCache.isEmpty() && mainCache.isEmpty();
    }

    @Override
    public void clear()
    {
        windowCache.clear();
        mainCache.clear();
    }
    
    
    // Simple LRU for window cache
    private static class LRUCache<K, V>
    {

        private final int capacity;
        private final LinkedHashMap<K, V> map;

        LRUCache(int capacity)
        {
            this.capacity = capacity;
            this.map = new LinkedHashMap<K,V>(capacity, 0.75f, true)
            {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
                {
                    return size() > capacity;
                }
            };
        }

        V get(K key)
        {
            return map.get(key);
        }

        void put(K key, V value)
        {
            map.put(key, value);
        }

        boolean contains(K key)
        {
            return map.containsKey(key);
        }

        Map.Entry<K, V> evict()
        {
            K oldestKey = map.keySet().iterator().next();
            V value = map.remove(oldestKey);
            return new AbstractMap.SimpleEntry<>(oldestKey, value);
        }

        int size()
        {
            return map.size();
        }
        boolean isEmpty()
        {
            return map.isEmpty();
        }
        void clear()
        {
            map.clear();
        }
    }

    // Segmented LFU (SLRU) - divides into protected and probation segments
    private static class SegmentedLFUCache<K, V>
    {

        private final int capacity;
        private final LinkedHashMap<K, V> probation; // 20%
        private final LinkedHashMap<K, V> protect;   // 80%
        private final int protectSize;

        SegmentedLFUCache(int capacity)
        {
            this.capacity = capacity;
            this.protectSize = (int) (capacity * 0.8);
            this.probation = new LinkedHashMap<>();
            this.protect = new LinkedHashMap<>(16, 0.75f, true);
        }

        V get(K key)
        {
            V value = protect.get(key);
            if (value != null)
            {
                return value;
            }

            value = probation.remove(key);
            if (value != null)
            {
                // Promote to protected
                promoteToProtected(key, value);
                return value;
            }

            return null;
        }

        void put(K key, V value)
        {
            if (protect.containsKey(key))
            {
                protect.put(key, value);
            }
            else if (probation.containsKey(key))
            {
                probation.remove(key);
                promoteToProtected(key, value);
            }
            else
            {
                probation.put(key, value);
            }
        }

        private void promoteToProtected(K key, V value)
        {
            if (protect.size() >= protectSize)
            {
                // Demote oldest from protected to probation
                K demoteKey = protect.keySet().iterator().next();
                V demoteValue = protect.remove(demoteKey);
                probation.put(demoteKey, demoteValue);
            }
            protect.put(key, value);
        }

        boolean contains(K key)
        {
            return protect.containsKey(key) || probation.containsKey(key);
        }

        K peekVictim()
        {
            if (!probation.isEmpty())
            {
                return probation.keySet().iterator().next();
            }
            if (!protect.isEmpty())
            {
                return protect.keySet().iterator().next();
            }
            return null;
        }

        void evictVictim()
        {
            if (!probation.isEmpty())
            {
                K key = probation.keySet().iterator().next();
                probation.remove(key);
            }
            else if (!protect.isEmpty())
            {
                K key = protect.keySet().iterator().next();
                protect.remove(key);
            }
        }

        int size()
        {
            return probation.size() + protect.size();
        }
        boolean isEmpty()
        {
            return probation.isEmpty() && protect.isEmpty();
        }
        void clear()
        {
            probation.clear();
            protect.clear();
        }
    }

    // Count-Min Sketch - probabilistic frequency counter
    private static class CountMinSketch<K>
    {

        private final int width;
        private final int depth;
        private final int[][] counters;
        private int size;
        private final int sampleSize;

        CountMinSketch(int sampleSize)
        {
            this.width = 2048; // Should be power of 2
            this.depth = 4;
            this.counters = new int[depth][width];
            this.sampleSize = sampleSize;
        }

        void increment(K key)
        {
            if (++size >= sampleSize)
            {
                reset();
            }

            int hash = key.hashCode();
            for (int i = 0; i < depth; i++)
            {
                int index = indexFor(hash, i);
                counters[i][index] = Math.min(15, counters[i][index] + 1);
            }
        }

        int estimate(K key)
        {
            int min = Integer.MAX_VALUE;
            int hash = key.hashCode();

            for (int i = 0; i < depth; i++)
            {
                int index = indexFor(hash, i);
                min = Math.min(min, counters[i][index]);
            }

            return min;
        }

        // Same mixing function used by both increment() and estimate() so that
        // a given (key, row) pair always maps to the same counter slot.
        // width is a power of two, so '& (width - 1)' is used instead of '%'
        // to also avoid Math.abs(Integer.MIN_VALUE) returning a negative value.
        private int indexFor(int hash, int row)
        {
            int mixed = hash ^ ((hash >>> 16) * (row + 1));
            return mixed & (width - 1);
        }

        private void reset()
        {
            // Decay all counters by half (aging)
            for (int i = 0; i < depth; i++)
            {
                for (int j = 0; j < width; j++)
                {
                    counters[i][j] = counters[i][j] >> 1;
                }
            }
            size = 0;
        }
    }
}
