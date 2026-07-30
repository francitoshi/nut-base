/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

public class HashMapCacheTest
{

    @Test
    public void testBasicOperations()
    {
        Cache<Integer, String> cache = new HashMapCache<>();
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

        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertNull(cache.get(1));
    }

    @Test
    public void testNullKeysAndValues()
    {
        Cache<Integer, String> cache = new HashMapCache<>();

        // Test null key
        try
        {
            cache.put(null, "nullKey");
            assertEquals(1, cache.size());
            assertEquals("nullKey", cache.get(null));
        }
        catch (Exception e)
        {
            fail("HashMapCache failed to support null keys: " + e.getMessage());
        }

        // Test null value
        try
        {
            cache.put(1, null);
            // Size should be 2 (null key + key 1)
            assertEquals(2, cache.size());
            assertNull(cache.get(1));
        }
        catch (Exception e)
        {
            fail("HashMapCache failed to support null values: " + e.getMessage());
        }
    }

    @Test
    public void testGetOrCreate()
    {
        Cache<Integer, String> cache = new HashMapCache<>();
        String val = cache.get(1, k -> "computed-" + k);
        assertEquals("computed-1", val);
        assertEquals("computed-1", cache.get(1));

        // Test that get-or-create on a null-valued entry does not invoke the creator again
        cache.put(2, null);
        String val2 = cache.get(2, k -> "fallback");
        assertNull(val2);
        assertNull(cache.get(2));
    }

    @Test
    public void testExpiration() throws InterruptedException
    {
        // Cache with 50 milliseconds TTL
        Cache<Integer, String> cache = new HashMapCache<>(16, 50, TimeUnit.MILLISECONDS);
        cache.put(1, "one");
        assertEquals("one", cache.get(1));
        assertEquals(1, cache.size());

        // Wait for it to expire
        Thread.sleep(70);

        assertNull(cache.get(1));
        assertEquals(0, cache.size());
    }

    @Test
    public void testPurgeExpired() throws InterruptedException
    {
        Cache<Integer, String> cache = new HashMapCache<>(16, 50, TimeUnit.MILLISECONDS);
        cache.put(1, "one");
        cache.put(2, "two");
        assertEquals(2, cache.size());

        Thread.sleep(70);

        // Explicitly purge expired entries
        cache.purgeExpired();
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testSynchronizedCache()
    {
        Cache<Integer, String> cache = new HashMapCache<>();
        Cache<Integer, String> syncCache = cache.synchronizedCache();
        assertNotNull(syncCache);

        syncCache.put(1, "one");
        assertEquals("one", syncCache.get(1));
        assertEquals(1, syncCache.size());
    }

    @Test
    public void testNullValues()
    {
        Cache<Integer, String> cache = new HashMapCache<>();

        // 1. Test put(key, null)
        try
        {
            cache.put(1, null);
            assertEquals(1, cache.size());
            assertNull(cache.get(1)); // Must return the stored null value
        }
        catch (Exception e)
        {
            fail("HashMapCache failed when storing/retrieving a null value via put(): " + e.getMessage());
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
            fail("HashMapCache failed when handling a null value via get(key, Function): " + e.getMessage());
        }

        final AtomicInteger i = new AtomicInteger();
        cache.get(3, (x) -> {i.incrementAndGet(); return null;});
        assertEquals(1, i.get());
        cache.get(3, (x) -> {i.incrementAndGet(); return null;});
        assertEquals(1, i.get());
    }
}
