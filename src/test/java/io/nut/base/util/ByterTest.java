/*
 *  ByterTest.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.util;

import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ByterTest
{

    private static final ByteOrder[] BYTE_ORDER = {null, ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN};

    private static final short[] SHORTS ={1,2,3,4,5,6,7,8,9,0};
    private static final char[] CHARS = "hello world".toCharArray();
    private static final int[] INTS = {1,2,3,4,5,6,7,8,9,0};
    private static final long[] LONGS = {1,2,3,4,5,6,7,8,9,0};

    /**
     * Test of bytes method, of class Byter.
     */
    @Test
    public void testBytes_shortArr_ByteOrder()
    {
        for(ByteOrder order : BYTE_ORDER)
        {
            byte[] bytes = Byter.bytes(SHORTS, order);
            short[] back = Byter.shorts(bytes, order);
            assertArrayEquals(SHORTS, back);

            assertNull(Byter.bytes((short[])null, order));
            assertNull(Byter.shorts(null, order));
        }
    }

    /**
     * Test of bytes method, of class Byter.
     */
    @Test
    public void testBytes_charArr_ByteOrder()
    {
        for(ByteOrder order : BYTE_ORDER)
        {
            byte[] bytes = Byter.bytes(CHARS, order);
            char[] back = Byter.chars(bytes, order);
            assertArrayEquals(CHARS, back);

            assertNull(Byter.bytes((char[])null, order));
            assertNull(Byter.chars(null, order));
        }
    }

    /**
     * Test of bytes method, of class Byter.
     */
    @Test
    public void testBytes_intArr_ByteOrder()
    {
        for(ByteOrder order : BYTE_ORDER)
        {
            byte[] bytes = Byter.bytes(INTS, order);
            int[] back = Byter.ints(bytes, order);
            assertArrayEquals(INTS, back);

            assertNull(Byter.bytes((int[])null, order));
            assertNull(Byter.ints(null, order));
        }
    }

    /**
     * Test of bytes method, of class Byter.
     */
    @Test
    public void testBytes_longArr_ByteOrder()
    {
        for(ByteOrder order : BYTE_ORDER)
        {
            byte[] bytes = Byter.bytes(LONGS, order);
            long[] back = Byter.longs(bytes, order);
            assertArrayEquals(LONGS, back);

            assertNull(Byter.bytes((long[])null, order));
            assertNull(Byter.longs(null, order));
        }
    }

    /**
     * Test of bytes method, of class Byter.
     */
    @Test
    public void testBytes_shortArr()
    {
        byte[] bytes = Byter.bytes(SHORTS);
        short[] back = Byter.shorts(bytes);
        assertArrayEquals(SHORTS, back);

        assertNull(Byter.bytes((short[])null));
        assertNull(Byter.shorts(null));
    }

    /**
     * Test of bytes method, of class Byter.
     */
    @Test
    public void testBytes_charArr()
    {
        byte[] bytes = Byter.bytes(CHARS);
        char[] back = Byter.chars(bytes);
        assertArrayEquals(CHARS, back);

        assertNull(Byter.bytes((char[])null));
        assertNull(Byter.chars(null));
    }

    /**
     * Test of bytes method, of class Byter.
     */
    @Test
    public void testBytes_intArr()
    {
        byte[] bytes = Byter.bytes(INTS);
        int[] back = Byter.ints(bytes);
        assertArrayEquals(INTS, back);

        assertNull(Byter.bytes((int[])null));
        assertNull(Byter.ints(null));

        int[] array = {1,2,3,4};
        byte[] exp = {0,0,0,1,0,0,0,2,0,0,0,3,0,0,0,4};
        byte[] result = Byter.bytes(array);
        assertArrayEquals(exp, result);
    }

    /**
     * Test of bytes method, of class Byter.
     */
    @Test
    public void testBytes_longArr()
    {
        byte[] bytes = Byter.bytes(LONGS);
        long[] back = Byter.longs(bytes);
        assertArrayEquals(LONGS, back);

        assertNull(Byter.bytes((long[]) null));
        assertNull(Byter.longs(null));

        long[] array = {1,2,3,4};
        byte[] exp = {0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,2,0,0,0,0,0,0,0,3,0,0,0,0,0,0,0,4};
        byte[] result = Byter.bytes(array);
        assertArrayEquals(exp, result);
    }

    /**
     * Test of bytesLE method, of class Byter.
     */
    @Test
    public void testBytesLE_shortArr()
    {
        byte[] bytes = Byter.bytesLE(SHORTS);
        short[] back = Byter.shortsLE(bytes);
        assertArrayEquals(SHORTS, back);

        assertNull(Byter.bytesLE((short[])null));
        assertNull(Byter.shortsLE(null));
    }

    /**
     * Test of bytesLE method, of class Byter.
     */
    @Test
    public void testBytesLE_charArr()
    {
        byte[] bytes = Byter.bytesLE(CHARS);
        char[] back = Byter.charsLE(bytes);
        assertArrayEquals(CHARS, back);

        assertNull(Byter.bytesLE((char[])null));
        assertNull(Byter.charsLE(null));
    }

    /**
     * Test of bytesLE method, of class Byter.
     */
    @Test
    public void testBytesLE_intArr()
    {
        byte[] bytes = Byter.bytesLE(INTS);
        int[] back = Byter.intsLE(bytes);
        assertArrayEquals(INTS, back);

        assertNull(Byter.bytesLE((int[])null));
        assertNull(Byter.ints(null));
    }

    /**
     * Test of bytesLE method, of class Byter.
     */
    @Test
    public void testBytesLE_longArr()
    {
        byte[] bytes = Byter.bytesLE(LONGS);
        long[] back = Byter.longsLE(bytes);
        assertArrayEquals(LONGS, back);

        assertNull(Byter.bytesLE((long[])null));
        assertNull(Byter.longsLE(null));
    }

    /**
     * Test of bytesUTF8 method, of class Byter.
     */
    @Test
    public void testBytesUTF8()
    {
        byte[] bytes = Byter.bytesUTF8(CHARS);
        char[] back = Byter.charsUTF8(bytes);
        assertArrayEquals(CHARS, back);

        assertNull(Byter.bytesUTF8((char[])null));
        assertNull(Byter.charsUTF8(null));
    }

}
