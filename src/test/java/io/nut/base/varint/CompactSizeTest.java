/*
 * CompactSizeTest.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
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
