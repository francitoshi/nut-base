/*
 * BitsTest.java
 *
 * Copyright (c) 2023 francitoshi@gmail.com
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

import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class BitsTest
{
    
    public BitsTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
    }
    
    @AfterAll
    public static void tearDownClass()
    {
    }
    
    @BeforeEach
    public void setUp()
    {
    }
    
    @AfterEach
    public void tearDown()
    {
    }
    /**
     * Test of bitCount method, of class Utils.
     */
    @Test
    public void testCardinality_byte()
    {
        assertEquals(0, Bits.bitCount((byte)0));
        assertEquals(1, Bits.bitCount((byte)1));
        assertEquals(1, Bits.bitCount((byte)2));
        assertEquals(2, Bits.bitCount((byte)3));
        assertEquals(1, Bits.bitCount((byte)4));
        assertEquals(2, Bits.bitCount((byte)5));
        assertEquals(2, Bits.bitCount((byte)6));
        assertEquals(3, Bits.bitCount((byte)7));
        assertEquals(1, Bits.bitCount((byte)8));
        assertEquals(2, Bits.bitCount((byte)9));
        assertEquals(3, Bits.bitCount((byte)13));
        assertEquals(3, Bits.bitCount((byte)21));
        assertEquals(2, Bits.bitCount((byte)34));
        assertEquals(5, Bits.bitCount((byte)55));
        assertEquals(4, Bits.bitCount((byte)89));
        assertEquals(7, Bits.bitCount((byte)127));

        assertEquals(8, Bits.bitCount((byte)-1));
        assertEquals(7, Bits.bitCount((byte)-2));
        assertEquals(7, Bits.bitCount((byte)-3));
        assertEquals(6, Bits.bitCount((byte)-4));
        assertEquals(7, Bits.bitCount((byte)-5));
        assertEquals(6, Bits.bitCount((byte)-6));
        assertEquals(6, Bits.bitCount((byte)-7));
        assertEquals(5, Bits.bitCount((byte)-8));
        assertEquals(7, Bits.bitCount((byte)-9));
        assertEquals(6, Bits.bitCount((byte)-13));
        assertEquals(6, Bits.bitCount((byte)-21));
        assertEquals(6, Bits.bitCount((byte)-34));
        assertEquals(4, Bits.bitCount((byte)-55));
        assertEquals(5, Bits.bitCount((byte)-89));
        assertEquals(2, Bits.bitCount((byte)-127));
        assertEquals(1, Bits.bitCount((byte)-128));
    }

    /**
     * Test of bitCount method, of class Utils.
     */
    @Test
    public void testCardinality_short()
    {
        assertEquals(0, Bits.bitCount((short)0));
        assertEquals(1, Bits.bitCount((short)1));
        assertEquals(1, Bits.bitCount((short)2));
        assertEquals(2, Bits.bitCount((short)3));
        assertEquals(1, Bits.bitCount((short)4));
        assertEquals(2, Bits.bitCount((short)5));
        assertEquals(2, Bits.bitCount((short)6));
        assertEquals(3, Bits.bitCount((short)7));
        assertEquals(1, Bits.bitCount((short)8));
        assertEquals(2, Bits.bitCount((short)9));
        assertEquals(3, Bits.bitCount((short)13));
        assertEquals(3, Bits.bitCount((short)21));
        assertEquals(2, Bits.bitCount((short)34));
        assertEquals(5, Bits.bitCount((short)55));
        assertEquals(4, Bits.bitCount((short)89));
        assertEquals(7, Bits.bitCount((short)127));
        
        assertEquals(16, Bits.bitCount((short)-1));
        assertEquals(15, Bits.bitCount((short)-2));
        assertEquals(15, Bits.bitCount((short)-3));
        assertEquals(14, Bits.bitCount((short)-4));
        assertEquals(15, Bits.bitCount((short)-5));
        assertEquals(14, Bits.bitCount((short)-6));
        assertEquals(14, Bits.bitCount((short)-7));
        assertEquals(13, Bits.bitCount((short)-8));
        assertEquals(15, Bits.bitCount((short)-9));
        assertEquals(14, Bits.bitCount((short)-13));
        assertEquals(14, Bits.bitCount((short)-21));
        assertEquals(14, Bits.bitCount((short)-34));
        assertEquals(12, Bits.bitCount((short)-55));
        assertEquals(13, Bits.bitCount((short)-89));
        assertEquals(10, Bits.bitCount((short)-127));
        assertEquals(9, Bits.bitCount((short)-128));
        assertEquals(2, Bits.bitCount((short)-16384));
    }

    /**
     * Test of bitCount method, of class Utils.
     */
    @Test
    public void testCardinality_char()
    {
        assertEquals(0, Bits.bitCount((char)0));
        assertEquals(1, Bits.bitCount((char)1));
        assertEquals(1, Bits.bitCount((char)2));
        assertEquals(2, Bits.bitCount((char)3));
        assertEquals(1, Bits.bitCount((char)4));
        assertEquals(2, Bits.bitCount((char)5));
        assertEquals(2, Bits.bitCount((char)6));
        assertEquals(3, Bits.bitCount((char)7));
        assertEquals(1, Bits.bitCount((char)8));
        assertEquals(2, Bits.bitCount((char)9));
        assertEquals(3, Bits.bitCount((char)13));
        assertEquals(3, Bits.bitCount((char)21));
        assertEquals(2, Bits.bitCount((char)34));
        assertEquals(5, Bits.bitCount((char)55));
        assertEquals(4, Bits.bitCount((char)89));
        assertEquals(7, Bits.bitCount((char)127));
        
        assertEquals(16, Bits.bitCount((char)-1));
        assertEquals(15, Bits.bitCount((char)-2));
        assertEquals(15, Bits.bitCount((char)-3));
        assertEquals(14, Bits.bitCount((char)-4));
        assertEquals(15, Bits.bitCount((char)-5));
        assertEquals(14, Bits.bitCount((char)-6));
        assertEquals(14, Bits.bitCount((char)-7));
        assertEquals(13, Bits.bitCount((char)-8));
        assertEquals(15, Bits.bitCount((char)-9));
        assertEquals(14, Bits.bitCount((char)-13));
        assertEquals(14, Bits.bitCount((char)-21));
        assertEquals(14, Bits.bitCount((char)-34));
        assertEquals(12, Bits.bitCount((char)-55));
        assertEquals(13, Bits.bitCount((char)-89));
        assertEquals(10, Bits.bitCount((char)-127));
        assertEquals(9, Bits.bitCount((char)-128));
        assertEquals(2, Bits.bitCount((char)-16384));
    }

}
