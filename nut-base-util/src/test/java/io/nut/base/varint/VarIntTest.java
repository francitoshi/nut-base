/*
 * VarIntTest.java
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
