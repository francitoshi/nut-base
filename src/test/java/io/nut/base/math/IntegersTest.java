/*
 *  IntegersTest.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.math;

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
public class IntegersTest
{
    
    public IntegersTest()
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


    @Test
    public void testReadUint16()
    {
        assertEquals(258L, Integers.readUint16(new byte[]
        {
            2, 1
        }, 0));
        assertEquals(258L, Integers.readUint16(new byte[]
        {
            2, 1, 3, 4
        }, 0));
        assertEquals(772L, Integers.readUint16(new byte[]
        {
            1, 2, 4, 3
        }, 2));
    }

    @Test
    public void testReadUint32()
    {
        assertEquals(258L, Integers.readUint32(new byte[]
        {
            2, 1, 0, 0
        }, 0));
        assertEquals(258L, Integers.readUint32(new byte[]
        {
            2, 1, 0, 0, 3, 4
        }, 0));
        assertEquals(772L, Integers.readUint32(new byte[]
        {
            1, 2, 4, 3, 0, 0
        }, 2));
    }

    
    @Test
    public void testReadInt64()
    {
        assertEquals(258L, Integers.readInt64(new byte[]
        {
            2, 1, 0, 0, 0, 0, 0, 0
        }, 0));
        assertEquals(258L, Integers.readInt64(new byte[]
        {
            2, 1, 0, 0, 0, 0, 0, 0, 3, 4
        }, 0));
        assertEquals(772L, Integers.readInt64(new byte[]
        {
            1, 2, 4, 3, 0, 0, 0, 0, 0, 0
        }, 2));
        assertEquals(-1L, Integers.readInt64(new byte[]
        {
            -1, -1, -1, -1, -1, -1, -1, -1
        }, 0));
    }

    @Test
    public void testReadUInt32BE()
    {
        assertEquals(258L, Integers.readUint32BE(new byte[]
        {
            0, 0, 1, 2
        }, 0));
        assertEquals(258L, Integers.readUint32BE(new byte[]
        {
            0, 0, 1, 2, 3, 4
        }, 0));
        assertEquals(772L, Integers.readUint32BE(new byte[]
        {
            1, 2, 0, 0, 3, 4
        }, 2));
    }

    @Test
    public void testReadUint16BE()
    {
        assertEquals(258L, Integers.readUint16BE(new byte[]
        {
            1, 2
        }, 0));
        assertEquals(258L, Integers.readUint16BE(new byte[]
        {
            1, 2, 3, 4
        }, 0));
        assertEquals(772L, Integers.readUint16BE(new byte[]
        {
            0, 0, 3, 4
        }, 2));
    }

    @Test
    public void testUnsigned()
    {
        int[] items =
        {
            0, 1, 255, 256, 32000, 33000, 65000
        };
        for (int i : items)
        {
            short s = (short) i;
            int u = Integers.unsigned(s);
            assertTrue(u >= 0, "u=" + u);

            assertEquals(258L, Integers.readUint16BE(new byte[]
            {
                1, 2
            }, 0));

        }
    }
    
}
