/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.varint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class SmallIntTest
{

    @Test
    public void testNull()
    {
        assertEquals(1, SmallInt.sizeOf(-1));
        assertEquals(1, SmallInt.encode(-1).length);
        assertArrayEquals(new byte[]{(byte) 0xC0}, SmallInt.encode(-1));
        assertEquals(-1, SmallInt.decode(new byte[]{(byte) 0xC0}));
    }

    @Test
    public void testZero()
    {
        assertEquals(1, SmallInt.sizeOf(0));
        assertArrayEquals(new byte[]{0}, SmallInt.encode(0));
        assertEquals(0, SmallInt.decode(new byte[]{0}));
    }

    @Test
    public void testBytes()
    {
        for (int value = 0; value <= 0x3F; value++)
        {
            assertEquals(1, SmallInt.sizeOf(value));
            assertEquals(1, SmallInt.encode(value).length);
            assertEquals(value, SmallInt.decode(SmallInt.encode(value)));
        }
    }

    @Test
    public void testByteMaximum()
    {
        int value = 0x3F;
        assertEquals(1, SmallInt.sizeOf(value));
        assertArrayEquals(new byte[]{0x3F}, SmallInt.encode(value));
        assertEquals(value, SmallInt.decode(new byte[]{0x3F}));
    }

    @Test
    public void testShorts()
    {
        for (int value = 0x40; value <= 0x3FFF; value += 7)
        {
            assertEquals(2, SmallInt.sizeOf(value));
            assertEquals(2, SmallInt.encode(value).length);
            assertEquals(value, SmallInt.decode(SmallInt.encode(value)));
        }
    }

    @Test
    public void testShortMaximum()
    {
        int value = 0x3FFF;
        assertEquals(2, SmallInt.sizeOf(value));
        assertEquals(2, SmallInt.encode(value).length);
        assertArrayEquals(new byte[]{0x7F, (byte) 0xFF}, SmallInt.encode(value));
        assertEquals(value, SmallInt.decode(new byte[]{0x7F, (byte) 0xFF}));
    }

    @Test
    public void testInts()
    {
        for (int value = 0x4000; value <= SmallInt.MAX_VALUE; value += 13)
        {
            assertEquals(3, SmallInt.sizeOf(value));
            assertEquals(3, SmallInt.encode(value).length);
            assertEquals(value, SmallInt.decode(SmallInt.encode(value)));
        }
    }

    @Test
    public void testIntMaximum()
    {
        int value = SmallInt.MAX_VALUE;
        assertEquals(3, SmallInt.sizeOf(value));
        assertEquals(3, SmallInt.encode(value).length);
        assertArrayEquals(new byte[]{(byte) 0xBF, (byte) 0xFF, (byte) 0xFF}, SmallInt.encode(value));
        assertEquals(value, SmallInt.decode(new byte[]{(byte) 0xBF, (byte) 0xFF, (byte) 0xFF}));
    }

    @Test
    public void testEncoding()
    {
        // the 6 least significant bits are in the first byte, the most significant ones in the following ones
        assertArrayEquals(new byte[]{0x40, 0x01}, SmallInt.encode(0x40));
        assertArrayEquals(new byte[]{(byte) 0x80, 0x00, 0x01}, SmallInt.encode(0x4000));
        assertEquals(0x40, SmallInt.decode(new byte[]{0x40, 0x01}));
        assertEquals(0x4000, SmallInt.decode(new byte[]{(byte) 0x80, 0x00, 0x01}));
    }

    @Test
    public void testEncodeAtOffset()
    {
        byte[] bytes = new byte[5];
        int written = SmallInt.encode(0x3FFF, bytes, 2);
        assertEquals(2, written);
        assertArrayEquals(new byte[]{0, 0, 0x7F, (byte) 0xFF, 0}, bytes);
        assertEquals(0x3FFF, SmallInt.decode(bytes, 2));
    }

    @Test
    public void testToHex()
    {
        assertEquals("00", SmallInt.toHex(0));
        assertEquals("C0", SmallInt.toHex(-1));
        assertEquals("BFFFFF", SmallInt.toHex(SmallInt.MAX_VALUE));
    }

    @Test
    public void testStream() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SmallInt.write(baos, -1);
        SmallInt.write(baos, 1);
        SmallInt.write(baos, 0x1234);
        SmallInt.write(baos, 0x3FFFFF);
        byte[] bytes = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        assertEquals(-1, SmallInt.read(bais));
        assertEquals(1, SmallInt.read(bais));
        assertEquals(0x1234, SmallInt.read(bais));
        assertEquals(0x3FFFFF, SmallInt.read(bais));
    }

    @Test
    public void testReadTruncated()
    {
        assertThrows(EOFException.class, () -> SmallInt.read(new ByteArrayInputStream(new byte[]{0x40})));
        assertThrows(EOFException.class, () -> SmallInt.read(new ByteArrayInputStream(new byte[]{(byte) 0x80, 0x00})));
        assertThrows(EOFException.class, () -> SmallInt.read(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    public void testInvalidNullPrefix()
    {
        // the null prefix requires all the other bits to be 0
        assertThrows(IllegalArgumentException.class, () -> SmallInt.decode(new byte[]{(byte) 0xC1}));
        assertThrows(IllegalArgumentException.class, () -> SmallInt.decode(new byte[]{(byte) 0xFF}));
    }

    @Test
    public void testBufferTooShort()
    {
        assertThrows(IllegalArgumentException.class, () -> SmallInt.decode(new byte[]{0x40}));
        assertThrows(IllegalArgumentException.class, () -> SmallInt.decode(new byte[]{(byte) 0x80, 0x00}));
    }

    @Test
    public void testOutOfRange()
    {
        assertThrows(IllegalArgumentException.class, () -> SmallInt.sizeOf(-2));
        assertThrows(IllegalArgumentException.class, () -> SmallInt.sizeOf(SmallInt.MAX_VALUE + 1));
        assertThrows(IllegalArgumentException.class, () -> SmallInt.encode(SmallInt.MAX_VALUE + 1));
    }
}
