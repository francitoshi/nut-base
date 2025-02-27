/*
 *  NumsTest.java
 *
 *  Copyright (C) 2024 francitoshi@gmail.com
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

import ch.obermuhlner.math.big.BigDecimalMath;
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
import io.nut.base.util.Utils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.NoSuchPaddingException;
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
    
    public NumsTest()
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
     * Test of buildE method, of class Nums.
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
    
     /**
     * Test of sum method, of class Nums.
     */
    @Test
    public void testSum_intArr()
    {
        double delta  = 0.000001;
        int[] values0 = {0,0,0,0,0};
        int[] values1 = {1,1,2,3,5};
        int[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Nums.sum(values0), delta);
        assertEquals(12, Nums.sum(values1), delta);
        assertEquals(131, Nums.sum(values2), delta);
    }

    /**
     * Test of sum method, of class Nums.
     */
    @Test
    public void testSum_longArr()
    {
        double delta  = 0.000001;
        long[] values0 = {0,0,0,0,0};
        long[] values1 = {1,1,2,3,5};
        long[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Nums.sum(values0), delta);
        assertEquals(12, Nums.sum(values1), delta);
        assertEquals(131, Nums.sum(values2), delta);
    }

    /**
     * Test of sum method, of class Nums.
     */
    @Test
    public void testSum_floatArr()
    {
        double delta  = 0.000001;
        float[] values0 = {0,0,0,0,0};
        float[] values1 = {1,1,2,3,5};
        float[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Nums.sum(values0), delta);
        assertEquals(12, Nums.sum(values1), delta);
        assertEquals(131, Nums.sum(values2), delta);
    }

    /**
     * Test of sum method, of class Nums.
     */
    @Test
    public void testSum_doubleArr()
    {
        double delta  = 0.000001;
        double[] values0 = {0,0,0,0,0};
        double[] values1 = {1,1,2,3,5};
        double[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Nums.sum(values0), delta);
        assertEquals(12, Nums.sum(values1), delta);
        assertEquals(131, Nums.sum(values2), delta);
    }

    /**
     * Test of avg method, of class Nums.
     */
    @Test
    public void testAvg_intArr()
    {
        double delta  = 0.000001;
        int[] values0 = {0,0,0,0,0};
        int[] values1 = {1,1,2,3,5};
        int[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Nums.avg(values0), delta);
        assertEquals(2.4, Nums.avg(values1), delta);
        assertEquals(26.2, Nums.avg(values2), delta);
    }

    /**
     * Test of avg method, of class Nums.
     */
    @Test
    public void testAvg_longArr()
    {
        double delta  = 0.000001;
        long[] values0 = {0,0,0,0,0};
        long[] values1 = {1,1,2,3,5};
        long[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Nums.avg(values0), delta);
        assertEquals(2.4, Nums.avg(values1), delta);
        assertEquals(26.2, Nums.avg(values2), delta);
    }

    /**
     * Test of avg method, of class Nums.
     */
    @Test
    public void testAvg_floatArr()
    {
        double delta  = 0.000001;
        float[] values0 = {0,0,0,0,0};
        float[] values1 = {1,1,2,3,5};
        float[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Nums.avg(values0), delta);
        assertEquals(2.4, Nums.avg(values1), delta);
        assertEquals(26.2, Nums.avg(values2), delta);
    }

    /**
     * Test of avg method, of class Nums.
     */
    @Test
    public void testAvg_doubleArr()
    {
        double delta  = 0.000001;
        double[] values0 = {0,0,0,0,0};
        double[] values1 = {1,1,2,3,5};
        double[] values2 = {8,13,21,34,55};
        
        assertEquals(0, Nums.avg(values0), delta);
        assertEquals(2.4, Nums.avg(values1), delta);
        assertEquals(26.2, Nums.avg(values2), delta);
    }


    /**
     * Test of sqrt method, of class Nums.
     */
    @Test
    public void testSqrt()
    {
        assertEquals(0, Nums.sqrt(BigDecimal.ONE).compareTo(BigDecimal.ONE));
        assertEquals(0, Nums.sqrt(BigDecimal.ZERO).compareTo(BigDecimal.ZERO));

        for(int i=1;i<1000;i++)
        {
            BigDecimal x = BigDecimal.valueOf(i*0.123456789);
            BigDecimal xx = x.multiply(x);
            assertEquals(0, Nums.sqrt(xx).compareTo(x));
        }
    }
    
    /**
     * Test of equals method, of class Nums.
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
        
        assertTrue(Nums.equalsEnough(d0, d0, delta0));
        assertTrue(Nums.equalsEnough(d0, d0, delta1));
        assertTrue(Nums.equalsEnough(d0, d0, delta2));

        assertFalse(Nums.equalsEnough(d1, d2, delta0));
        assertFalse(Nums.equalsEnough(d0, d2, delta1));
        assertTrue(Nums.equalsEnough(d0, d2, delta2));
        
        assertFalse(Nums.equalsEnough(d2, d1, delta0));
        assertFalse(Nums.equalsEnough(d2, d0, delta1));
        assertTrue(Nums.equalsEnough(d2, d0, delta2));
                
    }

    /**
     * Test of equals method, of class Nums.
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
        
        assertTrue(Nums.equalsEnough(d0, d0, delta0));
        assertTrue(Nums.equalsEnough(d0, d0, delta1));
        assertTrue(Nums.equalsEnough(d0, d0, delta2));

        assertFalse(Nums.equalsEnough(d1, d2, delta0));
        assertFalse(Nums.equalsEnough(d0, d2, delta1));
        assertTrue(Nums.equalsEnough(d0, d2, delta2));
        
        assertFalse(Nums.equalsEnough(d2, d1, delta0));
        assertFalse(Nums.equalsEnough(d2, d0, delta1));
        assertTrue(Nums.equalsEnough(d2, d0, delta2));
    }    
    
    /**
     * Test of equalsEnough method, of class Utils.
     */
    @Test
    public void testEqualsEnough()
    {
        assertTrue(Nums.equalsEnough(0, 0, 0));
        assertTrue(Nums.equalsEnough(0.4, 0.6, 0.5));
        assertTrue(Nums.equalsEnough(1.9, 2.1, 0.5));
        assertTrue(Nums.equalsEnough(0.000001, 0.000002, 0.000002));

        assertFalse(Nums.equalsEnough(0, 1, 0));
        assertFalse(Nums.equalsEnough(0.4, 0.6, 0.1));
        assertFalse(Nums.equalsEnough(1.9, 2.1, 0.1));
        assertFalse(Nums.equalsEnough(0.000001, 0.000003, 0.000001));
    }
    /**
     * Test of orderOfMagnitude method, of class Utils.
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
     * Test of orderOfMagnitude method, of class Utils.
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
     * Test of log2 method, of class FastMath.
     */
    @Test
    public void testLog2_long()
    {
        assertEquals(0, Nums.log2(1));
        assertEquals(1, Nums.log2(2));
        assertEquals(2, Nums.log2(4));
        assertEquals(3, Nums.log2(8));
        assertEquals(4, Nums.log2(16));
        assertEquals(5, Nums.log2(32));
        assertEquals(6, Nums.log2(64));
        assertEquals(7, Nums.log2(128));

        assertEquals(1, Nums.log2(3));
        assertEquals(2, Nums.log2(7));
        assertEquals(3, Nums.log2(15));
        assertEquals(4, Nums.log2(31));
        assertEquals(5, Nums.log2(63));
        assertEquals(6, Nums.log2(127));
    }

    /**
     * Test of log2 method, of class FastMath.
     */
    @Test
    public void testLog2_long_boolean()
    {
        assertEquals(0, Nums.log2(1,true));
        assertEquals(1, Nums.log2(2,true));
        assertEquals(2, Nums.log2(4,true));
        assertEquals(3, Nums.log2(8,true));
        assertEquals(4, Nums.log2(16,true));
        assertEquals(5, Nums.log2(32,true));
        assertEquals(6, Nums.log2(64,true));
        assertEquals(7, Nums.log2(128,true));

        assertEquals(2, Nums.log2(3,true));
        assertEquals(3, Nums.log2(7,true));
        assertEquals(4, Nums.log2(15,true));
        assertEquals(5, Nums.log2(31,true));
        assertEquals(6, Nums.log2(63,true));
        assertEquals(7, Nums.log2(127,true));
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
     * Test of log method, of class FastMath.
     */
    @Test
    public void testLog()
    {
        for(int i=0;i<1500;i++)
        {
            assertEquals(Math.log(i%300), Nums.log(i%300), 0.0);
        }
    }
    static final int[] FIBONACCI = {0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, 17711, 28657, 46368, 75025, 121393, 196418, 317811, 514229, 832040, 1346269, 2178309, 3524578, 5702887, 9227465, 14930352, 24157817, 39088169};
    static final int[] FIBO13_21 = {13, 21, 34, 55, 89, 144, 233, 377, 610, 987, 1597, 2584, 4181, 6765, 10946, 17711, 28657, 46368, 75025, 121393, 196418, 317811, 514229, 832040, 1346269, 2178309, 3524578, 5702887, 9227465, 14930352, 24157817, 39088169};

    static final BigInteger[] FIBONACCI_BIG = Utils.asBigIntegers(FIBONACCI);
    static final BigInteger[] FIBO13_21_BIG = Utils.asBigIntegers(FIBO13_21);
    
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
     * Test of maxDouble method, of class FastMath.
     */
    @Test
    public void testMaxDouble()
    {
        assertEquals(2d, Nums.maxDouble(1d, 2d), 0.0d);
        assertEquals(3d, Nums.maxDouble(1d, 2d, 3d), 0.0d);
        assertEquals(3d, Nums.maxDouble(3d, 2d, 1d), 0.0d);
        assertEquals(9d, Nums.maxDouble(9d, 8d, 7d, 6d, 5d), 0.0d);
    }

    /**
     * Test of maxFloat method, of class FastMath.
     */
    @Test
    public void testMaxFloat()
    {
        assertEquals(2f, Nums.maxFloat(1f, 2f), 0.0f);
        assertEquals(3f, Nums.maxFloat(1f, 2f, 3f), 0.0f);
        assertEquals(3f, Nums.maxFloat(3f, 2f, 1f), 0.0f);
        assertEquals(9f, Nums.maxFloat(9f, 8f, 7f, 6f, 5f), 0.0f);
    }

    /**
     * Test of maxInteger method, of class FastMath.
     */
    @Test
    public void testMaxInteger()
    {
        assertEquals(2, Nums.maxInteger(1, 2));
        assertEquals(3, Nums.maxInteger(1, 2, 3));
        assertEquals(3, Nums.maxInteger(3, 2, 1));
        assertEquals(9, Nums.maxInteger(9, 8, 7, 6, 5));
    }

    /**
     * Test of maxLong method, of class FastMath.
     */
    @Test
    public void testMaxLong()
    {
        assertEquals(2L, Nums.maxLong(1L, 2L));
        assertEquals(3L, Nums.maxLong(1L, 2L, 3L));
        assertEquals(3L, Nums.maxLong(3L, 2L, 1L));
        assertEquals(9L, Nums.maxLong(9L, 8L, 7L, 6L, 5L));
    }

    /**
     * Test of minDouble method, of class FastMath.
     */
    @Test
    public void testMinDouble()
    {
        assertEquals(1d, Nums.minDouble(1d, 2d), 0.0d);
        assertEquals(1d, Nums.minDouble(1d, 2d, 3d), 0.0d);
        assertEquals(1d, Nums.minDouble(3d, 2d, 1d), 0.0d);
        assertEquals(5d, Nums.minDouble(9d, 8d, 7d, 6d, 5d), 0.0d);
    }

    /**
     * Test of minFloat method, of class FastMath.
     */
    @Test
    public void testMinFloat()
    {
        assertEquals(1f, Nums.minFloat(1f, 2f), 0.0f);
        assertEquals(1f, Nums.minFloat(1f, 2f, 3f), 0.0f);
        assertEquals(1f, Nums.minFloat(3f, 2f, 1f), 0.0f);
        assertEquals(5f, Nums.minFloat(9f, 8f, 7f, 6f, 5f), 0.0f);
    }

    /**
     * Test of minInt method, of class FastMath.
     */
    @Test
    public void testMinInteger()
    {
        assertEquals(1, Nums.minInteger(1, 2));
        assertEquals(1, Nums.minInteger(1, 2, 3));
        assertEquals(1, Nums.minInteger(3, 2, 1));
        assertEquals(5, Nums.minInteger(9, 8, 7, 6, 5));
    }

    /**
     * Test of minLong method, of class FastMath.
     */
    @Test
    public void testMinLong()
    {
        assertEquals(1L, Nums.minLong(1L, 2L));
        assertEquals(1L, Nums.minLong(1L, 2L, 3L));
        assertEquals(1L, Nums.minLong(3L, 2L, 1L));
        assertEquals(5L, Nums.minLong(9L, 8L, 7L, 6L, 5L));
    }
    
    /**
     * Test of isPow2 method, of class FastMath.
     */
    @Test
    public void testIsPow2_int()
    {
        assertFalse(Nums.isPow2(-1));
        assertFalse(Nums.isPow2(0));
        assertFalse(Nums.isPow2(3));
        assertFalse(Nums.isPow2(5));
        assertFalse(Nums.isPow2(7));
        assertFalse(Nums.isPow2(9));
        assertFalse(Nums.isPow2(15));
        assertFalse(Nums.isPow2(17));
        assertFalse(Nums.isPow2(31));
        assertFalse(Nums.isPow2(33));
        assertFalse(Nums.isPow2(63));
        assertFalse(Nums.isPow2(65));
        assertFalse(Nums.isPow2(127));
        assertFalse(Nums.isPow2(129));
        
        assertTrue(Nums.isPow2(1));
        assertTrue(Nums.isPow2(2));
        assertTrue(Nums.isPow2(4));
        assertTrue(Nums.isPow2(8));
        assertTrue(Nums.isPow2(16));
        assertTrue(Nums.isPow2(32));
        assertTrue(Nums.isPow2(64));
        assertTrue(Nums.isPow2(128));
        
        assertFalse(Nums.isPow2(Byte.MAX_VALUE));
        assertFalse(Nums.isPow2(Short.MAX_VALUE));
        assertFalse(Nums.isPow2(Character.MAX_VALUE));
        assertFalse(Nums.isPow2(Integer.MAX_VALUE));

        assertTrue(Nums.isPow2(Byte.MAX_VALUE+1));
        assertTrue(Nums.isPow2(Short.MAX_VALUE+1));
        assertTrue(Nums.isPow2(Character.MAX_VALUE+1));

        assertFalse(Nums.isPow2(-Byte.MAX_VALUE));
        assertFalse(Nums.isPow2(-Short.MAX_VALUE));
        assertFalse(Nums.isPow2(-Character.MAX_VALUE));
        assertFalse(Nums.isPow2(-Integer.MAX_VALUE));

        assertFalse(Nums.isPow2(-Byte.MAX_VALUE-1));
        assertFalse(Nums.isPow2(-Short.MAX_VALUE-1));
        assertFalse(Nums.isPow2(-Character.MAX_VALUE-1));
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

    /**
     * Test of sumarize method, of class FastMath.
     */
    @Test
    public void testSum()
    {
        assertEquals(0, Nums.sum());
        assertEquals(0, Nums.sum(0));
        assertEquals(1, Nums.sum(0,1));
        assertEquals(3, Nums.sum(0,1,2));
        assertEquals(6, Nums.sum(0,1,2,3));
        assertEquals(11, Nums.sum(0,1,2,3,5));
    }

    /**
     * Test of mul method, of class FastMath.
     */
    @Test
    public void testMul()
    {
        assertEquals(1, Nums.mul());
        assertEquals(0, Nums.mul(0));
        assertEquals(1, Nums.mul(1));
        assertEquals(2, Nums.mul(1,2));
        assertEquals(6, Nums.mul(1,2,3));
        assertEquals(30, Nums.mul(1,2,3,5));
    }

    /**
     * Test of log method, of class FastMath.
     */
    @Test
    public void testLog_double_double()
    {
        for(int i=1;i<1_000_000;i++)
        {
             double a = Math.log(i);
             double b = Nums.log(2.718281828459045235360, i);

             double c = Math.log10(i);
             double d = Nums.log(10, i);
             
             assertEquals(a, b, 0.0000000000001);
             assertEquals(c, d, 0.0000000000001);
        }
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

    /**
     * Test of equals method, of class Nums.
     */
    @Test
    public void testEquals()
    {
        BigDecimal d1 = BigDecimal.valueOf(0.001);
        BigDecimal d2 = BigDecimal.valueOf(0.009);
        BigDecimal delta = BigDecimal.valueOf(0.01);

        assertTrue(Nums.equalsEnough(d1, d2, delta));
    }

    /**
     * Test of firstNonNullOrZero method, of class Nums.
     */
    @Test
    public void testFirstNonNullOrZero()
    {
        assertEquals(BigDecimal.ONE, Nums.firstNonNullOrZero(null, BigDecimal.ZERO,BigDecimal.ONE, BigDecimal.TEN));
        assertEquals(BigDecimal.TEN, Nums.firstNonNullOrZero(null, BigDecimal.ZERO,BigDecimal.TEN, BigDecimal.ONE));
        
        assertNull(Nums.firstNonNullOrZero());
        assertNull(Nums.firstNonNullOrZero((BigDecimal) null));
        assertNull(Nums.firstNonNullOrZero(null, null));
        
        assertEquals(BigDecimal.ZERO,Nums.firstNonNullOrZero(null, BigDecimal.ZERO));
        assertEquals(BigDecimal.ZERO,Nums.firstNonNullOrZero(BigDecimal.ZERO, null));
        assertEquals(BigDecimal.ONE, Nums.firstNonNullOrZero(null, BigDecimal.ZERO, BigDecimal.ONE));
        assertEquals(BigDecimal.ONE, Nums.firstNonNullOrZero(BigDecimal.ONE, BigDecimal.ZERO, null));
        assertEquals(BigDecimal.ONE, Nums.firstNonNullOrZero(BigDecimal.ONE, null, BigDecimal.ZERO));
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
     * Test of gcd method, of class Nums.
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
     * Test of gcd method, of class Nums.
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
     * Test of lcm method, of class Nums.
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
     * Test of lcm method, of class Nums.
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
     * Test of fibonacci method, of class Nums.
     */
    @Test
    public void testFibonacci_BigIntegerArr()
    {
        BigInteger[] fibonacci = new BigInteger[FIBONACCI.length];
        assertEquals(FIBONACCI_BIG.length, Nums.fibonacci(fibonacci));
        assertArrayEquals(FIBONACCI_BIG, fibonacci);
    }

    /**
     * Test of fibonacci method, of class Nums.
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
     * Test of fibonacci method, of class Nums.
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
     * Test of safePrime method, of class BigMath.
     */
    @Test
    public void testSafePrime() throws NoSuchAlgorithmException, InvalidParameterSpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException, NoSuchProviderException
    {
        BigInteger p = Nums.safePrime(rnd, 512);
        assertTrue(p.isProbablePrime(100));
        BigInteger q = p.subtract(BigInteger.ONE).divide(BIG_INT_TWO);
        assertTrue(q.isProbablePrime(100));
        
        BigInteger g = Nums.safePrime(rnd, 512);
        
    }

    /**
     * Test of gcd method, of class Nums.
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
     * Test of gcd method, of class Nums.
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
     * Test of lcm method, of class Nums.
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
     * Test of lcm method, of class Nums.
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

    static long[] FIBONACCI_LONG = Utils.asLongs(FIBONACCI);
    static long[] FIBO13_21_LONG = Utils.asLongs(FIBO13_21);
    
    /**
     * Test of fibonacci method, of class Nums.
     */
    @Test
    public void testFibonacci_longArr()
    {
        long[] fibonacci = new long[FIBONACCI_LONG.length];
        assertEquals(FIBONACCI_LONG.length, Nums.fibonacci(fibonacci));
        assertArrayEquals(FIBONACCI_LONG, fibonacci);
    }

    /**
     * Test of fibonacci method, of class Nums.
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
     * Test of fibonacci method, of class Nums.
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
        assertFalse(Nums.isPow2(-1L));
        assertFalse(Nums.isPow2(0L));
        assertFalse(Nums.isPow2(3L));
        assertFalse(Nums.isPow2(5L));
        assertFalse(Nums.isPow2(7L));
        assertFalse(Nums.isPow2(9L));
        assertFalse(Nums.isPow2(15L));
        assertFalse(Nums.isPow2(17L));
        assertFalse(Nums.isPow2(31L));
        assertFalse(Nums.isPow2(33L));
        assertFalse(Nums.isPow2(63L));
        assertFalse(Nums.isPow2(65L));
        assertFalse(Nums.isPow2(127L));
        assertFalse(Nums.isPow2(129L));
        
        assertTrue(Nums.isPow2(1L));
        assertTrue(Nums.isPow2(2L));
        assertTrue(Nums.isPow2(4L));
        assertTrue(Nums.isPow2(8L));
        assertTrue(Nums.isPow2(16L));
        assertTrue(Nums.isPow2(32L));
        assertTrue(Nums.isPow2(64L));
        assertTrue(Nums.isPow2(128L));
        
        assertFalse(Nums.isPow2((long)Byte.MAX_VALUE));
        assertFalse(Nums.isPow2((long)Short.MAX_VALUE));
        assertFalse(Nums.isPow2((long)Character.MAX_VALUE));
        assertFalse(Nums.isPow2((long)Integer.MAX_VALUE));
        assertFalse(Nums.isPow2(Long.MAX_VALUE));

        assertTrue(Nums.isPow2(Byte.MAX_VALUE+1L));
        assertTrue(Nums.isPow2(Short.MAX_VALUE+1L));
        assertTrue(Nums.isPow2(Character.MAX_VALUE+1L));
        assertTrue(Nums.isPow2(Integer.MAX_VALUE+1L));

        assertFalse(Nums.isPow2((long)-Byte.MAX_VALUE));
        assertFalse(Nums.isPow2((long)-Short.MAX_VALUE));
        assertFalse(Nums.isPow2((long)-Character.MAX_VALUE));
        assertFalse(Nums.isPow2((long)-Integer.MAX_VALUE));
        assertFalse(Nums.isPow2(-Long.MAX_VALUE));

        assertFalse(Nums.isPow2(-Byte.MAX_VALUE-1L));
        assertFalse(Nums.isPow2(-Short.MAX_VALUE-1L));
        assertFalse(Nums.isPow2(-Character.MAX_VALUE-1L));
        assertFalse(Nums.isPow2(-Integer.MAX_VALUE-1L));
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
     * Test of isEven method, of class Nums.
     */
    @Test
    public void testIsEven()
    {
        assertTrue(Nums.isEven(BigInteger.ZERO));
        assertFalse(Nums.isEven(BigInteger.ONE));
        assertTrue(Nums.isEven(BigInteger.TEN));
        assertTrue(Nums.isEven(BigInteger.ZERO.negate()));
        assertFalse(Nums.isEven(BigInteger.ONE.negate()));
        assertTrue(Nums.isEven(BigInteger.TEN.negate()));
        
        for(int i=0;i<1000;i+=97)
        {
            BigInteger value = BigInteger.valueOf(i);
            assertEquals( i%2==0, Nums.isEven(value), "i="+i);
        }
    }

    /**
     * Test of isOdd method, of class Nums.
     */
    @Test
    public void testIsOdd()
    {
        assertFalse(Nums.isOdd(BigInteger.ZERO));
        assertTrue(Nums.isOdd(BigInteger.ONE));
        assertFalse(Nums.isOdd(BigInteger.TEN));
        assertFalse(Nums.isOdd(BigInteger.ZERO.negate()));
        assertTrue(Nums.isOdd(BigInteger.ONE.negate()));
        assertFalse(Nums.isOdd(BigInteger.TEN.negate()));

        for(int i=0;i<1000;i+=97)
        {
            BigInteger value = BigInteger.valueOf(i);
            assertEquals( i%2==1, Nums.isOdd(value), "i="+i);
        }
    }

    /**
     * Test of nthRoot method, of class Nums.
     */
    @Test
    public void testNthRoot()
    {
        MathContext mc8 = new MathContext(8, RoundingMode.HALF_UP);
        
        assertEquals(BigDecimal.ONE, BigDecimalMath.root(BigDecimal.ONE, BigDecimal.ONE, mc8));
        assertTrue(BigDecimal.TEN.compareTo(BigDecimalMath.root(BigDecimal.TEN, BigDecimal.ONE, mc8))==0);
        assertTrue(BigDecimal.TEN.compareTo(BigDecimalMath.root(Nums.BIG_DEC_HUNDRED, Nums.BIG_DEC_TWO, mc8))==0);
        assertTrue(BigDecimal.TEN.compareTo(BigDecimalMath.root(Nums.BIG_DEC_THOUSAND, BigDecimal.valueOf(3), mc8))==0);
        assertTrue(Nums.BIG_DEC_TWO.compareTo(BigDecimalMath.root(BigDecimal.valueOf(1024), BigDecimal.TEN, mc8))==0);
        assertTrue(Nums.BIG_DEC_TWO.compareTo(BigDecimalMath.root(BigDecimal.valueOf(4294967296L), BigDecimal.valueOf(32), mc8))==0);

        MathContext mc365 = new MathContext(365, RoundingMode.HALF_UP);
        MathContext mc367 = new MathContext(370, RoundingMode.HALF_UP);
        BigDecimal root = BigDecimalMath.root(Nums.BIG_DEC_HUNDRED, BigDecimal.valueOf(365), mc367);
//        System.out.println(root);
        BigDecimal pow = BigDecimalMath.pow(root, BigDecimal.valueOf(365), mc367).round(mc365);
//        System.out.println(pow);
        assertTrue(Nums.BIG_DEC_HUNDRED.compareTo(pow)==0);
    }

    /**
     * Test of equalsEnough method, of class Nums.
     */
    @Test
    public void testEqualsEnough_3args_3()
    {
        assertTrue(Nums.equalsEnough(0f, 0f, 0f));
        assertTrue(Nums.equalsEnough(0f, 1f, 1f));
        assertFalse(Nums.equalsEnough(0f, 10f, 1f));

        assertTrue(Nums.equalsEnough(0.1001f, 0.1f, 0.01f));
        assertFalse(Nums.equalsEnough(0.1001f, 0.2f, 0.01f));
    }

    /**
     * Test of equalsEnough method, of class Nums.
     */
    @Test
    public void testEqualsEnough_3args_4()
    {
        assertTrue(Nums.equalsEnough(0d, 0d, 0d));
        assertTrue(Nums.equalsEnough(0d, 1d, 1d));
        assertFalse(Nums.equalsEnough(0d, 10d, 1d));

        assertTrue(Nums.equalsEnough(0.1001, 0.1, 0.01));
        assertFalse(Nums.equalsEnough(0.1001, 0.2, 0.01));
    }

    /**
     * Test of equalsEnough method, of class Nums.
     */
    @Test
    public void testEqualsEnough_3args_5()
    {
        assertTrue(Nums.equalsEnough(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        assertTrue(Nums.equalsEnough(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE));
        assertFalse(Nums.equalsEnough(BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE));

        assertTrue(Nums.equalsEnough(BigDecimal.valueOf(0.1001), BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.01)));
        assertFalse(Nums.equalsEnough(BigDecimal.valueOf(0.1001), BigDecimal.valueOf(0.2), BigDecimal.valueOf(0.01)));
    }
    
}
