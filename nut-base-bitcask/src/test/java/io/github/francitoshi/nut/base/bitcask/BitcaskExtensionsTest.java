/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the store-extension API used by BitcaskCacheStore: locate,
 * forEachEntry, flush, totalDataBytes and clear.
 */
public class BitcaskExtensionsTest
{
    @TempDir
    Path temp;

    private static byte[] bytes(String s)
    {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    public void locateReturnsMemoryPositions() throws IOException
    {
        Path dir = temp.resolve("s" + System.nanoTime());

        try (Bitcask bitcask = Bitcask.open(dir))
        {
            assertNull(bitcask.locate(bytes("missing")));

            bitcask.put(bytes("one"), bytes("value-one"));

            KeyDirEntry entry = bitcask.locate(bytes("one"));
            assertNotNull(entry);
            assertEquals(1, entry.fileId);
            assertEquals(0, entry.entryOffset);
            assertTrue(entry.entrySize > 0);

            bitcask.put(bytes("one"), bytes("bigger-replacement"));
            KeyDirEntry replaced = bitcask.locate(bytes("one"));
            assertEquals(entry.entrySize, replaced.entryOffset, "the second write lands right after the first");

            bitcask.delete(bytes("one"));
            assertNull(bitcask.locate(bytes("one")));
        }
    }

    @Test
    public void forEachEntryVisitsEveryKeyWithoutIO() throws IOException
    {
        Path dir = temp.resolve("s" + System.nanoTime());

        try (Bitcask bitcask = Bitcask.open(dir))
        {
            bitcask.put(bytes("a"), new byte[10]);
            bitcask.put(bytes("b"), new byte[10]);
            bitcask.delete(bytes("a"));

            List<String> seen = new ArrayList<>();
            long[] totalSize = {0};

            bitcask.forEachEntry((key, entry) ->
            {
                seen.add(new String(key, StandardCharsets.UTF_8));
                totalSize[0] += entry.entrySize;
            });

            seen.sort(String::compareTo);
            assertEquals("b", seen.get(0));
            assertEquals(1, seen.size());
            assertTrue(totalSize[0] > 0);
        }
    }

    @Test
    public void totalDataBytesTracksTheDirectory() throws IOException
    {
        Path dir = temp.resolve("s" + System.nanoTime());

        try (Bitcask bitcask = Bitcask.open(dir))
        {
            long before = bitcask.totalDataBytes();
            bitcask.put(bytes("a"), new byte[100]);

            assertTrue(bitcask.totalDataBytes() > before);
            assertEquals(before + 14 + 1 + 100, bitcask.totalDataBytes(),
                "one put adds exactly one 14-byte-header entry");

            bitcask.delete(bytes("a"));
            assertEquals(before + 2 * (14 + 1) + 100, bitcask.totalDataBytes(),
                "the tombstone also occupies the directory");
        }
    }

    @Test
    public void flushKeepsDataDurable() throws IOException
    {
        Path dir = temp.resolve("s" + System.nanoTime());

        try (Bitcask bitcask = Bitcask.builder(dir).sync(SyncMode.NONE).build())
        {
            bitcask.put(bytes("k"), bytes("v"));
            bitcask.flush();
        }

        try (Bitcask bitcask = Bitcask.open(dir))
        {
            assertArrayEquals(bytes("v"), bitcask.get(bytes("k")));
        }
    }

    @Test
    public void clearLeavesOneEmptyStoreBehind() throws IOException
    {
        Path dir = temp.resolve("s" + System.nanoTime());

        try (Bitcask bitcask = Bitcask.open(dir))
        {
            for (int i = 0; i < 5; i++)
            {
                bitcask.put(bytes("k" + i), new byte[30]);
            }

            bitcask.clear();
            assertEquals(0, bitcask.size());
            assertEquals(0, bitcask.totalDataBytes());

            bitcask.put(bytes("after"), bytes("works"));
            assertArrayEquals(bytes("works"), bitcask.get(bytes("after")));
        }

        try (Bitcask bitcask = Bitcask.open(dir))
        {
            assertNull(bitcask.get(bytes("k0")));
            assertArrayEquals(bytes("works"), bitcask.get(bytes("after")));
            assertEquals(1, bitcask.size());
        }
    }

    @Test
    public void clearCreatesANewActiveFile() throws IOException
    {
        Path dir = temp.resolve("s" + System.nanoTime());

        try (Bitcask bitcask = Bitcask.open(dir))
        {
            bitcask.put(bytes("k"), bytes("v"));
            assertEquals(1, maxFileId(dir));

            bitcask.clear();
            assertEquals(2, maxFileId(dir), "clear opens the next id as the new active file");
        }
    }

    private long maxFileId(Path dir) throws IOException
    {
        long max = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + Bitcask.DATA_SUFFIX))
        {
            for (Path path : stream)
            {
                String name = path.getFileName().toString();
                max = Math.max(max, Long.parseLong(name.substring(0, name.length() - Bitcask.DATA_SUFFIX.length())));
            }
        }
        return max;
    }
}
