/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Encoder and decoder for Bitcask data-file entries as defined in
 * bitcask-spec.md (section 4).
 * <p>
 * Layout of an entry (all integers big-endian):
 * <pre>
 *   0   4  crc    CRC-32 (IEEE 802.3) of the bytes [4 .. end of entry)
 *   4   4  tstamp u32, seconds since epoch (informational)
 *   8   2  ksz    u16, key length, 1..65535
 *   10  4  vsz   u32, value length, or 0xFFFFFFFF = tombstone
 *   14 ... key    key bytes
 *   ... ... value value bytes (absent for a tombstone)
 * </pre>
 * Header size is 14 bytes and the total entry size is
 * {@code 14 + ksz + (tombstone ? 0 : vsz)}.
 * <p>
 * A deleted key is written as an entry with {@code vsz = 0xFFFFFFFF} and no
 * value bytes. Values cannot exceed {@code Integer.MAX_VALUE - 1} bytes, so
 * any {@code vsz} in the range {@code 0x7FFFFFFF..0xFFFFFFFE} is invalid and
 * treated as corruption.
 */
public final class BitcaskFormat
{
    /** Size in bytes of the fixed entry header. */
    public static final int HEADER_SIZE = 14;

    /** Value-size sentinel stored in the {@code vsz} field of a tombstone. */
    public static final int TOMBSTONE_VSZ = 0xFFFFFFFF;

    /** Maximum valid value size in bytes (values use the sentinel above). */
    public static final int MAX_VALUE_SIZE = 0x7FFFFFFE;

    /** Maximum valid key size in bytes, 65535. */
    public static final int MAX_KEY_SIZE = 0xFFFF;

    /** Minimum valid key size in bytes; the empty key is forbidden. */
    public static final int MIN_KEY_SIZE = 1;

    private BitcaskFormat()
    {
    }

    /**
     * A decoded data-file entry.
     * <p>
     * The key and value arrays are defensive copies owned by the entry. For a
     * tombstone (a delete marker) {@code value} is {@code null}.
     */
    public static final class Entry
    {
        /** CRC-32 stored in the entry, as an unsigned value in the low 32 bits. */
        public final int crc;

        /** Timestamp in seconds since epoch; informational only. */
        public final int tstamp;

        /** The key bytes. */
        public final byte[] key;

        /** The value bytes, or {@code null} for a tombstone. */
        public final byte[] value;

        /**
         * Constructs an entry.
         *
         * @param crc the stored CRC-32
         * @param tstamp the timestamp in seconds since epoch
         * @param key the key bytes, copied defensively
         * @param value the value bytes, copied defensively, or {@code null} for a tombstone
         */
        public Entry(int crc, int tstamp, byte[] key, byte[] value)
        {
            this.crc = crc;
            this.tstamp = tstamp;
            this.key = key.clone();
            this.value = value == null ? null : value.clone();
        }

        /**
         * Returns true if this entry is a tombstone (a delete marker).
         *
         * @return true if this entry has no value
         */
        public boolean isTombstone()
        {
            return value == null;
        }
    }

    /**
     * Returns the total on-disk size in bytes of an entry with the given key
     * and value sizes.
     *
     * @param keySize the key size in bytes, 1..65535
     * @param valueSize the value size in bytes, or {@code -1} for a tombstone
     * @return the total entry size in bytes
     */
    public static int entrySize(int keySize, int valueSize)
    {
        long size = (long) HEADER_SIZE + keySize + (valueSize < 0 ? 0 : valueSize);
        if (size > Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException("entry size overflow");
        }
        return (int) size;
    }

    /**
     * Validates the key and value sizes against the limits of section 4.3.
     *
     * @param key the key to validate, must not be null and 1..65535 bytes long
     * @param value the value to validate, must not exceed MAX_VALUE_SIZE bytes; may be null for a tombstone
     */
    public static void validateSizes(byte[] key, byte[] value)
    {
        validateKeySize(key);
        validateValueSize(value == null ? -1 : value.length);
    }

    /**
     * Validates the key size against the limits of section 4.3.
     *
     * @param key the key to validate, must not be null and 1..65535 bytes long
     */
    public static void validateKeySize(byte[] key)
    {
        if (key == null || key.length < MIN_KEY_SIZE || key.length > MAX_KEY_SIZE)
        {
            throw new IllegalArgumentException("key length must be 1..65535, got " + (key == null ? "null" : key.length));
        }
    }

    /**
     * Validates a value size against the limits of section 4.3.
     *
     * @param valueSize the value size in bytes, 0..MAX_VALUE_SIZE, or {@code -1} for a tombstone
     */
    public static void validateValueSize(int valueSize)
    {
        if (valueSize != -1 && valueSize > MAX_VALUE_SIZE)
        {
            throw new IllegalArgumentException("value length must be 0.." + MAX_VALUE_SIZE + ", got " + valueSize);
        }
    }

    /**
     * Encodes a complete entry: put of a key with a value.
     *
     * @param key the key bytes, 1..65535 bytes long
     * @param value the value bytes, 0..MAX_VALUE_SIZE bytes long
     * @param tstamp the timestamp in seconds since epoch
     * @return the encoded entry
     */
    public static byte[] encode(byte[] key, byte[] value, int tstamp)
    {
        if (value == null)
        {
            throw new IllegalArgumentException("value must not be null, use encodeTombstone for deletions");
        }

        validateSizes(key, value);

        byte[] entry = new byte[entrySize(key.length, value.length)];
        ByteBuffer buf = ByteBuffer.wrap(entry);
        buf.position(4);
        buf.putInt(tstamp);
        buf.putShort((short) key.length);
        buf.putInt(value.length);
        buf.put(key);
        buf.put(value);
        buf.putInt(0, crc32(entry, 4, entry.length - 4));
        return entry;
    }

    /**
     * Encodes a tombstone: an entry that marks the deletion of a key.
     *
     * @param key the key bytes, 1..65535 bytes long
     * @param tstamp the timestamp in seconds since epoch
     * @return the encoded tombstone entry
     */
    public static byte[] encodeTombstone(byte[] key, int tstamp)
    {
        validateSizes(key, null);

        byte[] entry = new byte[entrySize(key.length, -1)];
        ByteBuffer buf = ByteBuffer.wrap(entry);
        buf.position(4);
        buf.putInt(tstamp);
        buf.putShort((short) key.length);
        buf.putInt(TOMBSTONE_VSZ);
        buf.put(key);
        buf.putInt(0, crc32(entry, 4, entry.length - 4));
        return entry;
    }

    /**
     * Decodes and verifies an encoded entry from a byte array that contains
     * exactly the entry.
     *
     * @param data the full encoded entry
     * @return the decoded entry
     * @throws TruncatedEntryException if the data is too short for the declared entry size
     * @throws CorruptEntryException if a field is invalid or the CRC does not match
     */
    public static Entry decode(byte[] data)
    {
        return decode(data, 0, data == null ? 0 : data.length);
    }

    /**
     * Decodes and verifies an encoded entry located at the given offset in a
     * byte array. The entry is decoded using its own declared size; extra
     * bytes after the entry are ignored.
     *
     * @param data the byte array containing the entry
     * @param offset the offset of the first byte (the CRC) of the entry
     * @param available the number of valid bytes in the array at the offset
     * @return the decoded entry
     * @throws TruncatedEntryException if fewer bytes than the declared entry size are available
     * @throws CorruptEntryException if a field is invalid or the CRC does not match
     */
    public static Entry decode(byte[] data, int offset, int available)
    {
        if (data == null || available < HEADER_SIZE)
        {
            throw new TruncatedEntryException("truncated header: available " + available + " bytes, need " + HEADER_SIZE);
        }

        ByteBuffer buf = ByteBuffer.wrap(data, offset, available);
        int tstamp = ByteBuffer.wrap(data, offset + 4, 4).getInt();
        int ksz = ByteBuffer.wrap(data, offset + 8, 2).getShort() & 0xFFFF;
        int vsz = ByteBuffer.wrap(data, offset + 10, 4).getInt();

        if (ksz == 0)
        {
            throw new CorruptEntryException("ksz is 0: empty keys are forbidden");
        }

        boolean tombstone = vsz == TOMBSTONE_VSZ;

        if (!tombstone && vsz > MAX_VALUE_SIZE)
        {
            throw new CorruptEntryException(String.format("vsz 0x%08X is in the invalid range or exceeds the maximum", vsz));
        }

        int size = entrySize(ksz, tombstone ? -1 : vsz);

        if (available < size)
        {
            throw new TruncatedEntryException("truncated entry: available " + available + " bytes, need " + size);
        }

        int end = offset + size;
        int crc = ByteBuffer.wrap(data, offset, 4).getInt();
        int computed = (int) crc32(data, offset + 4, size - 4);

        if (crc != computed)
        {
            throw new CorruptEntryException(String.format("CRC mismatch: stored 0x%08X, computed 0x%08X", crc, computed));
        }

        byte[] key = Arrays.copyOfRange(data, offset + HEADER_SIZE, offset + HEADER_SIZE + ksz);
        byte[] value = tombstone ? null
            : Arrays.copyOfRange(data, offset + HEADER_SIZE + ksz, end);

        return new Entry(crc, tstamp, key, value);
    }

    /**
     * Computes the CRC-32 (IEEE 802.3) of a byte range.
     *
     * @param data the byte array
     * @param offset the offset of the first byte
     * @param length the number of bytes
     * @return the CRC-32 as an unsigned value in the low 32 bits
     */
    public static int crc32(byte[] data, int offset, int length)
    {
        CRC32 crc = new CRC32();
        crc.update(data, offset, length);
        return (int) crc.getValue();
    }
}
