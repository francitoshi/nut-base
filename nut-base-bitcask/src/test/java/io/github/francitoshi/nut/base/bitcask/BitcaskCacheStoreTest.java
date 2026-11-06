/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import io.github.francitoshi.nut.base.commons.serializer.BasicSerializers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parity tests for BitcaskCacheStore against the contract and the behavior
 * of the FileCacheStore tests, plus the Bitcask-specific parts: hint-driven
 * reopen, cross-process lock and rotation.
 */
public class BitcaskCacheStoreTest
{
    @TempDir
    Path dir;

    private Path cacheDir()
    {
        return dir.resolve("cache");
    }

    private BitcaskCacheStore<String, String> store()
    {
        return new BitcaskCacheStore<>(cacheDir(), BasicSerializers.STRING, BasicSerializers.STRING);
    }

    private BitcaskCacheStore<String, String> store(long maxFileSize)
    {
        return new BitcaskCacheStore<>(cacheDir(), BasicSerializers.STRING, BasicSerializers.STRING, maxFileSize);
    }

    @Test
    public void testPutGet()
    {
        try (BitcaskCacheStore<String, String> store = store())
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
        try (BitcaskCacheStore<String, String> store = store())
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
        try (BitcaskCacheStore<String, String> store = store())
        {
            store.put("a", "alpha", Long.MAX_VALUE);
            store.put("b", "beta", Long.MAX_VALUE);
            store.put("a", "alpha2", Long.MAX_VALUE);
        }

        try (BitcaskCacheStore<String, String> store = store())
        {
            assertEquals(2, store.size());
            assertEquals("alpha2", store.get("a"));
            assertEquals("beta", store.get("b"));
        }
    }

    @Test
    public void testNullValueAndNullKey()
    {
        try (BitcaskCacheStore<String, String> store = store())
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

        try (BitcaskCacheStore<String, String> store = store())
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
        try (BitcaskCacheStore<String, String> store = store())
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
    public void testExpiryCanNeverFireEarly()
    {
        try (BitcaskCacheStore<String, String> store = store())
        {
            // an expiry within the same second as now must keep the entry
            // alive: the seconds rounding is always late, never early
            long soon = System.currentTimeMillis() + 900;
            store.put("soon", "v", soon);
            assertTrue(store.containsKey("soon"));
            assertEquals("v", store.get("soon"));
        }
    }

    @Test
    public void testExpiryBeyondTheTstampRangeIsRejected()
    {
        try (BitcaskCacheStore<String, String> store = store())
        {
            long beyond2106 = 5000000000000L;
            assertThrows(IllegalArgumentException.class, () -> store.put("k", "v", beyond2106));
        }
    }

    @Test
    public void testPurgeExpired()
    {
        try (BitcaskCacheStore<String, String> store = store())
        {
            long past = System.currentTimeMillis() - 1000;
            store.put("expired1", "v", past);
            store.put("expired2", "v", past);
            store.put("live", "v", Long.MAX_VALUE);
            store.purgeExpired();
            assertEquals(1, store.size());
            assertTrue(store.containsKey("live"));
        }

        try (BitcaskCacheStore<String, String> store = store())
        {
            assertEquals(1, store.size());
            assertEquals("v", store.get("live"));
            assertFalse(store.containsKey("expired1"));
        }
    }

    @Test
    public void testClear()
    {
        try (BitcaskCacheStore<String, String> store = store())
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);
            store.clear();
            assertTrue(store.isEmpty());
            assertEquals(0, store.size());
            assertNull(store.get("a"));

            store.put("c", "3", Long.MAX_VALUE);
            assertEquals("3", store.get("c"));
        }

        try (BitcaskCacheStore<String, String> store = store())
        {
            assertEquals(1, store.size()); // a and b are gone, c survives
            assertNull(store.get("a"));
            assertNull(store.get("b"));
            assertEquals("3", store.get("c"));
        }
    }

    @Test
    public void testTornTailIsDiscarded() throws IOException
    {
        try (BitcaskCacheStore<String, String> store = store())
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);
        }

        long validLength;
        Path last = maxDataFile();
        try
        {
            validLength = Files.size(last);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        Files.write(last, new byte[]{1, 0, 0, 0, 0, 0, 0, 0, 0}, StandardOpenOption.APPEND);
        assertTrue(Files.size(last) > validLength);

        try (BitcaskCacheStore<String, String> store = store())
        {
            assertEquals(2, store.size());
            assertEquals("1", store.get("a"));
            assertEquals("2", store.get("b"));
        }

        assertEquals(validLength, Files.size(last), "the truncated tail is removed");
    }

    @Test
    public void testCorruptLastRecordIsDiscarded() throws IOException
    {
        try (BitcaskCacheStore<String, String> store = store())
        {
            store.put("a", "1", Long.MAX_VALUE);
            store.put("b", "2", Long.MAX_VALUE);
        }

        Path last = maxDataFile();
        byte[] original = Files.readAllBytes(last);
        byte[] corrupt = original.clone();
        corrupt[corrupt.length - 1] ^= 0x7F;
        Files.write(last, corrupt);

        try (BitcaskCacheStore<String, String> store = store())
        {
            assertEquals(1, store.size());
            assertEquals("1", store.get("a"));
            assertNull(store.get("b"));
        }
    }

    @Test
    public void testForeignDirectoryIsRejected() throws IOException
    {
        Files.createDirectories(cacheDir());
        Files.write(cacheDir().resolve("readme.txt"), new byte[1]);
        Files.write(cacheDir().resolve("1" + Bitcask.DATA_SUFFIX), new byte[]{2, 0, 0, 0, 3, 0, 0, 0});

        assertThrows(java.io.UncheckedIOException.class, this::store);
    }

    @Test
    public void testSecondOpenFails()
    {
        try (BitcaskCacheStore<String, String> store = store())
        {
            store.put("k", "v", Long.MAX_VALUE);

            assertThrows(java.io.UncheckedIOException.class, this::store);

            // the failed open must not disturb the live store
            assertEquals("v", store.get("k"));
        }
    }

    @Test
    public void testMergeOnCloseLeavesHintsForFastReopen() throws IOException
    {
        try (BitcaskCacheStore<String, String> store = store())
        {
            for (int i = 0; i < 20; i++)
            {
                store.put("k" + i, "value-" + i, Long.MAX_VALUE);
            }
            store.remove("k0");   // leaves dead bytes behind
            store.put("k1", "after", Long.MAX_VALUE); // still more dead bytes
        }

        // close merged the dead bytes away: the outputs carry hint files
        assertFalse(listHintFiles().isEmpty(), "the close merge writes hints for a fast reopen");

        try (BitcaskCacheStore<String, String> store = store())
        {
            assertNull(store.get("k0"));
            assertEquals("after", store.get("k1"));
            assertEquals(19, store.size());
        }
    }

    @Test
    public void testRotationAcrossMaxFileSizeKeepsEverythingReadable()
    {
        try (BitcaskCacheStore<String, String> store = store(200))
        {
            for (int i = 0; i < 30; i++)
            {
                store.put("rotating-key-number-" + i, "value-" + i, Long.MAX_VALUE);
            }
            assertEquals(30, store.size());
        }

        try (BitcaskCacheStore<String, String> store = store(200))
        {
            assertEquals(30, store.size());
            for (int i = 0; i < 30; i++)
            {
                assertEquals("value-" + i, store.get("rotating-key-number-" + i));
            }
        }
    }

    @Test
    public void testDeadOverwriteIsCompactedDuringUse()
    {
        String value = bigValue();

        try (BitcaskCacheStore<String, String> store = store())
        {
            for (int i = 0; i < 100; i++)
            {
                store.put("k" + i, value, Long.MAX_VALUE);
            }

            long cleanTotal = store.getTotalBytes();
            assertTrue(cleanTotal >= store.getLiveBytes());

            // overwriting every key doubles the disk: the dead half lands
            // right at the 50% boundary, one extra removal pushes past it and
            // the automatic compaction rewrites the directory
            for (int i = 0; i < 100; i++)
            {
                store.put("k" + i, value, Long.MAX_VALUE);
            }

            store.remove("k99");

            long afterCompact = store.getTotalBytes();
            long liveAfterCompact = store.getLiveBytes();
            assertTrue(afterCompact <= cleanTotal,
                "the compacted directory must shrink to about the live bytes: " + cleanTotal + " -> " + afterCompact);
            assertEquals(afterCompact, liveAfterCompact, "after a merge live and total agree again");
            assertEquals(99, store.size());
        }

        try (BitcaskCacheStore<String, String> reopened = store())
        {
            assertEquals(99, reopened.size());
            for (int i = 0; i < 99; i++)
            {
                assertEquals(value, reopened.get("k" + i));
            }
            assertFalse(reopened.containsKey("k99"));
        }
    }

    @Test
    public void testIntegerLongSerializers() throws IOException
    {
        Path sub = dir.resolve("other");
        try (BitcaskCacheStore<Integer, Long> store = new BitcaskCacheStore<>(sub,
            BasicSerializers.INTEGER, BasicSerializers.LONG))
        {
            store.put(1, 10L, Long.MAX_VALUE);
            store.put(2, 20L, Long.MAX_VALUE);
        }

        try (BitcaskCacheStore<Integer, Long> store = new BitcaskCacheStore<>(sub,
            BasicSerializers.INTEGER, BasicSerializers.LONG))
        {
            assertEquals(10L, store.get(1));
            assertEquals(20L, store.get(2));
        }
    }

    private String bigValue()
    {
        byte[] big = new byte[400];
        java.util.Arrays.fill(big, (byte) 'x');
        return new String(big, StandardCharsets.UTF_8);
    }

    private Path maxDataFile()
    {
        List<Path> files = dataFiles();
        assertFalse(files.isEmpty(), "the store directory must hold data files for this test");
        Path max = null;
        long maxId = -1;
        for (Path file : files)
        {
            String name = file.getFileName().toString();
            long id = Long.parseLong(name.substring(0, name.length() - Bitcask.DATA_SUFFIX.length()));
            if (id > maxId)
            {
                maxId = id;
                max = file;
            }
        }
        return max;
    }

    private List<Path> dataFiles()
    {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDir(), "*" + Bitcask.DATA_SUFFIX))
        {
            for (Path path : stream)
            {
                files.add(path);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        files.sort(java.util.Comparator.comparingLong(BitcaskCacheStoreTest::fileId));
        return files;
    }

    private List<Path> listHintFiles()
    {
        List<Path> hints = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDir(), "*" + Bitcask.HINT_SUFFIX))
        {
            for (Path path : stream)
            {
                hints.add(path);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return hints;
    }

    private static long fileId(Path file)
    {
        String name = file.getFileName().toString();
        return Long.parseLong(name.substring(0, name.length() - Bitcask.DATA_SUFFIX.length()));
    }
}
