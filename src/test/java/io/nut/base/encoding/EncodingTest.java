/*
 *  EncodingTest.java
 *
 *  Copyright (C) 2024 francitoshi@gmail.com
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
package io.nut.base.encoding;

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
        {
            byte[] result = Encoding.decode("JxF12TrwUP45BMd", Encoding.Type.Base58);
            assertNotNull(result);
            assertEquals("Hello World", new String(result));
        }
        
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
