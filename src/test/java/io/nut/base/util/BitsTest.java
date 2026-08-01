/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

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
