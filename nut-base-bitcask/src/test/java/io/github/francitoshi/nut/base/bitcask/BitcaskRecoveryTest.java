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
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests: simulated crashes with truncated tails at the end of the
 * last data file, corruption of immutable files with and without forced
 * recovery, the full open-write-merge-reopen cycle and concurrent access.
 */
public class BitcaskRecoveryTest
{
    @TempDir
    Path temp;

    private Path dir()
    {
        return temp.resolve("store-" + System.nanoTime());
    }

    private static byte[] bytes(String s)
    {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    public void truncatedTailOfLastFileRecoversOnOpen() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            bitcask.put(bytes("a"), bytes("value-a"));
            bitcask.put(bytes("b"), bytes("value-b"));
        }

        // simulate a crash in the middle of a later write: partial garbage
        long lastId = maxId(directory);
        Path data = directory.resolve(lastId + Bitcask.DATA_SUFFIX);
        Files.write(data, new byte[9], StandardOpenOption.APPEND);

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertArrayEquals(bytes("value-a"), bitcask.get(bytes("a")));
            assertArrayEquals(bytes("value-b"), bitcask.get(bytes("b")));
            assertEquals(2, bitcask.size());

            // and the store keeps writing from a fresh active file
            bitcask.put(bytes("c"), bytes("value-c"));
        }

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertArrayEquals(bytes("value-c"), bitcask.get(bytes("c")));
        }
    }

    @Test
    public void partialTombstoneKeepsTheKeyAlive() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            bitcask.put(bytes("survivor"), bytes("value"));
        }

        byte[] tombstone = BitcaskFormat.encodeTombstone(bytes("survivor"), 1);
        long lastId = maxId(directory);
        Path data = directory.resolve(lastId + Bitcask.DATA_SUFFIX);
        Files.write(data, java.util.Arrays.copyOfRange(tombstone, 0, 8), StandardOpenOption.APPEND);

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            // the tombstone was cut at its 8th byte: truncated tail, the
            // deletion is lost and the key lives
            assertTrue(bitcask.containsKey(bytes("survivor")));
            assertArrayEquals(bytes("value"), bitcask.get(bytes("survivor")));
        }
    }

    @Test
    public void corruptEntryInImmutableFileAbortsOpen() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.builder(directory).maxFileSize(400).sync(SyncMode.NONE).build())
        {
            for (int i = 0; i < 9; i++)
            {
                bitcask.put(bytes("k" + i), new byte[100]);
            }
        }

        // entries are ~116 bytes: file 1 holds the first three keys (old
        // files stay immutable). Corrupt the value of the second entry.
        Path first = directory.resolve(1L + Bitcask.DATA_SUFFIX);
        byte[] corrupt = Files.readAllBytes(first);
        corrupt[116 + 30] ^= 0x01;
        Files.write(first, corrupt);

        assertThrows(CorruptEntryException.class, () -> Bitcask.open(directory));

        // forced recovery truncates the corrupt entry away and opens anyway
        try (Bitcask bitcask = Bitcask.builder(directory).forceRecovery(true).build())
        {
            assertArrayEquals(new byte[100], bitcask.get(bytes("k0")));
            assertNull(bitcask.get(bytes("k1"))); // killed by the truncation
            assertNull(bitcask.get(bytes("k2")));
            assertArrayEquals(new byte[100], bitcask.get(bytes("k5")));
            assertFalse(bitcask.containsKey(bytes("k1")));
        }

        // and after the recovered open the directory is lockable again
        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertArrayEquals(new byte[100], bitcask.get(bytes("k5")));
        }
    }

    @Test
    public void fullLifecycleWriteRotateMergeRecover() throws IOException
    {
        Path directory = dir();

        // 1. write, rotate several times, delete part of it
        try (Bitcask bitcask = Bitcask.builder(directory).maxFileSize(2048).sync(SyncMode.EVERY_WRITE).build())
        {
            for (int i = 0; i < 60; i++)
            {
                byte[] value = new byte[i % 80];
                for (int j = 0; j < value.length; j++)
                {
                    value[j] = (byte) (i + j);
                }
                bitcask.put(bytes("key-" + (char) ('a' + i % 26) + "-" + i), value);

                if (i % 3 == 0 && i > 0)
                {
                    bitcask.delete(bytes("key-" + (char) ('a' + ((i - 3) % 26)) + "-" + (i - 3)));
                }
            }
        }

        // 2. reopen: recovery from the data files
        try (Bitcask bitcask = Bitcask.builder(directory).sync(SyncMode.INTERVAL, 10).build())
        {
            assertEquals(41, bitcask.size()); // 60 puts minus the 19 deletes
            assertKeyValuesConsistent(bitcask, i -> i % 3 != 0 || i > 54); // deletes reach key-54 at most

            // 3. merge: dead entries leave the disk
            long maxBefore = maxId(directory);
            bitcask.merge();

            assertTrue(maxId(directory) > maxBefore);
            bitcask.put(bytes("post-merge"), bytes("writes-after-merge"));
        }

        // 4. reopen: recovery through the merge hint files
        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertEquals(42, bitcask.size()); // 41 survivors plus post-merge
            assertArrayEquals(bytes("writes-after-merge"), bitcask.get(bytes("post-merge")));
            assertKeyValuesConsistent(bitcask, i -> i % 3 != 0 || i > 54);
        }

        // 5. one more merge on the reopened store and a final reopen
        try (Bitcask bitcask = Bitcask.open(directory))
        {
            bitcask.merge();
            bitcask.merge(); // nothing live-dead left; must not fail
        }

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertEquals(42, bitcask.size());
        }
    }

    @Test
    public void reopeningWithoutWritesAlwaysCreatesNewActiveFile() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            bitcask.put(bytes("k"), bytes("v"));
        }

        long before = maxId(directory);

        try (Bitcask ignored = Bitcask.open(directory))
        {
            assertEquals(before + 1, maxId(directory), "every open must start a new active file");
        }
    }

    @Test
    public void intervalSyncModePersistsData() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.builder(directory).sync(SyncMode.INTERVAL, 5).build())
        {
            bitcask.put(bytes("interval"), bytes("synced"));
        }

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertArrayEquals(bytes("synced"), bitcask.get(bytes("interval")));
        }
    }

    @Test
    public void noneSyncModePersistsData() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.builder(directory).sync(SyncMode.NONE).build())
        {
            bitcask.put(bytes("none"), bytes("maybe-synced"));
        }

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertArrayEquals(bytes("maybe-synced"), bitcask.get(bytes("none")));
        }
    }

    @Test
    public void concurrentWritersSerializeAndReadersStayConsistent() throws Exception
    {
        ExecutorService pool = Executors.newFixedThreadPool(5);

        try
        {
            Bitcask bitcask = Bitcask.builder(dir()).sync(SyncMode.NONE).build();

            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch writers = new CountDownLatch(4);

            // 4 writers, interleaved keys
            for (int w = 0; w < 4; w++)
            {
                int worker = w;
                pool.submit((java.util.concurrent.Callable<Void>) () ->
                {
                    try
                    {
                        ready.await();
                        for (int i = 0; i < 50; i++)
                        {
                            bitcask.put(bytes("w" + worker + "-k" + i), new byte[20]);
                        }
                        return null;
                    }
                    finally
                    {
                        writers.countDown();
                    }
                });
            }

            // 1 reader hammering gets on existing and missing keys while
            // the writers run; it stops as soon as every writer is done
            pool.submit((java.util.concurrent.Callable<Void>) () ->
            {
                try
                {
                    ready.await();
                    while (!writers.await(5, TimeUnit.MILLISECONDS))
                    {
                        bitcask.get(bytes("w0-k0"));   // may be null until written
                        bitcask.get(bytes("nothing"));  // must be null, never throws
                    }
                    return null;
                }
                catch (Exception e)
                {
                    return null;
                }
            });

            ready.countDown();
            assertTrue(writers.await(30, TimeUnit.SECONDS));

            assertEquals(200, bitcask.size());
            for (int w = 0; w < 4; w++)
            {
                for (int i = 0; i < 50; i++)
                {
                    assertEquals(20, bitcask.get(bytes("w" + w + "-k" + i)).length);
                }
            }

            bitcask.close();
        }
        finally
        {
            pool.shutdownNow();
        }
    }

    /**
     * Reads every expected key back after recovery and checks that the
     * surviving values match what was written at their last put or delete.
     */
    private void assertKeyValuesConsistent(Bitcask bitcask, java.util.function.IntPredicate live) throws IOException
    {
        for (int i = 0; i < 60; i++)
        {
            byte[] key = bytes("key-" + (char) ('a' + i % 26) + "-" + i);

            if (live.test(i))
            {
                assertEquals(i % 80, bitcask.get(key).length, "wrong value for " + new String(key));
            }
            else
            {
                assertNull(bitcask.get(key), "key must be deleted: " + new String(key));
            }
        }
    }

    private long maxId(Path directory) throws IOException
    {
        long max = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + Bitcask.DATA_SUFFIX))
        {
            for (Path path : stream)
            {
                String name = path.getFileName().toString();
                long id = Long.parseLong(name.substring(0, name.length() - Bitcask.DATA_SUFFIX.length()));
                max = Math.max(max, id);
            }
        }
        return max;
    }
}
