/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
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
public class BigMathsTest
{
    public BigMathsTest()
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
     * Test of equalsEnough method, of class BigMaths.
     */
    @Test
    public void testEqualsEnough()
    {
        assertTrue(BigMaths.equalsEnough(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        assertTrue(BigMaths.equalsEnough(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE));
        assertFalse(BigMaths.equalsEnough(BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE));

        assertTrue(BigMaths.equalsEnough(BigDecimal.valueOf(0.1001), BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.01)));
        assertFalse(BigMaths.equalsEnough(BigDecimal.valueOf(0.1001), BigDecimal.valueOf(0.2), BigDecimal.valueOf(0.01)));

        BigDecimal d1 = BigDecimal.valueOf(0.001);
        BigDecimal d2 = BigDecimal.valueOf(0.009);
        BigDecimal delta = BigDecimal.valueOf(0.01);

        assertTrue(BigMaths.equalsEnough(d1, d2, delta));
    }

    /**
     * Test of isPositive method, of class BigMaths.
     */
    @Test
    public void testIsPositive_BigInteger()
    {
        assertTrue(BigMaths.isPositive(BigInteger.ONE));
        assertTrue(BigMaths.isPositive(BigInteger.TEN));
        assertFalse(BigMaths.isPositive(BigInteger.ZERO));
        assertFalse(BigMaths.isPositive(BigInteger.valueOf(-1)));
    }

    /**
     * Test of isPositive method, of class BigMaths.
     */
    @Test
    public void testIsPositive_BigDecimal()
    {
        assertTrue(BigMaths.isPositive(BigDecimal.ONE));
        assertTrue(BigMaths.isPositive(BigDecimal.TEN));
        assertFalse(BigMaths.isPositive(BigDecimal.ZERO));
        assertFalse(BigMaths.isPositive(BigDecimal.valueOf(-1)));
    }

    /**
     * Test of isPositiveOrZero method, of class BigMaths.
     */
    @Test
    public void testIsPositiveOrZero_BigInteger()
    {
        assertTrue(BigMaths.isPositiveOrZero(BigInteger.ONE));
        assertTrue(BigMaths.isPositiveOrZero(BigInteger.TEN));
        assertTrue(BigMaths.isPositiveOrZero(BigInteger.ZERO));
        assertFalse(BigMaths.isPositiveOrZero(BigInteger.valueOf(-1)));
    }

    /**
     * Test of isPositiveOrZero method, of class BigMaths.
     */
    @Test
    public void testIsPositiveOrZero_BigDecimal()
    {
        assertTrue(BigMaths.isPositiveOrZero(BigDecimal.ONE));
        assertTrue(BigMaths.isPositiveOrZero(BigDecimal.TEN));
        assertTrue(BigMaths.isPositiveOrZero(BigDecimal.ZERO));
        assertFalse(BigMaths.isPositiveOrZero(BigDecimal.valueOf(-1)));
    }

    /**
     * Test of isNegative method, of class BigMaths.
     */
    @Test
    public void testIsNegative_BigInteger()
    {
        assertTrue(BigMaths.isNegative(BigInteger.valueOf(-1)));
        assertTrue(BigMaths.isNegative(BigInteger.valueOf(-10)));
        assertFalse(BigMaths.isNegative(BigInteger.ZERO));
        assertFalse(BigMaths.isNegative(BigInteger.ONE));
    }

    /**
     * Test of isNegative method, of class BigMaths.
     */
    @Test
    public void testIsNegative_BigDecimal()
    {
        assertTrue(BigMaths.isNegative(BigDecimal.valueOf(-1)));
        assertTrue(BigMaths.isNegative(BigDecimal.valueOf(-10)));
        assertFalse(BigMaths.isNegative(BigDecimal.ZERO));
        assertFalse(BigMaths.isNegative(BigDecimal.ONE));
    }

    /**
     * Test of isNegativeOrZero method, of class BigMaths.
     */
    @Test
    public void testIsNegativeOrZero_BigInteger()
    {
        assertTrue(BigMaths.isNegativeOrZero(BigInteger.valueOf(-1)));
        assertTrue(BigMaths.isNegativeOrZero(BigInteger.valueOf(-10)));
        assertTrue(BigMaths.isNegativeOrZero(BigInteger.ZERO));
        assertFalse(BigMaths.isNegativeOrZero(BigInteger.ONE));
    }

    /**
     * Test of isNegativeOrZero method, of class BigMaths.
     */
    @Test
    public void testIsNegativeOrZero_BigDecimal()
    {
        assertTrue(BigMaths.isNegativeOrZero(BigDecimal.valueOf(-1)));
        assertTrue(BigMaths.isNegativeOrZero(BigDecimal.valueOf(-10)));
        assertTrue(BigMaths.isNegativeOrZero(BigDecimal.ZERO));
        assertFalse(BigMaths.isNegativeOrZero(BigDecimal.ONE));
    }

    /**
     * Test of isZero method, of class BigMaths.
     */
    @Test
    public void testIsZero_BigInteger()
    {
        assertFalse(BigMaths.isZero(BigInteger.valueOf(-1)));
        assertTrue(BigMaths.isZero(BigInteger.ZERO));
        assertFalse(BigMaths.isZero(BigInteger.ONE));
    }

    /**
     * Test of isZero method, of class BigMaths.
     */
    @Test
    public void testIsZero_BigDecimal()
    {
        assertFalse(BigMaths.isZero(BigDecimal.valueOf(-1)));
        assertTrue(BigMaths.isZero(BigDecimal.ZERO));
        assertTrue(BigMaths.isZero(new BigDecimal("0.00")));
        assertFalse(BigMaths.isZero(BigDecimal.ONE));
    }

    /**
     * Test of isZero method, of class BigMaths.
     */
    @Test
    public void testIsZero_BigDecimal_BigDecimal()
    {
        BigDecimal delta = new BigDecimal("0.002");
        assertFalse(BigMaths.isZero(BigDecimal.valueOf(-1), delta));
        assertTrue(BigMaths.isZero(BigDecimal.ZERO, delta));
        assertFalse(BigMaths.isZero(BigDecimal.ONE, delta));

        assertFalse(BigMaths.isZero(new BigDecimal(0.003), delta));
        assertTrue(BigMaths.isZero(new BigDecimal(0.001), delta));
        assertTrue(BigMaths.isZero(new BigDecimal(-0.001), delta));
        assertFalse(BigMaths.isZero(new BigDecimal(-0.003), delta));
    }

    /**
     * Test of isNullOrZero method, of class BigMaths.
     */
    @Test
    public void testIsNullOrZero_BigInteger()
    {
        assertTrue(BigMaths.isNullOrZero((BigInteger) null));
        assertFalse(BigMaths.isNullOrZero(BigInteger.valueOf(-1)));
        assertTrue(BigMaths.isNullOrZero(BigInteger.ZERO));
        assertFalse(BigMaths.isNullOrZero(BigInteger.ONE));
    }

    /**
     * Test of isNullOrZero method, of class BigMaths.
     */
    @Test
    public void testIsNullOrZero_BigDecimal()
    {
        assertTrue(BigMaths.isNullOrZero((BigDecimal) null));
        assertFalse(BigMaths.isNullOrZero(BigDecimal.valueOf(-1)));
        assertTrue(BigMaths.isNullOrZero(BigDecimal.ZERO));
        assertFalse(BigMaths.isNullOrZero(BigDecimal.ONE));
    }

    /**
     * Test of isNullOrZero method, of class BigMaths.
     */
    @Test
    public void testIsNullOrZero_BigDecimal_BigDecimal()
    {
        BigDecimal delta = new BigDecimal("0.002");
        assertTrue(BigMaths.isNullOrZero((BigDecimal) null, delta));
        assertFalse(BigMaths.isNullOrZero(BigDecimal.valueOf(-1), delta));
        assertTrue(BigMaths.isNullOrZero(BigDecimal.ZERO, delta));
        assertFalse(BigMaths.isNullOrZero(BigDecimal.ONE, delta));

        assertFalse(BigMaths.isNullOrZero(new BigDecimal(0.003), delta));
        assertTrue(BigMaths.isNullOrZero(new BigDecimal(0.001), delta));
        assertTrue(BigMaths.isNullOrZero(new BigDecimal(-0.001), delta));
        assertFalse(BigMaths.isNullOrZero(new BigDecimal(-0.003), delta));
    }

    /**
     * Test of isEven method, of class BigMaths.
     */
    @Test
    public void testIsEven()
    {
        assertTrue(BigMaths.isEven(BigInteger.ZERO));
        assertFalse(BigMaths.isEven(BigInteger.ONE));
        assertTrue(BigMaths.isEven(BigInteger.TEN));
        assertTrue(BigMaths.isEven(BigInteger.ZERO.negate()));
        assertFalse(BigMaths.isEven(BigInteger.ONE.negate()));
        assertTrue(BigMaths.isEven(BigInteger.TEN.negate()));
        
        for(int i=0;i<1000;i+=97)
        {
            BigInteger value = BigInteger.valueOf(i);
            assertEquals( i%2==0, BigMaths.isEven(value), "i="+i);
        }
    }

    /**
     * Test of isOdd method, of class BigMaths.
     */
    @Test
    public void testIsOdd()
    {
        assertFalse(BigMaths.isOdd(BigInteger.ZERO));
        assertTrue(BigMaths.isOdd(BigInteger.ONE));
        assertFalse(BigMaths.isOdd(BigInteger.TEN));
        assertFalse(BigMaths.isOdd(BigInteger.ZERO.negate()));
        assertTrue(BigMaths.isOdd(BigInteger.ONE.negate()));
        assertFalse(BigMaths.isOdd(BigInteger.TEN.negate()));

        for(int i=0;i<1000;i+=97)
        {
            BigInteger value = BigInteger.valueOf(i);
            assertEquals( i%2==1, BigMaths.isOdd(value), "i="+i);
        }
    }

    /**
     * Test of firstNonNullOrZero method, of class BigMaths.
     */
    @Test
    public void testFirstNonNullOrZero()
    {
        assertEquals(BigDecimal.ONE, BigMaths.firstNonNullOrZero(null, BigDecimal.ZERO,BigDecimal.ONE, BigDecimal.TEN));
        assertEquals(BigDecimal.TEN, BigMaths.firstNonNullOrZero(null, BigDecimal.ZERO,BigDecimal.TEN, BigDecimal.ONE));
        
        assertNull(BigMaths.firstNonNullOrZero());
        assertNull(BigMaths.firstNonNullOrZero((BigDecimal) null));
        assertNull(BigMaths.firstNonNullOrZero(null, null));
        
        assertEquals(BigDecimal.ZERO,BigMaths.firstNonNullOrZero(null, BigDecimal.ZERO));
        assertEquals(BigDecimal.ZERO,BigMaths.firstNonNullOrZero(BigDecimal.ZERO, null));
        assertEquals(BigDecimal.ONE, BigMaths.firstNonNullOrZero(null, BigDecimal.ZERO, BigDecimal.ONE));
        assertEquals(BigDecimal.ONE, BigMaths.firstNonNullOrZero(BigDecimal.ONE, BigDecimal.ZERO, null));
        assertEquals(BigDecimal.ONE, BigMaths.firstNonNullOrZero(BigDecimal.ONE, null, BigDecimal.ZERO));
    }
    
    /**
     * Test of log method, of class BigMaths.
     */
    @Test
    public void testLog()
    {
        assertEquals(BigDecimal.ZERO, BigMaths.log(BigDecimal.ONE));
        assertThrows(ArithmeticException.class, ()->BigMaths.log(BigDecimal.ZERO));
        assertThrows(ArithmeticException.class, ()->BigMaths.log(BigDecimal.valueOf(-1)));

        for(int i=1;i<1000;i++)
        {
            BigDecimal value = BigDecimal.valueOf(i);
            assertEquals(Math.log(i), BigMaths.log(value).doubleValue(), 0.0000001);
        }
    }

    /**
     * Test of log10 method, of class BigMaths.
     */
    @Test
    public void testLog10()
    {
        assertEquals(BigDecimal.ZERO, BigMaths.log10(BigDecimal.ONE));
        assertThrows(ArithmeticException.class, ()->BigMaths.log10(BigDecimal.ZERO));
        assertThrows(ArithmeticException.class, ()->BigMaths.log10(BigDecimal.valueOf(-1)));

        for(int i=1;i<1000;i++)
        {
            BigDecimal value = BigDecimal.valueOf(i);
            assertEquals(Math.log10(i), BigMaths.log10(value).doubleValue(), 0.0000001);
        }

        assertEquals(Math.log10(0.5), BigMaths.log10(new BigDecimal("0.5")).doubleValue(), 0.0000001);
    }

    /**
     * Test of log2 method, of class BigMaths.
     */
    @Test
    public void testLog2_BigInteger()
    {
        for(int i=0;i<64;i++)
        {
            assertEquals(i, BigMaths.log2(BigInteger.ONE.shiftLeft(i)));
        }
        assertEquals(0, BigMaths.log2(BigInteger.ONE));
        assertEquals(1, BigMaths.log2(BigInteger.valueOf(2)));
        assertEquals(1, BigMaths.log2(BigInteger.valueOf(3)));
        assertEquals(2, BigMaths.log2(BigInteger.valueOf(7)));

        assertEquals(-1, BigMaths.log2(null));
        assertEquals(-1, BigMaths.log2(BigInteger.ZERO));
        assertEquals(-1, BigMaths.log2(BigInteger.valueOf(-8)));
    }

    /**
     * Test of log2 method, of class BigMaths.
     */
    @Test
    public void testLog2_BigInteger_boolean()
    {
        for(int i=0;i<64;i++)
        {
            assertEquals(i, BigMaths.log2(BigInteger.ONE.shiftLeft(i), true));
        }
        assertEquals(2, BigMaths.log2(BigInteger.valueOf(3), true));
        assertEquals(3, BigMaths.log2(BigInteger.valueOf(7), true));
        assertEquals(20, BigMaths.log2(BigInteger.valueOf(1000000), true));

        assertEquals(-1, BigMaths.log2(null, true));
        assertEquals(-1, BigMaths.log2(BigInteger.ZERO, true));
    }

    /**
     * Test of sum method of class BigMaths, with a default value for null elements.
     */
    @Test
    public void testSum_BigDecimal_defaultValue()
    {
        assertEquals(BigDecimal.ZERO, BigMaths.sum(BigDecimal.ZERO));
        assertEquals(BigDecimal.ZERO, BigMaths.sum(BigDecimal.ONE));
        assertEquals(BigDecimal.ZERO, BigMaths.sum(BigDecimal.ZERO, (BigDecimal[]) null));
        assertEquals(BigDecimal.ONE, BigMaths.sum(BigDecimal.ONE, (BigDecimal) null));
        assertEquals(BigDecimal.ONE, BigMaths.sum(BigDecimal.ZERO, BigDecimal.ONE));
        assertEquals(BigDecimal.valueOf(3), BigMaths.sum(BigDecimal.ONE, (BigDecimal) null, BigDecimal.valueOf(2)));
        assertEquals(BigDecimal.valueOf(6), BigMaths.sum(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), null));
    }

    /**
     * Test of avg method, of class BigMaths.
     */
    @Test
    public void testAvg_3args()
    {
        BigDecimal result = BigMaths.avg(BigDecimal.TEN, MathContext.DECIMAL128, BigDecimal.ONE, BigDecimal.TEN);
        assertEquals(BigDecimal.valueOf(5.5), result);
    }
}
