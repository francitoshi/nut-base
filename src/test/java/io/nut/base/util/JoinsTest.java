/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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
        List<String> c = As.list("a","b","c");
        List<String> d = As.list("d","e");
        
        List<String> result = Joins.join(a,b,c, d);
        List<String> expected = As.list("a","b","c","d","e");
        assertEquals(expected, result);
    }

    /**
     * Test of cat method, of class Joins.
     */
    @Test
    public void testCat_byteArr_byteArr()
    {
        byte[] src = {1,2};
        byte[] expResult = {1,2,3,4};
        byte[] result = Joins.cat(src, (byte)3,(byte)4);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Joins.
     */
    @Test
    public void testCat_intArr_intArr()
    {
        int[] src = {1,2};
        int[] expResult = {1,2,3,4};
        int[] result = Joins.cat(src, 3,4);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Joins.
     */
    @Test
    public void testCat_shortArr_shortArr()
    {
        short[] src = {1,2};
        short[] expResult = {1,2,3,4};
        short[] result = Joins.cat(src, (short)3,(short)4);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Joins.
     */
    @Test
    public void testCat_charArr_charArr()
    {
        char[] src = {1,2};
        char[] expResult = {1,2,3,4};
        char[] result = Joins.cat(src, (char)3,(char)4);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Joins.
     */
    @Test
    public void testCat_longArr_longArr()
    {
        long[] src = {1,2};
        long[] expResult = {1,2,3,4};
        long[] result = Joins.cat(src, 3L, 4L);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Joins.
     */
    @Test
    public void testCat_floatArr_floatArr()
    {
        float[] src = {1,2};
        float[] expResult = {1,2,3,4};
        float[] result = Joins.cat(src, 3f, 4f);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of cat method, of class Joins.
     */
    @Test
    public void testCat_doubleArr_doubleArr()
    {
        double[] src = {1,2};
        double[] expResult = {1,2,3,4};
        double[] result = Joins.cat(src, 3d, 4d);
        assertArrayEquals(expResult, result);
    }
}
