/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BigRationalTest
{
    static final BigInteger BIG_INTEGER_TWO = BigInteger.valueOf(2);
    @Test
    public void testConstants()
    {
        assertEquals("0", BigRational.ZERO.toString(true));
        assertEquals("2", BigRational.TWO.toString(true));
        assertEquals("1", BigRational.ONE.toString(true));
        assertEquals("10", BigRational.TEN.toString(true));
        assertEquals(0, BigRational.ZERO.signum());
        assertEquals(1, BigRational.ONE.signum());
    }

    @Test
    public void testConstructors()
    {
        BigRational r1 = new BigRational(BigInteger.ONE, BIG_INTEGER_TWO);
        assertEquals(BigInteger.ONE, r1.numerator());
        assertEquals(BIG_INTEGER_TWO, r1.denominator());
        assertTrue(r1.simplify().denominator().signum() > 0);
    }

    @Test
    public void testNormalization()
    {
        // negative denominator
        BigRational r = new BigRational(BigInteger.ONE, BigInteger.valueOf(-2));
        assertEquals(BigInteger.ONE.negate(), r.numerator());
        assertEquals(BIG_INTEGER_TWO, r.denominator());
        
        r = new BigRational(BigInteger.valueOf(-1), BigInteger.valueOf(-2));
        assertEquals(BigInteger.ONE, r.numerator());
        assertEquals(BIG_INTEGER_TWO, r.denominator());
    }

    @Test
    public void testZeroNormalization()
    {
        BigRational r = new BigRational(BigInteger.ZERO, BigInteger.valueOf(5));
        assertEquals(BigInteger.ZERO, r.numerator());
        assertEquals(BigInteger.ONE, r.denominator());
    }

    @Test
    public void testDivisionByZero()
    {
        assertThrows(ArithmeticException.class, () -> new BigRational(BigInteger.ONE, BigInteger.ZERO));
        assertThrows(ArithmeticException.class, () -> BigRational.ZERO.div(0));
        assertThrows(ArithmeticException.class, () -> BigRational.ONE.div(BigInteger.ZERO));
        BigRational r = new BigRational(BigInteger.ONE, BIG_INTEGER_TWO);
        assertThrows(ArithmeticException.class, () -> r.div(BigRational.ZERO));
    }

    @Test
    public void testReduction()
    {
        BigRational r = new BigRational(BigInteger.valueOf(4), BIG_INTEGER_TWO);
        assertEquals(BIG_INTEGER_TWO, r.numerator());
        assertEquals(BigInteger.ONE, r.denominator());
        assertTrue(r.simplified);
        
        r = new BigRational(BIG_INTEGER_TWO, BigInteger.valueOf(4));
        assertEquals(BigInteger.ONE, r.numerator());
        assertEquals(BIG_INTEGER_TWO, r.denominator());
    }

    @Test
    public void testValueOf_long()
    {
        assertEquals(BigRational.ZERO, BigRational.valueOf(0L));
        assertEquals(BigRational.ONE, BigRational.valueOf(1L));
        assertEquals(BigRational.TEN, BigRational.valueOf(10L));
        BigRational r = BigRational.valueOf(-5L);
        assertEquals(BigInteger.valueOf(-5), r.numerator());
        assertEquals(BigInteger.ONE, r.denominator());
    }

    @Test
    public void testValueOf_BigInteger()
    {
        assertEquals(BigRational.ZERO, BigRational.valueOf(BigInteger.ZERO));
        assertEquals(BigRational.ONE, BigRational.valueOf(BigInteger.ONE));
        assertEquals(BigRational.TEN, BigRational.valueOf(BigInteger.TEN));
    }

    @Test
    public void testValueOf_BigDecimal()
    {
        assertEquals(BigRational.ZERO, BigRational.valueOf(BigDecimal.ZERO));
        assertEquals(BigRational.ONE, BigRational.valueOf(BigDecimal.ONE));
        assertEquals(BigRational.valueOf(2), BigRational.valueOf(BigDecimal.valueOf(2.0)));
        BigRational r = BigRational.valueOf(BigDecimal.valueOf(0.25));
        assertEquals(BigInteger.ONE, r.numerator());
        assertEquals(BigInteger.valueOf(4), r.denominator());
    }

    @Test
    public void testAdd()
    {
        BigRational v = BigRational.valueOf(5);
        assertEquals(BigRational.TEN, v.add(5L));
        assertEquals(BigRational.TEN, v.add(BigInteger.valueOf(5)));
        assertEquals(BigRational.TEN, v.add(v));
        
        // different denominators
        BigRational a = new BigRational(BigInteger.ONE, BIG_INTEGER_TWO);
        BigRational b = new BigRational(BigInteger.ONE, BigInteger.valueOf(4));
        BigRational s = a.add(b);
        assertEquals(BigInteger.valueOf(3), s.numerator());
        assertEquals(BigInteger.valueOf(4), s.denominator());
    }

    @Test
    public void testSub()
    {
        BigRational v = BigRational.valueOf(5);
        assertEquals(BigRational.ZERO, v.sub(5L));
        assertEquals(BigRational.ZERO, v.sub(BigInteger.valueOf(5)));
        assertEquals(BigRational.ZERO, v.sub(v));
    }

    @Test
    public void testMul()
    {
        BigRational v = BigRational.valueOf(5);
        assertEquals(BigRational.valueOf(25), v.mul(5));
        assertEquals(BigRational.valueOf(25), v.mul(BigInteger.valueOf(5)));
        assertEquals(BigRational.valueOf(25), v.mul(v));
        
        BigRational a = new BigRational(BigInteger.ONE, BIG_INTEGER_TWO);
        BigRational m = a.mul(a);
        assertEquals(BigInteger.ONE, m.numerator());
        assertEquals(BigInteger.valueOf(4), m.denominator());
    }

    @Test
    public void testDiv()
    {
        BigRational v = BigRational.valueOf(5);
        assertEquals(BigRational.ONE, v.div(5).simplify());
        assertEquals(BigRational.ONE, v.div(BigInteger.valueOf(5)).simplify());
        assertEquals(BigRational.ONE, v.div(v).simplify());
        
        BigRational a = new BigRational(BigInteger.ONE, BIG_INTEGER_TWO);
        BigRational b = new BigRational(BigInteger.ONE, BigInteger.valueOf(4));
        BigRational d = a.div(b);
        assertEquals(BIG_INTEGER_TWO, d.numerator());
        assertEquals(BigInteger.ONE, d.denominator());
    }

    @Test
    public void testNegateAbsSignum()
    {
        BigRational v = BigRational.ZERO;
        assertEquals(BigRational.ZERO, v.negate());
        assertTrue(v.isZero());
        assertFalse(v.isPositive());
        assertFalse(v.isNegative());
        
        v = BigRational.valueOf(-1);
        assertEquals(BigRational.ONE, v.negate());
        assertEquals(BigInteger.ONE.negate(), v.numerator()); // original unchanged? new instance
        assertTrue(v.isNegative());
        assertFalse(v.isPositive());
        
        v = BigRational.valueOf(1);
        assertEquals(BigRational.ONE.negate(), v.negate());
        assertEquals(BigInteger.ONE, v.numerator());
        assertTrue(v.isPositive());
        assertEquals(1, v.signum());
        assertEquals(BigRational.ONE, v.abs());
    }

    @Test
    public void testEqualsHashCode()
    {
        BigRational a = new BigRational(BigInteger.ONE, BIG_INTEGER_TWO);
        BigRational b = new BigRational(BIG_INTEGER_TWO, BigInteger.valueOf(4));
        BigRational c = new BigRational(BigInteger.ONE, BigInteger.valueOf(3));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertEquals(BigRational.ZERO, new BigRational(BigInteger.ZERO, BigInteger.valueOf(123)));
    }

    @Test
    public void testCompareTo()
    {
        BigRational a = BigRational.valueOf(1);
        BigRational b = BigRational.valueOf(2);
        BigRational c = BigRational.valueOf(3);
        BigRational cc = BigRational.valueOf(3);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(c) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(c.compareTo(b) > 0);
        assertEquals(0, c.compareTo(cc));
        
        // fractions
        assertTrue(new BigRational(BigInteger.ONE, BigInteger.valueOf(3)).compareTo(new BigRational(BigInteger.ONE, BIG_INTEGER_TWO)) < 0);
        assertTrue(new BigRational(BigInteger.ONE, BIG_INTEGER_TWO).compareTo(new BigRational(BigInteger.ONE, BigInteger.valueOf(3))) > 0);
    }

    @Test
    public void testNumberValues()
    {
        BigRational br = BigRational.TEN;
        assertEquals(10, br.intValue());
        assertEquals(10L, br.longValue());
        assertEquals(10f, br.floatValue(), 0.0f);
        assertEquals(10d, br.doubleValue(), 0.0d);
        
        BigRational half = new BigRational(BigInteger.ONE, BIG_INTEGER_TWO);
        assertEquals(0, half.intValue()); // trunc toward zero
        assertEquals(0L, half.longValue());
    }

    @Test
    public void testToString()
    {
        assertEquals("0/1", BigRational.ZERO.toString());
        assertEquals("1/1", BigRational.ONE.toString());
        assertEquals("10/1", BigRational.TEN.toString());
        assertEquals("1/2", new BigRational(BigInteger.ONE, BIG_INTEGER_TWO).toString());
        assertEquals("0", BigRational.ZERO.toString(true));
        assertEquals("1", BigRational.ONE.toString(true));
        assertEquals("1/2", new BigRational(BigInteger.ONE, BIG_INTEGER_TWO).toString(true));
    }

    @Test
    public void testBuild()
    {
        assertEquals(BigRational.ZERO, BigRational.build(BigDecimal.ZERO, 12));
        assertEquals(BigRational.ONE, BigRational.build(BigDecimal.ONE, 12));
        assertEquals(BigRational.valueOf(2), BigRational.build(BigDecimal.valueOf(2), 12));
        
        BigRational br = BigRational.build(BigDecimal.valueOf(0.25), 12);
        assertEquals(BigInteger.ONE, br.numerator());
        assertEquals(BigInteger.valueOf(4), br.denominator());
        
        br = BigRational.build(BigDecimal.valueOf(0.666666666667), 12);
        assertEquals(BIG_INTEGER_TWO, br.numerator());
        assertEquals(BigInteger.valueOf(3), br.denominator());
        
        MathContext mc = new MathContext(12);
        br = BigRational.build(BigDecimal.valueOf(65521).divide(BigDecimal.valueOf(64513), mc), 12);
        assertEquals(BigInteger.valueOf(65521), br.numerator());
        assertEquals(BigInteger.valueOf(64513), br.denominator());
    }

    @Test
    public void testBuildDouble()
    {
        assertEquals("1", BigRational.build(1.0001, 6, 0.0005).toString(true));
        assertEquals("1/3", BigRational.build(0.3333, 6, 0.0001).toString(true));
        assertEquals("2/3", BigRational.build(0.6666, 6, 0.0001).toString(true));
        assertEquals("1/5", BigRational.build(0.2, 3).toString(true));
        assertEquals("2/5", BigRational.build(0.4, 3).toString(true));
        assertEquals("3/5", BigRational.build(0.6, 3).toString(true));
        assertEquals("4/5", BigRational.build(0.8, 3).toString(true));
    }

    @Test
    public void testBigDecimalConversion()
    {
        BigRational br = BigRational.valueOf(1).div(64);
        BigDecimal bd = br.BigDecimalValue(new MathContext(12));
        assertEquals(1.0 / 64.0, bd.doubleValue(), 0.000001);
        assertNotNull(br.toBigDecimal(new MathContext(6)));
        assertNotNull(br.toBigDecimal(6, RoundingMode.HALF_UP));
    }
}
