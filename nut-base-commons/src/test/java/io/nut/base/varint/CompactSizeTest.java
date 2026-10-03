/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.varint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class CompactSizeTest
{

    @Test
    public void testBytes()
    {
        long value = 10;
        assertEquals(1, CompactSize.sizeOf(value));
        assertEquals(1, CompactSize.encode(value).length);
        assertEquals(value, CompactSize.decode(CompactSize.encode(value)));
    }

    @Test
    public void testShorts()
    {
        long value = 64000;
        assertEquals(3, CompactSize.sizeOf(value));
        assertEquals(3, CompactSize.encode(value).length);
        assertEquals(value, CompactSize.decode(CompactSize.encode(value)));
    }

    @Test
    public void testShortFFFF()
    {
        long value = 0xFFFFL;
        assertEquals(3, CompactSize.sizeOf(value));
        assertEquals(3, CompactSize.encode(value).length);
        assertEquals(value, CompactSize.decode(CompactSize.encode(value)));
    }

    @Test
    public void testInts()
    {
        long value = 0xAABBCCDDL;
        assertEquals(5, CompactSize.sizeOf(value));
        assertEquals(5, CompactSize.encode(value).length);
        assertEquals(value, CompactSize.decode(CompactSize.encode(value)));
    }

    @Test
    public void testIntFFFFFFFF()
    {
        long value = 0xFFFFFFFFL;
        assertEquals(5, CompactSize.sizeOf(value));
        assertEquals(5, CompactSize.encode(value).length);
        assertEquals(value, CompactSize.decode(CompactSize.encode(value)));
    }

    @Test
    public void testLong()
    {
        long value = 0xCAFEBABEDEADBEEFL;
        assertEquals(9, CompactSize.sizeOf(value));
        assertEquals(9, CompactSize.encode(value).length);
        assertEquals(value, CompactSize.decode(CompactSize.encode(value)));
    }

    @Test
    public void testSizeOfZeroInt()
    {
        assertEquals(CompactSize.sizeOf(0), CompactSize.encode(0).length);
    }

    @Test
    public void testSizeOfNegativeInt()
    {
        assertEquals(CompactSize.sizeOf(-1), CompactSize.encode(-1).length);
    }
}
