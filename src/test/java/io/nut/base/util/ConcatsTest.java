/*
 * ConcatsTest.java
 *
 * Copyright (c) 2023-2025 francitoshi@gmail.com
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
public class ConcatsTest
{
    /**
     * Test of cat method, of class Concats.
     */
    @Test
    public void testCat_byteArr_byteArr()
    {
        byte[] src = {1,2};
        byte[] expResult = {1,2,3,4};
        byte[] result = Concats.cat(src, (byte)3,(byte)4);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Concats.
     */
    @Test
    public void testCat_intArr_intArr()
    {
        int[] src = {1,2};
        int[] expResult = {1,2,3,4};
        int[] result = Concats.cat(src, 3,4);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Concats.
     */
    @Test
    public void testCat_shortArr_shortArr()
    {
        short[] src = {1,2};
        short[] expResult = {1,2,3,4};
        short[] result = Concats.cat(src, (short)3,(short)4);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Concats.
     */
    @Test
    public void testCat_charArr_charArr()
    {
        char[] src = {1,2};
        char[] expResult = {1,2,3,4};
        char[] result = Concats.cat(src, (char)3,(char)4);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Concats.
     */
    @Test
    public void testCat_longArr_longArr()
    {
        long[] src = {1,2};
        long[] expResult = {1,2,3,4};
        long[] result = Concats.cat(src, 3L, 4L);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Concats.
     */
    @Test
    public void testCat_floatArr_floatArr()
    {
        float[] src = {1,2};
        float[] expResult = {1,2,3,4};
        float[] result = Concats.cat(src, 3f, 4f);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Concats.
     */
    @Test
    public void testCat_doubleArr_doubleArr()
    {
        double[] src = {1,2};
        double[] expResult = {1,2,3,4};
        double[] result = Concats.cat(src, 3d, 4d);
        assertArrayEquals(expResult, result);
    }

}
