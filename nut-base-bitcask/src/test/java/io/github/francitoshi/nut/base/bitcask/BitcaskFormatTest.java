/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the entry codec against the verifiable byte vectors of
 * bitcask-spec.md section 10, plus error cases.
 */
public class BitcaskFormatTest
{
    /** Vector from section 10: put("k", "hi") with tstamp = 1. */
    private static final byte[] PUT_K_HI =
    {
        (byte) 0x5b, (byte) 0xdd, (byte) 0xf9, (byte) 0x3b,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x02,
        0x6b, 0x68, 0x69
    };

    /** Vector from section 10: delete("k") with tstamp = 2. */
    private static final byte[] DELETE_K =
    {
        (byte) 0x01, (byte) 0x43, (byte) 0xd5, (byte) 0x06,
        0x00, 0x00, 0x00, 0x02, 0x00, 0x01, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        0x6b
    };

    @Test
    public void encodeMatchesPutVector()
    {
        byte[] encoded = BitcaskFormat.encode(bytes("k"), bytes("hi"), 1);
        assertArrayEquals(PUT_K_HI, encoded);
        assertEquals(17, encoded.length);
    }

    @Test
    public void encodeTombstoneMatchesDeleteVector()
    {
        byte[] encoded = BitcaskFormat.encodeTombstone(bytes("k"), 2);
        assertArrayEquals(DELETE_K, encoded);
        assertEquals(15, encoded.length);
    }

    @Test
    public void decodePutVector()
    {
        BitcaskFormat.Entry entry = BitcaskFormat.decode(PUT_K_HI);
        assertFalse(entry.isTombstone());
        assertEquals(0x5BDDF93B, entry.crc);
        assertEquals(1, entry.tstamp);
        assertArrayEquals(bytes("k"), entry.key);
        assertArrayEquals(bytes("hi"), entry.value);
    }

    @Test
    public void decodeDeleteVector()
    {
        BitcaskFormat.Entry entry = BitcaskFormat.decode(DELETE_K);
        assertTrue(entry.isTombstone());
        assertEquals(0x0143D506, entry.crc);
        assertEquals(2, entry.tstamp);
        assertArrayEquals(bytes("k"), entry.key);
        assertNull(entry.value);
    }

    @Test
    public void encodeDecodeRoundTrip()
    {
        byte[] key = bytes("some-key");
        byte[] value = new byte[777];
        for (int i = 0; i < value.length; i++)
        {
            value[i] = (byte) i;
        }

        byte[] encoded = BitcaskFormat.encode(key, value, 123456);
        assertEquals(BitcaskFormat.entrySize(key.length, value.length), encoded.length);

        BitcaskFormat.Entry entry = BitcaskFormat.decode(encoded);
        assertFalse(entry.isTombstone());
        assertEquals(123456, entry.tstamp);
        assertArrayEquals(key, entry.key);
        assertArrayEquals(value, entry.value);
    }

    @Test
    public void encodeDecodeTombstoneRoundTrip()
    {
        byte[] encoded = BitcaskFormat.encodeTombstone(bytes("gone"), 42);
        assertEquals(BitcaskFormat.entrySize(4, -1), encoded.length);

        BitcaskFormat.Entry entry = BitcaskFormat.decode(encoded);
        assertTrue(entry.isTombstone());
        assertEquals(42, entry.tstamp);
        assertArrayEquals(bytes("gone"), entry.key);
    }

    @Test
    public void decodesEntryWithTrailingBytes()
    {
        byte[] padded = new byte[PUT_K_HI.length + 5];
        System.arraycopy(PUT_K_HI, 0, padded, 0, PUT_K_HI.length);

        BitcaskFormat.Entry entry = BitcaskFormat.decode(padded);
        assertArrayEquals(bytes("k"), entry.key);
    }

    @Test
    public void emptyValueIsValid()
    {
        byte[] encoded = BitcaskFormat.encode(bytes("k"), new byte[0], 5);
        assertEquals(BitcaskFormat.HEADER_SIZE + 1, encoded.length);

        BitcaskFormat.Entry entry = BitcaskFormat.decode(encoded);
        assertFalse(entry.isTombstone());
        assertEquals(0, entry.value.length);
    }

    @Test
    public void emptyKeyIsRejected()
    {
        assertThrows(IllegalArgumentException.class,
            () -> BitcaskFormat.encode(new byte[0], bytes("v"), 1));
        assertThrows(IllegalArgumentException.class,
            () -> BitcaskFormat.encodeTombstone(new byte[0], 1));
        assertThrows(IllegalArgumentException.class,
            () -> BitcaskFormat.encode(null, bytes("v"), 1));
    }

    @Test
    public void oversizedValueIsRejected()
    {
        assertThrows(IllegalArgumentException.class,
            () -> BitcaskFormat.validateValueSize(BitcaskFormat.MAX_VALUE_SIZE + 1));
        assertThrows(IllegalArgumentException.class,
            () -> BitcaskFormat.entrySize(BitcaskFormat.MAX_KEY_SIZE, BitcaskFormat.MAX_VALUE_SIZE));
        assertEquals(BitcaskFormat.HEADER_SIZE + BitcaskFormat.MAX_KEY_SIZE + 1,
            (long) BitcaskFormat.entrySize(BitcaskFormat.MAX_KEY_SIZE, 1));
    }

    @Test
    public void nullValueIsRejectedByEncode()
    {
        assertThrows(IllegalArgumentException.class,
            () -> BitcaskFormat.encode(bytes("k"), null, 1));
    }

    @Test
    public void truncatedHeaderIsDetected()
    {
        assertThrows(TruncatedEntryException.class,
            () -> BitcaskFormat.decode(new byte[BitcaskFormat.HEADER_SIZE - 1]));
        assertThrows(TruncatedEntryException.class,
            () -> BitcaskFormat.decode(new byte[0]));
    }

    @Test
    public void truncatedEntryIsDetected()
    {
        byte[] truncated = new byte[PUT_K_HI.length - 1];
        System.arraycopy(PUT_K_HI, 0, truncated, 0, truncated.length);

        assertThrows(TruncatedEntryException.class, () -> BitcaskFormat.decode(truncated));
    }

    @Test
    public void corruptCrcIsDetected()
    {
        byte[] corrupt = PUT_K_HI.clone();
        corrupt[0] ^= 0x01; // corrupt one CRC bit

        assertThrows(CorruptEntryException.class, () -> BitcaskFormat.decode(corrupt));
    }

    @Test
    public void corruptValueByteIsDetected()
    {
        byte[] corrupt = PUT_K_HI.clone();
        corrupt[corrupt.length - 1] ^= 0x01; // corrupt one value bit

        assertThrows(CorruptEntryException.class, () -> BitcaskFormat.decode(corrupt));
    }

    @Test
    public void zeroKeySizeIsCorruption()
    {
        byte[] corrupt = PUT_K_HI.clone();
        corrupt[9] = 0; // ksz = 0x0000: empty keys are forbidden

        assertThrows(CorruptEntryException.class, () -> BitcaskFormat.decode(corrupt));
    }

    @Test
    public void invalidValueSizeRangeIsCorruption()
    {
        // vsz = 0x7FFFFFFF is in the invalid range 0x7FFFFFFF..0xFFFFFFFE
        byte[] corrupt = new byte[BitcaskFormat.HEADER_SIZE + 1];
        ByteBuffer header = ByteBuffer.wrap(corrupt, 0, BitcaskFormat.HEADER_SIZE);
        header.getInt(); // skip crc, irrelevant: ksz/vsz checks run first
        header.position(4);
        header.putInt(1);        // tstamp
        header.putShort((short) 1); // ksz
        header.putInt(0x7FFFFFFF); // invalid vsz
        corrupt[14] = 0x6b;

        assertThrows(CorruptEntryException.class, () -> BitcaskFormat.decode(corrupt));
    }

    @Test
    public void crc32MatchesKnownValue()
    {
        // CRC-32 of the 11 payload bytes of the section-10 put vector.
        byte[] payload = java.util.Arrays.copyOfRange(PUT_K_HI, 4, PUT_K_HI.length);
        assertEquals(0x5BDDF93B, BitcaskFormat.crc32(payload, 0, payload.length));
    }

    private static byte[] bytes(String s)
    {
        java.nio.charset.Charset utf8 = java.nio.charset.StandardCharsets.UTF_8;
        return s.getBytes(utf8);
    }
}
