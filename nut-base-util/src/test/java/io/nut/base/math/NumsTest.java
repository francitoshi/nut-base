/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

import io.nut.base.commons.lang.BigMaths;
import io.nut.base.commons.lang.Maths;
import static io.nut.base.math.Nums.BIG_DEC_ATTO;
import static io.nut.base.math.Nums.BIG_DEC_EXA;
import static io.nut.base.math.Nums.BIG_DEC_EXBI;
import static io.nut.base.math.Nums.BIG_DEC_FEMTO;
import static io.nut.base.math.Nums.BIG_DEC_GIBI;
import static io.nut.base.math.Nums.BIG_DEC_GIGA;
import static io.nut.base.math.Nums.BIG_DEC_KIBI;
import static io.nut.base.math.Nums.BIG_DEC_KILO;
import static io.nut.base.math.Nums.BIG_DEC_MEBI;
import static io.nut.base.math.Nums.BIG_DEC_MEGA;
import static io.nut.base.math.Nums.BIG_DEC_MICRO;
import static io.nut.base.math.Nums.BIG_DEC_MILI;
import static io.nut.base.math.Nums.BIG_DEC_NANO;
import static io.nut.base.math.Nums.BIG_DEC_PEBI;
import static io.nut.base.math.Nums.BIG_DEC_PETA;
import static io.nut.base.math.Nums.BIG_DEC_PICO;
import static io.nut.base.math.Nums.BIG_DEC_TEBI;
import static io.nut.base.math.Nums.BIG_DEC_TERA;
import static io.nut.base.math.Nums.BIG_DEC_YOBI;
import static io.nut.base.math.Nums.BIG_DEC_YOCTO;
import static io.nut.base.math.Nums.BIG_DEC_YOTTA;
import static io.nut.base.math.Nums.BIG_DEC_ZEBI;
import static io.nut.base.math.Nums.BIG_DEC_ZEPTO;
import static io.nut.base.math.Nums.BIG_DEC_ZETTA;
import static io.nut.base.math.Nums.BIG_INT_EXA;
import static io.nut.base.math.Nums.BIG_INT_EXBI;
import static io.nut.base.math.Nums.BIG_INT_GIBI;
import static io.nut.base.math.Nums.BIG_INT_GIGA;
import static io.nut.base.math.Nums.BIG_INT_KIBI;
import static io.nut.base.math.Nums.BIG_INT_KILO;
import static io.nut.base.math.Nums.BIG_INT_MEBI;
import static io.nut.base.math.Nums.BIG_INT_MEGA;
import static io.nut.base.math.Nums.BIG_INT_PEBI;
import static io.nut.base.math.Nums.BIG_INT_PETA;
import static io.nut.base.math.Nums.BIG_INT_TEBI;
import static io.nut.base.math.Nums.BIG_INT_TERA;
import static io.nut.base.math.Nums.BIG_INT_TWO;
import static io.nut.base.math.Nums.BIG_INT_YOBI;
import static io.nut.base.math.Nums.BIG_INT_YOTTA;
import static io.nut.base.math.Nums.BIG_INT_ZEBI;
import static io.nut.base.math.Nums.BIG_INT_ZETTA;
import io.nut.base.commons.lang.As;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class NumsTest
{
    final static BigInteger _1 = BigInteger.ONE;
    final static BigInteger _2 = BigInteger.valueOf(2);
    final static BigInteger _3 = BigInteger.valueOf(3);
    final static BigInteger _4 = BigInteger.valueOf(4);
    final static BigInteger _5 = BigInteger.valueOf(5);
    final static BigInteger _6 = BigInteger.valueOf(6);
    final static BigInteger _7 = BigInteger.valueOf(7);
    final static BigInteger _8 = BigInteger.valueOf(8);
    final static BigInteger _9 = BigInteger.valueOf(9);
    final static BigInteger _10 = BigInteger.valueOf(10);
    final static BigInteger _11 = BigInteger.valueOf(11);
    final static BigInteger _12 = BigInteger.valueOf(12);
    final static BigInteger _13 = BigInteger.valueOf(13);
    final static BigInteger _14 = BigInteger.valueOf(14);
    final static BigInteger _15 = BigInteger.valueOf(15);
    final static BigInteger _20 = BigInteger.valueOf(20);
    final static BigInteger _24 = BigInteger.valueOf(24);
    final static BigInteger _35 = BigInteger.valueOf(35);
    final static BigInteger _36 = BigInteger.valueOf(36);
    final static BigInteger _42 = BigInteger.valueOf(42);
    final static BigInteger _56 = BigInteger.valueOf(56);

    final static BigInteger _60 = BigInteger.valueOf(60);
    
    /**
     * Test of buildE method, of class BigMaths.
     */
    @Test
    public void testBuildE()
    {
        BigDecimal e;
        BigDecimal r;
        
        e = new BigDecimal("2.7182818284590452353602874713526624977572470936999595749669676277240766303535475945713821785251664274");
        r = Nums.buildE(101);
        assertEquals(e, r);
    }
    

    @Test
    public void testCuberoot()
    {
        assertEquals(0, Nums.cuberoot(BigDecimal.ONE).compareTo(BigDecimal.ONE));
        assertEquals(0, Nums.cuberoot(BigDecimal.ZERO).compareTo(BigDecimal.ZERO));

        BigDecimal root8 = Nums.cuberoot(BigDecimal.valueOf(8));
        assertEquals(0, root8.compareTo(BigDecimal.valueOf(2)));

        BigDecimal root27 = Nums.cuberoot(BigDecimal.valueOf(27));
        assertEquals(0, root27.compareTo(BigDecimal.valueOf(3)));

        BigDecimal root1000 = Nums.cuberoot(BigDecimal.valueOf(1000));
        assertEquals(0, root1000.compareTo(BigDecimal.valueOf(10)));
    }
    
    /**
     * Test of orderOfMagnitude method, of class BigMaths.
     */
    @Test
    public void testOrderOfMagnitude_long()
    {
        assertEquals(0,  Nums.orderOfMagnitude(1));
        assertEquals(1,  Nums.orderOfMagnitude(10));
        assertEquals(2,  Nums.orderOfMagnitude(100));
        assertEquals(3,  Nums.orderOfMagnitude(1000));
        assertEquals(6,  Nums.orderOfMagnitude(1000_000));
        assertEquals(9,  Nums.orderOfMagnitude(1000_000_000));
        assertEquals(12, Nums.orderOfMagnitude(1000_000_000_000L));
        assertEquals(15, Nums.orderOfMagnitude(1000_000_000_000_000L));
        assertEquals(18, Nums.orderOfMagnitude(1000_000_000_000_000_000L));
        
        assertEquals(0,  Nums.orderOfMagnitude(5));
        assertEquals(1,  Nums.orderOfMagnitude(7));
        assertEquals(1,  Nums.orderOfMagnitude(44));
        
        assertEquals(0,  Nums.orderOfMagnitude(-5));
        assertEquals(1,  Nums.orderOfMagnitude(-7));
        assertEquals(1,  Nums.orderOfMagnitude(-44));
    }

    /**
     * Test of orderOfMagnitude method, of class BigMaths.
     */
    @Test
    public void testOrderOfMagnitude_double()
    {
        assertEquals(-1,  Nums.orderOfMagnitude(0.325));
        assertEquals(-1,  Nums.orderOfMagnitude(0.5));

        assertEquals(0,  Nums.orderOfMagnitude(1.0));
        assertEquals(1,  Nums.orderOfMagnitude(10.0));
        assertEquals(2,  Nums.orderOfMagnitude(100.0));
        assertEquals(3,  Nums.orderOfMagnitude(1000.0));
        assertEquals(6,  Nums.orderOfMagnitude(1000_000.0));
        assertEquals(9,  Nums.orderOfMagnitude(1000_000_000.0));
        assertEquals(12, Nums.orderOfMagnitude(1000_000_000_000.0));
        assertEquals(15, Nums.orderOfMagnitude(1000_000_000_000_000.0));
        assertEquals(18, Nums.orderOfMagnitude(1000_000_000_000_000_000.0));
        
        assertEquals(0,  Nums.orderOfMagnitude(5.0));
        assertEquals(1,  Nums.orderOfMagnitude(7.0));
        assertEquals(1,  Nums.orderOfMagnitude(44.0));
        
        assertEquals(0,  Nums.orderOfMagnitude(-5.0));
        assertEquals(1,  Nums.orderOfMagnitude(-7.0));
        assertEquals(1,  Nums.orderOfMagnitude(-44.0));
        
        assertEquals(-24,  Nums.orderOfMagnitude(0.000_000_000_000_000_000_000_001));
        assertEquals(-21,  Nums.orderOfMagnitude(0.000_000_000_000_000_000_001));
        assertEquals(-18,  Nums.orderOfMagnitude(0.000_000_000_000_000_001));
        assertEquals(-15,  Nums.orderOfMagnitude(0.000_000_000_000_001));
        assertEquals(-12,  Nums.orderOfMagnitude(0.000_000_000_001));
        assertEquals(-9,   Nums.orderOfMagnitude(0.000_000_001));
        assertEquals(-6,   Nums.orderOfMagnitude(0.000_001));
        assertEquals(-3,   Nums.orderOfMagnitude(0.001));
        assertEquals(-2,   Nums.orderOfMagnitude(0.01));
        assertEquals(-1,   Nums.orderOfMagnitude(0.1));
    }

    /**
     * Test of pow2 method, of class FastMath.
     */
    @Test
    public void testPow2()
    {
        assertEquals(1, Nums.pow2(0));
        assertEquals(2, Nums.pow2(1));
        assertEquals(4, Nums.pow2(2));
        assertEquals(8, Nums.pow2(3));
        assertEquals(16, Nums.pow2(4));
        assertEquals(32, Nums.pow2(5));
        assertEquals(64, Nums.pow2(6));
        assertEquals(128, Nums.pow2(7));
        assertEquals(256, Nums.pow2(8));
    }

    /**
     * Test of gcd method, of class FastMath.
     */
    @Test
    public void testGcd_int_int()
    {
        assertEquals(1, Nums.gcd(2,3));
        assertEquals(1, Nums.gcd(3,5));
        assertEquals(1, Nums.gcd(5,7));
        assertEquals(1, Nums.gcd(7,9));
        assertEquals(1, Nums.gcd(9,11));
        assertEquals(1, Nums.gcd(11,13));
        
        assertEquals(2, Nums.gcd(2,6));
        assertEquals(3, Nums.gcd(6,9));
        assertEquals(4, Nums.gcd(8,12));
        assertEquals(5, Nums.gcd(5,10));
        assertEquals(6, Nums.gcd(6,12));
        assertEquals(7, Nums.gcd(7,14));
    }

    /**
     * Test of gcd method, of class FastMath.
     */
    @Test
    public void testGcd_int_intArr()
    {
        assertEquals(1, Nums.gcd(2,3,4,5));
        assertEquals(2, Nums.gcd(4,6,8,12));
        assertEquals(3, Nums.gcd(6,9,12));
        assertEquals(4, Nums.gcd(4,8,12));
    }

    /**
     * Test of gcd method with negative numbers, of class FastMath.
     */
    @Test
    public void testGcd_negative()
    {
        assertEquals(2, Nums.gcd(-4, 6));
        assertEquals(2, Nums.gcd(4, -6));
        assertEquals(2, Nums.gcd(-4, -6));
        assertEquals(3, Nums.gcd(-6, -9));
        assertEquals(12, Nums.gcd(-24, 36));
        assertEquals(5, Nums.gcd(0, -5));
        assertEquals(1, Nums.gcd(-7, 3));
        assertEquals(2, Nums.gcd(-2, 0));
        assertEquals(2, Nums.gcd(-4, -6, 8));
        assertEquals(2, Nums.gcd(4, -6, -8));
    }

    /**
     * Test of gcd method with negative long numbers, of class FastMath.
     */
    @Test
    public void testGcd_long_negative()
    {
        assertEquals(2L, Nums.gcd(-4L, 6L));
        assertEquals(2L, Nums.gcd(4L, -6L));
        assertEquals(2L, Nums.gcd(-4L, -6L));
        assertEquals(3L, Nums.gcd(-6L, -9L));
        assertEquals(12L, Nums.gcd(-24L, 36L));
        assertEquals(5L, Nums.gcd(0L, -5L));
        assertEquals(1L, Nums.gcd(-7L, 3L));
        assertEquals(2L, Nums.gcd(-2L, 0L));
        assertEquals(2L, Nums.gcd(-4L, -6L, 8L));
        assertEquals(2L, Nums.gcd(4L, -6L, -8L));
    }

    /**
     * Test of lcm method, of class FastMath.
     */
    @Test
    public void testLcm_int_int()
    {
        assertEquals(2, Nums.lcm(1,2));
        assertEquals(6, Nums.lcm(2,3));
        assertEquals(12, Nums.lcm(3,4));
        assertEquals(20, Nums.lcm(4,5));
        assertEquals(42, Nums.lcm(6,7));
        assertEquals(56, Nums.lcm(7,8));
        
        assertEquals(6, Nums.lcm(2,6));
        assertEquals(6, Nums.lcm(3,6));
        assertEquals(24, Nums.lcm(6,8));
        assertEquals(12, Nums.lcm(3,12));
        assertEquals(10, Nums.lcm(5,10));
        assertEquals(12, Nums.lcm(6,12));
    }

    /**
     * Test of lcm method, of class FastMath.
     */
    @Test
    public void testLcm_int_intArr()
    {
        assertEquals(60, Nums.lcm(2,3,4,5));
        assertEquals(24, Nums.lcm(4,6,8,12));
        assertEquals(36, Nums.lcm(6,9,12));
        assertEquals(24, Nums.lcm(4,8,12));
        assertEquals(1155, Nums.lcm(3, 5, 7, 11));
    }

    /**
     * Test of lcm method with negative numbers, of class FastMath.
     */
    @Test
    public void testLcm_negative()
    {
        assertEquals(12, Nums.lcm(-4, 6));
        assertEquals(12, Nums.lcm(4, -6));
        assertEquals(12, Nums.lcm(-4, -6));
        assertEquals(18, Nums.lcm(-6, -9));
        assertEquals(72, Nums.lcm(-24, 36));
        assertEquals(5, Nums.lcm(-1, -5));
        assertEquals(21, Nums.lcm(-7, 3));
        assertEquals(0, Nums.lcm(-2, 0));
        assertEquals(24, Nums.lcm(-4, -6, 8));
        assertEquals(24, Nums.lcm(4, -6, -8));
        assertEquals(6, Nums.lcm(-6));
        assertEquals(12L, Nums.lcm(-4L, 6L));
        assertEquals(12L, Nums.lcm(4L, -6L));
        assertEquals(12L, Nums.lcm(-4L, -6L));
        assertEquals(18L, Nums.lcm(-6L, -9L));
        assertEquals(72L, Nums.lcm(-24L, 36L));
        assertEquals(5L, Nums.lcm(-1L, -5L));
        assertEquals(21L, Nums.lcm(-7L, 3L));
        assertEquals(0L, Nums.lcm(-2L, 0L));
        assertEquals(24L, Nums.lcm(-4L, -6L, 8L));
        assertEquals(24L, Nums.lcm(4L, -6L, -8L));
        assertEquals(6L, Nums.lcm(-6L));
        assertEquals(BigInteger.valueOf(12), Nums.lcm(BigInteger.valueOf(-4), BigInteger.valueOf(6)));
        assertEquals(BigInteger.valueOf(12), Nums.lcm(BigInteger.valueOf(-4), BigInteger.valueOf(-6)));
        assertEquals(BigInteger.valueOf(72), Nums.lcm(BigInteger.valueOf(-24), BigInteger.valueOf(36)));
        assertEquals(BigInteger.valueOf(6), Nums.lcm(BigInteger.valueOf(-6)));
    }


  
    static final int[] FIBONACCI = {0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, 17711, 28657, 46368, 75025, 121393, 196418, 317811, 514229, 832040, 1346269, 2178309, 3524578, 5702887, 9227465, 14930352, 24157817, 39088169};
    static final int[] FIBO13_21 = {13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, 17711, 28657, 46368, 75025, 121393, 196418, 317811, 514229, 832040, 1346269, 2178309, 3524578, 5702887, 9227465, 14930352, 24157817, 39088169};

    static final BigInteger[] FIBONACCI_BIG = As.bigIntegers(FIBONACCI);
    static final BigInteger[] FIBO13_21_BIG = As.bigIntegers(FIBO13_21);
    
    /**
     * Test of fibonacci method, of class FastMath.
     */
    @Test
    public void testFibonacci_intArr()
    {
        int[] fibonacci = new int[FIBONACCI.length];
        assertEquals(FIBONACCI.length, Nums.fibonacci(fibonacci));
        assertArrayEquals(FIBONACCI, fibonacci);
    }

    /**
     * Test of fibonacci method, of class FastMath.
     */
    @Test
    public void testFibonacci_3args()
    {
        int[] fibonacci = new int[FIBONACCI.length];
        assertEquals(FIBONACCI.length, Nums.fibonacci(fibonacci, 0, 1));
        assertArrayEquals(FIBONACCI, fibonacci);
        
        fibonacci = new int[FIBO13_21.length];
        assertEquals(FIBO13_21.length, Nums.fibonacci(fibonacci, 13, 21));
        assertArrayEquals(FIBO13_21, fibonacci);
    }

    /**
     * Test of fibonacci method, of class FastMath.
     */
    @Test
    public void testFibonacci_5args()
    {
        int[] fibonacci = new int[FIBONACCI.length];
        Nums.fibonacci(fibonacci, 0, 1, 0, FIBONACCI.length/2);
        Nums.fibonacci(fibonacci, FIBONACCI[ FIBONACCI.length/2],  FIBONACCI[ FIBONACCI.length/2+1],  FIBONACCI.length/2,  FIBONACCI.length);
        assertArrayEquals(FIBONACCI, fibonacci);
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
    public void testIsZero_BigDecimal()
    {
        assertFalse(BigMaths.isZero(BigDecimal.valueOf(-1)));
        assertTrue(BigMaths.isZero(BigDecimal.ZERO));
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
    public void testIsNullOrZero_BigDecimal()
    {
        assertFalse(BigMaths.isNullOrZero(BigDecimal.valueOf(-1)));
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
        assertFalse(BigMaths.isNullOrZero(BigDecimal.valueOf(-1), delta));
        assertTrue(BigMaths.isNullOrZero(BigDecimal.ZERO, delta));
        assertFalse(BigMaths.isNullOrZero(BigDecimal.ONE, delta));

        assertFalse(BigMaths.isNullOrZero(new BigDecimal(0.003), delta));
        assertTrue(BigMaths.isNullOrZero(new BigDecimal(0.001), delta));
        assertTrue(BigMaths.isNullOrZero(new BigDecimal(-0.001), delta));
        assertFalse(BigMaths.isNullOrZero(new BigDecimal(-0.003), delta));
    }

    /**
     * Test of sum method of class BigMaths, with BigInteger.
     */
    @Test
    public void testSum_BigIntegerArr()
    {
        assertEquals(BigInteger.ZERO, BigMaths.sum(BigInteger.ZERO));
        assertEquals(BigInteger.ZERO, BigMaths.sum(BigInteger.ZERO, BigInteger.ZERO));
        assertEquals(BigInteger.ONE, BigMaths.sum(BigInteger.ONE, BigInteger.ZERO));
        assertEquals(BigInteger.valueOf(3), BigMaths.sum(BigInteger.ONE, BigInteger.valueOf(2)));
        assertEquals(BigInteger.valueOf(6), BigMaths.sum(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3)));
        assertEquals(BigInteger.valueOf(6), BigMaths.sum(BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3), null));
    }


    /**
     * Test of sum method with MathContext, of class BigMaths.
     */
    @Test
    public void testSum_MathContext()
    {
        assertEquals(BigDecimal.ZERO, BigMaths.sum(MathContext.DECIMAL32));
        assertEquals(BigDecimal.ONE, BigMaths.sum(MathContext.DECIMAL32, BigDecimal.ONE, BigDecimal.ZERO));
        assertEquals(new BigDecimal("0.3333333"), BigMaths.sum(MathContext.DECIMAL32, BigDecimal.ZERO, new BigDecimal("0.33333333")));
        assertEquals(BigDecimal.valueOf(6), BigMaths.sum(MathContext.DECIMAL32, BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3), null));
    }

    /**
     * Test of isPow method, of class FastMath.
     */
    @Test
    public void testIsIntegerPow()
    {
        assertFalse(Nums.isIntegerPow(-1));
        assertTrue(Nums.isIntegerPow(0));
        assertTrue(Nums.isIntegerPow(1));
        assertFalse(Nums.isIntegerPow(2));
        
        assertFalse(Nums.isIntegerPow(3));
        assertTrue(Nums.isIntegerPow(4));
        assertFalse(Nums.isIntegerPow(5));
        
        assertFalse(Nums.isIntegerPow(8));
        assertTrue(Nums.isIntegerPow(9));
        assertFalse(Nums.isIntegerPow(10));

        assertFalse(Nums.isIntegerPow(15));
        assertTrue(Nums.isIntegerPow(16));
        assertFalse(Nums.isIntegerPow(17));

        assertFalse(Nums.isIntegerPow(24));
        assertTrue(Nums.isIntegerPow(25));
        assertFalse(Nums.isIntegerPow(26));
    }


    @Test
    public void testSomeMethod()
    {
        BigDecimal[] pow01 = {BIG_DEC_MILI, BIG_DEC_MICRO, BIG_DEC_NANO, BIG_DEC_PICO, BIG_DEC_FEMTO, BIG_DEC_ATTO, BIG_DEC_ZEPTO, BIG_DEC_YOCTO};	
        BigDecimal[] pow10 = {BIG_DEC_KILO, BIG_DEC_MEGA, BIG_DEC_GIGA, BIG_DEC_TERA, BIG_DEC_PETA, BIG_DEC_EXA, BIG_DEC_ZETTA, BIG_DEC_YOTTA};	
        BigDecimal[] pow2 = {BIG_DEC_KIBI, BIG_DEC_MEBI, BIG_DEC_GIBI, BIG_DEC_TEBI, BIG_DEC_PEBI, BIG_DEC_EXBI, BIG_DEC_ZEBI, BIG_DEC_YOBI};
        
        BigDecimal k10 = new BigDecimal(1000);
        BigDecimal k2 = new BigDecimal(1024);
        
        for(int i=1;i<pow01.length;i++)
        {
            assertEquals(pow01[i], pow01[i-1].divide(k10), ""+i);
        }
        for(int i=1;i<pow10.length;i++)
        {
            assertEquals(pow10[i], pow10[i-1].multiply(k10), ""+i);
        }
        for(int i=1;i<pow2.length;i++)
        {
            assertEquals(pow2[i], pow2[i-1].multiply(k2), ""+i);
        }
    }

    /**
     * Test of serie125 method, of class BigDecimals.
     */
    @Test
    public void testSerie125_BigDecimal()
    {
        {
            BigDecimal[] expected = {BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(5)};
            BigDecimal[] result = Nums.serie125(BigDecimal.ONE, BigDecimal.TEN);
            assertArrayEquals(expected, result);
        }
        {        
            BigDecimal[] expected = {new BigDecimal("0.1"), new BigDecimal("0.2"), new BigDecimal("0.5"), BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(5)};
            BigDecimal[] result = Nums.serie125(new BigDecimal("0.1"), BigDecimal.TEN);
            assertArrayEquals(expected, result);
        }
        {        
            BigDecimal[] expected = {new BigDecimal("0.1"), new BigDecimal("0.2"), new BigDecimal("0.5")};
            BigDecimal[] result = Nums.serie125(new BigDecimal("0.06"), BigDecimal.ONE);
            assertArrayEquals(expected, result);
        }
        {        
            BigDecimal[] expected = {BigDecimal.ONE, new BigDecimal("0.5"), new BigDecimal("0.2")};
            BigDecimal[] result = Nums.serie125(BigDecimal.ONE, new BigDecimal("0.1"));
            assertArrayEquals(expected, result);
        }
        {
            BigDecimal[] expected = {BigDecimal.TEN, BigDecimal.valueOf(5), BigDecimal.valueOf(2)};
            BigDecimal[] result = Nums.serie125(BigDecimal.TEN, BigDecimal.ONE);
            assertArrayEquals(expected, result);
        }
    }

    @Test
    public void testSomeMethod_BigInteger()
    {
        BigInteger[] pow10 = {BIG_INT_KILO, BIG_INT_MEGA, BIG_INT_GIGA, BIG_INT_TERA, BIG_INT_PETA, BIG_INT_EXA, BIG_INT_ZETTA, BIG_INT_YOTTA};	
        BigInteger[] pow2 = {BIG_INT_KIBI, BIG_INT_MEBI, BIG_INT_GIBI, BIG_INT_TEBI, BIG_INT_PEBI, BIG_INT_EXBI, BIG_INT_ZEBI, BIG_INT_YOBI};
        
        BigInteger k10 = BigInteger.valueOf(1000);
        BigInteger k2 = BigInteger.valueOf(1024);
        
        for(int i=1;i<pow10.length;i++)
        {
            assertEquals(pow10[i], pow10[i-1].multiply(k10), ""+i);
        }
        for(int i=1;i<pow2.length;i++)
        {
            assertEquals(pow2[i], pow2[i-1].multiply(k2), ""+i);
        }
    }

    /**
     * Test of serie125 method, of class BigIntegers.
     */
    @Test
    public void testSerie125_BigInteger()
    {
        {
            BigInteger[] expected = {BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(5)};
            BigInteger[] result = Nums.serie125(BigInteger.ONE, BigInteger.TEN);
            assertArrayEquals(expected, result);
        }
        {
            BigInteger[] expected = {BigInteger.TEN, BigInteger.valueOf(5), BigInteger.valueOf(2)};
            BigInteger[] result = Nums.serie125(BigInteger.TEN, BigInteger.ONE);
            assertArrayEquals(expected, result);
        }
    }

    /**
     * Test of gcd method, of class BigMaths.
     */
    @Test
    public void testGcd_BigInteger_BigInteger()
    {
        assertEquals(_1, Nums.gcd(_2,_3));
        assertEquals(_1, Nums.gcd(_3,_5));
        assertEquals(_1, Nums.gcd(_5,_7));
        assertEquals(_1, Nums.gcd(_7,_9));
        assertEquals(_1, Nums.gcd(_9,_11));
        assertEquals(_1, Nums.gcd(_11,_13));
        
        assertEquals(_2, Nums.gcd(_2,_6));
        assertEquals(_3, Nums.gcd(_6,_9));
        assertEquals(_4, Nums.gcd(_8,_12));
        assertEquals(_5, Nums.gcd(_5,_10));
        assertEquals(_6, Nums.gcd(_6,_12));
        assertEquals(_7, Nums.gcd(_7,_14));
    }

    /**
     * Test of gcd method, of class BigMaths.
     */
    @Test
    public void testGcd_BigInteger_BigIntegerArr()
    {
        assertEquals(_1, Nums.gcd(_2,_3,_4,_5));
        assertEquals(_2, Nums.gcd(_4,_6,_8,_12));
        assertEquals(_3, Nums.gcd(_6,_9,_12));
        assertEquals(_4, Nums.gcd(_4,_8,_12));
    }

    /**
     * Test of lcm method, of class BigMaths.
     */
    @Test
    public void testLcm_BigInteger_BigInteger()
    {
        assertEquals(_2, Nums.lcm(_1,_2));
        assertEquals(_6, Nums.lcm(_2,_3));
        assertEquals(_12, Nums.lcm(_3,_4));
        assertEquals(_20, Nums.lcm(_4,_5));
        assertEquals(_42, Nums.lcm(_6,_7));
        assertEquals(_56, Nums.lcm(_7,_8));
        
        assertEquals(_6, Nums.lcm(_2,_6));
        assertEquals(_6, Nums.lcm(_3,_6));
        assertEquals(_24, Nums.lcm(_6,_8));
        assertEquals(_12, Nums.lcm(_3,_12));
        assertEquals(_10, Nums.lcm(_5,_10));
        assertEquals(_12, Nums.lcm(_6,_12));
    }

    /**
     * Test of lcm method, of class BigMaths.
     */
    @Test
    public void testLcm_BigInteger_BigIntegerArr()
    {
        assertEquals(_60, Nums.lcm(_2,_3,_4,_5));
        assertEquals(_24, Nums.lcm(_4,_6,_8,_12));
        assertEquals(_36, Nums.lcm(_6,_9,_12));
        assertEquals(_24, Nums.lcm(_4,_8,_12));
    }


    /**
     * Test of fibonacci method, of class BigMaths.
     */
    @Test
    public void testFibonacci_BigIntegerArr()
    {
        BigInteger[] fibonacci = new BigInteger[FIBONACCI.length];
        assertEquals(FIBONACCI_BIG.length, Nums.fibonacci(fibonacci));
        assertArrayEquals(FIBONACCI_BIG, fibonacci);
    }

    /**
     * Test of fibonacci method, of class BigMaths.
     */
    @Test
    public void testFibonacci_BigInteger_3args()
    {
        BigInteger[] fibonacci = new BigInteger[FIBONACCI_BIG.length];
        assertEquals(FIBONACCI_BIG.length, Nums.fibonacci(fibonacci, BigInteger.ZERO, BigInteger.ONE));
        assertArrayEquals(FIBONACCI_BIG, fibonacci);
        
        fibonacci = new BigInteger[FIBO13_21_BIG.length];
        assertEquals(FIBO13_21_BIG.length, Nums.fibonacci(fibonacci, BigInteger.valueOf(13), BigInteger.valueOf(21)));
        assertArrayEquals(FIBO13_21_BIG, fibonacci);
    }

    /**
     * Test of fibonacci method, of class BigMaths.
     */
    @Test
    public void testFibonacci_BigInteger_5args()
    {
        BigInteger[] fibonacci = new BigInteger[FIBONACCI_BIG.length];
        Nums.fibonacci(fibonacci, BigInteger.ZERO, BigInteger.ONE, 0, FIBONACCI.length/2);
        Nums.fibonacci(fibonacci, FIBONACCI_BIG[ FIBONACCI_BIG.length/2],  FIBONACCI_BIG[FIBONACCI_BIG.length/2+1],  FIBONACCI_BIG.length/2,  FIBONACCI_BIG.length);
        assertArrayEquals(FIBONACCI_BIG, fibonacci);
    }
    
    /**
     * Test of serie125 method, of class BigIntegers.
     */
    @Test
    public void testSerie125()
    {
        {
            BigInteger[] expected = {BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(5)};
            BigInteger[] result = Nums.serie125(BigInteger.ONE, BigInteger.TEN);
            assertArrayEquals(expected, result);
        }
        {
            BigInteger[] expected = {BigInteger.TEN, BigInteger.valueOf(5), BigInteger.valueOf(2)};
            BigInteger[] result = Nums.serie125(BigInteger.TEN, BigInteger.ONE);
            assertArrayEquals(expected, result);
        }
    }

    static SecureRandom rnd = new SecureRandom();
    /**
     * Test of safePrime method, of class BigMaths.
     */
    @Test
    public void testSafePrime()
    {
        BigInteger p = Nums.safePrime(rnd, 500);
        assertTrue(p.isProbablePrime(99));
        BigInteger q = p.subtract(BigInteger.ONE).divide(BIG_INT_TWO);
        assertTrue(q.isProbablePrime(99));
    }

    /**
     * Test of gcd method, of class BigMaths.
     */
    @Test
    public void testGcd_long_long()
    {
        assertEquals(1L, Nums.gcd(2L, 3L));
        assertEquals(1L, Nums.gcd(3L, 5L));
        assertEquals(1L, Nums.gcd(5L, 7L));
        assertEquals(1L, Nums.gcd(7L, 9L));
        assertEquals(1L, Nums.gcd(9L, 11L));
        assertEquals(1L, Nums.gcd(11L, 13L));
        
        assertEquals(2L, Nums.gcd(2L, 6L));
        assertEquals(3L, Nums.gcd(6L, 9L));
        assertEquals(4L, Nums.gcd(8L, 12L));
        assertEquals(5L, Nums.gcd(5L, 10L));
        assertEquals(6L, Nums.gcd(6L, 12L));
        assertEquals(7L, Nums.gcd(7L, 14L));
    }

    /**
     * Test of gcd method, of class BigMaths.
     */
    @Test
    public void testGcd_long_longArr()
    {
        assertEquals(1L, Nums.gcd(2L, 3L, 4L, 5L));
        assertEquals(2L, Nums.gcd(4L, 6L, 8L, 12L));
        assertEquals(3L, Nums.gcd(6L, 9L, 12L));
        assertEquals(4L, Nums.gcd(4L, 8L, 12L));
    }

    /**
     * Test of lcm method, of class BigMaths.
     */
    @Test
    public void testLcm_long_long()
    {
        assertEquals(2L, Nums.lcm(1L, 2L));
        assertEquals(6L, Nums.lcm(2L, 3L));
        assertEquals(12L, Nums.lcm(3L, 4L));
        assertEquals(20L, Nums.lcm(4L, 5L));
        assertEquals(42L, Nums.lcm(6L, 7L));
        assertEquals(56L, Nums.lcm(7L, 8L));
        
        assertEquals(6L, Nums.lcm(2L, 6L));
        assertEquals(6L, Nums.lcm(3L, 6L));
        assertEquals(24L, Nums.lcm(6L, 8L));
        assertEquals(12L, Nums.lcm(3L, 12L));
        assertEquals(10L, Nums.lcm(5L, 10L));
        assertEquals(12L, Nums.lcm(6L, 12L));
    }

    /**
     * Test of lcm method, of class BigMaths.
     */
    @Test
    public void testLcm_long_longArr()
    {
        assertEquals(60L, Nums.lcm(2L, 3L, 4L, 5L));
        assertEquals(24L, Nums.lcm(4L, 6L, 8L, 12L));
        assertEquals(36L, Nums.lcm(6L, 9L, 12L));
        assertEquals(24L, Nums.lcm(4L, 8L, 12L));
        assertEquals(1155L, Nums.lcm(3L, 5L, 7L, 11L));
    }

    static long[] FIBONACCI_LONG = As.longs(FIBONACCI);
    static long[] FIBO13_21_LONG = As.longs(FIBO13_21);
    
    /**
     * Test of fibonacci method, of class BigMaths.
     */
    @Test
    public void testFibonacci_longArr()
    {
        long[] fibonacci = new long[FIBONACCI_LONG.length];
        assertEquals(FIBONACCI_LONG.length, Nums.fibonacci(fibonacci));
        assertArrayEquals(FIBONACCI_LONG, fibonacci);
    }

    /**
     * Test of fibonacci method, of class BigMaths.
     */
    @Test
    public void testFibonacci_long_3args()
    {
        long[] fibonacci = new long[FIBONACCI_LONG.length];
        assertEquals(FIBONACCI_LONG.length, Nums.fibonacci(fibonacci, 0L, 1L));
        assertArrayEquals(FIBONACCI_LONG, fibonacci);
        
        fibonacci = new long[FIBO13_21_LONG.length];
        assertEquals(FIBO13_21_LONG.length, Nums.fibonacci(fibonacci, 13, 21));
        assertArrayEquals(FIBO13_21_LONG, fibonacci);
    }

    /**
     * Test of fibonacci method, of class BigMaths.
     */
    @Test
    public void testFibonacci_long_5args()
    {
        long[] fibonacci = new long[FIBONACCI_LONG.length];
        Nums.fibonacci(fibonacci, 0L, 1L, 0, FIBONACCI_LONG.length/2);
        Nums.fibonacci(fibonacci, FIBONACCI_LONG[ FIBONACCI_LONG.length/2],  FIBONACCI_LONG[ FIBONACCI_LONG.length/2+1],  FIBONACCI_LONG.length/2,  FIBONACCI_LONG.length);
        assertArrayEquals(FIBONACCI_LONG, fibonacci);
    }

    /**
     * Test of isPow2 method, of class FastMath.
     */
    @Test
    public void testIsPow2_long()
    {
        assertFalse(Maths.isPow2(-1L));
        assertFalse(Maths.isPow2(0L));
        assertFalse(Maths.isPow2(3L));
        assertFalse(Maths.isPow2(5L));
        assertFalse(Maths.isPow2(7L));
        assertFalse(Maths.isPow2(9L));
        assertFalse(Maths.isPow2(15L));
        assertFalse(Maths.isPow2(17L));
        assertFalse(Maths.isPow2(31L));
        assertFalse(Maths.isPow2(33L));
        assertFalse(Maths.isPow2(63L));
        assertFalse(Maths.isPow2(65L));
        assertFalse(Maths.isPow2(127L));
        assertFalse(Maths.isPow2(129L));
        
        assertTrue(Maths.isPow2(1L));
        assertTrue(Maths.isPow2(2L));
        assertTrue(Maths.isPow2(4L));
        assertTrue(Maths.isPow2(8L));
        assertTrue(Maths.isPow2(16L));
        assertTrue(Maths.isPow2(32L));
        assertTrue(Maths.isPow2(64L));
        assertTrue(Maths.isPow2(128L));
        
        assertFalse(Maths.isPow2((long)Byte.MAX_VALUE));
        assertFalse(Maths.isPow2((long)Short.MAX_VALUE));
        assertFalse(Maths.isPow2((long)Character.MAX_VALUE));
        assertFalse(Maths.isPow2((long)Integer.MAX_VALUE));
        assertFalse(Maths.isPow2(Long.MAX_VALUE));

        assertTrue(Maths.isPow2(Byte.MAX_VALUE+1L));
        assertTrue(Maths.isPow2(Short.MAX_VALUE+1L));
        assertTrue(Maths.isPow2(Character.MAX_VALUE+1L));
        assertTrue(Maths.isPow2(Integer.MAX_VALUE+1L));

        assertFalse(Maths.isPow2((long)-Byte.MAX_VALUE));
        assertFalse(Maths.isPow2((long)-Short.MAX_VALUE));
        assertFalse(Maths.isPow2((long)-Character.MAX_VALUE));
        assertFalse(Maths.isPow2((long)-Integer.MAX_VALUE));
        assertFalse(Maths.isPow2(-Long.MAX_VALUE));

        assertFalse(Maths.isPow2(-Byte.MAX_VALUE-1L));
        assertFalse(Maths.isPow2(-Short.MAX_VALUE-1L));
        assertFalse(Maths.isPow2(-Character.MAX_VALUE-1L));
        assertFalse(Maths.isPow2(-Integer.MAX_VALUE-1L));
    }

    /**
     * Test of isProbablePrime method, of class FastMath.
     */
    @Test
    public void testIsProbablePrime()
    {
        assertTrue(Nums.isProbablePrime(0,0));
        assertTrue(Nums.isProbablePrime(1,0));
        assertTrue(Nums.isProbablePrime(2,0));
        assertTrue(Nums.isProbablePrime(3,0));
        
        assertFalse(Nums.isProbablePrime(0,1024));
        assertFalse(Nums.isProbablePrime(1,1024));
        assertFalse(Nums.isProbablePrime(4,1024));
        assertFalse(Nums.isProbablePrime(8,1024));
        
        for(long prime : Nums.SmalPrimesHolder.SMALL_PRIMES)
        {
            assertTrue(Nums.isProbablePrime((int)prime,1024));
        }

        assertEquals(true, Nums.isProbablePrime(100003L,1024));
        assertEquals(false, Nums.isProbablePrime(10005L,1024));        
    }

    /**
     * Test of equalsEnough method, of class BigMaths.
     */
    @Test
    public void testEqualsEnough_3args_3()
    {
        assertTrue(Maths.equalsEnough(0f, 0f, 0f));
        assertTrue(Maths.equalsEnough(0f, 1f, 1f));
        assertFalse(Maths.equalsEnough(0f, 10f, 1f));

        assertTrue(Maths.equalsEnough(0.1001f, 0.1f, 0.01f));
        assertFalse(Maths.equalsEnough(0.1001f, 0.2f, 0.01f));
    }

    /**
     * Test of equalsEnough method, of class BigMaths.
     */
    @Test
    public void testEqualsEnough_3args_4()
    {
        assertTrue(Maths.equalsEnough(0d, 0d, 0d));
        assertTrue(Maths.equalsEnough(0d, 1d, 1d));
        assertFalse(Maths.equalsEnough(0d, 10d, 1d));

        assertTrue(Maths.equalsEnough(0.1001, 0.1, 0.01));
        assertFalse(Maths.equalsEnough(0.1001, 0.2, 0.01));
    }

    /**
     * Test of equalsEnough method, of class BigMaths.
     */
    @Test
    public void testEqualsEnough_3args_5()
    {
        assertTrue(BigMaths.equalsEnough(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        assertTrue(BigMaths.equalsEnough(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE));
        assertFalse(BigMaths.equalsEnough(BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE));

        assertTrue(BigMaths.equalsEnough(BigDecimal.valueOf(0.1001), BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.01)));
        assertFalse(BigMaths.equalsEnough(BigDecimal.valueOf(0.1001), BigDecimal.valueOf(0.2), BigDecimal.valueOf(0.01)));
    }


    /**
     * Test of ceilDiv method, of class BigMaths.
     */
    @Test
    public void testCeilDiv_int()
    {
        assertEquals(3, Nums.ceilDiv(5, 2));
        assertEquals(2, Nums.ceilDiv(4, 2));
        assertEquals(1, Nums.ceilDiv(1, 2));
        assertEquals(0, Nums.ceilDiv(0, 2));
        assertEquals(-2, Nums.ceilDiv(-5, 2));
        assertEquals(-2, Nums.ceilDiv(-4, 2));
        assertEquals(-2, Nums.ceilDiv(5, -2));
        assertEquals(-2, Nums.ceilDiv(4, -2));
        assertEquals(3, Nums.ceilDiv(-5, -2));
        assertEquals(2, Nums.ceilDiv(-4, -2));
        assertEquals(Integer.MAX_VALUE, Nums.ceilDiv(Integer.MAX_VALUE, 1));
        assertEquals(Integer.MIN_VALUE, Nums.ceilDiv(Integer.MIN_VALUE, 1));
        assertThrows(ArithmeticException.class, () -> Nums.ceilDiv(1, 0));
    }

    /**
     * Test of ceilDiv method, of class BigMaths.
     */
    @Test
    public void testCeilDiv_long()
    {
        assertEquals(3L, Nums.ceilDiv(5L, 2L));
        assertEquals(2L, Nums.ceilDiv(4L, 2L));
        assertEquals(1L, Nums.ceilDiv(1L, 2L));
        assertEquals(0L, Nums.ceilDiv(0L, 2L));
        assertEquals(-2L, Nums.ceilDiv(-5L, 2L));
        assertEquals(-2L, Nums.ceilDiv(-4L, 2L));
        assertEquals(-2L, Nums.ceilDiv(5L, -2L));
        assertEquals(-2L, Nums.ceilDiv(4L, -2L));
        assertEquals(3L, Nums.ceilDiv(-5L, -2L));
        assertEquals(2L, Nums.ceilDiv(-4L, -2L));
        assertEquals(Long.MAX_VALUE, Nums.ceilDiv(Long.MAX_VALUE, 1L));
        assertEquals(Long.MIN_VALUE, Nums.ceilDiv(Long.MIN_VALUE, 1L));
        assertThrows(ArithmeticException.class, () -> Nums.ceilDiv(1L, 0L));
    }

    /**
     * Test of between method, of class BigMaths.
     */
    @Test
    public void testBetween_byte()
    {
        assertTrue(Maths.between((byte)1, (byte)5, (byte)1, (byte)3, (byte)5));
        assertTrue(Maths.between((byte)5, (byte)1, (byte)3, (byte)5, (byte)1));
        assertTrue(Maths.between((byte)2, (byte)2, (byte)2, (byte)2));
        assertFalse(Maths.between((byte)1, (byte)5, (byte)0, (byte)3));
        assertFalse(Maths.between((byte)1, (byte)5, (byte)3, (byte)6));
        assertTrue(Maths.between((byte)1, (byte)5));
        assertTrue(Maths.between((byte)1, (byte)5, new byte[0]));
        assertTrue(Maths.between((byte)1, (byte)5, (byte[])null));
    }

    /**
     * Test of between method, of class BigMaths.
     */
    @Test
    public void testBetween_int()
    {
        assertTrue(Maths.between(1, 5, 1, 3, 5));
        assertTrue(Maths.between(5, 1, 3, 5, 1));
        assertTrue(Maths.between(2, 2, 2, 2));
        assertFalse(Maths.between(1, 5, 0, 3));
        assertFalse(Maths.between(1, 5, 3, 6));
        assertTrue(Maths.between(1, 5));
        assertTrue(Maths.between(1, 5, new int[0]));
        assertTrue(Maths.between(1, 5, (int[])null));
    }

    /**
     * Test of between method, of class BigMaths.
     */
    @Test
    public void testBetween_long()
    {
        assertTrue(Maths.between(1L, 5L, 1L, 3L, 5L));
        assertTrue(Maths.between(5L, 1L, 3L, 5L, 1L));
        assertTrue(Maths.between(2L, 2L, 2L, 2L));
        assertFalse(Maths.between(1L, 5L, 0L, 3L));
        assertFalse(Maths.between(1L, 5L, 3L, 6L));
        assertTrue(Maths.between(1L, 5L));
        assertTrue(Maths.between(1L, 5L, new long[0]));
        assertTrue(Maths.between(1L, 5L, (long[])null));
    }

    /**
     * Test of between method, of class BigMaths.
     */
    @Test
    public void testBetween_float()
    {
        assertTrue(Maths.between(1.0f, 5.0f, 1.0f, 3.0f, 5.0f));
        assertTrue(Maths.between(5.0f, 1.0f, 3.0f, 5.0f, 1.0f));
        assertTrue(Maths.between(2.0f, 2.0f, 2.0f, 2.0f));
        assertFalse(Maths.between(1.0f, 5.0f, 0.0f, 3.0f));
        assertFalse(Maths.between(1.0f, 5.0f, 3.0f, 6.0f));
        assertTrue(Maths.between(1.0f, 5.0f));
        assertTrue(Maths.between(1.0f, 5.0f, new float[0]));
        assertTrue(Maths.between(1.0f, 5.0f, (float[])null));
    }

    /**
     * Test of between method, of class BigMaths.
     */
    @Test
    public void testBetween_double()
    {
        assertTrue(Maths.between(1.0, 5.0, 1.0, 3.0, 5.0));
        assertTrue(Maths.between(5.0, 1.0, 3.0, 5.0, 1.0));
        assertTrue(Maths.between(2.0, 2.0, 2.0, 2.0));
        assertFalse(Maths.between(1.0, 5.0, 0.0, 3.0));
        assertFalse(Maths.between(1.0, 5.0, 3.0, 6.0));
        assertTrue(Maths.between(1.0, 5.0));
        assertTrue(Maths.between(1.0, 5.0, new double[0]));
        assertTrue(Maths.between(1.0, 5.0, (double[])null));
    }

    /**
     * Test of bound method, of class BigMaths.
     */
    @Test
    public void testBound_3args_1()
    {
        byte z = 0;
        byte c = 100;
        byte n = -1;
        byte m = 50;
        byte o = 127;
        assertEquals(z, Maths.bound(z, c, n));
        assertEquals(z, Maths.bound(z, c, z));
        assertEquals(m, Maths.bound(z, c, m));
        assertEquals(c, Maths.bound(z, c, c));
        assertEquals(c, Maths.bound(z, c, o));
        assertEquals(c, Maths.bound(c, z, o));
    }

    /**
     * Test of bound method, of class BigMaths.
     */
    @Test
    public void testBound_3args_2()
    {
        short z = 0;
        short c = 100;
        short n = -1;
        short m = 50;
        short o = 127;
        assertEquals(z, Maths.bound(z, c, n));
        assertEquals(z, Maths.bound(z, c, z));
        assertEquals(m, Maths.bound(z, c, m));
        assertEquals(c, Maths.bound(z, c, c));
        assertEquals(c, Maths.bound(z, c, o));
        assertEquals(c, Maths.bound(c, z, o));
    }

    /**
     * Test of bound method, of class BigMaths.
     */
    @Test
    public void testBound_3args_3()
    {
        int z = 0;
        int c = 100;
        int n = -1;
        int m = 50;
        int o = 127;
        assertEquals(z, Maths.bound(z, c, n));
        assertEquals(z, Maths.bound(z, c, z));
        assertEquals(m, Maths.bound(z, c, m));
        assertEquals(c, Maths.bound(z, c, c));
        assertEquals(c, Maths.bound(z, c, o));
        assertEquals(c, Maths.bound(c, z, o));
    }

    /**
     * Test of bound method, of class BigMaths.
     */
    @Test
    public void testBound_3args_4()
    {
        long z = 0;
        long c = 100;
        long n = -1;
        long m = 50;
        long o = 127;
        assertEquals(z, Maths.bound(z, c, n));
        assertEquals(z, Maths.bound(z, c, z));
        assertEquals(m, Maths.bound(z, c, m));
        assertEquals(c, Maths.bound(z, c, c));
        assertEquals(c, Maths.bound(z, c, o));
        assertEquals(c, Maths.bound(c, z, o));
    }

    /**
     * Test of bound method, of class BigMaths.
     */
    @Test
    public void testBound_3args_5()
    {
        float z = 0;
        float c = 100;
        float n = -1;
        float m = 50;
        float o = 127;
        assertEquals(z, Maths.bound(z, c, n), 0.0f);
        assertEquals(z, Maths.bound(z, c, z), 0.0f);
        assertEquals(m, Maths.bound(z, c, m), 0.0f);
        assertEquals(c, Maths.bound(z, c, c), 0.0f);
        assertEquals(c, Maths.bound(z, c, o), 0.0f);
        assertEquals(c, Maths.bound(c, z, o));
    }

    /**
     * Test of bound method, of class BigMaths.
     */
    @Test
    public void testBound_3args_6()
    {
        double z = 0;
        double c = 100;
        double n = -1;
        double m = 50;
        double o = 127;
        assertEquals(z, Maths.bound(z, c, n), 0.0);
        assertEquals(z, Maths.bound(z, c, z), 0.0);
        assertEquals(m, Maths.bound(z, c, m), 0.0);
        assertEquals(c, Maths.bound(z, c, c), 0.0);
        assertEquals(c, Maths.bound(z, c, o), 0.0);
        assertEquals(c, Maths.bound(c, z, o));
    }

        /**
     * Test of bound method, of class BigMaths.
     */
    @Test
    public void testBound_3args_7()
    {
        BigInteger z = BigInteger.ZERO;
        BigInteger c = BigInteger.valueOf(100);
        BigInteger n = BigInteger.valueOf(-1);
        BigInteger m = BigInteger.valueOf(50);
        BigInteger o = BigInteger.valueOf(127);
        assertEquals(z, BigMaths.bound(z, c, n));
        assertEquals(z, BigMaths.bound(z, c, z));
        assertEquals(m, BigMaths.bound(z, c, m));
        assertEquals(c, BigMaths.bound(z, c, c));
        assertEquals(c, BigMaths.bound(z, c, o));
        assertEquals(c, BigMaths.bound(c, z, o));
    }

    /**
     * Test of bound method, of class BigMaths.
     */
    @Test
    public void testBound_3args_8()
    {
        BigDecimal z = BigDecimal.ZERO;
        BigDecimal c = BigDecimal.valueOf(100);
        BigDecimal n = BigDecimal.valueOf(-1);
        BigDecimal m = BigDecimal.valueOf(50);
        BigDecimal o = BigDecimal.valueOf(127);
        assertEquals(z, BigMaths.bound(z, c, n));
        assertEquals(z, BigMaths.bound(z, c, z));
        assertEquals(m, BigMaths.bound(z, c, m));
        assertEquals(c, BigMaths.bound(z, c, c));
        assertEquals(c, BigMaths.bound(z, c, o));
        assertEquals(c, BigMaths.bound(c, z, o));
    }
        
    /**
     * Test of increment method, of class Utils.
     */
    @Test
    public void testIncrementByte()
    {
        byte[] b   = {0,0,0,0,0};
        byte[] b01 = {0,0,0,0,1};
        byte[] b02 = {0,0,0,0,2};
        byte[] b10 = {0,0,0,1,0};
        byte[] b11 = {0,0,0,1,1};
        byte[] b12 = {0,0,0,1,2};
        byte[] b20 = {0,0,0,2,0};
        byte[] b21 = {0,0,0,2,1};
        byte[] bc  = {0,0,0,0,0};
        
        byte module = 3;
        
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b01, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b02, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b10, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b11, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b12, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b20, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b21, b);
        assertFalse(Nums.inc(b, module));
        
        b = new byte[3];
        
        for(int i=0;i<0xfff;i++)
        {
            assertFalse(Nums.inc(b, (byte)16));
        }
        assertTrue(Nums.inc(b, (byte)16));
        byte[] b0 = new byte[3];
        assertArrayEquals(b0, b);
    }
    /**
     * Test of increment method, of class Utils.
     */
    @Test
    public void testIncrementShort()
    {
        short[] b   = {0,0,0,0,0};
        short[] b01 = {0,0,0,0,1};
        short[] b02 = {0,0,0,0,2};
        short[] b10 = {0,0,0,1,0};
        short[] b11 = {0,0,0,1,1};
        short[] b12 = {0,0,0,1,2};
        short[] b20 = {0,0,0,2,0};
        short[] b21 = {0,0,0,2,1};
        short[] bc  = {0,0,0,0,0};
        
        short module = 3;
        
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b01, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b02, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b10, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b11, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b12, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b20, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b21, b);
        assertFalse(Nums.inc(b, module));
        
        b = new short[3];
        
        for(int i=0;i<0xfff;i++)
        {
            assertFalse(Nums.inc(b, (short)16));
        }
        assertTrue(Nums.inc(b, (short)16));
        short[] b0 = new short[3];
        assertArrayEquals(b0, b);
    }
    /**
     * Test of increment method, of class Utils.
     */
    @Test
    public void testIncrementInt()
    {
        int[] b   = {0,0,0,0,0};
        int[] b01 = {0,0,0,0,1};
        int[] b02 = {0,0,0,0,2};
        int[] b10 = {0,0,0,1,0};
        int[] b11 = {0,0,0,1,1};
        int[] b12 = {0,0,0,1,2};
        int[] b20 = {0,0,0,2,0};
        int[] b21 = {0,0,0,2,1};
        int[] bc  = {0,0,0,0,0};
        
        byte module = 3;
        
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b01, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b02, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b10, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b11, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b12, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b20, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b21, b);
        assertFalse(Nums.inc(b, module));
        
        b = new int[3];
        
        for(int i=0;i<0xfff;i++)
        {
            assertFalse(Nums.inc(b, 16));
        }
        assertTrue(Nums.inc(b, 16));
        int[] b0 = new int[3];
        assertArrayEquals(b0, b);
    }
    /**
     * Test of increment method, of class Utils.
     */
    @Test
    public void testIncrementLong()
    {
        long[] b   = {0,0,0,0,0};
        long[] b01 = {0,0,0,0,1};
        long[] b02 = {0,0,0,0,2};
        long[] b10 = {0,0,0,1,0};
        long[] b11 = {0,0,0,1,1};
        long[] b12 = {0,0,0,1,2};
        long[] b20 = {0,0,0,2,0};
        long[] b21 = {0,0,0,2,1};
        long[] bc  = {0,0,0,0,0};
        
        long module = 3;
        
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b01, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b02, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b10, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b11, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b12, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b20, b);
        assertFalse(Nums.inc(b, module));
        assertArrayEquals(b21, b);
        assertFalse(Nums.inc(b, module));
        
        b = new long[3];
        
        for(int i=0;i<0xfff;i++)
        {
            assertFalse(Nums.inc(b, 16));
        }
        assertTrue(Nums.inc(b, 16));
        long[] b0 = new long[3];
        assertArrayEquals(b0, b);
    }
}
