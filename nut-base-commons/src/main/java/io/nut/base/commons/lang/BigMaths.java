/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.lang;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * Utility class providing mathematical methods for {@link BigInteger} and
 * {@link BigDecimal} values.
 *
 * @author franci
 */
public class BigMaths
{
    private static final int SCALE = 18;

    /** The natural logarithm of 10, used to convert base-10 logarithms into natural logarithms. */
    private static final BigDecimal LN10 = new BigDecimal("2.30258509299404568401799145468436420760110148862877297603332790");

    /**
     * Returns whether the two given values are equal within the given tolerance.
     *
     * @param d1 the first value.
     * @param d2 the second value.
     * @param delta the maximum absolute difference considered to be equal.
     * @return true if d1 and d2 are equal enough.
     */
    public static boolean equalsEnough(BigDecimal d1, BigDecimal d2, BigDecimal delta)
    {
        if(d1.compareTo(d2) == 0)
        {
            return true;
        }
        if(delta.equals(BigDecimal.ZERO))
        {
            return d1.compareTo(d2) == 0;
        }
        return d1.subtract(d2).abs().compareTo(delta) <= 0;
    }

    /**
     * Returns whether the given value is strictly greater than zero.
     *
     * @param n the value to test.
     * @return true if n is positive.
     */
    public static boolean isPositive(BigInteger n)
    {
        return n.compareTo(BigInteger.ZERO) > 0;
    }

    /**
     * Returns whether the given value is strictly greater than zero.
     *
     * @param n the value to test.
     * @return true if n is positive.
     */
    public static boolean isPositive(BigDecimal n)
    {
        return n.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns whether the given value is greater than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is positive or zero.
     */
    public static boolean isPositiveOrZero(BigInteger n)
    {
        return n.compareTo(BigInteger.ZERO) >= 0;
    }

    /**
     * Returns whether the given value is greater than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is positive or zero.
     */
    public static boolean isPositiveOrZero(BigDecimal n)
    {
        return n.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Returns whether the given value is strictly less than zero.
     *
     * @param n the value to test.
     * @return true if n is negative.
     */
    public static boolean isNegative(BigInteger n)
    {
        return n.compareTo(BigInteger.ZERO) < 0;
    }

    /**
     * Returns whether the given value is strictly less than zero.
     *
     * @param n the value to test.
     * @return true if n is negative.
     */
    public static boolean isNegative(BigDecimal n)
    {
        return n.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Returns whether the given value is less than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is negative or zero.
     */
    public static boolean isNegativeOrZero(BigInteger n)
    {
        return n.compareTo(BigInteger.ZERO) <= 0;
    }

    /**
     * Returns whether the given value is less than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is negative or zero.
     */
    public static boolean isNegativeOrZero(BigDecimal n)
    {
        return n.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Returns whether the given value is equal to zero.
     *
     * @param n the value to test.
     * @return true if n is zero.
     */
    public static boolean isZero(BigInteger n)
    {
        return n.compareTo(BigInteger.ZERO) == 0;
    }

    /**
     * Returns whether the given value is equal to zero.
     *
     * @param n the value to test.
     * @return true if n is zero.
     */
    public static boolean isZero(BigDecimal n)
    {
        return n.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Returns whether the given value is zero within the given tolerance.
     *
     * @param n the value to test.
     * @param delta the maximum absolute value considered to be zero.
     * @return true if |n| <= |delta|.
     */
    public static boolean isZero(BigDecimal n, BigDecimal delta)
    {
        return n.abs().compareTo(delta.abs()) <= 0;
    }

    /**
     * Returns whether the given value is null or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is null or zero.
     */
    public static boolean isNullOrZero(BigInteger n)
    {
        return n == null || isZero(n);
    }

    /**
     * Returns whether the given value is null or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is null or zero.
     */
    public static boolean isNullOrZero(BigDecimal n)
    {
        return n == null || isZero(n);
    }

    /**
     * Returns whether the given value is null or zero within the given tolerance.
     *
     * @param n the value to test.
     * @param delta the maximum absolute value considered to be zero.
     * @return true if n is null or |n| <= |delta|.
     */
    public static boolean isNullOrZero(BigDecimal n, BigDecimal delta)
    {
        return n == null || isZero(n, delta);
    }

    /**
     * Returns whether the given value is even.
     *
     * @param value the value to test.
     * @return true if the value is even.
     */
    public static boolean isEven(BigInteger value)
    {
        return value.testBit(0)==false;
    }
    
    /**
     * Returns whether the given value is odd.
     *
     * @param value the value to test.
     * @return true if the value is odd.
     */
    public static boolean isOdd(BigInteger value)
    {
        return value.testBit(0)==true;
    }
        

    /**
     *  Returns the first item that is not a null value or is not a BigDecimal.ZERO, 
     *  it also returns a ZERO if any or null if all values are null or array is empty
     * 
     * @param items
     * @return 
     */
    public static BigDecimal firstNonNullOrZero(BigDecimal... items)
    {
        BigDecimal def = null;
        if(items==null || items.length==0)
        {
            return def;
        }
        for(BigDecimal item : items)
        {
            if(item!=null)
            {
                if(item.compareTo(BigDecimal.ZERO)!=0)
                {
                    return item;
                }
                if(def==null)
                {
                    def = item;
                }
            }
        }
        return def;
    }
    
    /**
     * Computes the natural logarithm of the given BigDecimal value.
     *
     * @param b the value (must be positive).
     * @return the natural logarithm of b.
     * @throws ArithmeticException if b is zero or negative.
     */
    public static BigDecimal log(BigDecimal b)
    {
        if(b.signum() <= 0)
        {
            throw new ArithmeticException("log of a negative number! (or zero)");
        }
        if(b.compareTo(BigDecimal.ONE) == 0)
        {
            return BigDecimal.ZERO;
        }
        MathContext mc = new MathContext(SCALE + 2, RoundingMode.HALF_EVEN);
        return log10(b).multiply(LN10, mc);
    }

    /**
     * Computes the base-10 logarithm of the given BigDecimal value.
     *
     * @param b the value (must be positive).
     * @return the base-10 logarithm of b.
     * @throws ArithmeticException if b is zero or negative.
     */
    public static BigDecimal log10(BigDecimal b)
    {
        final int NUM_OF_DIGITS = SCALE + 2;
            // need to add one to get the right number of dp
            //  and then add one again to get the next number
            //  so I can round it correctly.

        MathContext mc = new MathContext(NUM_OF_DIGITS, RoundingMode.HALF_EVEN);
        //special conditions:
        // log(-x) -> exception
        // log(1) == 0 exactly;
        // log of a number lessthan one = -log(1/x)
        if(b.signum() <= 0)
        {
                throw new ArithmeticException("log of a negative number! (or zero)");

        }
        else if (b.compareTo(BigDecimal.ONE) == 0)
        {
            return BigDecimal.ZERO;
        }
        else if (b.compareTo(BigDecimal.ONE) < 0)
        {
            return (log10((BigDecimal.ONE).divide(b, mc))).negate();
        }

        StringBuilder sb = new StringBuilder();
        //number of digits on the left of the decimal point
        int leftDigits = b.precision() - b.scale();

        //so, the first digits of the log10 are:
        sb.append(leftDigits - 1).append(".");

        //this is the algorithm outlined in the webpage
        int n = 0;
        while (n < NUM_OF_DIGITS)
        {
            b = (b.movePointLeft(leftDigits - 1)).pow(10, mc);
            leftDigits = b.precision() - b.scale();
            sb.append(leftDigits - 1);
            n++;
        }

        BigDecimal ans = new BigDecimal(sb.toString());

        //Round the number to the correct number of decimal places.
        ans = ans.round(new MathContext(ans.precision() - ans.scale() + SCALE, RoundingMode.HALF_EVEN));
        return ans;
    }

    /**
     * Returns the base-2 logarithm of the given value (floor).
     *
     * @param n the value (must be &gt; 0).
     * @return floor(log2(n)), or -1 if n is null or n &lt;= 0.
     */
    public static int log2(BigInteger n)
    {
        return log2(n,false);
    }

    /**
     * Returns the base-2 logarithm of the given value, optionally rounding up.
     *
     * @param n the value (must be &gt; 0).
     * @param ceil if true, returns the ceiling instead of the floor.
     * @return floor(log2(n)) or ceil(log2(n)), or -1 if n is null or n &lt;= 0.
     */
    public static int log2(BigInteger n, boolean ceil)
    {
        if (n == null || n.signum() <= 0)
        {
            return -1; // or what it defines for the invalid case
        }
        int floor = n.bitLength() - 1;
        if (ceil && !n.equals(BigInteger.ONE.shiftLeft(floor)))
        {
            floor++; // It is not an exact power of 2
        }
        return floor;
    }

    /**
     * Returns the sum of all the given values, skipping null values.
     *
     * @param values the values to add.
     * @return the sum of the values.
     */
    public static BigInteger sum(BigInteger... values)
    {
        BigInteger s = BigInteger.ZERO;
        if (values == null)
        {
            return s;
        }
        for (BigInteger v : values)
        {
            if (v != null)
            {
                s = s.add(v);
            }
        }
        return s;
    }

    /**
     * Returns the sum of all the given values skipping null values.
     *
     * @param values the values to add.
     * @return the sum of the values.
     */
    public static BigDecimal sum(BigDecimal... values)
    {
        BigDecimal s = BigDecimal.ZERO;
        if (values == null)
        {
            return s;
        }
        for (BigDecimal v : values)
        {
            if (v != null)
            {
                s = s.add(v);
            }
        }
        return s;
    }

    /**
     * Returns the sum of all the given values using the given MathContext,
     * skipping null values.
     *
     * @param mc the MathContext to use.
     * @param values the values to add.
     * @return the sum of the values.
     */
    public static BigDecimal sum(MathContext mc, BigDecimal... values)
    {
        BigDecimal s = BigDecimal.ZERO;
        if (values == null)
        {
            return s;
        }
        for (BigDecimal v : values)
        {
            if (v != null)
            {
                s = s.add(v, mc);
            }
        }
        return s;
    }

    /**
     * Returns the average of all the given BigDecimal values using the specified MathContext.
     * Null values are replaced by the given default value (if non-null and non-zero).
     *
     * @param defaultValue the value used in place of null entries, or null to skip nulls entirely.
     * @param mc the MathContext used for division.
     * @param values the values to average.
     * @return the arithmetic mean, or {@link BigDecimal#ZERO} if null or no values are given.
     */
    public static BigDecimal avg(BigDecimal defaultValue, MathContext mc, BigDecimal... values)
    {
        if (values == null || values.length==0)
        {
            return BigDecimal.ZERO;
        }
        int count = 0;
        BigDecimal ret = BigDecimal.ZERO;
        for (BigDecimal v : values)
        {
            if (v != null)
            {
                ret = ret.add(v);
                count++;
            }
            else if (defaultValue != null && defaultValue != BigDecimal.ZERO)
            {
                ret = ret.add(defaultValue);
                count++;
            }
        }
        return ret.divide(BigDecimal.valueOf(count), mc);
    }
    
    //java9 new BigInteger​​(int signum, byte[] magnitude, int off, int len)
    public static BigInteger newBigInteger(int signum, byte[] magnitude, int off, int len)
    {
        return new BigInteger(signum, Arrays.copyOfRange(magnitude, off, off+len));
    }
    
    //java9 new BigInteger​​(byte[] val, int off, int len)
    public static BigInteger newBigInteger​(byte[] val, int off, int len)
    {
        return new BigInteger(Arrays.copyOfRange(val, off, off+len));
    }    
    

    /**
     * Clamps the given {@link BigInteger} {@code value} to be within the inclusive range of {@code [min, max]}.
     * <p>
     * If {@code min} is greater than {@code max}, the bounds are automatically swapped before clamping.
     *
     * @param min   the lower bound of the range (inclusive).
     * @param max   the upper bound of the range (inclusive).
     * @param value the BigInteger value to clamp.
     * @return      the clamped value; {@code min} if {@code value} is less than {@code min},
     *              {@code max} if {@code value} is greater than {@code max}, and {@code value} otherwise.
     */
    public static BigInteger bound(BigInteger min, BigInteger max, BigInteger value)
    {
        if(min.compareTo(max)>0)
        {
            return bound(max,min,value);
        }
        if (value.compareTo(min) < 0)
        {
            return min;
        }
        if (value.compareTo(max) > 0)
        {
            return max;
        }
        return value;
    }

    /**
     * Clamps the given {@link BigDecimal} {@code value} to be within the inclusive range of {@code [min, max]}.
     * <p>
     * If {@code min} is greater than {@code max}, the bounds are automatically swapped before clamping.
     *
     * @param min   the lower bound of the range (inclusive).
     * @param max   the upper bound of the range (inclusive).
     * @param value the BigDecimal value to clamp.
     * @return      the clamped value; {@code min} if {@code value} is less than {@code min},
     *              {@code max} if {@code value} is greater than {@code max}, and {@code value} otherwise.
     */
    public static BigDecimal bound(BigDecimal min, BigDecimal max, BigDecimal value)
    {
        if(min.compareTo(max)>0)
        {
            return bound(max,min,value);
        }
        if (value.compareTo(min) < 0)
        {
            return min;
        }
        if (value.compareTo(max) > 0)
        {
            return max;
        }
        return value;
    }
        
    /**
     * Compute the square root of x to a given scale, x &gt;= 0.
     * Use Newton's algorithm.
     * @param x the value of x
     * @return the result value
     */
    public static BigDecimal sqrt(BigDecimal x)
    {
        if(x.compareTo(BigDecimal.ZERO)==0 || x.compareTo(BigDecimal.ONE)==0)
        {
            return x;
        }
        // Check that x >= 0.
        if (x.signum() < 0)
        {
            throw new ArithmeticException("x < 0");
        }

        // n = x*(10^(2*SCALE))
        BigInteger n = x.movePointRight(SCALE << 1).toBigInteger();

        // The first approximation is the upper half of n.
        int bits = (n.bitLength() + 1) >> 1;
        BigInteger ix = n.shiftRight(bits);
        BigInteger ixPrev;

        // Loop until the approximations converge
        // (two successive approximations are equal after rounding).
        do
        {
            ixPrev = ix;
            // x = (x + n/x)/2
            ix = ix.add(n.divide(ix)).shiftRight(1);
        } 
        while (ix.compareTo(ixPrev) != 0);

        return new BigDecimal(ix, SCALE);
    }

}
