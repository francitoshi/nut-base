/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ARCCacheTest
{

    @Test
    public void testBasicOperations()
    {
        Cache<Integer, String> cache = new ARCCache<>(2);
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        cache.put(1, "one");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());
        assertEquals("one", cache.get(1));

        cache.put(2, "two");
        assertEquals(2, cache.size());
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
    }

    @Test
    public void testARCEvictionAndAdaptation()
    {
        Cache<Integer, String> cache = new ARCCache<>(2);

        // 1. Fill cache to capacity
        cache.put(1, "one");
        cache.put(2, "two");

        // Move 1 to T2 by accessing it
        assertEquals("one", cache.get(1));
        // State now: T1: [2], T2: [1], B1: [], B2: []

        // 2. Put a new item (3) -> should evict LRU of T1 (which is 2) to B1
        cache.put(3, "three");

        assertEquals(2, cache.size());
        assertNull(cache.get(2)); // 2 is evicted (in B1)
        assertEquals("one", cache.get(1)); // 1 is kept (moves to MRU of T2)
        assertEquals("three", cache.get(3)); // 3 is kept (moves to MRU of T2)
        // State now: T1: [], T2: [3, 1], B1: [2], B2: []

        // 3. Put 2 again (hit in B1, ghost hit) -> should adjust p to favor recency
        // This triggers replace, evicting LRU of T2 (which is 1, since 3 was accessed after 1) to B2
        cache.put(2, "two-new");

        assertEquals(2, cache.size());
        assertEquals("three", cache.get(3)); // 3 is kept
        assertEquals("two-new", cache.get(2)); // 2 is readmitted (in T2)
        assertNull(cache.get(1)); // 1 is evicted to B2

        // State now: T1: [], T2: [2, 3], B1: [], B2: [1], p = 1.0
        // 4. Put 1 again (hit in B2, ghost hit) -> should adjust p to favor frequency
        // This triggers replace, evicting LRU of T2 (which is 3) to B2
        cache.put(1, "one-new");

        assertEquals(2, cache.size());
        assertEquals("one-new", cache.get(1)); // 1 is readmitted
        assertEquals("two-new", cache.get(2)); // 2 is kept
        assertNull(cache.get(3)); // 3 is evicted to B2
    }

    @Test
    public void testClear()
    {
        Cache<Integer, String> cache = new ARCCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        assertEquals(2, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
        assertNull(cache.get(1));
    }

    @Test
    public void testGetOrCreate()
    {
        Cache<Integer, String> cache = new ARCCache<>(2);
        String val = cache.get(1, k -> "computed-" + k);
        assertEquals("computed-1", val);
        assertEquals("computed-1", cache.get(1));
    }

    @Test
    public void testSynchronizedCache()
    {
        Cache<Integer, String> cache = new ARCCache<>(2);
        Cache<Integer, String> syncCache = cache.synchronizedCache();
        assertNotNull(syncCache);

        syncCache.put(1, "one");
        assertEquals("one", syncCache.get(1));
        assertEquals(1, syncCache.size());
    }

    @Test
    public void testInvalidCapacity()
    {
        assertThrows(IllegalArgumentException.class, () -> new ARCCache<>(0));
        assertThrows(IllegalArgumentException.class, () -> new ARCCache<>(-5));
    }

    @Test
    public void testNullValues()
    {
        Cache<Integer, String> cache = new ARCCache<>(2);

        // 1. Test put(key, null)
        try
        {
            cache.put(1, null);
            assertEquals(1, cache.size());
            assertNull(cache.get(1)); // Must return the stored null value
        }
        catch (Exception e)
        {
            fail("ARCCache failed when storing/retrieving a null value via put(): " + e.getMessage());
        }

        // 2. Test get(key, Function) returning null
        try
        {
            String val = cache.get(2, k -> null);
            assertNull(val);
            assertNull(cache.get(2));
        }
        catch (Exception e)
        {
            fail("ARCCache failed when handling a null value via get(key, Function): " + e.getMessage());
        }
        
        final AtomicInteger i = new AtomicInteger();
        cache.get(3, (x) -> {i.incrementAndGet(); return null;});
        assertEquals(1, i.get());
        cache.get(3, (x) -> {i.incrementAndGet(); return null;});
        assertEquals(1, i.get());
        
    }
}
