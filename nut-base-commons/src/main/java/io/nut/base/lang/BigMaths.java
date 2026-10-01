/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

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
     * Returns the sum of all the given BigDecimal values, skipping nulls.
     * Null values are replaced by the given default value (if non-null and non-zero).
     *
     * @param defaultValue the value used in place of null entries, or null to skip nulls entirely.
     * @param values the values to add.
     * @return the sum of the values, or {@link BigDecimal#ZERO} if null or no values are given.
     */
    public static BigDecimal sum(BigDecimal defaultValue, BigDecimal... values)
    {
        if (values == null || values.length==0)
        {
            return BigDecimal.ZERO;
        }
        BigDecimal ret = BigDecimal.ZERO;
        for (BigDecimal v : values)
        {
            if (v != null)
            {
                ret = ret.add(v);
            }
            else if (defaultValue != null && defaultValue != BigDecimal.ZERO)
            {
                ret = ret.add(defaultValue);
            }
        }
        return ret;
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
    
}
