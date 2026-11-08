/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import io.github.francitoshi.nut.base.commons.cache.CacheType;
import io.github.francitoshi.nut.base.commons.cache.persistent.PersistentCache;
import io.github.francitoshi.nut.base.commons.serializer.BasicSerializers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The usage the store was built for: BitcaskCacheStore as the durable tier
 * of {@link PersistentCache}, wired exactly like the FileCacheStore variants
 * but backed by the Bitcask directory. Covers the two-tier behavior, the
 * common get-or-create path and the Bitcask reopen advantage: hint files for
 * a replay-free startup.
 */
public class BitcaskPersistentCacheTest
{
    @TempDir
    Path temp;

    private Path cacheDir()
    {
        return temp.resolve("cache-" + System.nanoTime());
    }

    private PersistentCache<String, String> cacheOf(Path directory, long ttl, TimeUnit unit)
    {
        return BitcaskCacheStore.open(directory, CacheType.ARC, 128, ttl, unit, BasicSerializers.STRING, BasicSerializers.STRING);
    }

    @Test
    public void twoTiersWorkTogether()
    {
        try (PersistentCache<String, String> cache = cacheOf(cacheDir(), 1, TimeUnit.HOURS))
        {
            cache.put("one", "1");
            cache.put("two", "2");

            assertEquals("1", cache.get("one"));      // served from memory
            assertEquals("2", cache.get("two"));
            assertEquals(2, cache.size());             // the store is authoritative

            cache.remove("one");
            assertNull(cache.get("one"));
            assertFalse(cache.containsKey("one"));
        }
    }

    @Test
    public void getOrCreateFillsBothTiers()
    {
        try (PersistentCache<String, String> cache = cacheOf(cacheDir(), 1, TimeUnit.HOURS))
        {
            assertEquals("created", cache.get("k", k -> "created"));

            // second call: the creator must not run again
            assertEquals("created", cache.get("k", k -> { throw new IllegalStateException("must not run"); }));
        }
    }

    @Test
    public void survivesTheRestartThroughTheStore()
    {
        Path directory = cacheDir();

        try (PersistentCache<String, String> cache = cacheOf(directory, 1, TimeUnit.HOURS))
        {
            for (int i = 0; i < 20; i++)
            {
                cache.put("element-" + i, "value-" + i);
            }
            cache.remove("element-0");
        }

        try (PersistentCache<String, String> cache = cacheOf(directory, 1, TimeUnit.HOURS))
        {
            assertEquals(19, cache.size());
            assertNull(cache.get("element-0"));
            assertEquals("value-7", cache.get("element-7")); // restored: store to memory
        }
    }

    @Test
    public void expiryPurgesAcrossTiers() throws InterruptedException
    {
        try (PersistentCache<String, String> cache = cacheOf(cacheDir(), 1, TimeUnit.SECONDS))
        {
            cache.put("gone", "value");

            assertEquals("value", cache.get("gone"));

            // the expiry is the next whole second after the expiry instant
            // plus one: waiting past two seconds covers the worst rounding
            Thread.sleep(2500);

            cache.purgeExpired();
            assertNull(cache.get("gone"));
            assertFalse(cache.containsKey("gone"));
            assertEquals(0, cache.size());
        }
    }

    @Test
    public void restartOfAMergedStoreIsAlmostFree() throws IOException
    {
        Path directory = cacheDir();
        int keys = 2000;
        String payload = "horizontal";

        // 1. build a cache with lots of dead data and close: the close merge
        //    leaves hint files behind
        try (PersistentCache<String, String> cache = cacheOf(directory, 1, TimeUnit.HOURS))
        {
            for (int i = 0; i < keys; i++)
            {
                cache.put("item-" + i, valueFor(i, payload));
            }

            for (int i = 0; i < keys / 2; i++)
            {
                cache.put("item-" + i, "rewritten-" + i); // dead half
            }
        }

        assertTrue(hintCount(directory) > 0, "the close merge writes hints");

        // 2. the reopen goes through the hint files: it reads only headers
        //    plus keys, none of the values; then every value is readable
        long start = System.nanoTime();
        try (PersistentCache<String, String> cache = cacheOf(directory, 1, TimeUnit.HOURS))
        {
            long openMillis = (System.nanoTime() - start) / 1_000_000;

            assertEquals(keys, cache.size());
            assertEquals("rewritten-0", cache.get("item-0"));
            assertEquals("rewritten-99", cache.get("item-99"));
            assertEquals(valueFor(1999, payload), cache.get("item-1999"));

            if (openMillis > 50)
            {
                System.out.println("reopen of " + keys + " entries (hint driven): " + openMillis + " ms");
            }
        }
    }

    private static String valueFor(int i, String payload)
    {
        return payload + i;
    }

    private int hintCount(Path directory) throws IOException
    {
        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + Bitcask.HINT_SUFFIX))
        {
            for (Path ignored : stream)
            {
                count++;
            }
        }
        return count;
    }
}
