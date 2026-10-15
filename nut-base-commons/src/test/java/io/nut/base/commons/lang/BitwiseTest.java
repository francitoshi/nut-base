/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.lang;

import io.nut.base.commons.lang.Bitwise;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class BitwiseTest
{
    
    @Test
    public void testCardinality_byte()
    {
        assertEquals(0, Bitwise.bitCount((byte)0));
        assertEquals(1, Bitwise.bitCount((byte)1));
        assertEquals(1, Bitwise.bitCount((byte)2));
        assertEquals(2, Bitwise.bitCount((byte)3));
        assertEquals(1, Bitwise.bitCount((byte)4));
        assertEquals(2, Bitwise.bitCount((byte)5));
        assertEquals(2, Bitwise.bitCount((byte)6));
        assertEquals(3, Bitwise.bitCount((byte)7));
        assertEquals(1, Bitwise.bitCount((byte)8));
        assertEquals(2, Bitwise.bitCount((byte)9));
        assertEquals(3, Bitwise.bitCount((byte)13));
        assertEquals(3, Bitwise.bitCount((byte)21));
        assertEquals(2, Bitwise.bitCount((byte)34));
        assertEquals(5, Bitwise.bitCount((byte)55));
        assertEquals(4, Bitwise.bitCount((byte)89));
        assertEquals(7, Bitwise.bitCount((byte)127));

        assertEquals(8, Bitwise.bitCount((byte)-1));
        assertEquals(7, Bitwise.bitCount((byte)-2));
        assertEquals(7, Bitwise.bitCount((byte)-3));
        assertEquals(6, Bitwise.bitCount((byte)-4));
        assertEquals(7, Bitwise.bitCount((byte)-5));
        assertEquals(6, Bitwise.bitCount((byte)-6));
        assertEquals(6, Bitwise.bitCount((byte)-7));
        assertEquals(5, Bitwise.bitCount((byte)-8));
        assertEquals(7, Bitwise.bitCount((byte)-9));
        assertEquals(6, Bitwise.bitCount((byte)-13));
        assertEquals(6, Bitwise.bitCount((byte)-21));
        assertEquals(6, Bitwise.bitCount((byte)-34));
        assertEquals(4, Bitwise.bitCount((byte)-55));
        assertEquals(5, Bitwise.bitCount((byte)-89));
        assertEquals(2, Bitwise.bitCount((byte)-127));
        assertEquals(1, Bitwise.bitCount((byte)-128));
    }

    @Test
    public void testCardinality_short()
    {
        assertEquals(0, Bitwise.bitCount((short)0));
        assertEquals(1, Bitwise.bitCount((short)1));
        assertEquals(1, Bitwise.bitCount((short)2));
        assertEquals(2, Bitwise.bitCount((short)3));
        assertEquals(1, Bitwise.bitCount((short)4));
        assertEquals(2, Bitwise.bitCount((short)5));
        assertEquals(2, Bitwise.bitCount((short)6));
        assertEquals(3, Bitwise.bitCount((short)7));
        assertEquals(1, Bitwise.bitCount((short)8));
        assertEquals(2, Bitwise.bitCount((short)9));
        assertEquals(3, Bitwise.bitCount((short)13));
        assertEquals(3, Bitwise.bitCount((short)21));
        assertEquals(2, Bitwise.bitCount((short)34));
        assertEquals(5, Bitwise.bitCount((short)55));
        assertEquals(4, Bitwise.bitCount((short)89));
        assertEquals(7, Bitwise.bitCount((short)127));
        
        assertEquals(16, Bitwise.bitCount((short)-1));
        assertEquals(15, Bitwise.bitCount((short)-2));
        assertEquals(15, Bitwise.bitCount((short)-3));
        assertEquals(14, Bitwise.bitCount((short)-4));
        assertEquals(15, Bitwise.bitCount((short)-5));
        assertEquals(14, Bitwise.bitCount((short)-6));
        assertEquals(14, Bitwise.bitCount((short)-7));
        assertEquals(13, Bitwise.bitCount((short)-8));
        assertEquals(15, Bitwise.bitCount((short)-9));
        assertEquals(14, Bitwise.bitCount((short)-13));
        assertEquals(14, Bitwise.bitCount((short)-21));
        assertEquals(14, Bitwise.bitCount((short)-34));
        assertEquals(12, Bitwise.bitCount((short)-55));
        assertEquals(13, Bitwise.bitCount((short)-89));
        assertEquals(10, Bitwise.bitCount((short)-127));
        assertEquals(9, Bitwise.bitCount((short)-128));
        assertEquals(2, Bitwise.bitCount((short)-16384));
    }

    @Test
    public void testCardinality_char()
    {
        assertEquals(0, Bitwise.bitCount((char)0));
        assertEquals(1, Bitwise.bitCount((char)1));
        assertEquals(1, Bitwise.bitCount((char)2));
        assertEquals(2, Bitwise.bitCount((char)3));
        assertEquals(1, Bitwise.bitCount((char)4));
        assertEquals(2, Bitwise.bitCount((char)5));
        assertEquals(2, Bitwise.bitCount((char)6));
        assertEquals(3, Bitwise.bitCount((char)7));
        assertEquals(1, Bitwise.bitCount((char)8));
        assertEquals(2, Bitwise.bitCount((char)9));
        assertEquals(3, Bitwise.bitCount((char)13));
        assertEquals(3, Bitwise.bitCount((char)21));
        assertEquals(2, Bitwise.bitCount((char)34));
        assertEquals(5, Bitwise.bitCount((char)55));
        assertEquals(4, Bitwise.bitCount((char)89));
        assertEquals(7, Bitwise.bitCount((char)127));
        
        assertEquals(16, Bitwise.bitCount((char)-1));
        assertEquals(15, Bitwise.bitCount((char)-2));
        assertEquals(15, Bitwise.bitCount((char)-3));
        assertEquals(14, Bitwise.bitCount((char)-4));
        assertEquals(15, Bitwise.bitCount((char)-5));
        assertEquals(14, Bitwise.bitCount((char)-6));
        assertEquals(14, Bitwise.bitCount((char)-7));
        assertEquals(13, Bitwise.bitCount((char)-8));
        assertEquals(15, Bitwise.bitCount((char)-9));
        assertEquals(14, Bitwise.bitCount((char)-13));
        assertEquals(14, Bitwise.bitCount((char)-21));
        assertEquals(14, Bitwise.bitCount((char)-34));
        assertEquals(12, Bitwise.bitCount((char)-55));
        assertEquals(13, Bitwise.bitCount((char)-89));
        assertEquals(10, Bitwise.bitCount((char)-127));
        assertEquals(9, Bitwise.bitCount((char)-128));
        assertEquals(2, Bitwise.bitCount((char)-16384));
    }

    @Test
    public void testXor()
    {
        byte[] x0 = {3,  1, 1};
        byte[] y0 = {12, 1, 2, 3};
        byte[] xy0= {15, 0, 3, 3};
                
        byte[] result0 = Bitwise.xor(x0, y0);
        assertArrayEquals(xy0, result0);

        byte[] result = Bitwise.xor(y0, x0);
        assertArrayEquals(xy0, result);
    }

    @Test
    public void testRollXor()
    {
        long xor = 1234567890L;
        byte[] data = {0, 1, 2, 3, 4};
        assertArrayEquals(data, Bitwise.rollXor(Bitwise.rollXor(data, xor), xor));
    }

    @Test
    public void testBitSet()
    {
        assertEquals(0b00000001, Bitwise.bitSet((byte) 0b00000000, 0, true));
        assertEquals(0b00000010, Bitwise.bitSet((byte) 0b00000000, 1, true));
        assertEquals(0b00000100, Bitwise.bitSet((byte) 0b00000000, 2, true));
        assertEquals(0b00001000, Bitwise.bitSet((byte) 0b00000000, 3, true));
        assertEquals(0b00010000, Bitwise.bitSet((byte) 0b00000000, 4, true));
        assertEquals(0b00100000, Bitwise.bitSet((byte) 0b00000000, 5, true));
        assertEquals(0b01000000, Bitwise.bitSet((byte) 0b00000000, 6, true));
        assertEquals((byte) 0b10000000, Bitwise.bitSet((byte) 0b00000000, 7, true));

        assertEquals((byte) 0b11111110, Bitwise.bitSet((byte) 0b11111111, 0, false));
        assertEquals((byte) 0b11111101, Bitwise.bitSet((byte) 0b11111111, 1, false));
        assertEquals((byte) 0b11111011, Bitwise.bitSet((byte) 0b11111111, 2, false));
        assertEquals((byte) 0b11110111, Bitwise.bitSet((byte) 0b11111111, 3, false));
        assertEquals((byte) 0b11101111, Bitwise.bitSet((byte) 0b11111111, 4, false));
        assertEquals((byte) 0b11011111, Bitwise.bitSet((byte) 0b11111111, 5, false));
        assertEquals((byte) 0b10111111, Bitwise.bitSet((byte) 0b11111111, 6, false));
        assertEquals((byte) 0b01111111, Bitwise.bitSet((byte) 0b11111111, 7, false));
    }
}
