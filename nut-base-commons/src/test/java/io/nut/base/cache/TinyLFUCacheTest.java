/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TinyLFUCacheTest
{
    @Test
    public void testMain()
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(3);

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Access key 1 multiple times - increases frequency
        cache.get(1);
        cache.get(1);
        cache.get(1);

        // Access key 2 once
        cache.get(2);

        // Add new item - should evict key 3 (lowest frequency, least recent)
        cache.put(4, "four");

        assertNotNull(cache.get(1));    // "one" - kept due to high frequency
        assertNotNull(cache.get(2));    // "two" - kept
        assertNull(cache.get(3));       // null - evicted
        assertNotNull(cache.get(4));    // "four" - new entry
    }

    @Test
    public void testGet()
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(3);
        assertNull(cache.get(1));

        cache.put(1, "one");
        assertNotNull(cache.get(1));
    }

    @Test
    public void testSize()
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(3);
        assertEquals(0, cache.size());

        cache.put(1, "one");
        assertEquals(1, cache.size());
    }

    @Test
    public void testClear()
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(3);
        cache.put(1, "one");
        assertEquals(1, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    public void testIsEmpty()
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(3);
        assertTrue(cache.isEmpty());

        cache.put(1, "one");
        assertFalse(cache.isEmpty());

        cache.clear();
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testNullValues()
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(2);

        // 1. Test put(key, null)
        try
        {
            cache.put(1, null);
            assertEquals(1, cache.size());
            assertNull(cache.get(1)); // Must return the stored null value
        }
        catch (Exception e)
        {
            fail("TinyLFUCache failed when storing/retrieving a null value via put(): " + e.getMessage());
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
            fail("TinyLFUCache failed when handling a null value via get(key, Function): " + e.getMessage());
        }

        final AtomicInteger i = new AtomicInteger();
        cache.get(3, (x) -> {i.incrementAndGet(); return null;});
        assertEquals(1, i.get());
        cache.get(3, (x) -> {i.incrementAndGet(); return null;});
        assertEquals(1, i.get());
    }
}
