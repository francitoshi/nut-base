/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

/**
 * Utility class providing mathematical methods for primitive numbers.
 *
 * @author franci
 */
public class Maths
{
    static final double[] log_cache=new double[256];

    /**
     * Returns whether the two given values are equal within the given tolerance.
     *
     * @param f1 the first value.
     * @param f2 the second value.
     * @param delta the maximum absolute difference considered to be equal.
     * @return true if f1 and f2 are equal enough.
     */
    public static boolean equalsEnough(float f1, float f2, float delta)
    {
        return (f1 == f2) || (Math.abs(f1 - f2) <= delta);
    }

    /**
     * Returns whether the two given values are equal within the given tolerance.
     *
     * @param d1 the first value.
     * @param d2 the second value.
     * @param delta the maximum absolute difference considered to be equal.
     * @return true if d1 and d2 are equal enough.
     */
    public static boolean equalsEnough(double d1, double d2, double delta)
    {
        return (d1 == d2) || (Math.abs(d1 - d2) <= delta);
    }

    /**
     * Returns whether the given value is strictly greater than zero.
     *
     * @param n the value to test.
     * @return true if n is positive.
     */
    public static boolean isPositive(int n)
    {
        return n > 0;
    }

    /**
     * Returns whether the given value is strictly greater than zero.
     *
     * @param n the value to test.
     * @return true if n is positive.
     */
    public static boolean isPositive(long n)
    {
        return n > 0;
    }

    /**
     * Returns whether the given value is strictly greater than zero.
     *
     * @param n the value to test.
     * @return true if n is positive.
     */
    public static boolean isPositive(float n)
    {
        return n > 0;
    }

    /**
     * Returns whether the given value is strictly greater than zero.
     *
     * @param n the value to test.
     * @return true if n is positive.
     */
    public static boolean isPositive(double n)
    {
        return n > 0;
    }

    /**
     * Returns whether the given value is greater than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is positive or zero.
     */
    public static boolean isPositiveOrZero(int n)
    {
        return n >= 0;
    }

    /**
     * Returns whether the given value is greater than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is positive or zero.
     */
    public static boolean isPositiveOrZero(long n)
    {
        return n >= 0;
    }

    /**
     * Returns whether the given value is greater than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is positive or zero.
     */
    public static boolean isPositiveOrZero(float n)
    {
        return n >= 0;
    }

    /**
     * Returns whether the given value is greater than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is positive or zero.
     */
    public static boolean isPositiveOrZero(double n)
    {
        return n >= 0;
    }

    /**
     * Returns whether the given value is strictly less than zero.
     *
     * @param n the value to test.
     * @return true if n is negative.
     */
    public static boolean isNegative(int n)
    {
        return n < 0;
    }

    /**
     * Returns whether the given value is strictly less than zero.
     *
     * @param n the value to test.
     * @return true if n is negative.
     */
    public static boolean isNegative(long n)
    {
        return n < 0;
    }

    /**
     * Returns whether the given value is strictly less than zero.
     *
     * @param n the value to test.
     * @return true if n is negative.
     */
    public static boolean isNegative(float n)
    {
        return n < 0;
    }

    /**
     * Returns whether the given value is strictly less than zero.
     *
     * @param n the value to test.
     * @return true if n is negative.
     */
    public static boolean isNegative(double n)
    {
        return n < 0;
    }

    /**
     * Returns whether the given value is less than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is negative or zero.
     */
    public static boolean isNegativeOrZero(int n)
    {
        return n <= 0;
    }

    /**
     * Returns whether the given value is less than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is negative or zero.
     */
    public static boolean isNegativeOrZero(long n)
    {
        return n <= 0;
    }

    /**
     * Returns whether the given value is less than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is negative or zero.
     */
    public static boolean isNegativeOrZero(float n)
    {
        return n <= 0;
    }

    /**
     * Returns whether the given value is less than or equal to zero.
     *
     * @param n the value to test.
     * @return true if n is negative or zero.
     */
    public static boolean isNegativeOrZero(double n)
    {
        return n <= 0;
    }

    /**
     * Returns whether the given value is equal to zero.
     *
     * @param n the value to test.
     * @return true if n is zero.
     */
    public static boolean isZero(int n)
    {
        return n == 0;
    }

    /**
     * Returns whether the given value is equal to zero.
     *
     * @param n the value to test.
     * @return true if n is zero.
     */
    public static boolean isZero(long n)
    {
        return n == 0;
    }

    /**
     * Returns whether the given value is equal to zero.
     *
     * @param n the value to test.
     * @return true if n is zero.
     */
    public static boolean isZero(float n)
    {
        return n == 0;
    }

    /**
     * Returns whether the given value is equal to zero.
     *
     * @param n the value to test.
     * @return true if n is zero.
     */
    public static boolean isZero(double n)
    {
        return n == 0;
    }

    /**
     * Returns whether the given value is zero within the given tolerance.
     *
     * @param n the value to test.
     * @param delta the maximum absolute value considered to be zero.
     * @return true if |n| <= |delta|.
     */
    public static boolean isZero(float n, float delta)
    {
        return Math.abs(n) <= Math.abs(delta);
    }

    /**
     * Returns whether the given value is zero within the given tolerance.
     *
     * @param n the value to test.
     * @param delta the maximum absolute value considered to be zero.
     * @return true if |n| <= |delta|.
     */
    public static boolean isZero(double n, double delta)
    {
        return Math.abs(n) <= Math.abs(delta);
    }

    /**
     * Returns the natural logarithm of the given integer, using a cache for small values.
     *
     * @param n the value.
     * @return ln(n).
     */
    public static double log(int n)
    {
        if(n==1)
        {
            return 0;
        }
        if(n<0 || n>=log_cache.length)
        {
            return Math.log(n);
        }
        double val = log_cache[n];
        if(val==0)
        {
            val = log_cache[n] = Math.log(n);
        }
        return val;
    }

    /**
     * Computes the logarithm of value with the given base.
     *
     * @param base the base of the logarithm.
     * @param value the value.
     * @return log_base(value).
     */
    public static double log(double base, double value)
    {
        //log is faster than log10
        return Math.log(value)/Math.log(base);
    }

    /**
     * Returns the base-2 logarithm of the given value (floor).
     *
     * @param n the value (must be &gt; 0).
     * @return floor(log2(n)), or -1 if n &lt;= 0.
     */
    public static int log2(long n)
    {
        return log2(n,false);
    }

    /**
     * Returns the base-2 logarithm of the given value, optionally rounding up.
     *
     * @param n the value (must be &gt; 0).
     * @param ceil if true, returns the ceiling instead of the floor.
     * @return floor(log2(n)) or ceil(log2(n)), or -1 if n &lt;= 0.
     */
    public static int log2(long n, boolean ceil)
    {
        if (n <= 0)
        {
            return -1; // or what it defines for the invalid case
        }
        int floor = 63 - Long.numberOfLeadingZeros(n);
        if (ceil && (n & (n - 1)) != 0)
        {
            floor++; // It is not an exact power of 2
        }
        return floor;
    }
    
    /**
     * Returns the sum of all the given byte values.
     *
     * @param values the values to add.
     * @return the sum of the values, or 0 if null or no values are given.
     */
    public static long sum(byte... values)
    {
        if(values==null || values.length==0)
        {
            return 0;
        }
        long ret = 0;
        for (int v : values)
        {
            ret += v;
        }
        return ret;
    }

    /**
     * Returns the sum of all the given int values.
     *
     * @param values the values to add.
     * @return the sum of the values, or 0 if null or no values are given.
     */
    public static long sum(int... values)
    {
        if(values==null || values.length==0)
        {
            return 0;
        }
        long ret = 0;
        for (int v : values)
        {
            ret += v;
        }
        return ret;
    }

    /**
     * Returns the sum of all the given long values.
     *
     * @param values the values to add.
     * @return the sum of the values, or 0 if null or no values are given.
     */
    public static long sum(long... values)
    {
        if(values==null || values.length==0)
        {
            return 0;
        }
        long ret = 0;
        for (long v : values)
        {
            ret += v;
        }
        return ret;
    }

    /**
     * Returns the sum of all the given float values.
     *
     * @param values the values to add.
     * @return the sum of the values, or 0 if null or no values are given.
     */
    public static double sum(float... values)
    {
        if(values==null || values.length==0)
        {
            return 0;
        }
        double ret = 0;
        for (double v : values)
        {
            ret += v;
        }
        return ret;
    }

    /**
     * Returns the sum of all the given double values.
     *
     * @param values the values to add.
     * @return the sum of the values, or 0 if null or no values are given.
     */
    public static double sum(double... values)
    {
        if(values==null || values.length==0)
        {
            return 0;
        }
        double ret = 0;
        for (double v : values)
        {
            ret += v;
        }
        return ret;
    }

    /**
     * Returns the average of all the given int values.
     *
     * @param values the values to average.
     * @return the arithmetic mean, or 0 if null or no values are given.
     */
    public static double avg(int... values)
    {
        if (values == null || values.length==0)
        {
            return 0;
        }
        return sum(values) / (double) values.length;
    }

    /**
     * Returns the average of all the given long values.
     *
     * @param values the values to average.
     * @return the arithmetic mean, or 0 if null or no values are given.
     */
    public static double avg(long... values)
    {
        if (values == null || values.length==0)
        {
            return 0;
        }
        return sum(values) / (double) values.length;
    }

    /**
     * Returns the average of all the given float values.
     *
     * @param values the values to average.
     * @return the arithmetic mean, or 0 if null or no values are given.
     */
    public static double avg(float... values)
    {
        if (values == null || values.length==0)
        {
            return 0;
        }
        return sum(values) / (double) values.length;
    }

    /**
     * Returns the average of all the given double values.
     *
     * @param values the values to average.
     * @return the arithmetic mean, or 0 if null or no values are given.
     */
    public static double avg(double... values)
    {
        if (values == null || values.length==0)
        {
            return 0;
        }
        return sum(values) / (double) values.length;
    }
    
}
