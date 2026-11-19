/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.francitoshi.nut.base.commons.cache.persistent.CacheStore;
import io.github.francitoshi.nut.base.commons.cache.persistent.PersistentCache;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class CacheShrinkTest
{
    // -----------------------------------------------------------------
    // LRULFUCache: evicts least-frequent (LRU tiebreaker), capacity intact
    // -----------------------------------------------------------------

    @Test
    void shrinkEvictsTheLeastFrequentEntriesAndKeepsTheCapacity()
    {
        LRULFUCache<String, Integer> cache = new LRULFUCache<>(4);
        cache.put("k1", 1);
        cache.put("k2", 2);
        cache.put("k3", 3);
        cache.put("k4", 4);
        cache.get("k1");   // frequency 2
        cache.get("k1");   // frequency 3
        cache.get("k2");   // frequency 2

        assertEquals(2, cache.shrink(2));
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsKey("k2"));
        assertFalse(cache.containsKey("k3"));
        assertFalse(cache.containsKey("k4"));

        // the configured capacity is untouched: the cache re-grows
        cache.put("k5", 5);
        cache.put("k6", 6);
        assertEquals(4, cache.size());
    }

    @Test
    void shrinkBelowCurrentSizeIsANoop()
    {
        LRULFUCache<String, Integer> cache = new LRULFUCache<>(4);
        cache.put("a", 1);
        cache.put("b", 2);

        assertEquals(0, cache.shrink(2));
        assertEquals(0, cache.shrink(99));
        assertEquals(2, cache.size());
    }

    @Test
    void shrinkPurgesExpiredEntriesBeforeCountingEvictions() throws Exception
    {
        LRULFUCache<String, Integer> cache = new LRULFUCache<>(4, 1, TimeUnit.MILLISECONDS);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.put("d", 4);
        Thread.sleep(5);   // everything is expired

        assertEquals(0, cache.shrink(2), "expired entries are purged, not evicted");
        assertEquals(0, cache.size());
    }

    // -----------------------------------------------------------------
    // TinyLFUCache: window first, then main victims; no re-admission
    // -----------------------------------------------------------------

    @Test
    void shrinkReducesTinyLFUAndLetsItRegrow()
    {
        TinyLFUCache<String, Integer> cache = new TinyLFUCache<>(10);
        for (int i = 0; i < 10; i++)
        {
            cache.put("k" + i, i);
        }
        int evicted = cache.shrink(3);
        assertTrue(evicted >= 7, "all the excess entries must be evicted, got " + evicted);
        assertTrue(cache.size() <= 3);

        // re-grows towards the original capacity
        for (int i = 10; i < 20; i++)
        {
            cache.put("k" + i, i);
        }
        assertTrue(cache.size() <= 10);
        assertTrue(cache.size() >= 3, "the cache must accept new entries after a shrink");
    }

    @Test
    void shrinkTargetsBelowSizeEvictNothing()
    {
        TinyLFUCache<String, Integer> cache = new TinyLFUCache<>(10);
        cache.put("a", 1);
        assertEquals(0, cache.shrink(5));
        assertEquals(1, cache.size());
    }

    // -----------------------------------------------------------------
    // ARCCache: evictions become ghosts, adaptation is preserved
    // -----------------------------------------------------------------

    @Test
    void arcShrinkTurnsEvictedEntriesIntoGhostsThatCanBeReadmitted()
    {
        ARCCache<String, Integer> cache = new ARCCache<>(4);
        cache.put("k1", 1);
        cache.put("k2", 2);
        cache.put("k3", 3);
        cache.put("k4", 4);
        cache.get("k1");   // k1 moves to T2; T1 holds k2,k3,k4 (MRU first)

        assertEquals(2, cache.shrink(2));
        assertEquals(2, cache.size());
        // the LRU entries of T1 (k2, k3) are evicted; k4 (MRU of T1) and k1
        // (in T2) survive
        assertFalse(cache.containsKey("k2"));
        assertFalse(cache.containsKey("k3"));
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsKey("k4"));

        // a ghost hit readmits the entry and keeps the adaptive machinery happy
        Integer readmitted = cache.get("k3", k -> 30);
        assertEquals(30, readmitted);
        assertTrue(cache.containsKey("k3"));

        // the configured capacity is untouched
        cache.put("k5", 5);
        cache.put("k6", 6);
        assertTrue(cache.size() <= 4);
    }

    // -----------------------------------------------------------------
    // HashMapCache: arbitrary (iteration-order) eviction
    // -----------------------------------------------------------------

    @Test
    void hashMapShrinkEvictsToTheTargetInArbitraryOrder()
    {
        HashMapCache<String, Integer> cache = new HashMapCache<>(8, Long.MAX_VALUE);
        for (int i = 0; i < 5; i++)
        {
            cache.put("k" + i, i);
        }
        assertEquals(3, cache.shrink(2));
        assertEquals(2, cache.size());
        assertEquals(0, cache.shrink(2));
    }

    // -----------------------------------------------------------------
    // Synchronized wrapper and NullCache
    // -----------------------------------------------------------------

    @Test
    void shrinkWorksThroughTheSynchronizedWrapper()
    {
        LRULFUCache<String, Integer> cache = new LRULFUCache<>(4);
        for (int i = 0; i < 4; i++)
        {
            cache.put("k" + i, i);
        }
        Cache<String, Integer> wrapped = cache.synchronizedCache();

        assertEquals(2, wrapped.shrink(2));
        assertEquals(2, wrapped.size());
        assertEquals(2, cache.size(), "the wrapper delegates to the same instance");
    }

    @Test
    void nullCacheShrinkIsANoop()
    {
        NullCache<String, Integer> cache = new NullCache<>();
        assertEquals(0, cache.shrink(0));
    }

    // -----------------------------------------------------------------
    // PersistentCache: only the memory tier shrinks, the store is intact
    // -----------------------------------------------------------------

    /** Minimal in-memory store for testing the tier split. */
    private static class StubStore implements CacheStore<String, Integer>
    {
        private final Map<String, long[]> expirations = new HashMap<>();
        private final Map<String, Integer> values = new HashMap<>();

        @Override
        public Integer get(String key)
        {
            long[] exp = expirations.get(key);
            if (exp == null || (exp[0] != Long.MAX_VALUE && System.currentTimeMillis() >= exp[0]))
            {
                return null;
            }
            return values.get(key);
        }

        @Override
        public void put(String key, Integer value, long expiresAtEpochMillis)
        {
            values.put(key, value);
            expirations.put(key, new long[]{expiresAtEpochMillis});
        }

        @Override
        public void remove(String key)
        {
            values.remove(key);
            expirations.remove(key);
        }

        @Override
        public boolean containsKey(String key)
        {
            return get(key) != null;
        }

        @Override
        public long size()
        {
            purgeExpired();
            return values.size();
        }

        @Override
        public void clear()
        {
            values.clear();
            expirations.clear();
        }

        @Override
        public void purgeExpired()
        {
            long now = System.currentTimeMillis();
            expirations.entrySet().removeIf(e ->
                    e.getValue()[0] != Long.MAX_VALUE && now >= e.getValue()[0]);
            values.keySet().retainAll(expirations.keySet());
        }

        @Override
        public void flush()
        {
        }

        @Override
        public void close()
        {
        }
    }

    @Test
    void persistentCacheShrinksOnlyTheMemoryTier()
    {
        StubStore store = new StubStore();
        PersistentCache<String, Integer> cache = new PersistentCache<>(
                new LRULFUCache<>(8, Long.MAX_VALUE), store, Long.MAX_VALUE);

        for (int i = 0; i < 5; i++)
        {
            cache.put("k" + i, i);
        }
        assertEquals(5, cache.size());

        assertEquals(3, cache.shrink(2));
        assertEquals(2, cache.getMemory().size(), "the memory tier shrank");
        assertEquals(5, store.size(), "the durable store is the source of truth and never shrinks");
        assertEquals(5, cache.size(), "PersistentCache.size() reports the store contents");

        // a dropped key is restored from the store on the next miss
        assertEquals(2, cache.get("k2"));
        assertEquals(3, cache.getMemory().size(), "the restored entry is back in the memory tier");
    }
}
