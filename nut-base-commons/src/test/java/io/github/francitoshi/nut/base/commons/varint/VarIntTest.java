/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.varint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class VarIntTest
{

    @Test
    public void testBytes()
    {
        long value = 10;
        assertEquals(1, VarInt.sizeOf(value));
        assertEquals(1, VarInt.encode(value).length);
        assertEquals(value, VarInt.decode(VarInt.encode(value)));
    }

    @Test
    public void testShorts()
    {
        long value = 64000;
        assertEquals(3, VarInt.sizeOf(value));
        assertEquals(3, VarInt.encode(value).length);
        assertEquals(value, VarInt.decode(VarInt.encode(value)));
    }

    @Test
    public void testShortFFFF()
    {
        long value = 0xFFFFL;
        assertEquals(3, VarInt.sizeOf(value));
        assertEquals(3, VarInt.encode(value).length);
        assertEquals(value, VarInt.decode(VarInt.encode(value)));
    }

    @Test
    public void testInts()
    {
        long value = 0xAABBCCDDL;
        assertEquals(5, VarInt.sizeOf(value));
        assertEquals(5, VarInt.encode(value).length);
        assertEquals(value, VarInt.decode(VarInt.encode(value)));
    }

    @Test
    public void testIntFFFFFFFF()
    {
        long value = 0xFFFFFFFFL;
        assertEquals(5, VarInt.sizeOf(value));
        assertEquals(5, VarInt.encode(value).length);
        assertEquals(value, VarInt.decode(VarInt.encode(value)));
    }

    @Test
    public void testLong()
    {
        long value = Long.MAX_VALUE;
        assertEquals(9, VarInt.sizeOf(value));
        assertEquals(9, VarInt.encode(value).length);
        assertEquals(value, VarInt.decode(VarInt.encode(value)));
    }

    @Test
    public void testSizeOfZero()
    {
        assertEquals(VarInt.sizeOf(0), VarInt.encode(0).length);
    }
}
