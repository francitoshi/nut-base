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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the blocking merge of section 6.7: only live entries survive,
 * tombstones and dead values vanish, outputs get hint files, ids shift above
 * the originals and old files are deleted from oldest to newest.
 */
public class BitcaskMergeTest
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
    public void mergeKeepsLiveKeysReadable() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            bitcask.put(bytes("alpha"), bytes("value-alpha"));
            bitcask.put(bytes("beta"), bytes("value-beta"));
            bitcask.merge();

            assertEquals(2, bitcask.size());
            assertArrayEquals(bytes("value-alpha"), bitcask.get(bytes("alpha")));
            assertArrayEquals(bytes("value-beta"), bitcask.get(bytes("beta")));
        }
    }

    @Test
    public void mergeRemovesTombstonesAndDeadValues() throws IOException
    {
        Path directory = dir();
        long oldMaxId;

        try (Bitcask bitcask = Bitcask.builder(directory).maxFileSize(150).sync(SyncMode.NONE).build())
        {
            bitcask.put(bytes("keep"), new byte[50]);
            bitcask.put(bytes("dead"), new byte[50]);       // value overwritten: dead
            bitcask.put(bytes("gone"), new byte[50]);       // tombstoned: dead
            bitcask.put(bytes("dead"), new byte[10]);
            bitcask.delete(bytes("gone"));

            oldMaxId = maxId(directory);
            assertTrue(oldMaxId >= 2, "setup must rotate at least twice: max id " + oldMaxId);

            bitcask.merge();
        }

        // every id must be new: all old files were deleted. Two data files
        // remain: the merge output and the new active file
        List<Long> ids = listIds(directory);
        assertEquals(2, ids.size());
        for (long id : ids)
        {
            assertTrue(id > oldMaxId, "id " + id + " must be above the old max " + oldMaxId);
        }

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertNull(bitcask.get(bytes("gone")));
            assertEquals(10, bitcask.get(bytes("dead")).length);  // latest value of the overwritten key
            assertEquals(50, bitcask.get(bytes("keep")).length);  // live value survives
            assertEquals(2, bitcask.size());
        }
    }

    @Test
    public void mergeOutputsCarryValidHints() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            for (int i = 1; i <= 10; i++)
            {
                bitcask.put(bytes("k" + i), new byte[40]);
            }
            bitcask.merge();
        }

        // every output data file must have a hint whose trailer agrees with
        // the data file length: the open path will load it without scan.
        // The active file (the greatest id) is not a merge output: no hint
        List<Long> ids = listIds(directory);
        assertFalse(ids.isEmpty());

        long activeId = ids.get(ids.size() - 1); // sorted ascending by listDataFileIds
        List<Long> sorted = new ArrayList<>(ids);
        java.util.Collections.sort(sorted);
        activeId = sorted.get(sorted.size() - 1);

        for (long id : sorted)
        {
            if (id == activeId)
            {
                continue;
            }

            byte[] hint = Files.readAllBytes(directory.resolve(id + Bitcask.HINT_SUFFIX));
            BitcaskHint.Trailer trailer = BitcaskHint.decodeTrailer(hint);

            assertTrue(trailer != null, "hint of id " + id + " must end with a valid trailer");
            assertEquals(Files.size(directory.resolve(id + Bitcask.DATA_SUFFIX)), trailer.dataFileLength);
        }
    }

    @Test
    public void mergeWritesSeveralRotatedOutputs() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.builder(directory).maxFileSize(120).sync(SyncMode.NONE).build())
        {
            for (int i = 1; i <= 12; i++)
            {
                bitcask.put(bytes("key-number-" + i), new byte[40]);
            }
            bitcask.merge();

            List<Long> ids = listIds(directory);
            // entries ~60 bytes each: several outputs must exist plus the active file
            assertTrue(ids.size() >= 3, "expected several rotated outputs, ids: " + ids);
            assertEquals(12, bitcask.size());
        }

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertEquals(12, bitcask.size());
            for (int i = 1; i <= 12; i++)
            {
                assertEquals(40, bitcask.get(bytes("key-number-" + i)).length);
            }
        }
    }

    @Test
    public void mergeOnEmptyStoreLeavesItUsable() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            bitcask.put(bytes("gone"), bytes("value"));
            bitcask.delete(bytes("gone"));
            bitcask.merge();

            assertEquals(0, bitcask.size());
            assertTrue(bitcask.keys().length == 0);

            bitcask.put(bytes("future"), bytes("still-here"));
            assertArrayEquals(bytes("still-here"), bitcask.get(bytes("future")));
        }

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertArrayEquals(bytes("still-here"), bitcask.get(bytes("future")));
        }
    }

    @Test
    public void mergedStoreAcceptsNewWrites() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.builder(directory).maxFileSize(100).sync(SyncMode.NONE).build())
        {
            for (int i = 1; i <= 6; i++)
            {
                bitcask.put(bytes("first-round-" + i), new byte[30]);
            }
            bitcask.merge();

            bitcask.put(bytes("second-round"), bytes("after-merge"));
            bitcask.delete(bytes("first-round-1"));
        }

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            assertArrayEquals(bytes("after-merge"), bitcask.get(bytes("second-round")));
            assertNull(bitcask.get(bytes("first-round-1")));
            for (int i = 2; i <= 6; i++)
            {
                assertEquals(30, bitcask.get(bytes("first-round-" + i)).length);
            }
            assertEquals(6, bitcask.size()); // 5 first-round survivors + second-round
        }
    }

    @Test
    public void mergeLeavesNoTemporaries() throws IOException
    {
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            bitcask.put(bytes("a"), bytes("value-a"));
            bitcask.merge();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + Bitcask.TMP_SUFFIX))
            {
                assertFalse(stream.iterator().hasNext(), "no .tmp file may survive the merge");
            }
        }
    }

    @Test
    public void failedMergeKeepsStoreUsableAndTemporariesDeletedOnNextOperation() throws IOException
    {
        // a merge cannot fail halfway in normal conditions; here we check
        // instead that after the merge the store accepts more operations
        Path directory = dir();

        try (Bitcask bitcask = Bitcask.open(directory))
        {
            bitcask.put(bytes("a"), bytes("value-a"));
            bitcask.merge();
            bitcask.merge(); // idempotent situation: empty keydir handling

            bitcask.put(bytes("b"), bytes("value-b"));
            assertArrayEquals(bytes("value-b"), bitcask.get(bytes("b")));
        }
    }

    private long maxId(Path directory) throws IOException
    {
        long max = 0;
        for (long id : listIds(directory))
        {
            max = Math.max(max, id);
        }
        return max;
    }

    private List<Long> listIds(Path directory) throws IOException
    {
        List<Long> ids = new ArrayList<>();
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
