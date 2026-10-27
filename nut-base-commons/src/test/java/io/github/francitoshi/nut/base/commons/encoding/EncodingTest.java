/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.encoding;

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
public class EncodingTest
{
    
    public EncodingTest()
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

    static final String HELLO_WORLD = "Hello World!";
    
    /**
     * Test of stringToBytes method, of class Utils.
     */
    @Test
    public void testSome()
    {
        
        for(Encoding.Type encoding : Encoding.Type.values())
        {
            String encoded = Encoding.encode(HELLO_WORLD.getBytes(), encoding);
            byte[] decoded = Encoding.decode(encoded, encoding);
            assertEquals(HELLO_WORLD, new String(decoded));
        }
    }
    /**
     * Test of bytesToString method, of class Utils.
     */
    @Test
    public void testEncodeDecode()
    {        
        for(Encoding.Type type : Encoding.Type.values())
        {
            System.out.println(type);
            byte[] src = new byte[1];
            for(int i=Byte.MIN_VALUE;i<=Byte.MAX_VALUE;i++)
            {
                src[0] = (byte)i;
                String result = Encoding.encode(src, type);
                byte[] dst = Encoding.decode(result, type);
                assertArrayEquals(src, dst);
            }
            src = new byte[256];
            for(int i=0;i<src.length;i++)
            {
                src[i] = (byte)i;
            }
            String result = Encoding.encode(src, type);
            byte[] dst = Encoding.decode(result, type);
            assertArrayEquals(src, dst);
        }
    }
    
}
