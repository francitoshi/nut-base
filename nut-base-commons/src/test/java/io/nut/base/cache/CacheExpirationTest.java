/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cache;

import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class CacheExpirationTest
{
    private static final long TTL_MS = 50;
    private static final long TTL_NANOS = TimeUnit.MILLISECONDS.toNanos(TTL_MS);

    @Test
    public void testHashMapCacheExpiration() throws InterruptedException
    {
        Cache<Integer, String> cache = new HashMapCache<>(10, TTL_NANOS);
        runExpirationTest(cache);
    }

    @Test
    public void testARCCacheExpiration() throws InterruptedException
    {
        Cache<Integer, String> cache = new ARCCache<>(10, TTL_NANOS);
        runExpirationTest(cache);
    }

    @Test
    public void testLRULFUCacheExpiration() throws InterruptedException
    {
        Cache<Integer, String> cache = new LRULFUCache<>(10, TTL_NANOS);
        runExpirationTest(cache);
    }

    @Test
    public void testTinyLFUCacheExpiration() throws InterruptedException
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(10, TTL_NANOS);
        runExpirationTest(cache);
    }

    @Test
    public void testFactoryExpiration() throws InterruptedException
    {
        for (CacheType type : CacheType.values())
        {
            Cache<Integer, String> cache = CacheFactory.getInstance(type, 10, TTL_NANOS);
            runExpirationTest(cache);
        }
    }

    @Test
    public void testNullValueCaching() throws InterruptedException
    {
        Cache<Integer, String> cache = new HashMapCache<>(10, TTL_NANOS);
        cache.put(1, null);
        
        // Before expiration, we should get null (which is the actual cached value)
        assertNull(cache.get(1));
        assertEquals(1, cache.size());

        // Wait for expiration
        Thread.sleep(TTL_MS * 2);

        // After expiration, get(1) will remove the expired item and return null
        assertNull(cache.get(1));
        assertEquals(0, cache.size());
    }

    @Test
    public void testPurgeExpired() throws InterruptedException
    {
        for (CacheType type : CacheType.values())
        {
            Cache<Integer, String> cache = CacheFactory.getInstance(type, 10, TTL_NANOS);
            cache.put(1, "one");
            cache.put(2, "two");

            // Wait for expiration
            Thread.sleep(TTL_MS * 2);

            // Call purgeExpired
            cache.purgeExpired();

            // The cache should be empty
            assertEquals(0, cache.size());
            assertNull(cache.get(1));
            assertNull(cache.get(2));
        }
    }

    private void runExpirationTest(Cache<Integer, String> cache) throws InterruptedException
    {
        cache.put(1, "one");
        cache.put(2, "two");

        // Immediately after putting, they should be present
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals(2, cache.size());

        // Wait for expiration
        Thread.sleep(TTL_MS * 2);

        // They should be gone
        assertNull(cache.get(1));
        assertNull(cache.get(2));
        assertEquals(0, cache.size());
    }
}
