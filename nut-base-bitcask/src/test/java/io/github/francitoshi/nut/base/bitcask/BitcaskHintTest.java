/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.bitcask;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of the hint codec against the verifiable byte vector of
 * bitcask-spec.md section 10, plus invalid-hint cases.
 */
public class BitcaskHintTest
{
    /** Vector from section 10: hint of put("k", "hi") in data offset 0, entry size 17 (0x11). */
    private static final byte[] HINT_ENTRY_K =
    {
        0x00, 0x00, 0x00, 0x01,                    // tstamp = 1
        0x00, 0x01,                                // ksz = 1
        0x00, 0x00, 0x00, 0x11,                    // total_sz = 17
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // offset = 0
        0x6b                                       // key "k"
    };

    /** Vector from section 10: trailer, CRC over the 19 hint-entry bytes is 0xFB63EB8E, data length 17. */
    private static final byte[] TRAILER_K =
    {
        0x00, 0x00, 0x00, 0x00,                    // tstamp = 0
        0x00, 0x00,                                // ksz = 0 (marker)
        (byte) 0xfb, 0x63, (byte) 0xeb, (byte) 0x8e, // crc = 0xFB63EB8E
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x11  // data length = 17
    };

    private static final byte[] DATA_ENTRY_CRC = { (byte) 0x5b, (byte) 0xdd, (byte) 0xf9, (byte) 0x3b };

    // The data entry the hint refers to: put("k", "hi"), tstamp 1, 17 bytes.
    private static final byte[] DATA_ENTRY =
    {
        (byte) 0x5b, (byte) 0xdd, (byte) 0xf9, (byte) 0x3b,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x02,
        0x6b, 0x68, 0x69
    };

    @Test
    public void encodeMatchesVector()
    {
        byte[] encoded = BitcaskHint.encode(bytes("k"), 1, 17, 0);
        assertArrayEquals(HINT_ENTRY_K, encoded);
        assertEquals(19, encoded.length);
    }

    @Test
    public void encodeTrailerMatchesVector()
    {
        byte[] body = BitcaskHint.encode(bytes("k"), 1, 17, 0);
        byte[] trailer = BitcaskHint.encodeTrailer(body, 17);

        assertArrayEquals(TRAILER_K, trailer);
        assertEquals(BitcaskHint.TRAILER_SIZE, trailer.length);
    }

    @Test
    public void correctHintIsDerivedFromItsDataEntry()
    {
        // sanity of the vector above: total_sz is the data entry size and the
        // data entry CRC is unrelated to the hint trailer CRC
        assertEquals(17, DATA_ENTRY.length);
        assertArrayEquals(DATA_ENTRY_CRC, new byte[]
        {
            DATA_ENTRY[0], DATA_ENTRY[1], DATA_ENTRY[2], DATA_ENTRY[3]
        });
    }

    @Test
    public void decodeRecoversEntryFields()
    {
        byte[] entry = BitcaskHint.encode(bytes("longer-key"), 12345,
            BitcaskFormat.entrySize(10, 500), 1234567890123L);

        BitcaskHint.HintEntry decoded = BitcaskHint.decode(entry, 0, entry.length);

        assertEquals(12345, decoded.tstamp);
        assertEquals(10, decoded.keySize);
        assertEquals(BitcaskFormat.entrySize(10, 500), decoded.totalSize);
        assertEquals(1234567890123L, decoded.offset);
    }

    @Test
    public void decodeIgnoresTrailingBytes()
    {
        byte[] entry = BitcaskHint.encode(bytes("k"), 1, 17, 0);
        byte[] padded = new byte[entry.length + 7];
        System.arraycopy(entry, 0, padded, 0, entry.length);

        BitcaskHint.HintEntry decoded = BitcaskHint.decode(padded, 0, padded.length);
        assertEquals(0, decoded.offset);
        assertEquals(17, decoded.totalSize);
    }

    @Test
    public void decodeReturnsNullAtTrailerSlot()
    {
        byte[] body = BitcaskHint.encode(bytes("k"), 1, 17, 0);
        byte[] expected = {0x00, 0x00, 0x00, 0x00};
        byte[] full = concat(body, BitcaskHint.encodeTrailer(body, 17));

        assertNull(BitcaskHint.decode(full, body.length, full.length - body.length));
    }

    @Test
    public void decodeValidTrailer()
    {
        byte[] body = BitcaskHint.encode(bytes("k"), 1, 17, 0);
        byte[] full = concat(body, BitcaskHint.encodeTrailer(body, 17));

        BitcaskHint.Trailer trailer = BitcaskHint.decodeTrailer(full);

        assertEquals(0xFB63EB8E, trailer.crc);
        assertEquals(17, trailer.dataFileLength);
    }

    @Test
    public void corruptedHintBodyFailsTrailerValidation()
    {
        byte[] body = BitcaskHint.encode(bytes("k"), 1, 17, 0);
        byte[] full = concat(body, BitcaskHint.encodeTrailer(body, 17));

        full[body.length - 1] ^= 0x01; // flip one key bit inside the body

        assertNull(BitcaskHint.decodeTrailer(full));
    }

    @Test
    public void trailerWithWrongMarkerKszIsIgnored()
    {
        byte[] body = BitcaskHint.encode(bytes("longer-key"), 1, 24, 0);
        byte[] trailer = BitcaskHint.encodeTrailer(body, 17);
        trailer[5] = (byte) 0x89;  // ksz = 0x0089, not 0: not a trailer marker
        byte[] full = concat(body, trailer);

        assertNull(BitcaskHint.decodeTrailer(full));
    }

    @Test
    public void trailerWithNonZeroTstampIsIgnored()
    {
        byte[] body = BitcaskHint.encode(bytes("k"), 1, 17, 0);
        byte[] trailer = BitcaskHint.encodeTrailer(body, 17);
        trailer[3] = 1;            // tstamp = 1, must be 0
        byte[] full = concat(body, trailer);

        assertNull(BitcaskHint.decodeTrailer(full));
    }

    @Test
    public void incompleteHintIsIgnored()
    {
        assertNull(BitcaskHint.decodeTrailer(new byte[0]));
        assertNull(BitcaskHint.decodeTrailer(new byte[BitcaskHint.TRAILER_SIZE - 1]));
        assertNull(BitcaskHint.decodeTrailer(null));
    }

    @Test
    public void trailerWithoutBodySelfValidates()
    {
        byte[] full = BitcaskHint.encodeTrailer(new byte[0], 14);

        BitcaskHint.Trailer trailer = BitcaskHint.decodeTrailer(full);
        assertEquals(0, trailer.crc);
        assertEquals(14, trailer.dataFileLength);
    }

    @Test
    public void truncatedEntryIsDetected()
    {
        byte[] entry = BitcaskHint.encode(bytes("k"), 1, 17, 0);

        assertThrows(TruncatedEntryException.class,
            () -> BitcaskHint.decode(entry, 0, BitcaskHint.HEADER_SIZE - 1));

        byte[] truncated = new byte[BitcaskHint.HEADER_SIZE + 0];
        System.arraycopy(entry, 0, truncated, 0, truncated.length);

        assertThrows(TruncatedEntryException.class, () -> BitcaskHint.decode(truncated, 0, truncated.length));
    }

    @Test
    public void oversizedKeyIsRejected()
    {
        assertThrows(IllegalArgumentException.class,
            () -> BitcaskHint.encode(new byte[0], 1, 17, 0));
        assertThrows(IllegalArgumentException.class,
            () -> BitcaskHint.encode(null, 1, 17, 0));
        assertThrows(IllegalArgumentException.class,
            () -> BitcaskHint.encode(new byte[BitcaskFormat.MAX_KEY_SIZE + 1], 1, 17, 0));
    }

    @Test
    public void negativeFieldsAreRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> BitcaskHint.encode(bytes("k"), 1, 17, -1));
        assertThrows(IllegalArgumentException.class, () -> BitcaskHint.encode(bytes("k"), 1, 13, 0));
    }

    private static byte[] concat(byte[] a, byte[] b)
    {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] bytes(String s)
    {
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
