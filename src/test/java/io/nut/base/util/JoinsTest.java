/*
 * UtilsTest.java
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

import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class JoinsTest
{

    /**
     * Test of join method, of class Joins.
     */
    @Test
    public void testJoin_intArrArr()
    {
        int[][] src = 
        {
            {1},
            {1,2},
            {1,2,3},
            {},
            null,
            {1,2,3,4},
        };
        int[] exp = {1,1,2,1,2,3,1,2,3,4};
        
        int[] result = Joins.join(src);
        assertArrayEquals(exp, result);
    }

    /**
     * Test of join method, of class Joins.
     */
    @Test
    public void testJoin_longArrArr()
    {
        long[][] src = 
        {
            {1},
            {1,2},
            {1,2,3},
            {},
            null,
            {1,2,3,4},
        };
        long[] exp = {1,1,2,1,2,3,1,2,3,4};
        
        long[] result = Joins.join(src);
        assertArrayEquals(exp, result);
    }

    /**
     * Test of join method, of class Joins.
     */
    @Test
    public void testJoin_shortArrArr()
    {
        short[][] src = 
        {
            {1},
            {1,2},
            {1,2,3},
            {},
            null,
            {1,2,3,4},
        };
        short[] exp = {1,1,2,1,2,3,1,2,3,4};
        
        short[] result = Joins.join(src);
        assertArrayEquals(exp, result);
    }

    /**
     * Test of join method, of class Joins.
     */
    @Test
    public void testJoin_charArrArr()
    {
        char[][] src = 
        {
            {1},
            {1,2},
            {1,2,3},
            {},
            null,
            {1,2,3,4},
        };
        char[] exp = {1,1,2,1,2,3,1,2,3,4};
        
        char[] result = Joins.join(src);
        assertArrayEquals(exp, result);
    }

    /**
     * Test of join method, of class Joins.
     */
    @Test
    public void testJoin_floatArrArr()
    {
        float[][] src = 
        {
            {1},
            {1,2},
            {1,2,3},
            {},
            null,
            {1,2,3,4},
        };
        float[] exp = {1,1,2,1,2,3,1,2,3,4};
        
        float[] result = Joins.join(src);
        assertArrayEquals(exp, result, 0);
    }

    /**
     * Test of join method, of class Joins.
     */
    @Test
    public void testJoin_doubleArrArr()
    {
        double[][] src = 
        {
            {1},
            {1,2},
            {1,2,3},
            {},
            null,
            {1,2,3,4},
        };
        double[] exp = {1,1,2,1,2,3,1,2,3,4};
        
        double[] result = Joins.join(src);
        assertArrayEquals(exp, result, 0);
    }

    /**
     * Test of join method, of class Joins.
     */
    @Test
    public void testJoin_GenericType()
    {
        String[][] src = 
        {
            {"1"},
            {"1","2"},
            {"1","2","3"},
            {},
            null,
            {"1","2","3","4"},
        };
        String[] exp = {"1","1","2","1","2","3","1","2","3","4"};
        
        String[] result = Joins.join(String.class, src);
        assertArrayEquals(exp, result);
    }
    /**
     * Test of join method, of class Joins.
     */
    @Test
    public void testJoin_GenericType2()
    {
        String[] src0 = {"1"};
        String[] src1 = {"1","2"};
        String[] src2 = {"1","2","3"};
        String[] src3 = {};
        String[] src4 = null;
        String[] src5 = {"1","2","3","4"};
        
        String[][] src = {src0, src1, src2, src3, src4, src5};
        
        String[] exp = {"1","1","2","1","2","3","1","2","3","4"};
        
        String[] result1 = Joins.join(src0,src1,src2,src3,src4,src5);
        String[] result2 = Joins.join(src);
        
        assertArrayEquals(exp, result1);
        assertArrayEquals(exp, result2);
    }

    /**
     * Test of join method, of class Joins.
     */
    @Test
    public void testJoin_StringArr()
    {
        String[] src = {"1","1","2","1","2","3","",null,"1","2","3","4"};
        String exp = "1121231234";
        
        String result = Joins.join(src);
        assertEquals(exp, result);
    }

    /**
     * Test of join method, of class Joins.
     */
    @Test
    public void testJoin_byteArrArr()
    {
        byte[][] src = 
        {
            {1},
            {1,2},
            {1,2,3},
            {},
            null,
            {1,2,3,4},
        };
        byte[] exp = {1,1,2,1,2,3,1,2,3,4};
        
        byte[] result = Joins.join(src);
        assertArrayEquals(exp, result);
    }

    /**
     * Test of join method, of class Joins.
     */
    @Test
    public void testJoin_ListArr()
    {
        List<String> a = null;
        List<String> b = Collections.EMPTY_LIST;
        List<String> c = Utils.listOf("a","b","c");
        List<String> d = Utils.listOf("d","e");
        
        List<String> result = Joins.join(a,b,c, d);
        List<String> expected = Utils.listOf("a","b","c","d","e");
        assertEquals(expected, result);
    }

}
