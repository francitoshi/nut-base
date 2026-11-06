/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Loads a data file into the keydir by scanning its entries, following
 * section 6.2 of bitcask-spec.md.
 * <p>
 * Entries are applied in file order (ascending offset) so the last one wins:
 * a put replaces the keydir entry and a tombstone removes the key. This
 * assumes the caller scans data files in ascending fileId order, each file
 * by ascending offset.
 * <p>
 * Failure treatment: a truncated tail or a corrupt entry in the file with
 * the highest id (the most recently written one) is assumed to be an
 * interrupted write, so the file is truncated to the offset of the failing
 * entry and scanning ends there. The same failure in any other file is
 * real corruption: the exception is rethrown to abort the open.
 */
public final class DataFileScanner
{
    private DataFileScanner()
    {
    }

    /**
     * Scans the whole data file reachable through the given channel and
     * loads its entries into the keydir.
     *
     * @param channel the channel of the data file
     * @param fileId the id of the data file, recorded in the keydir entries
     * @param keydir the keydir to load into
     * @param last true if this is the data file with the highest id, whose
     *             failures are treated as interrupted writes and truncated
     * @return the total size the file has after scanning: the same as when
     *         scanning started, unless the last file was truncated
     * @throws CorruptEntryException if the file is corrupt and not last
     * @throws IOException if the file cannot be read
     */
    public static long scan(FileChannel channel, long fileId, KeyDir keydir, boolean last) throws IOException
    {
        long size = channel.size();
        long offset = 0;

        ByteBuffer header = ByteBuffer.allocate(BitcaskFormat.HEADER_SIZE);
        ByteBuffer entry = ByteBuffer.allocate(4096);

        while (offset < size)
        {
            long remaining = size - offset;

            try
            {
                if (remaining < BitcaskFormat.HEADER_SIZE)
                {
                    throw new TruncatedEntryException(
                        "truncated header tail: " + remaining + " bytes remain at offset " + offset);
                }

                header.clear();
                readFully(channel, header, offset);

                // the 14-byte header is crc(4) tstamp(4) ksz(2) vsz(4), big-endian
                byte[] h = header.array();
                int ksz = ((h[8] & 0xFF) << 8) | (h[9] & 0xFF);
                int vsz = ((h[10] & 0xFF) << 24) | ((h[11] & 0xFF) << 16) | ((h[12] & 0xFF) << 8) | (h[13] & 0xFF);
                boolean tombstone = vsz == BitcaskFormat.TOMBSTONE_VSZ;

                if (ksz == 0)
                {
                    throw new CorruptEntryException("ksz is 0 at offset " + offset);
                }

                if (!tombstone && vsz > BitcaskFormat.MAX_VALUE_SIZE)
                {
                    throw new CorruptEntryException(String.format("vsz 0x%08X is invalid at offset %d", vsz, offset));
                }

                int entrySize = BitcaskFormat.entrySize(ksz, tombstone ? -1 : vsz);

                if (entrySize > remaining)
                {
                    throw new TruncatedEntryException(
                        "truncated entry at offset " + offset + ": declared " + entrySize + " bytes but " + remaining + " remain");
                }

                if (entry.capacity() < entrySize)
                {
                    entry = ByteBuffer.allocate(Math.max(entrySize, entry.capacity() * 2));
                }

                entry.clear().limit(entrySize);
                readFully(channel, entry, offset);

                // the whole entry is contiguous in the buffer: reuse the fully tested codec
                BitcaskFormat.Entry record = BitcaskFormat.decode(entry.array(), 0, entrySize);

                if (record.isTombstone())
                {
                    keydir.remove(record.key);
                }
                else
                {
                    keydir.put(record.key, new KeyDirEntry(fileId, offset, entrySize, record.tstamp));
                }

                offset += entrySize;
            }
            catch (CorruptEntryException e)
            {
                if (!last)
                {
                    throw e;
                }

                // interrupted write: drop the failing entry and everything after it
                channel.truncate(offset);
                return offset;
            }
        }

        return offset;
    }

    /**
     * Reads until the buffer is full: FileChannel.read may return early, so
     * reading once is not enough.
     */
    private static void readFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException
    {
        while (buffer.hasRemaining())
        {
            int read = channel.read(buffer, position);

            if (read < 0)
            {
                throw new java.io.EOFException("unexpected end of file at position " + position);
            }

            position += read;
        }
    }
}
