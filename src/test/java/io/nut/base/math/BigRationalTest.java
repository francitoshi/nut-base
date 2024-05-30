/*
 *  BigRationalTest.java
 *
 *  Copyright (c) 2012-2024 francitoshi@gmail.com
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BigRationalTest
{
    /**
     * Test of add method, of class BigRational.
     */
    @Test
    public void testAdd_long()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.add(5L);
        assertEquals(value.n, BigInteger.TEN);
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of add method, of class BigRational.
     */
    @Test
    public void testAdd_BigInteger()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.add(BigInteger.valueOf(5));
        assertEquals(value.n, BigInteger.TEN);
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of add method, of class BigRational.
     */
    @Test
    public void testAdd_BigRational()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.add(value);
        assertEquals(value.n, BigInteger.TEN);
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of sub method, of class BigRational.
     */
    @Test
    public void testSub_long()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.sub(5L);
        assertEquals(value.n, BigInteger.ZERO);
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of sub method, of class BigRational.
     */
    @Test
    public void testSub_BigInteger()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.sub(BigInteger.valueOf(5));
        assertEquals(value.n, BigInteger.ZERO);
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of sub method, of class BigRational.
     */
    @Test
    public void testSub_BigRational()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.sub(value);
        assertEquals(value.n, BigInteger.ZERO);
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of mul method, of class BigRational.
     */
    @Test
    public void testMul_long()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.mul(5);
        assertEquals(value.n, BigInteger.valueOf(25));
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of mul method, of class BigRational.
     */
    @Test
    public void testMul_BigInteger()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.mul(BigInteger.valueOf(5));
        assertEquals(value.n, BigInteger.valueOf(25));
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of mul method, of class BigRational.
     */
    @Test
    public void testMul_BigRational()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.mul(value);
        assertEquals(value.n, BigInteger.valueOf(25));
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of div method, of class BigRational.
     */
    @Test
    public void testDiv_long()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.div(5).simplify();
        assertEquals(value.n, BigInteger.ONE);
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of div method, of class BigRational.
     */
    @Test
    public void testDiv_BigInteger()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.div(BigInteger.valueOf(5)).simplify();
        assertEquals(value.n, BigInteger.ONE);
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of div method, of class BigRational.
     */
    @Test
    public void testDiv_BigRational()
    {
        BigRational value = BigRational.valueOf(5);
        value = value.div(value).simplify();
        assertEquals(value.n, BigInteger.ONE);
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of negate method, of class BigRational.
     */
    @Test
    public void testNegate()
    {
        BigRational value= BigRational.valueOf(0);
        value = value.negate();
        assertEquals(value.n, BigInteger.ZERO);
        assertEquals(value.d, BigInteger.ONE);
        
        value = BigRational.valueOf(-1);
        value = value.negate();
        assertEquals(value.n, BigInteger.ONE);
        assertEquals(value.d, BigInteger.ONE);
    }

    /**
     * Test of intValue method, of class BigRational.
     */
    @Test
    public void testIntValue()
    {
        BigRational br = BigRational.TEN;
        int result = br.intValue();
        assertEquals(10, result);
    }

    /**
     * Test of longValue method, of class BigRational.
     */
    @Test
    public void testLongValue()
    {
        BigRational br = BigRational.TEN;
        long result = br.longValue();
        assertEquals(10, result);
    }

    /**
     * Test of floatValue method, of class BigRational.
     */
    @Test
    public void testFloatValue()
    {
        BigRational br = BigRational.TEN;
        float result = br.floatValue();
        assertEquals(10f, result, 0.0f);
    }

    /**
     * Test of doubleValue method, of class BigRational.
     */
    @Test
    public void testDoubleValue()
    {
        BigRational br = BigRational.TEN;
        double result = br.doubleValue();
        assertEquals(10d, result, 0.0d);
    }

    /**
     * Test of compareTo method, of class BigRational.
     */
    @Test
    public void testCompareTo()
    {
        BigRational a = BigRational.valueOf(1);
        BigRational b = BigRational.valueOf(2);
        BigRational c = BigRational.valueOf(3);
        BigRational cc = BigRational.valueOf(3);
    
        assertTrue(a.compareTo(b)<0);
        assertTrue(b.compareTo(c)<0);
        assertTrue(b.compareTo(a)>0);
        assertTrue(c.compareTo(b)>0);
        assertTrue(c.compareTo(cc)==0);
    }

    /**
     * Test of valueOf method, of class BigRational.
     */
    @Test
    public void testValueOf_long()
    {
        long value0 = 0L;
        BigRational r0 = BigRational.valueOf(value0);
        assertEquals(r0.n, BigInteger.ZERO);
        assertEquals(r0.d, BigInteger.ONE);

        long value1 = 1L;
        BigRational r1 = BigRational.valueOf(value1);
        assertEquals(r1.n, BigInteger.ONE);
        assertEquals(r1.d, BigInteger.ONE);

        long value10 = 10L;
        BigRational r10 = BigRational.valueOf(value10);
        assertEquals(r10.n, BigInteger.TEN);
        assertEquals(r10.d, BigInteger.ONE);
    }

    /**
     * Test of valueOf method, of class BigRational.
     */
    @Test
    public void testValueOf_BigInteger()
    {
        BigInteger value0 = BigInteger.ZERO;
        BigRational r0 = BigRational.valueOf(value0);
        assertEquals(r0.n, BigInteger.ZERO);
        assertEquals(r0.d, BigInteger.ONE);

        BigInteger value1 = BigInteger.ONE;
        BigRational r1 = BigRational.valueOf(value1);
        assertEquals(r1.n, BigInteger.ONE);
        assertEquals(r1.d, BigInteger.ONE);

        BigInteger value10 = BigInteger.TEN;
        BigRational r10 = BigRational.valueOf(value10);
        assertEquals(r10.n, BigInteger.TEN);
        assertEquals(r10.d, BigInteger.ONE);
        BigDecimal a;
    }

    /**
     * Test of simplify method, of class BigRational.
     */
    @Test
    public void testSimplify()
    {
        BigRational one = BigRational.valueOf(10).div(10).simplify();
        assertEquals(one.n, BigInteger.ONE);
        assertEquals(one.d, BigInteger.ONE);

        BigRational two = BigRational.valueOf(10).div(5).simplify();
        assertEquals(two.n, BigInteger.valueOf(2));
        assertEquals(two.d, BigInteger.ONE);

        BigRational ten = BigRational.valueOf(100).div(10).simplify();
        assertEquals(ten.n, BigInteger.TEN);
        assertEquals(ten.d, BigInteger.ONE);

        BigRational ten_nine = BigRational.valueOf(10).div(9).simplify();
        assertEquals(ten_nine.n, BigInteger.TEN);
        assertEquals(ten_nine.d, BigInteger.valueOf(9));
    }

    /**
     * Test of toString method, of class BigRational.
     */
    @Test
    public void testToString_boolean()
    {
        assertEquals("0/1", BigRational.ZERO.toString(false));
        assertEquals("1/1", BigRational.ONE.toString(false));
        assertEquals("10/1", BigRational.TEN.toString(false));
        assertEquals("2/2", BigRational.valueOf(2).div(2).toString(false));

        assertEquals("0", BigRational.ZERO.toString(true));
        assertEquals("1", BigRational.ONE.toString(true));
        assertEquals("10", BigRational.TEN.toString(true));
        assertEquals("2/2", BigRational.valueOf(2).div(2).toString(true));
        assertEquals("4/2", BigRational.valueOf(4).div(2).toString(true));

    }

    /**
     * Test of toString method, of class BigRational.
     */
    @Test
    public void testToString_0args()
    {
        assertEquals("0/1", BigRational.ZERO.toString());
        assertEquals("1/1", BigRational.ONE.toString());
        assertEquals("10/1", BigRational.TEN.toString());
        assertEquals("2/2", BigRational.valueOf(2).div(2).toString());
    }

    /**
     * Test of BigDecimalValue method, of class BigRational.
     */
    @Test
    public void testBigDecimalValue()
    {
        BigRational br = BigRational.valueOf(1).div(64);
        BigDecimal bd = br.BigDecimalValue(new MathContext(12));
        assertEquals(1.0/64.0, bd.doubleValue(),0.000001);
    }

    /**
     * Test of build method, of class BigRational.
     */
    @Test
    public void testBuild()
    {
        assertEquals(BigRational.ZERO, BigRational.build(BigDecimal.ZERO, 12));
        assertEquals(BigRational.ONE, BigRational.build(BigDecimal.ONE, 12));
        assertEquals(BigRational.valueOf(2), BigRational.build(BigDecimal.valueOf(2), 12));
        
        BigRational br;
        
        br= BigRational.build(BigDecimal.valueOf(0.25), 12);
        assertEquals(BigInteger.ONE, br.n );
        assertEquals(BigInteger.valueOf(4), br.d);
        
        br = BigRational.build(BigDecimal.valueOf(0.666666666667), 12);
        assertEquals(BigInteger.valueOf(2), br.n);
        assertEquals(BigInteger.valueOf(3), br.d);

        MathContext mc = new MathContext(12);
        br = BigRational.build(BigDecimal.valueOf(65521).divide(BigDecimal.valueOf(64513),mc),12);
        
        assertEquals(BigInteger.valueOf(65521), br.n);
        assertEquals(BigInteger.valueOf(64513), br.d);
    }

    /**
     * Test of build method, of class BigRational.
     */
    @Test
    public void testBuild_double_int()
    {
        assertEquals("1", BigRational.build(1.0001, 6, 0.0005).toString(true));
        assertEquals("1/3", BigRational.build(0.3333, 6, 0.0001).toString(true));
        assertEquals("2/3", BigRational.build(0.6666, 6, 0.0001).toString(true));
        assertEquals("1/5", BigRational.build(0.2, 3).toString(true));
        assertEquals("2/5", BigRational.build(0.4, 3).toString(true));
        assertEquals("3/5", BigRational.build(0.6, 3).toString(true));
        assertEquals("4/5", BigRational.build(0.8, 3).toString(true));
    }

}
