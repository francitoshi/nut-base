/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests of the main Bitcask API: puts, gets, deletes,
 * persistence across reopen, rotation, and the directory lock.
 */
public class BitcaskTest
{
    @TempDir
    Path temp;

    private static byte[] bytes(String s)
    {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private Path dir()
    {
        return temp.resolve("store-" + System.nanoTime());
    }

    @Test
    public void putGetRoundTrip() throws IOException
    {
        try (Bitcask bitcask = Bitcask.open(dir()))
        {
            bitcask.put(bytes("alpha"), bytes("value-alpha"));
            bitcask.put(bytes("beta"), new byte[0]);
            bitcask.put(bytes("k"), new byte[7777]);

            assertArrayEquals(bytes("value-alpha"), bitcask.get(bytes("alpha")));
            assertArrayEquals(new byte[0], bitcask.get(bytes("beta")));
            assertEquals(7777, bitcask.get(bytes("k")).length);

            assertNull(bitcask.get(bytes("missing")));
        }
    }

    @Test
    public void deleteRemovesKeyAndIsIdempotent() throws IOException
    {
        try (Bitcask bitcask = Bitcask.open(dir()))
        {
            bitcask.put(bytes("k"), bytes("v"));

            assertTrue(bitcask.delete(bytes("k")));
            assertFalse(bitcask.delete(bytes("k"))); // unknown key: no tombstone again
            assertFalse(bitcask.delete(bytes("other")));

            assertNull(bitcask.get(bytes("k")));
            assertFalse(bitcask.containsKey(bytes("k")));
            assertEquals(0, bitcask.size());
        }
    }

    @Test
    public void deletesAndRePutsResolveLastWins() throws IOException
    {
        try (Bitcask bitcask = Bitcask.open(dir()))
        {
            bitcask.put(bytes("k"), bytes("v1"));
            bitcask.delete(bytes("k"));
            bitcask.put(bytes("k"), bytes("v2"));

            assertArrayEquals(bytes("v2"), bitcask.get(bytes("k")));
        }
    }

    @Test
    public void keysSizeAndContains() throws IOException
    {
        try (Bitcask bitcask = Bitcask.open(dir()))
        {
            bitcask.put(bytes("one"), bytes("1"));
            bitcask.put(bytes("two"), bytes("2"));
            bitcask.put(bytes("three"), bytes("3"));
            bitcask.delete(bytes("two"));

            assertEquals(2, bitcask.size());
            assertTrue(bitcask.containsKey(bytes("one")));
            assertFalse(bitcask.containsKey(bytes("two")));

            Map<String, Boolean> names = new HashMap<>();
            for (byte[] key : bitcask.keys())
            {
                names.put(new String(key, StandardCharsets.UTF_8), true);
            }
            assertTrue(names.containsKey("one"));
            assertTrue(names.containsKey("three"));
            assertEquals(2, names.size());
        }
    }

    @Test
    public void dataSurvivesCloseAndReopen() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            bitcask.put(bytes("a"), bytes("value-a"));
            bitcask.put(bytes("b"), new byte[0]);
            bitcask.put(bytes("c"), bytes("value-c"));
            bitcask.delete(bytes("b"));
        }

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertArrayEquals(bytes("value-a"), bitcask.get(bytes("a")));
            assertNull(bitcask.get(bytes("b"))); // tombstone survived
            assertArrayEquals(bytes("value-c"), bitcask.get(bytes("c")));
            assertEquals(2, bitcask.size());
        }
    }

    @Test
    public void dataSurvivesReopenAfterDeletionOnly() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            bitcask.put(bytes("a"), bytes("value-a"));
            bitcask.delete(bytes("a"));
        }

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertNull(bitcask.get(bytes("a")));
            assertEquals(0, bitcask.size());
        }
    }

    @Test
    public void rotationAcrossMaxFileSize() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.builder(directory).maxFileSize(150).sync(SyncMode.NONE).build())
        {
            for (int i = 1; i <= 20; i++)
            {
                bitcask.put(String.format("key-%04d", i).getBytes(StandardCharsets.UTF_8),
                    new byte[40]);
            }

            // with 150-byte entries a 1-entry file is allowed: rotation only
            // happens between writes, so at least 2 files must exist by now
            assertTrue(listIds(directory).size() >= 3, "expected rotation: ids " + listIds(directory));
        }

        // every value must be readable again after reopening, across all files
        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertEquals(20, bitcask.size());
            for (int i = 1; i <= 20; i++)
            {
                assertEquals(40, bitcask.get(String.format("key-%04d", i).getBytes(StandardCharsets.UTF_8)).length);
            }
        }
    }

    @Test
    public void secondOpenOfSameDirectoryFails() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            bitcask.put(bytes("locked"), bytes("yes"));

            assertThrows(IOException.class, () -> Bitcask.open(directory));
            // the failed open must not corrupt the first store
            assertArrayEquals(bytes("yes"), bitcask.get(bytes("locked")));
        }

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertArrayEquals(bytes("yes"), bitcask.get(bytes("locked")));
        }
    }

    @Test
    public void closedStoreRejectsOperations() throws IOException
    {
        Bitcask bitcask = Bitcask.open(dir());
        bitcask.put(bytes("k"), bytes("v"));
        bitcask.close();

        assertThrows(IllegalStateException.class, () -> bitcask.put(bytes("k"), new byte[1]));
        assertThrows(IllegalStateException.class, () -> bitcask.get(bytes("k")));
        assertThrows(IllegalStateException.class, () -> bitcask.delete(bytes("k")));
        assertThrows(IllegalStateException.class, () -> bitcask.keys());
        assertThrows(IllegalStateException.class, () -> bitcask.size());
    }

    @Test
    public void closeIsIdempotent() throws IOException
    {
        Bitcask bitcask = Bitcask.open(dir());
        bitcask.put(bytes("k"), bytes("v"));
        bitcask.close();
        bitcask.close(); // must not throw
        assertThrows(IllegalStateException.class, () -> bitcask.get(bytes("k")));
    }

    private static java.util.List<Long> listIds(Path directory) throws IOException
    {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + Bitcask.DATA_SUFFIX))
        {
            for (Path path : stream)
            {
                String name = path.getFileName().toString();
                ids.add(Long.parseLong(name.substring(0, name.length() - Bitcask.DATA_SUFFIX.length())));
            }
        }
        return ids;
    }
}
