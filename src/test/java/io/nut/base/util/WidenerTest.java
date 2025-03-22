/*
 *  WidenerTest.java
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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class WidenerTest
{
    
    /**
     * Test of shorts method, of class Widener.
     */
    @Test
    public void testAll()
    {
        {
            byte[] src = null;
            assertNull(Widener.shorts(src));
            assertNull(Widener.ints(src));
            assertNull(Widener.longs(src));
        }
        {
            short[] src = null;
            assertNull(Widener.ints(src));
            assertNull(Widener.longs(src));
        }
        {
            int[] src = null;
            assertNull(Widener.longs(src));
        }
        {
            byte[] src = {};
            assertEquals(0, Widener.shorts(src).length);
        }
        {
            byte[] src = {};
            assertEquals(0, Widener.shorts(src).length);
            assertEquals(0, Widener.ints(src).length);
            assertEquals(0, Widener.longs(src).length);
        }
        {
            short[] src = {};
            assertEquals(0, Widener.ints(src).length);
            assertEquals(0, Widener.longs(src).length);
        }
        {
            int[] src = {};
            assertEquals(0, Widener.longs(src).length);
        }
        
        {
            byte[] bytes   = {Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE};
            short[] shorts = {Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE};
            int[] ints     = {Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE};
            long[] longs   = {Byte.MIN_VALUE, -1, 0, +1, Byte.MAX_VALUE};
            
            assertArrayEquals(shorts, Widener.shorts(bytes));
            assertArrayEquals(ints, Widener.ints(bytes));
            assertArrayEquals(longs, Widener.longs(bytes));

            assertArrayEquals(ints, Widener.ints(shorts));
            assertArrayEquals(longs, Widener.longs(shorts));

            assertArrayEquals(longs, Widener.longs(ints));
            
        }
    }
}
