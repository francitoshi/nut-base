/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.nio.ByteBuffer;

/**
 * Encoder and decoder for Bitcask hint files as defined in bitcask-spec.md
 * (section 7). A hint contains the same keys as the associated data file but
 * no values, so opening a database can skip the data scan.
 * <p>
 * Layout of a hint entry (all integers big-endian):
 * <pre>
 *   0   4  tstamp   u32
 *   4   2  ksz      u16, 1..65535
 *   6   4  total_sz u32, total size of the entry in the data file
 *   10  8  offset   u64, position of the entry in the data file
 *   18 ... key
 * </pre>
 * Header size is 18 bytes.
 * <p>
 * A valid hint ends with an 18-byte trailer that marks the file as complete:
 * {@code tstamp = 0}, {@code ksz = 0} (no valid entry has a zero key size),
 * {@code total_sz = CRC-32} of every byte before the trailer, and
 * {@code offset = expected length of the associated data file}.
 * <p>
 * A hint is only used when it ends with a correct trailer, the trailer CRC
 * matches and the data file length equals the trailer offset (section 7.3).
 * An invalid hint is never an error: it is ignored and the data file is
 * scanned instead.
 */
public final class BitcaskHint
{
    /** Size in bytes of the fixed hint entry header. */
    public static final int HEADER_SIZE = 18;

    /** Size in bytes of the trailer marker record. */
    public static final int TRAILER_SIZE = 18;

    private BitcaskHint()
    {
    }

    /**
     * A decoded hint entry: the address of one key's entry in the associated
     * data file.
     */
    public static final class HintEntry
    {
        /** Timestamp in seconds since epoch of the data file entry. */
        public final int tstamp;

        /** Key size in bytes. */
        public final int keySize;

        /** Total size in bytes of the data file entry. */
        public final int totalSize;

        /** Position of the entry within the data file. */
        public final long offset;

        /**
         * Constructs a hint entry.
         *
         * @param tstamp the timestamp in seconds since epoch
         * @param keySize the key size in bytes
         * @param totalSize the total size of the data file entry
         * @param offset the position of the entry within the data file
         */
        public HintEntry(int tstamp, int keySize, int totalSize, long offset)
        {
            this.tstamp = tstamp;
            this.keySize = keySize;
            this.totalSize = totalSize;
            this.offset = offset;
        }
    }

    /**
     * The decoded trailer of a hint, confirming the hint is complete.
     */
    public static final class Trailer
    {
        /** CRC-32 stored in the trailer, over every byte before it. */
        public final int crc;

        /** Expected length in bytes of the associated data file. */
        public final long dataFileLength;

        /**
         * Constructs a trailer record.
         *
         * @param crc the CRC-32 over all hint bytes before the trailer
         * @param dataFileLength the expected length of the associated data file
         */
        public Trailer(int crc, long dataFileLength)
        {
            this.crc = crc;
            this.dataFileLength = dataFileLength;
        }
    }

    /**
     * Encodes one hint entry.
     *
     * @param key the key bytes, 1..65535 bytes long
     * @param tstamp the timestamp in seconds since epoch
     * @param totalSize the total size in bytes of the data file entry
     * @param offset the position of the entry within the data file, 0 or greater
     * @return the encoded hint entry, HEADER_SIZE + key.length bytes long
     */
    public static byte[] encode(byte[] key, int tstamp, int totalSize, long offset)
    {
        BitcaskFormat.validateKeySize(key);

        if (totalSize < BitcaskFormat.HEADER_SIZE + key.length)
        {
            throw new IllegalArgumentException(
                "totalSize must be at least " + (BitcaskFormat.HEADER_SIZE + key.length) + ", got " + totalSize);
        }

        if (offset < 0)
        {
            throw new IllegalArgumentException("offset must not be negative, got " + offset);
        }

        byte[] entry = new byte[HEADER_SIZE + key.length];
        ByteBuffer buf = ByteBuffer.wrap(entry);
        buf.putInt(tstamp);
        buf.putShort((short) key.length);
        buf.putInt(totalSize);
        buf.putLong(offset);
        buf.put(key);
        return entry;
    }

    /**
     * Decodes one hint entry. A slot whose key size is zero marks the
     * trailer: this method returns null there and the caller must validate
     * the trailer and stop scanning.
     *
     * @param data the hint bytes
     * @param offset the offset of the hint entry
     * @param available the number of valid bytes in the array at the offset
     * @return the decoded hint entry, or null if the slot is the trailer
     * @throws TruncatedEntryException if fewer bytes than the entry needs are available
     * @throws CorruptEntryException if the entry sizes are invalid
     */
    public static HintEntry decode(byte[] data, int offset, int available)
    {
        if (data == null || available < HEADER_SIZE)
        {
            throw new TruncatedEntryException("truncated hint header: available " + available + " bytes, need " + HEADER_SIZE);
        }

        int tstamp = ByteBuffer.wrap(data, offset, 4).getInt();
        int ksz = ByteBuffer.wrap(data, offset + 4, 2).getShort() & 0xFFFF;

        if (ksz == 0)
        {
            return null; // trailer marker
        }

        if (ksz > BitcaskFormat.MAX_KEY_SIZE)
        {
            throw new CorruptEntryException("hint ksz " + ksz + " exceeds the maximum key size");
        }

        int totalSize = ByteBuffer.wrap(data, offset + 6, 4).getInt();
        long dataOffset = ByteBuffer.wrap(data, offset + 10, 8).getLong();

        if (totalSize < 0)
        {
            throw new CorruptEntryException("hint total_sz is negative");
        }

        if (dataOffset < 0)
        {
            throw new CorruptEntryException("hint offset is negative");
        }

        if (available < HEADER_SIZE + ksz)
        {
            throw new TruncatedEntryException(
                "truncated hint entry: available " + available + " bytes, need " + (HEADER_SIZE + ksz));
        }

        return new HintEntry(tstamp, ksz, totalSize, dataOffset);
    }

    /**
     * Prepares the 18-byte trailer that closes a hint. The trailer CRC is
     * computed over the given hint body, which must be the entire hint
     * content before the trailer.
     *
     * @param hintBody the whole hint content before the trailer
     * @param dataFileLength the length of the associated data file
     * @return the encoded trailer, always 18 bytes
     */
    public static byte[] encodeTrailer(byte[] hintBody, long dataFileLength)
    {
        byte[] trailer = new byte[TRAILER_SIZE];
        ByteBuffer buf = ByteBuffer.wrap(trailer);
        buf.putInt(0);          // tstamp = 0
        buf.putShort((short) 0); // ksz = 0: trailer marker
        buf.putInt(BitcaskFormat.crc32(hintBody, 0, hintBody.length));
        buf.putLong(dataFileLength);
        return trailer;
    }

    /**
     * Returns the trailer of the given hint if the hint is complete and
     * self-consistent (section 7.3, first two conditions). The third
     * condition, that the data file length equals the trailer offset, must
     * be checked against the data file by the caller.
     *
     * @param hint the whole hint file content, or a byte array containing it at the start
     * @return the trailer, or null if the hint is not valid; never throws
     */
    public static Trailer decodeTrailer(byte[] hint)
    {
        if (hint == null || hint.length < TRAILER_SIZE)
        {
            return null;
        }

        int trailerOffset = hint.length - TRAILER_SIZE;

        int tstamp = ByteBuffer.wrap(hint, trailerOffset, 4).getInt();
        if (tstamp != 0)
        {
            return null;
        }

        int ksz = ByteBuffer.wrap(hint, trailerOffset + 4, 2).getShort() & 0xFFFF;
        if (ksz != 0)
        {
            return null;
        }

        int crc = ByteBuffer.wrap(hint, trailerOffset + 6, 4).getInt();
        long dataFileLength = ByteBuffer.wrap(hint, trailerOffset + 10, 8).getLong();

        int computed = BitcaskFormat.crc32(hint, 0, trailerOffset);
        if (crc != computed)
        {
            return null;
        }

        return new Trailer(crc, dataFileLength);
    }
}
