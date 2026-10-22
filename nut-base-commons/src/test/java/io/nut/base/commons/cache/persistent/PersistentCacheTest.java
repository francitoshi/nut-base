/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.cache.persistent;

import io.nut.base.commons.cache.Cache;
import io.nut.base.commons.cache.CacheType;
import io.nut.base.commons.serializer.BasicSerializers;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

public class PersistentCacheTest
{

    @TempDir
    Path dir;

    private Path file()
    {
        return dir.resolve("cache.bin");
    }

    private PersistentCache<String, Long> open(CacheType type, int capacity, long ttl, TimeUnit unit)
    {
        return PersistentCache.open(file(), type, capacity, ttl, unit,
                BasicSerializers.STRING, BasicSerializers.LONG);
    }

    @Test
    public void testWriteThroughAndReopen()
    {
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            assertTrue(cache.isEmpty());
            cache.put("a", 1L);
            cache.put("b", 2L);
            assertEquals(2, cache.size());
            assertEquals(1L, cache.get("a"));
            assertEquals(2L, cache.get("b"));
            assertTrue(cache.containsKey("a"));
        }
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            assertEquals(2, cache.size());
            assertEquals(1L, cache.get("a"));
            assertEquals(2L, cache.get("b"));
            assertNull(cache.get("c"));
            assertFalse(cache.containsKey("c"));
        }
    }

    @Test
    public void testGetOrCreatePopulatesDisk()
    {
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            assertEquals(10L, cache.get("k", k -> 10L));
            assertEquals(10L, cache.get("k", k ->
            {
                fail("creator should not be invoked on memory hit");
                return null;
            }));
        }
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            assertEquals(10L, cache.get("k", k ->
            {
                fail("creator should not be invoked on store hit");
                return null;
            }));
        }
    }

    @Test
    public void testCreatorOnlyOnBothTierMiss()
    {
        AtomicInteger calls = new AtomicInteger();
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            Long v = cache.get("k", k ->
            {
                calls.incrementAndGet();
                return 42L;
            });
            assertEquals(42L, v);
            cache.get("k", k ->
            {
                calls.incrementAndGet();
                return 43L;
            });
            assertEquals(1, calls.get());
        }
    }

    @Test
    public void testCreatorSkippedWhenMemoryEvictedButStoreHasEntry()
    {
        AtomicInteger calls = new AtomicInteger();
        try (PersistentCache<String, Long> cache = open(CacheType.ARC, 2, 1, TimeUnit.HOURS))
        {
            cache.put("k1", 1L);
            cache.put("k2", 2L);
            cache.put("k3", 3L);
            Long v = cache.get("k1", k ->
            {
                calls.incrementAndGet();
                return 99L;
            });
            assertEquals(1L, v);
            assertEquals(0, calls.get());
        }
    }

    @Test
    public void testTtlExpiresAcrossRestart() throws InterruptedException
    {
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 50, TimeUnit.MILLISECONDS))
        {
            cache.put("k", 1L);
            assertEquals(1L, cache.get("k"));
        }
        Thread.sleep(150);
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 50, TimeUnit.MILLISECONDS))
        {
            assertNull(cache.get("k"));
            assertFalse(cache.containsKey("k"));
            assertTrue(cache.isEmpty());
        }
    }

    @Test
    public void testRemainingTtlSurvivesRestart()
    {
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            cache.put("k", 7L);
        }
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            assertEquals(7L, cache.get("k"));
        }
    }

    @Test
    public void testNoExpiration()
    {
        try (PersistentCache<String, Long> cache = PersistentCache.open(file(), CacheType.HASH_MAP, 16,
                Long.MAX_VALUE, BasicSerializers.STRING, BasicSerializers.LONG))
        {
            cache.put("k", 1L);
        }
        try (PersistentCache<String, Long> cache = PersistentCache.open(file(), CacheType.HASH_MAP, 16,
                Long.MAX_VALUE, BasicSerializers.STRING, BasicSerializers.LONG))
        {
            assertEquals(1L, cache.get("k"));
        }
    }

    @Test
    public void testAllCacheTypes()
    {
        for (CacheType type : CacheType.values())
        {
            Path file = dir.resolve("cache-" + type + ".bin");
            try (PersistentCache<String, Long> cache = PersistentCache.open(file, type, 64, 1, TimeUnit.HOURS,
                    BasicSerializers.STRING, BasicSerializers.LONG))
            {
                cache.put(type.name(), 1L);
                assertEquals(1L, cache.get(type.name()));
            }
            try (PersistentCache<String, Long> cache = PersistentCache.open(file, type, 64, 1, TimeUnit.HOURS,
                    BasicSerializers.STRING, BasicSerializers.LONG))
            {
                assertEquals(1L, cache.get(type.name()));
                assertEquals(1, cache.size());
            }
        }
    }

    @Test
    public void testSynchronizedCache()
    {
        try (PersistentCache<String, Long> persistent = open(CacheType.TINY_LFU, 64, 1, TimeUnit.HOURS))
        {
            Cache<String, Long> cache = persistent.synchronizedCache();
            cache.put("a", 1L);
            cache.put("b", 2L);
            assertEquals(2, cache.size());
            assertEquals(1L, cache.get("a"));
            assertEquals(2L, cache.get("b", k -> 2L));
            assertTrue(cache.containsKey("b"));
            cache.purgeExpired();
            assertEquals(2, cache.size());
            cache.clear();
            assertTrue(cache.isEmpty());
        }
    }

    @Test
    public void testSizeAndClear()
    {
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            assertTrue(cache.isEmpty());
            cache.put("a", 1L);
            cache.put("b", 2L);
            assertEquals(2, cache.size());
            assertFalse(cache.isEmpty());
            cache.clear();
            assertTrue(cache.isEmpty());
            assertEquals(0, cache.size());
            assertNull(cache.get("a"));
        }
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            assertTrue(cache.isEmpty());
        }
    }

    @Test
    public void testNullValueWithHashMap()
    {
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            cache.put("k", null);
            assertTrue(cache.containsKey("k"));
            assertNull(cache.get("k"));
            assertEquals(1, cache.size());
        }
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            assertTrue(cache.containsKey("k"));
            assertNull(cache.get("k"));
        }
    }

    @Test
    public void testIntegerKeysAndStringValues()
    {
        Path file = dir.resolve("int-string.bin");
        try (PersistentCache<Integer, String> cache = PersistentCache.open(file, CacheType.LRU_LFU, 64, 1, TimeUnit.HOURS,
                BasicSerializers.INTEGER, BasicSerializers.STRING))
        {
            cache.put(1, "one");
            cache.put(2, "two");
        }
        try (PersistentCache<Integer, String> cache = PersistentCache.open(file, CacheType.LRU_LFU, 64, 1, TimeUnit.HOURS,
                BasicSerializers.INTEGER, BasicSerializers.STRING))
        {
            assertEquals("one", cache.get(1));
            assertEquals("two", cache.get(2));
        }
    }

    @Test
    public void testRemoveFromBothTiers()
    {
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            cache.put("a", 1L);
            cache.put("b", 2L);

            cache.remove("a");
            assertNull(cache.get("a"));
            assertFalse(cache.containsKey("a"));
            assertEquals(1, cache.size());
            assertEquals(2L, cache.get("b"));

            cache.remove("missing");
            assertEquals(1, cache.size());
        }
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            assertNull(cache.get("a"));
            assertFalse(cache.containsKey("a"));
            assertEquals(2L, cache.get("b"));
            assertEquals(1, cache.size());
        }
    }

    @Test
    public void testFlush()
    {
        try (PersistentCache<String, Long> cache = open(CacheType.HASH_MAP, 16, 1, TimeUnit.HOURS))
        {
            cache.put("a", 1L);
            cache.flush();
            assertEquals(1L, cache.get("a"));
        }
    }
}
