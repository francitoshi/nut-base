/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the data-file scan and recovery following section 6.2 of
 * bitcask-spec.md: loading into the keydir ("last wins"), truncation of an
 * interrupted write in the last file, and corruption abort elsewhere.
 */
public class DataFileScannerTest
{
    @TempDir
    Path temp;

    @Test
    public void putsAndDeletesResolveInOrderSoLastWins() throws IOException
    {
        byte[] putOne = BitcaskFormat.encode(bytes("one"), bytes("value-one"), 1);
        byte[] putTwo = BitcaskFormat.encode(bytes("two"), new byte[0], 2);
        byte[] putThree = BitcaskFormat.encode(bytes("three"), bytes("v3"), 3);
        byte[] deleteOne = BitcaskFormat.encodeTombstone(bytes("one"), 4);
        byte[] putTwoLater = BitcaskFormat.encode(bytes("two"), bytes("two-later"), 5);

        byte[] file = concat(putOne, putTwo, putThree, deleteOne, putTwoLater);

        KeyDir keydir = new KeyDir();
        assertEquals(file.length, DataFileScanner.scan(channel(file), 1, keydir, false));

        assertNull(keydir.get(bytes("one")));
        assertEquals(putTwoLater.length, keydir.get(bytes("two")).entrySize);
        assertTrue(keydir.containsKey(bytes("three")));
        assertEquals(2, keydir.size());
    }

    @Test
    public void recordsOffsetFileIdAndTimestamp() throws IOException
    {
        byte[] a = BitcaskFormat.encode(bytes("alpha"), bytes("aa"), 10);
        byte[] b = BitcaskFormat.encode(bytes("beta"), new byte[0], 20);

        byte[] file = concat(a, b);

        KeyDir keydir = new KeyDir();
        DataFileScanner.scan(channel(file), 42, keydir, false);

        KeyDirEntry alpha = keydir.get(bytes("alpha"));
        assertEquals(42, alpha.fileId);
        assertEquals(0, alpha.entryOffset);
        assertEquals(a.length, alpha.entrySize);
        assertEquals(10, alpha.tstamp);

        KeyDirEntry beta = keydir.get(bytes("beta"));
        assertEquals(a.length, beta.entryOffset);
        assertEquals(b.length, beta.entrySize);
        assertEquals(20, beta.tstamp);
    }

    @Test
    public void shortPartialTailInLastFileIsTruncated() throws IOException
    {
        byte[] a = BitcaskFormat.encode(bytes("a"), bytes("value-a"), 1);
        byte[] b = BitcaskFormat.encode(bytes("b"), bytes("value-b"), 2);
        byte[] tail = new byte[10]; // fewer bytes than any header + key

        byte[] file = concat(a, b, tail);

        FileChannel channel = writable(file);
        KeyDir keydir = new KeyDir();

        long size = DataFileScanner.scan(channel, 1, keydir, true);

        assertEquals(a.length + b.length, size);
        assertEquals(a.length + b.length, channel.size());
        assertEquals(2, keydir.size());
        assertTrue(keydir.containsKey(bytes("a")));
        assertTrue(keydir.containsKey(bytes("b")));

        channel.close();
    }

    @Test
    public void headerOnlyTailInLastFileIsTruncated() throws IOException
    {
        byte[] a = BitcaskFormat.encode(bytes("a"), bytes("value-a"), 1);
        byte[] headerOnly = new byte[BitcaskFormat.HEADER_SIZE];

        byte[] file = concat(a, headerOnly);

        FileChannel channel = writable(file);
        KeyDir keydir = new KeyDir();

        assertEquals(a.length, DataFileScanner.scan(channel, 1, keydir, true));
        assertEquals(a.length, channel.size());

        channel.close();
    }

    @Test
    public void corruptEntryInLastFileTruncatesBeforeIt() throws IOException
    {
        byte[] a = BitcaskFormat.encode(bytes("a"), bytes("value-a"), 1);
        byte[] bad = BitcaskFormat.encode(bytes("b"), bytes("value-b"), 2);
        bad[bad.length - 1] ^= 0x01; // CRC mismatch

        byte[] file = concat(a, bad);

        FileChannel channel = writable(file);
        KeyDir keydir = new KeyDir();

        assertEquals(a.length, DataFileScanner.scan(channel, 1, keydir, true));
        assertEquals(a.length, channel.size());

        channel.close();
    }

    @Test
    public void corruptEntryInImmutableFileAborts() throws IOException
    {
        byte[] a = BitcaskFormat.encode(bytes("a"), bytes("value-a"), 1);
        byte[] bad = BitcaskFormat.encode(bytes("b"), bytes("value-b"), 2);
        bad[0] ^= 0x01;

        KeyDir keydir = new KeyDir();
        assertThrows(CorruptEntryException.class, () -> DataFileScanner.scan(channel(concat(a, bad)), 1, keydir, false));
    }

    @Test
    public void truncatedTailInImmutableFileAborts() throws IOException
    {
        byte[] a = BitcaskFormat.encode(bytes("a"), bytes("value-a"), 1);

        KeyDir keydir = new KeyDir();
        assertThrows(TruncatedEntryException.class, () -> DataFileScanner.scan(channel(concat(a, new byte[8])), 1, keydir, false));
    }

    @Test
    public void emptyFileScansToNothing() throws IOException
    {
        KeyDir keydir = new KeyDir();
        assertEquals(0, DataFileScanner.scan(channel(new byte[0]), 1, keydir, false));
        assertTrue(keydir.isEmpty());
    }

    @Test
    public void largeEntrySpansTheSizingBuffer() throws IOException
    {
        byte[] small = BitcaskFormat.encode(bytes("small"), bytes("s"), 1);

        byte[] largeValue = new byte[10_000];
        for (int i = 0; i < largeValue.length; i++)
        {
            largeValue[i] = (byte) i;
        }
        byte[] large = BitcaskFormat.encode(bytes("large"), largeValue, 2);

        byte[] after = BitcaskFormat.encode(bytes("after"), bytes("done"), 3);

        byte[] file = concat(small, large, after);

        KeyDir keydir = new KeyDir();
        DataFileScanner.scan(channel(file), 7, keydir, false);

        assertEquals(3, keydir.size());

        KeyDirEntry entry = keydir.get(bytes("large"));
        assertEquals(large.length, entry.entrySize);
        assertEquals(small.length, entry.entryOffset);
        assertEquals(7, entry.fileId);
    }

    private FileChannel channel(byte[] contents) throws IOException
    {
        Path path = temp.resolve("scan-" + System.nanoTime() + ".bitcask.data");
        Files.write(path, contents);
        return FileChannel.open(path, StandardOpenOption.READ);
    }

    private FileChannel writable(byte[] contents) throws IOException
    {
        Path path = temp.resolve("scan-writable-" + System.nanoTime() + ".bitcask.data");
        Files.write(path, contents);
        return FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
    }

    private static byte[] concat(byte[]... parts)
    {
        int length = 0;
        for (byte[] part : parts)
        {
            length += part.length;
        }

        byte[] out = new byte[length];
        int offset = 0;
        for (byte[] part : parts)
        {
            System.arraycopy(part, 0, out, offset, part.length);
            offset += part.length;
        }

        return out;
    }

    private static byte[] bytes(String s)
    {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
