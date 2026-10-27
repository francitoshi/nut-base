/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.cache.persistent;

import io.github.francitoshi.nut.base.commons.serializer.BasicSerializers;
import io.github.francitoshi.nut.base.commons.serializer.Serializer;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

public class FileCacheStoreTest
{

    @TempDir
    Path dir;

    private Path file()
    {
        return dir.resolve("store.bin");
    }

    private FileCacheStore<String, String> store()
    {
        return new FileCacheStore<>(file(), BasicSerializers.STRING, BasicSerializers.STRING);
    }

    @Test
    public void testPutGet()
    {
        try (FileCacheStore<String, String> store = store())
        {
            assertTrue(store.isEmpty());
            assertEquals(0, store.size());

            store.put("one", "1", Long.MAX_VALUE);
            store.put("two", "2", Long.MAX_VALUE);
            assertEquals(2, store.size());
            assertFalse(store.isEmpty());
            assertEquals("1", store.get("one"));
            assertEquals("2", store.get("two"));
            assertNull(store.get("three"));
            assertTrue(store.containsKey("one"));
            assertFalse(store.containsKey("three"));
        }
    }

    @Test
    public void testOverwrite()
    {
        try (FileCacheStore<String, String> store = store())
        {
            store.put("k", "v1", Long.MAX_VALUE);
            store.put("k", "v2", Long.MAX_VALUE);
            assertEquals(1, store.size());
            assertEquals("v2", store.get("k"));
        }
    }

    @Test
    public void testReopen()
    {
        try (FileCacheStore<String, String> store = store())
        {
            store.put("a", "alpha", Long.MAX_VALUE);
            store.put("b", "beta", Long.MAX_VALUE);
            store.put("a", "alpha2", Long.MAX_VALUE);
        }
        try (FileCacheStore<String, String> store = store())
        {
            assertEquals(2, store.size());
            assertEquals("alpha2", store.get("a"));
            assertEquals("beta", store.get("b"));
        }
    }

    @Test
    public void testNullValueAndNullKey()
    {
        try (FileCacheStore<String, String> store = store())
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
        try (FileCacheStore<String, String> store = store())
        {
            assertTrue(store.containsKey("k"));
            assertNull(store.get("k"));
            assertEquals(2, store.size());
            assertTrue(store.containsKey(null));
            assertEquals("nullKey", store.get(null));
        }
    }

    @Test
    public void testExpiredOnRead()
    {
        try (FileCacheStore<String, String> store = store())
        {
            long past = System.currentTimeMillis() - 1000;
            store.put("expired", "v", past);
            store.put("live", "v", Long.MAX_VALUE);
            assertEquals(1, store.size());
            assertNull(store.get("expired"));
            assertFalse(store.containsKey("expired"));
            assertEquals("v", store.get("live"));
        }
    }

    @Test
    public void testPurgeExpired()
    {
        try (FileCacheStore<String, String> store = store())
        {
            long past = System.currentTimeMillis() - 1000;
            store.put("expired1", "v", past);
            store.put("expired2", "v", past);
            store.put("live", "v", Long.MAX_VALUE);
            store.purgeExpired();
            assertEquals(1, store.size());
            assertTrue(store.containsKey("live"));
        }
        try (FileCacheStore<String, String> store = store())
        {
            assertEquals(1, store.size());
            assertEquals("v", store.get("live"));
            assertFalse(store.containsKey("expired1"));
        }
    }

    @Test
    public void testClear()
    {
        try (FileCacheStore<String, String> store = store())
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);
            store.clear();
            assertTrue(store.isEmpty());
            assertEquals(0, store.size());
            assertNull(store.get("a"));
        }
        try (FileCacheStore<String, String> store = store())
        {
            assertTrue(store.isEmpty());
            assertNull(store.get("a"));
        }
    }

    @Test
    public void testTornTailIsDiscarded() throws IOException
    {
        try (FileCacheStore<String, String> store = store())
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);
        }
        long validLength = Files.size(file());
        try (RandomAccessFile raf = new RandomAccessFile(file().toFile(), "rw"))
        {
            raf.seek(validLength);
            raf.write(new byte[]{1, 0, 0});
        }
        assertTrue(Files.size(file()) > validLength);
        try (FileCacheStore<String, String> store = store())
        {
            assertEquals(2, store.size());
            assertEquals("1", store.get("a"));
            assertEquals("2", store.get("b"));
        }
        assertEquals(validLength, Files.size(file()));
    }

    @Test
    public void testCorruptRecordIsDiscarded() throws IOException
    {
        try (FileCacheStore<String, String> store = store())
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);
        }
        byte[] bytes = Files.readAllBytes(file());
        bytes[bytes.length - 6] ^= 0x7F;
        Files.write(file(), bytes);
        try (FileCacheStore<String, String> store = store())
        {
            assertEquals(1, store.size());
            assertEquals("1", store.get("a"));
            assertNull(store.get("b"));
        }
    }

    @Test
    public void testBadMagicRejected() throws IOException
    {
        Files.write(file(), new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
        assertThrows(java.io.UncheckedIOException.class, this::store);
    }

    @Test
    public void testCompaction()
    {
        String value = String.join("", java.util.Collections.nCopies(64, "x"));
        FileCacheStore<String, String> store = store();
        for (int i = 0; i < 400; i++)
        {
            store.put("k" + i, value, Long.MAX_VALUE);
        }
        store.close();
        long firstClose = file().toFile().length();

        store = store();
        for (int i = 0; i < 400; i++)
        {
            store.put("k" + i, value, Long.MAX_VALUE);
        }
        long peak = file().toFile().length();
        assertTrue(peak > firstClose * 3 / 2,
                "overwrites should leave dead records behind: " + firstClose + " -> " + peak);
        store.close();
        long compacted = file().toFile().length();
        assertTrue(compacted < peak,
                "compaction should shrink the file: " + peak + " -> " + compacted);

        try (FileCacheStore<String, String> reopened = store())
        {
            assertEquals(400, reopened.size());
            for (int i = 0; i < 400; i++)
            {
                assertEquals(value, reopened.get("k" + i));
            }
        }
    }

    @Test
    public void testBasicSerializersRoundtrip()
    {
        Path intFile = dir.resolve("int.bin");
        try (FileCacheStore<Integer, Long> store = new FileCacheStore<>(intFile,
                BasicSerializers.INTEGER, BasicSerializers.LONG))
        {
            store.put(1, 10L, Long.MAX_VALUE);
            store.put(2, 20L, Long.MAX_VALUE);
        }
        try (FileCacheStore<Integer, Long> store = new FileCacheStore<>(intFile,
                BasicSerializers.INTEGER, BasicSerializers.LONG))
        {
            assertEquals(10L, store.get(1));
            assertEquals(20L, store.get(2));
        }

        Path doubleFile = dir.resolve("double.bin");
        try (FileCacheStore<Integer, Double> store = new FileCacheStore<>(doubleFile,
                BasicSerializers.INTEGER, BasicSerializers.DOUBLE))
        {
            store.put(1, 1.5, Long.MAX_VALUE);
        }
        try (FileCacheStore<Integer, Double> store = new FileCacheStore<>(doubleFile,
                BasicSerializers.INTEGER, BasicSerializers.DOUBLE))
        {
            assertEquals(1.5, store.get(1));
        }
    }

    @Test
    public void testClosedStoreThrows()
    {
        FileCacheStore<String, String> store = store();
        store.put("a", "1", Long.MAX_VALUE);
        store.close();
        assertThrows(IllegalStateException.class, () -> store.get("a"));
        assertThrows(IllegalStateException.class, () -> store.put("b", "2", Long.MAX_VALUE));
        store.close();
    }

    @Test
    public void testSingleSerializerConstructor()
    {
        Path file = dir.resolve("single.bin");
        try (FileCacheStore<String, String> store = new FileCacheStore<>(file, BasicSerializers.STRING))
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);
            assertEquals(2, store.size());
            assertEquals("1", store.get("a"));
            assertEquals("2", store.get("b"));
        }
        try (FileCacheStore<String, String> store = new FileCacheStore<>(file, BasicSerializers.STRING))
        {
            assertEquals(2, store.size());
            assertEquals("1", store.get("a"));
            assertEquals("2", store.get("b"));
        }
    }

    @Test
    public void testSingleSerializerConstructorWithOtherType()
    {
        Path file = dir.resolve("single-int.bin");
        try (FileCacheStore<Integer, Integer> store = new FileCacheStore<>(file, BasicSerializers.INTEGER))
        {
            store.put(1, 10, Long.MAX_VALUE);
            store.put(2, 20, Long.MAX_VALUE);
            assertEquals(10, store.get(1));
            assertEquals(20, store.get(2));
        }
        try (FileCacheStore<Integer, Integer> store = new FileCacheStore<>(file, BasicSerializers.INTEGER))
        {
            assertEquals(10, store.get(1));
            assertEquals(20, store.get(2));
        }
    }

    @Test
    public void testSingleSerializerFactory()
    {
        Path file = dir.resolve("single-of.bin");
        try (FileCacheStore<String, String> store = FileCacheStore.of(file, BasicSerializers.STRING))
        {
            store.put("a", "1", Long.MAX_VALUE);
            assertEquals("1", store.get("a"));
        }
        try (FileCacheStore<String, String> store = FileCacheStore.of(file, BasicSerializers.STRING))
        {
            assertEquals("1", store.get("a"));
        }
    }

    @Test
    public void testNullSerializerRejected()
    {
        assertThrows(NullPointerException.class, () -> new FileCacheStore<>(file(), (Serializer<String>) null));
        assertThrows(NullPointerException.class, () -> FileCacheStore.of(file(), null));
    }

    @Test
    public void testTtlUnitConversion()
    {
        try (FileCacheStore<String, String> store = store())
        {
            store.put("short", "v", System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60));
            assertTrue(store.containsKey("short"));
        }
    }

    @Test
    public void testRemove()
    {
        try (FileCacheStore<String, String> store = store())
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);

            store.remove("a");
            assertNull(store.get("a"));
            assertFalse(store.containsKey("a"));
            assertEquals(1, store.size());
            assertEquals("2", store.get("b"));

            store.remove("missing");
            assertEquals(1, store.size());
        }
        try (FileCacheStore<String, String> store = store())
        {
            assertFalse(store.containsKey("a"));
            assertEquals("2", store.get("b"));
            assertEquals(1, store.size());
        }
    }

    @Test
    public void testFlush()
    {
        try (FileCacheStore<String, String> store = store())
        {
            store.put("k", "v", Long.MAX_VALUE);
            store.flush();
            assertEquals("v", store.get("k"));
        }
        try (FileCacheStore<String, String> store = store())
        {
            store.close();
            assertThrows(IllegalStateException.class, store::flush);
        }
    }
}
