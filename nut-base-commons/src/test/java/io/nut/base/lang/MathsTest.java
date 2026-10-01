/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

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
public class MathsTest
{
    public MathsTest()
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
    
}
