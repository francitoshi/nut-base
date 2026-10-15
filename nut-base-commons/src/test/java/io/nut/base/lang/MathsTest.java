/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class MathsTest
{

    /**
     * Test of equalsEnough method, of class Maths.
     */
    @Test
    public void testEqualsEnough_double()
    {
        double d0 = 0;
        double d1 = 0.000001;
        double d2 = 0.000002;
        double delta0 = 0.0;
        double delta1 = 0.000001;
        double delta2 = 0.000002;

        assertTrue(Maths.equalsEnough(d0, d0, delta0));
        assertTrue(Maths.equalsEnough(d0, d0, delta1));
        assertTrue(Maths.equalsEnough(d0, d0, delta2));

        assertFalse(Maths.equalsEnough(d1, d2, delta0));
        assertFalse(Maths.equalsEnough(d0, d2, delta1));
        assertTrue(Maths.equalsEnough(d0, d2, delta2));

        assertFalse(Maths.equalsEnough(d2, d1, delta0));
        assertFalse(Maths.equalsEnough(d2, d0, delta1));
        assertTrue(Maths.equalsEnough(d2, d0, delta2));

        assertTrue(Maths.equalsEnough(0, 0, 0));
        assertTrue(Maths.equalsEnough(0.4, 0.6, 0.5));
        assertTrue(Maths.equalsEnough(1.9, 2.1, 0.5));
        assertTrue(Maths.equalsEnough(0.000001, 0.000002, 0.000002));

        assertFalse(Maths.equalsEnough(0, 1, 0));
        assertFalse(Maths.equalsEnough(0.4, 0.6, 0.1));
        assertFalse(Maths.equalsEnough(1.9, 2.1, 0.1));
        assertFalse(Maths.equalsEnough(0.000001, 0.000003, 0.000001));
    }

    /**
     * Test of equalsEnough method, of class Maths.
     */
    @Test
    public void testEqualsEnough_float()
    {
        float d0 = 0;
        float d1 = 0.001f;
        float d2 = 0.002f;
        float delta0 = 0.0f;
        float delta1 = 0.001f;
        float delta2 = 0.002f;

        assertTrue(Maths.equalsEnough(d0, d0, delta0));
        assertTrue(Maths.equalsEnough(d0, d0, delta1));
        assertTrue(Maths.equalsEnough(d0, d0, delta2));

        assertFalse(Maths.equalsEnough(d1, d2, delta0));
        assertFalse(Maths.equalsEnough(d0, d2, delta1));
        assertTrue(Maths.equalsEnough(d0, d2, delta2));

        assertFalse(Maths.equalsEnough(d2, d1, delta0));
        assertFalse(Maths.equalsEnough(d2, d0, delta1));
        assertTrue(Maths.equalsEnough(d2, d0, delta2));

        assertTrue(Maths.equalsEnough(0f, 0f, 0f));
        assertTrue(Maths.equalsEnough(0f, 1f, 1f));
        assertFalse(Maths.equalsEnough(0f, 10f, 1f));

        assertTrue(Maths.equalsEnough(0.1001f, 0.1f, 0.01f));
        assertFalse(Maths.equalsEnough(0.1001f, 0.2f, 0.01f));
    }

    /**
     * Test of isPositive method, of class Maths.
     */
    @Test
    public void testIsPositive()
    {
        assertTrue(Maths.isPositive(1));
        assertTrue(Maths.isPositive(1L));
        assertTrue(Maths.isPositive(1.5f));
        assertTrue(Maths.isPositive(0.5));

        assertFalse(Maths.isPositive(0));
        assertFalse(Maths.isPositive(0L));
        assertFalse(Maths.isPositive(0.0f));
        assertFalse(Maths.isPositive(0.0));

        assertFalse(Maths.isPositive(-1));
        assertFalse(Maths.isPositive(-1L));
        assertFalse(Maths.isPositive(-1.5f));
        assertFalse(Maths.isPositive(-0.5));
    }

    /**
     * Test of isPositiveOrZero method, of class Maths.
     */
    @Test
    public void testIsPositiveOrZero()
    {
        assertTrue(Maths.isPositiveOrZero(1));
        assertTrue(Maths.isPositiveOrZero(1L));
        assertTrue(Maths.isPositiveOrZero(1.5f));
        assertTrue(Maths.isPositiveOrZero(0.5));

        assertTrue(Maths.isPositiveOrZero(0));
        assertTrue(Maths.isPositiveOrZero(0L));
        assertTrue(Maths.isPositiveOrZero(0.0f));
        assertTrue(Maths.isPositiveOrZero(0.0));

        assertFalse(Maths.isPositiveOrZero(-1));
        assertFalse(Maths.isPositiveOrZero(-1L));
        assertFalse(Maths.isPositiveOrZero(-1.5f));
        assertFalse(Maths.isPositiveOrZero(-0.5));
    }

    /**
     * Test of isNegative method, of class Maths.
     */
    @Test
    public void testIsNegative()
    {
        assertTrue(Maths.isNegative(-1));
        assertTrue(Maths.isNegative(-1L));
        assertTrue(Maths.isNegative(-1.5f));
        assertTrue(Maths.isNegative(-0.5));

        assertFalse(Maths.isNegative(0));
        assertFalse(Maths.isNegative(0L));
        assertFalse(Maths.isNegative(0.0f));
        assertFalse(Maths.isNegative(0.0));

        assertFalse(Maths.isNegative(1));
        assertFalse(Maths.isNegative(1L));
        assertFalse(Maths.isNegative(1.5f));
        assertFalse(Maths.isNegative(0.5));
    }

    /**
     * Test of isNegativeOrZero method, of class Maths.
     */
    @Test
    public void testIsNegativeOrZero()
    {
        assertTrue(Maths.isNegativeOrZero(-1));
        assertTrue(Maths.isNegativeOrZero(-1L));
        assertTrue(Maths.isNegativeOrZero(-1.5f));
        assertTrue(Maths.isNegativeOrZero(-0.5));

        assertTrue(Maths.isNegativeOrZero(0));
        assertTrue(Maths.isNegativeOrZero(0L));
        assertTrue(Maths.isNegativeOrZero(0.0f));
        assertTrue(Maths.isNegativeOrZero(0.0));

        assertFalse(Maths.isNegativeOrZero(1));
        assertFalse(Maths.isNegativeOrZero(1L));
        assertFalse(Maths.isNegativeOrZero(1.5f));
        assertFalse(Maths.isNegativeOrZero(0.5));
    }

    /**
     * Test of isZero method, of class Maths.
     */
    @Test
    public void testIsZero()
    {
        assertTrue(Maths.isZero(0));
        assertTrue(Maths.isZero(0L));
        assertTrue(Maths.isZero(0.0f));
        assertTrue(Maths.isZero(0.0));

        assertFalse(Maths.isZero(1));
        assertFalse(Maths.isZero(1L));
        assertFalse(Maths.isZero(1.5f));
        assertFalse(Maths.isZero(0.5));

        assertFalse(Maths.isZero(-1));
        assertFalse(Maths.isZero(-1L));
        assertFalse(Maths.isZero(-1.5f));
        assertFalse(Maths.isZero(-0.5));
    }

    /**
     * Test of isZero method with a tolerance, of class Maths.
     */
    @Test
    public void testIsZero_double_double()
    {
        double delta = 0.002;

        assertFalse(Maths.isZero(-1, delta));
        assertTrue(Maths.isZero(0, delta));
        assertFalse(Maths.isZero(1, delta));

        assertFalse(Maths.isZero(0.003, delta));
        assertTrue(Maths.isZero(0.001, delta));
        assertTrue(Maths.isZero(-0.001, delta));
        assertFalse(Maths.isZero(-0.003, delta));
    }

    /**
     * Test of isZero method with a tolerance, of class Maths.
     */
    @Test
    public void testIsZero_float_float()
    {
        float delta = 0.002f;

        assertFalse(Maths.isZero(-1, delta));
        assertTrue(Maths.isZero(0, delta));
        assertFalse(Maths.isZero(1, delta));

        assertFalse(Maths.isZero(0.003f, delta));
        assertTrue(Maths.isZero(0.001f, delta));
        assertTrue(Maths.isZero(-0.001f, delta));
        assertFalse(Maths.isZero(-0.003f, delta));
    }


    /**
     * Test of log method, of class Maths.
     */
    @Test
    public void testLog_int()
    {
        for(int i=0;i<1500;i++)
        {
            assertEquals(Math.log(i%300), Maths.log(i%300), 0.0);
        }
    }

    /**
     * Test of log method, of class Maths.
     */
    @Test
    public void testLog_double_double()
    {
        for(int i=1;i<1_000_000;i++)
        {
             double a = Math.log(i);
             double b = Maths.log(2.718281828459045235360, i);

             double c = Math.log10(i);
             double d = Maths.log(10, i);

             assertEquals(a, b, 0.0000000000001);
             assertEquals(c, d, 0.0000000000001);
        }
    }

    /**
     * Test of log2 method, of class Maths.
     */
    @Test
    public void testLog2_long()
    {
        assertEquals(0, Maths.log2(1));
        assertEquals(1, Maths.log2(2));
        assertEquals(2, Maths.log2(4));
        assertEquals(3, Maths.log2(8));
        assertEquals(4, Maths.log2(16));
        assertEquals(5, Maths.log2(32));
        assertEquals(6, Maths.log2(64));
        assertEquals(7, Maths.log2(128));

        assertEquals(1, Maths.log2(3));
        assertEquals(2, Maths.log2(7));
        assertEquals(3, Maths.log2(15));
        assertEquals(4, Maths.log2(31));
        assertEquals(5, Maths.log2(63));
        assertEquals(6, Maths.log2(127));

        assertEquals(-1, Maths.log2(0));
        assertEquals(-1, Maths.log2(-1));
    }

    /**
     * Test of log2 method, of class Maths.
     */
    @Test
    public void testLog2_long_boolean()
    {
        assertEquals(0, Maths.log2(1,true));
        assertEquals(1, Maths.log2(2,true));
        assertEquals(2, Maths.log2(4,true));
        assertEquals(3, Maths.log2(8,true));
        assertEquals(4, Maths.log2(16,true));
        assertEquals(5, Maths.log2(32,true));
        assertEquals(6, Maths.log2(64,true));
        assertEquals(7, Maths.log2(128,true));

        assertEquals(2, Maths.log2(3,true));
        assertEquals(3, Maths.log2(7,true));
        assertEquals(4, Maths.log2(15,true));
        assertEquals(5, Maths.log2(31,true));
        assertEquals(6, Maths.log2(63,true));
        assertEquals(7, Maths.log2(127,true));
    }

     /**
     * Test of sum method, of class Maths.
     */
    @Test
    public void testSum_intArr()
    {
        double delta  = 0.000001;
        int[] values0 = {0,0,0,0,0};
        int[] values1 = {1,1,2,3,5};
        int[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Maths.sum(values0), delta);
        assertEquals(12, Maths.sum(values1), delta);
        assertEquals(131, Maths.sum(values2), delta);
    }

    /**
     * Test of sum method, of class Maths.
     */
    @Test
    public void testSum_longArr()
    {
        double delta  = 0.000001;
        long[] values0 = {0,0,0,0,0};
        long[] values1 = {1,1,2,3,5};
        long[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Maths.sum(values0), delta);
        assertEquals(12, Maths.sum(values1), delta);
        assertEquals(131, Maths.sum(values2), delta);
    }

    /**
     * Test of sum method, of class Maths.
     */
    @Test
    public void testSum_floatArr()
    {
        double delta  = 0.000001;
        float[] values0 = {0,0,0,0,0};
        float[] values1 = {1,1,2,3,5};
        float[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Maths.sum(values0), delta);
        assertEquals(12, Maths.sum(values1), delta);
        assertEquals(131, Maths.sum(values2), delta);
    }

    /**
     * Test of sum method, of class Maths.
     */
    @Test
    public void testSum_doubleArr()
    {
        double delta  = 0.000001;
        double[] values0 = {0,0,0,0,0};
        double[] values1 = {1,1,2,3,5};
        double[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Maths.sum(values0), delta);
        assertEquals(12, Maths.sum(values1), delta);
        assertEquals(131, Maths.sum(values2), delta);
    }

    /**
     * Test of avg method, of class Maths.
     */
    @Test
    public void testAvg_intArr()
    {
        double delta  = 0.000001;
        int[] values0 = {0,0,0,0,0};
        int[] values1 = {1,1,2,3,5};
        int[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Maths.avg(values0), delta);
        assertEquals(2.4, Maths.avg(values1), delta);
        assertEquals(26.2, Maths.avg(values2), delta);
    }

    /**
     * Test of avg method, of class Maths.
     */
    @Test
    public void testAvg_longArr()
    {
        double delta  = 0.000001;
        long[] values0 = {0,0,0,0,0};
        long[] values1 = {1,1,2,3,5};
        long[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Maths.avg(values0), delta);
        assertEquals(2.4, Maths.avg(values1), delta);
        assertEquals(26.2, Maths.avg(values2), delta);
    }

    /**
     * Test of avg method, of class Maths.
     */
    @Test
    public void testAvg_floatArr()
    {
        double delta  = 0.000001;
        float[] values0 = {0,0,0,0,0};
        float[] values1 = {1,1,2,3,5};
        float[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Maths.avg(values0), delta);
        assertEquals(2.4, Maths.avg(values1), delta);
        assertEquals(26.2, Maths.avg(values2), delta);
    }

    /**
     * Test of avg method, of class Maths.
     */
    @Test
    public void testAvg_doubleArr()
    {
        double delta  = 0.000001;
        double[] values0 = {0,0,0,0,0};
        double[] values1 = {1,1,2,3,5};
        double[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Maths.avg(values0), delta);
        assertEquals(2.4, Maths.avg(values1), delta);
        assertEquals(26.2, Maths.avg(values2), delta);
    }

    /**
     * Test of sumarize method, of class FastMath.
     */
    @Test
    public void testSum()
    {
        assertEquals(0, Maths.sum(0));
        assertEquals(0, Maths.sum(0));
        assertEquals(1, Maths.sum(0,1));
        assertEquals(3, Maths.sum(0,1,2));
        assertEquals(6, Maths.sum(0,1,2,3));
        assertEquals(11, Maths.sum(0,1,2,3,5));
    }
    /**
     * Test of equals method, of class Maths.
     */
    @Test
    public void testEqualsEnough_3args_1()
    {
        double d0 = 0;
        double d1 = 0.000001;
        double d2 = 0.000002;
        double delta0 = 0.0;
        double delta1 = 0.000001;
        double delta2 = 0.000002;
        
        assertTrue(Maths.equalsEnough(d0, d0, delta0));
        assertTrue(Maths.equalsEnough(d0, d0, delta1));
        assertTrue(Maths.equalsEnough(d0, d0, delta2));

        assertFalse(Maths.equalsEnough(d1, d2, delta0));
        assertFalse(Maths.equalsEnough(d0, d2, delta1));
        assertTrue(Maths.equalsEnough(d0, d2, delta2));
        
        assertFalse(Maths.equalsEnough(d2, d1, delta0));
        assertFalse(Maths.equalsEnough(d2, d0, delta1));
        assertTrue(Maths.equalsEnough(d2, d0, delta2));
                
    }

    /**
     * Test of equals method, of class Maths.
     */
    @Test
    public void testEqualsEnough_3args_2()
    {
        float d0 = 0;
        float d1 = 0.001f;
        float d2 = 0.002f;
        float delta0 = 0.0f;
        float delta1 = 0.001f;
        float delta2 = 0.002f;
        
        assertTrue(Maths.equalsEnough(d0, d0, delta0));
        assertTrue(Maths.equalsEnough(d0, d0, delta1));
        assertTrue(Maths.equalsEnough(d0, d0, delta2));

        assertFalse(Maths.equalsEnough(d1, d2, delta0));
        assertFalse(Maths.equalsEnough(d0, d2, delta1));
        assertTrue(Maths.equalsEnough(d0, d2, delta2));
        
        assertFalse(Maths.equalsEnough(d2, d1, delta0));
        assertFalse(Maths.equalsEnough(d2, d0, delta1));
        assertTrue(Maths.equalsEnough(d2, d0, delta2));
    }    
    
    /**
     * Test of equalsEnough method, of class Maths.
     */
    @Test
    public void testEqualsEnough()
    {
        assertTrue(Maths.equalsEnough(0, 0, 0));
        assertTrue(Maths.equalsEnough(0.4, 0.6, 0.5));
        assertTrue(Maths.equalsEnough(1.9, 2.1, 0.5));
        assertTrue(Maths.equalsEnough(0.000001, 0.000002, 0.000002));

        assertFalse(Maths.equalsEnough(0, 1, 0));
        assertFalse(Maths.equalsEnough(0.4, 0.6, 0.1));
        assertFalse(Maths.equalsEnough(1.9, 2.1, 0.1));
        assertFalse(Maths.equalsEnough(0.000001, 0.000003, 0.000001));
    }
    
    /**
     * Test of minOf method, of class Maths.
     */
    @Test
    public void testMinOf_doubleArr()
    {
        assertEquals(1.0, Maths.minOf(1.0), 0.0);
        assertEquals(1.0, Maths.minOf(2.0, 1.0), 0.0);
        assertEquals(1.0, Maths.minOf(3.0, 2.0, 1.0), 0.0);
        assertEquals(1.0, Maths.minOf(3.0, 2.0, 1.0, 4.0), 0.0);
    }

    /**
     * Test of log method, of class FastMath.
     */
    @Test
    public void testLog()
    {
        for(int i=0;i<1500;i++)
        {
            assertEquals(Math.log(i%300), Maths.log(i%300), 0.0);
        }
    }

    /**
     * Test of minOf method, of class BigMaths.
     */
    @Test
    public void testMinOf_intArr()
    {
        assertEquals(1, Maths.minOf(1));
        assertEquals(1, Maths.minOf(2,1));
        assertEquals(1, Maths.minOf(3,2,1));
        assertEquals(1, Maths.minOf(3,2,1,4));
    }

    /**
     * Test of minOf method, of class BigMaths.
     */
    @Test
    public void testMinOf_longArr()
    {
        assertEquals(1L, Maths.minOf(1L));
        assertEquals(1L, Maths.minOf(2L,1L));
        assertEquals(1L, Maths.minOf(3L,2L,1L));
        assertEquals(1L, Maths.minOf(3L,2L,1L,4L));
    }

    /**
     * Test of maxOf method, of class BigMaths.
     */
    @Test
    public void testMaxOf_doubleArr()
    {
        assertEquals(2d, Maths.maxOf(1d, 2d), 0.0d);
        assertEquals(3d, Maths.maxOf(1d, 2d, 3d), 0.0d);
        assertEquals(3d, Maths.maxOf(3d, 2d, 1d), 0.0d);
        assertEquals(9d, Maths.maxOf(9d, 8d, 7d, 6d, 5d), 0.0d);
    }

    /**
     * Test of maxOf method, of class BigMaths.
     */
    @Test
    public void testMaxOf_floatArr()
    {
        assertEquals(2f, Maths.maxOf(1f, 2f), 0.0f);
        assertEquals(3f, Maths.maxOf(1f, 2f, 3f), 0.0f);
        assertEquals(3f, Maths.maxOf(3f, 2f, 1f), 0.0f);
        assertEquals(9f, Maths.maxOf(9f, 8f, 7f, 6f, 5f), 0.0f);
        assertEquals(0f, Maths.maxOf(new float[0]), 0.0f);
    }
    
    /**
     * Test of maxOf method, of class BigMaths.
     */
    @Test
    public void testMaxOf_intArr()
    {
        assertEquals(2, Maths.maxOf(1, 2));
        assertEquals(3, Maths.maxOf(1, 2, 3));
        assertEquals(3, Maths.maxOf(3, 2, 1));
        assertEquals(9, Maths.maxOf(9, 8, 7, 6, 5));
        assertEquals(0, Maths.maxOf(new int[0]));
    }

    /**
     * Test of maxOf method, of class BigMaths.
     */
    @Test
    public void testMaxOf_longArr()
    {
        assertEquals(2L, Maths.maxOf(1L, 2L));
        assertEquals(3L, Maths.maxOf(1L, 2L, 3L));
        assertEquals(3L, Maths.maxOf(3L, 2L, 1L));
        assertEquals(9L, Maths.maxOf(9L, 8L, 7L, 6L, 5L));
    }

    /**
     * Test of max method with arrays, of class BigMaths.
     */
    @Test
    public void testMax_intArr()
    {
        assertEquals(0, Maths.max(new int[]{}));
        assertEquals(5, Maths.max(new int[]{5}));
        assertEquals(2, Maths.max(new int[]{1, 2}));
        assertEquals(9, Maths.max(new int[]{9, 8, 7, 6, 5}));
    }

    /**
     * Test of max method with arrays, of class BigMaths.
     */
    @Test
    public void testMax_longArr()
    {
        assertEquals(0L, Maths.max(new long[]{}));
        assertEquals(5L, Maths.max(new long[]{5L}));
        assertEquals(2L, Maths.max(new long[]{1L, 2L}));
        assertEquals(9L, Maths.max(new long[]{9L, 8L, 7L, 6L, 5L}));
    }

    /**
     * Test of max method with arrays, of class BigMaths.
     */
    @Test
    public void testMax_doubleArr()
    {
        assertEquals(0.0, Maths.max(new double[]{}), 0.0);
        assertEquals(5.0, Maths.max(new double[]{5.0}), 0.0);
        assertEquals(2.0, Maths.max(new double[]{1.0, 2.0}), 0.0);
        assertEquals(9.0, Maths.max(new double[]{9.0, 8.0, 7.0, 6.0, 5.0}), 0.0);
    }

    /**
     * Test of minMax method, of class BigMaths.
     */
    @Test
    public void testMinMax_byte()
    {
        assertNull(Maths.minMax((byte[]) null));
        assertArrayEquals(new byte[]{0, 0}, Maths.minMax(new byte[]{}));
        assertArrayEquals(new byte[]{5, 5}, Maths.minMax(new byte[]{5}));
        assertArrayEquals(new byte[]{2, 8}, Maths.minMax(new byte[]{2, 8}));
        assertArrayEquals(new byte[]{2, 8}, Maths.minMax(new byte[]{8, 2}));
        assertArrayEquals(new byte[]{-5, 10}, Maths.minMax(new byte[]{3, 10, -5, 0, 7}));
    }

    /**
     * Test of minMax method, of class BigMaths.
     */
    @Test
    public void testMinMax_short()
    {
        assertNull(Maths.minMax((short[]) null));
        assertArrayEquals(new short[]{0, 0}, Maths.minMax(new short[]{}));
        assertArrayEquals(new short[]{5, 5}, Maths.minMax(new short[]{5}));
        assertArrayEquals(new short[]{2, 8}, Maths.minMax(new short[]{2, 8}));
        assertArrayEquals(new short[]{2, 8}, Maths.minMax(new short[]{8, 2}));
        assertArrayEquals(new short[]{-5, 10}, Maths.minMax(new short[]{3, 10, -5, 0, 7}));
    }

    /**
     * Test of minMax method, of class BigMaths.
     */
    @Test
    public void testMinMax_char()
    {
        assertNull(Maths.minMax((char[]) null));
        assertArrayEquals(new char[]{0, 0}, Maths.minMax(new char[]{}));
        assertArrayEquals(new char[]{'a', 'a'}, Maths.minMax(new char[]{'a'}));
        assertArrayEquals(new char[]{'a', 'z'}, Maths.minMax(new char[]{'a', 'z'}));
        assertArrayEquals(new char[]{'a', 'z'}, Maths.minMax(new char[]{'z', 'a'}));
        assertArrayEquals(new char[]{'b', 'y'}, Maths.minMax(new char[]{'m', 'y', 'b', 'd', 's'}));
    }

    /**
     * Test of minMax method, of class BigMaths.
     */
    @Test
    public void testMinMax_int()
    {
        assertNull(Maths.minMax((int[]) null));
        assertArrayEquals(new int[]{0, 0}, Maths.minMax(new int[]{}));
        assertArrayEquals(new int[]{5, 5}, Maths.minMax(new int[]{5}));
        assertArrayEquals(new int[]{2, 8}, Maths.minMax(new int[]{2, 8}));
        assertArrayEquals(new int[]{2, 8}, Maths.minMax(new int[]{8, 2}));
        assertArrayEquals(new int[]{-5, 10}, Maths.minMax(new int[]{3, 10, -5, 0, 7}));
    }

    /**
     * Test of minMax method, of class BigMaths.
     */
    @Test
    public void testMinMax_long()
    {
        assertNull(Maths.minMax((long[]) null));
        assertArrayEquals(new long[]{0L, 0L}, Maths.minMax(new long[]{}));
        assertArrayEquals(new long[]{5L, 5L}, Maths.minMax(new long[]{5L}));
        assertArrayEquals(new long[]{2L, 8L}, Maths.minMax(new long[]{2L, 8L}));
        assertArrayEquals(new long[]{2L, 8L}, Maths.minMax(new long[]{8L, 2L}));
        assertArrayEquals(new long[]{-5L, 10L}, Maths.minMax(new long[]{3L, 10L, -5L, 0L, 7L}));
    }

    /**
     * Test of minMax method, of class BigMaths.
     */
    @Test
    public void testMinMax_float()
    {
        assertNull(Maths.minMax((float[]) null));
        assertArrayEquals(new float[]{0.0f, 0.0f}, Maths.minMax(new float[]{}), 0.0f);
        assertArrayEquals(new float[]{5.0f, 5.0f}, Maths.minMax(new float[]{5.0f}), 0.0f);
        assertArrayEquals(new float[]{2.0f, 8.0f}, Maths.minMax(new float[]{2.0f, 8.0f}), 0.0f);
        assertArrayEquals(new float[]{2.0f, 8.0f}, Maths.minMax(new float[]{8.0f, 2.0f}), 0.0f);
        assertArrayEquals(new float[]{-5.0f, 10.0f}, Maths.minMax(new float[]{3.0f, 10.0f, -5.0f, 0.0f, 7.0f}), 0.0f);
    }

    /**
     * Test of minMax method, of class BigMaths.
     */
    @Test
    public void testMinMax_double()
    {
        assertNull(Maths.minMax((double[]) null));
        assertArrayEquals(new double[]{0.0, 0.0}, Maths.minMax(new double[]{}), 0.0);
        assertArrayEquals(new double[]{5.0, 5.0}, Maths.minMax(new double[]{5.0}), 0.0);
        assertArrayEquals(new double[]{2.0, 8.0}, Maths.minMax(new double[]{2.0, 8.0}), 0.0);
        assertArrayEquals(new double[]{2.0, 8.0}, Maths.minMax(new double[]{8.0, 2.0}), 0.0);
        assertArrayEquals(new double[]{-5.0, 10.0}, Maths.minMax(new double[]{3.0, 10.0, -5.0, 0.0, 7.0}), 0.0);
    }

    /**
     * Test of minOf method, of class BigMaths.
     */
    @Test
    public void testMinOf_floatArr()
    {
        assertEquals(1f, Maths.minOf(1f, 2f), 0.0f);
        assertEquals(1f, Maths.minOf(1f, 2f, 3f), 0.0f);
        assertEquals(1f, Maths.minOf(3f, 2f, 1f), 0.0f);
        assertEquals(5f, Maths.minOf(9f, 8f, 7f, 6f, 5f), 0.0f);
        assertEquals(0f, Maths.minOf(new float[0]), 0.0f);
    }

    /**
     * Test of minOf method, of class BigMaths.
     */
    @Test
    public void testMinLong()
    {
        assertEquals(1L, Maths.minOf(1L, 2L));
        assertEquals(1L, Maths.minOf(1L, 2L, 3L));
        assertEquals(1L, Maths.minOf(3L, 2L, 1L));
        assertEquals(5L, Maths.minOf(9L, 8L, 7L, 6L, 5L));
    }
    
    /**
     * Test of isPow2 method, of class FastMath.
     */
    @Test
    public void testIsPow2_int()
    {
        assertFalse(Maths.isPow2(-1));
        assertFalse(Maths.isPow2(0));
        assertFalse(Maths.isPow2(3));
        assertFalse(Maths.isPow2(5));
        assertFalse(Maths.isPow2(7));
        assertFalse(Maths.isPow2(9));
        assertFalse(Maths.isPow2(15));
        assertFalse(Maths.isPow2(17));
        assertFalse(Maths.isPow2(31));
        assertFalse(Maths.isPow2(33));
        assertFalse(Maths.isPow2(63));
        assertFalse(Maths.isPow2(65));
        assertFalse(Maths.isPow2(127));
        assertFalse(Maths.isPow2(129));
        
        assertTrue(Maths.isPow2(1));
        assertTrue(Maths.isPow2(2));
        assertTrue(Maths.isPow2(4));
        assertTrue(Maths.isPow2(8));
        assertTrue(Maths.isPow2(16));
        assertTrue(Maths.isPow2(32));
        assertTrue(Maths.isPow2(64));
        assertTrue(Maths.isPow2(128));
        
        assertFalse(Maths.isPow2(Byte.MAX_VALUE));
        assertFalse(Maths.isPow2(Short.MAX_VALUE));
        assertFalse(Maths.isPow2(Character.MAX_VALUE));
        assertFalse(Maths.isPow2(Integer.MAX_VALUE));

        assertTrue(Maths.isPow2(Byte.MAX_VALUE+1));
        assertTrue(Maths.isPow2(Short.MAX_VALUE+1));
        assertTrue(Maths.isPow2(Character.MAX_VALUE+1));

        assertFalse(Maths.isPow2(-Byte.MAX_VALUE));
        assertFalse(Maths.isPow2(-Short.MAX_VALUE));
        assertFalse(Maths.isPow2(-Character.MAX_VALUE));
        assertFalse(Maths.isPow2(-Integer.MAX_VALUE));

        assertFalse(Maths.isPow2(-Byte.MAX_VALUE-1));
        assertFalse(Maths.isPow2(-Short.MAX_VALUE-1));
        assertFalse(Maths.isPow2(-Character.MAX_VALUE-1));
    }
    
    /**
     * Test of mul method, of class FastMath.
     */
    @Test
    public void testMul()
    {
        assertEquals(1, Maths.mul());
        assertEquals(0, Maths.mul(0));
        assertEquals(1, Maths.mul(1));
        assertEquals(2, Maths.mul(1,2));
        assertEquals(6, Maths.mul(1,2,3));
        assertEquals(30, Maths.mul(1,2,3,5));
    }

    
}
