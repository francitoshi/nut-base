/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import io.github.francitoshi.nut.base.commons.cache.Cache;
import io.github.francitoshi.nut.base.commons.cache.CacheFactory;
import io.github.francitoshi.nut.base.commons.cache.CacheType;
import io.github.francitoshi.nut.base.commons.cache.persistent.PersistentCache;
import io.github.francitoshi.nut.base.commons.serializer.BasicSerializers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of EncryptedBitcaskCacheStore: cache-store contract parity, the
 * password handling, the encrypted-on-disk guarantee, crash recovery of the
 * encrypted data files and the PersistentCache wiring.
 */
public class EncryptedBitcaskCacheStoreTest
{
    @TempDir
    Path temp;

    private static final char[] PASSWORD = "correct horse battery staple".toCharArray();

    private Path cacheDir()
    {
        return temp.resolve("cache"); // @TempDir is per-method, isolation is given
    }

    private EncryptedBitcaskCacheStore<String, String> store()
    {
        return new EncryptedBitcaskCacheStore<>(cacheDir(), PASSWORD,
            BasicSerializers.STRING, BasicSerializers.STRING, 2000, 300);
    }

    private EncryptedBitcaskCacheStore<String, String> store(Path directory)
    {
        return new EncryptedBitcaskCacheStore<>(directory, PASSWORD,
            BasicSerializers.STRING, BasicSerializers.STRING, 2000, 300);
    }

    @Test
    public void testPutGetOverwrite()
    {
        try (EncryptedBitcaskCacheStore<String, String> store = store())
        {
            store.put("one", "1", Long.MAX_VALUE);
            store.put("two", "2", Long.MAX_VALUE);
            assertEquals(2, store.size());
            assertEquals("1", store.get("one"));
            assertEquals("2", store.get("two"));
            assertNull(store.get("three"));

            store.put("one", "1-again", Long.MAX_VALUE);
            assertEquals(2, store.size()); // one + two; "one" got replaced
            assertEquals("1-again", store.get("one"));
            assertTrue(store.getTotalBytes() > store.getLiveBytes(), "dead record(s) remain");
        }
    }

    @Test
    public void testNullValueAndNullKey()
    {
        try (EncryptedBitcaskCacheStore<String, String> store = store())
        {
            store.put("k", null, Long.MAX_VALUE);
            assertTrue(store.containsKey("k"));
            assertNull(store.get("k"));
            assertEquals(1, store.size());

            store.put(null, "nullKey", Long.MAX_VALUE);
            assertTrue(store.containsKey(null));
            assertEquals("nullKey", store.get(null));
            assertEquals(2, store.size());
        }

        try (EncryptedBitcaskCacheStore<String, String> store = store())
        {
            assertTrue(store.containsKey("k"));
            assertNull(store.get("k"));
            assertTrue(store.containsKey(null));
            assertEquals("nullKey", store.get(null));
            assertEquals(2, store.size());
        }
    }

    @Test
    public void testExpiryAndPurge()
    {
        try (EncryptedBitcaskCacheStore<String, String> store = store())
        {
            long past = System.currentTimeMillis() - 1000;
            store.put("expired", "v", past);
            store.put("live", "v", Long.MAX_VALUE);
            assertEquals(1, store.size());
            assertNull(store.get("expired"));
            assertFalse(store.containsKey("expired"));

            store.purgeExpired();
            assertEquals(1, store.size());
            assertTrue(store.containsKey("live"));
        }

        try (EncryptedBitcaskCacheStore<String, String> store = store())
        {
            assertEquals(1, store.size());
            assertEquals("v", store.get("live"));
            assertFalse(store.containsKey("expired"));
        }
    }

    @Test
    public void testExpiryCanNeverFireEarly()
    {
        try (EncryptedBitcaskCacheStore<String, String> store = store())
        {
            long soon = System.currentTimeMillis() + 900;
            store.put("soon", "v", soon);
            assertTrue(store.containsKey("soon"));
            assertEquals("v", store.get("soon"));
        }
    }

    @Test
    public void testPersistenceAndReopen()
    {
        Path directory = cacheDir();

        try (EncryptedBitcaskCacheStore<String, String> store = store(directory))
        {
            store.put("a", "alpha", Long.MAX_VALUE);
            store.put("b", null, Long.MAX_VALUE);
        }

        try (EncryptedBitcaskCacheStore<String, String> store = store(directory))
        {
            assertEquals(2, store.size());
            assertEquals("alpha", store.get("a"));
            assertNull(store.get("b"));
            assertTrue(store.containsKey("b")); // null value survives the reopen
        }
    }

    @Test
    public void wrongPasswordIsRejected()
    {
        Path directory = cacheDir();

        try (EncryptedBitcaskCacheStore<String, String> store = store(directory))
        {
            store.put("secret", "value", Long.MAX_VALUE);
        }

        try
        {
            new EncryptedBitcaskCacheStore<>(directory, new char[]{'w', 'r', 'o', 'n', 'g'},
                BasicSerializers.STRING, BasicSerializers.STRING, 2000, 300).close();
            throw new AssertionError("the wrong password must fail the open");
        }
        catch (java.io.UncheckedIOException expected)
        {
            assertTrue(expected.getMessage().contains("wrong password"), expected.getMessage());
        }
    }

    @Test
    public void secondOpenFailsWhileLocked()
    {
        Path directory = cacheDir();

        try (EncryptedBitcaskCacheStore<String, String> store = store(directory))
        {
            store.put("k", "v", Long.MAX_VALUE);

            assertThrows(java.io.UncheckedIOException.class, () -> store(directory));

            assertEquals("v", store.get("k")); // untouched by the failed open
        }
    }

    @Test
    public void nothingClearEverReachesDisk() throws IOException
    {
        Path directory = cacheDir();

        try (EncryptedBitcaskCacheStore<String, String> store = store(directory))
        {
            for (int i = 0; i < 10; i++)
            {
                store.put("agent-" + i, "topsecret-" + i, Long.MAX_VALUE);
            }
            store.remove("agent-3");
            store.put("after", "topsecret-merged", Long.MAX_VALUE);
        } // close() merges the dead bytes away before releasing the store

        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + Bitcask.DATA_SUFFIX))
        {
            for (Path path : stream)
            {
                raw.write(Files.readAllBytes(path));
            }
        }

        String onDisk = new String(raw.toByteArray(), StandardCharsets.ISO_8859_1);
        assertFalse(onDisk.contains("agent-"), "keys must never reach the disk in the clear");
        assertFalse(onDisk.contains("topsecret-"), "values must never reach the disk in the clear");
    }

    @Test
    public void tornTailOfTheEncryptedFileRecovers() throws IOException
    {
        Path directory = cacheDir();

        try (EncryptedBitcaskCacheStore<String, String> store = store(directory))
        {
            store.put("a", "value-a", Long.MAX_VALUE);
            store.put("b", "value-b", Long.MAX_VALUE);
        }

        Path last = maxDataFile(directory);
        long validLength = Files.size(last);

        Files.write(last, new byte[]{(byte) 0x9c, 0x31, 0x00, (byte) 0xff, 0x73}, StandardOpenOption.APPEND);

        try (EncryptedBitcaskCacheStore<String, String> store = store(directory))
        {
            // the truncated tail is dropped at open time
            assertEquals(2, store.size());
            assertEquals("value-a", store.get("a"));
            assertEquals("value-b", store.get("b"));
            assertTrue(Files.size(last) <= validLength, "the partial record was truncated away");

            store.put("c", "value-c", Long.MAX_VALUE);
        }

        try (EncryptedBitcaskCacheStore<String, String> store = store(directory))
        {
            assertEquals(3, store.size());
            assertEquals("value-c", store.get("c"));
        }
    }

    @Test
    public void clearLeavesAnEmptyEncryptedStore() throws IOException
    {
        Path directory = cacheDir();

        try (EncryptedBitcaskCacheStore<String, String> store = store(directory))
        {
            store.put("gone", "value", Long.MAX_VALUE);
            store.clear();
            assertEquals(0, store.size());
            assertNull(store.get("gone"));
            // the encrypted profile writes a 29-byte header in the new active file
            assertEquals(EncryptedBitcask.DATA_HEADER_SIZE, store.getTotalBytes());

            store.put("future", "value", Long.MAX_VALUE);
        }

        try (EncryptedBitcaskCacheStore<String, String> store = store(directory))
        {
            assertEquals(1, store.size());
            assertEquals("value", store.get("future"));
        }
    }

    @Test
    public void persistentCacheWiringWithWrongPasswordGuards()
    {
        Path directory = cacheDir();

        try (PersistentCache<String, String> cache = encryptedCacheOf(directory, 1, TimeUnit.HOURS, PASSWORD))
        {
            cache.put("one", "1");
            cache.put("two", "2");

            assertEquals("created", cache.get("creator", k -> "created")); // both tiers
            assertTrue(containsGuard(cache));

            cache.remove("one");
            assertNull(cache.get("one"));
        }

        try (PersistentCache<String, String> cache = encryptedCacheOf(directory, 1, TimeUnit.HOURS, PASSWORD))
        {
            assertEquals(2, cache.size());
            assertEquals("2", cache.get("two"));
            assertEquals("created", cache.get("creator")); // restored via the store
        }

        try (PersistentCache<String, String> cache = encryptedCacheOf(directory, 1, TimeUnit.HOURS, new char[]{'x'}))
        {
            throw new AssertionError("a wrong password must not open the cache");
        }
        catch (java.io.UncheckedIOException expected)
        {
            // expected
        }
    }

    private boolean containsGuard(PersistentCache<String, String> cache)
    {
        return cache.containsKey("creator");
    }

    private PersistentCache<String, String> encryptedCacheOf(Path directory, long ttl, TimeUnit unit, char[] password)
    {
        EncryptedBitcaskCacheStore<String, String> store = new EncryptedBitcaskCacheStore<>(directory, password,
            BasicSerializers.STRING, BasicSerializers.STRING, 2000, 300);
        Cache<String, String> memory = CacheFactory.getInstance(CacheType.ARC, 128, ttl, unit);
        return new PersistentCache<>(memory, store, unit.toNanos(ttl));
    }

    private Path maxDataFile(Path directory)
    {
        Path max = null;
        long maxId = -1;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + Bitcask.DATA_SUFFIX))
        {
            for (Path path : stream)
            {
                String name = path.getFileName().toString();
                long id = Long.parseLong(name.substring(0, name.length() - Bitcask.DATA_SUFFIX.length()));

                if (id > maxId)
                {
                    maxId = id;
                    max = path;
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return max;
    }

    private long fileSize(Path file)
    {
        try
        {
            return Files.size(file);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
