/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.lang;

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
     * Returns whether the given int value is a power of 2.
     *
     * @param i the value to test.
     * @return true if i is a positive power of 2.
     */
    //borrowed from http://graphics.stanford.edu/~seander/bithacks.html#DetermineIfPowerOf2
    public static boolean isPow2(int i)
    {
        return i>0 && (i & (i - 1))==0;
    }

    /**
     * Returns whether the given long value is a power of 2.
     *
     * @param i the value to test.
     * @return true if i is a positive power of 2.
     */
    //borrowed from http://graphics.stanford.edu/~seander/bithacks.html#DetermineIfPowerOf2
    public static boolean isPow2(long i)
    {
        return i>0 && (i & (i - 1))==0;
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
     * Returns the product of all the given int values.
     *
     * @param values the values to multiply.
     * @return the product of the values, or 1 if null or no values are given.
     */
    public static long mul(int... values)
    {
        long m = 1;
        if(values==null || values.length==0)
        {
            return m;
        }
        for(int i=0;i<values.length;i++)
        {
            m *= values[i];
        }
        return m;
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
    
    /**
     * Returns the minimum value in the given array.
     *
     * @param values the array whose minimum is to be computed.
     * @return the minimum value, or 0 if the array is empty.
     */
    public static byte min(byte[] values)
    {
        byte m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                byte item = values[i];
                if (item < m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    /**
     * Returns the minimum value in the given array.
     *
     * @param values the array whose minimum is to be computed.
     * @return the minimum value, or 0 if the array is empty.
     */
    public static short min(short[] values)
    {
        short m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                short item = values[i];
                if (item < m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    /**
     * Returns the minimum value in the given array.
     *
     * @param values the array whose minimum is to be computed.
     * @return the minimum value, or 0 if the array is empty.
     */
    public static char min(char[] values)
    {
        char m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                char item = values[i];
                if (item < m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    /**
     * Returns the minimum value in the given array.
     *
     * @param values the array whose minimum is to be computed.
     * @return the minimum value, or 0 if the array is empty.
     */
    public static int min(int[] values)
    {
        int m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                int item = values[i];
                if (item < m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    /**
     * Returns the minimum value in the given array.
     *
     * @param values the array whose minimum is to be computed.
     * @return the minimum value, or 0 if the array is empty.
     */
    public static long min(long[] values)
    {
        long m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                long item = values[i];
                if (item < m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    /**
     * Returns the minimum value in the given array.
     *
     * @param values the array whose minimum is to be computed.
     * @return the minimum value, or 0 if the array is empty.
     */
    public static float min(float[] values)
    {
        float m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                float item = values[i];
                if (item < m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    /**
     * Returns the minimum value in the given array.
     *
     * @param values the array whose minimum is to be computed.
     * @return the minimum value, or 0 if the array is empty.
     */
    public static double min(double[] values)
    {
        double m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                double item = values[i];
                if (item < m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    /**
     * Returns the minimum value among the given values.
     *
     * @param values the values whose minimum is to be computed.
     * @return the minimum value, or 0 if null or no values are given.
     */
    public static byte minOf(byte... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        return min(values);
    }

    /**
     * Returns the minimum value among the given values.
     *
     * @param values the values whose minimum is to be computed.
     * @return the minimum value, or 0 if null or no values are given.
     */
    public static short minOf(short... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        return min(values);
    }

    /**
     * Returns the minimum value among the given values.
     *
     * @param values the values whose minimum is to be computed.
     * @return the minimum value, or 0 if null or no values are given.
     */
    public static char minOf(char... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        return min(values);
    }

    /**
     * Returns the minimum value among the given values.
     *
     * @param values the values whose minimum is to be computed.
     * @return the minimum value, or 0 if null or no values are given.
     */
    public static int minOf(int... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        return min(values);
    }

    /**
     * Returns the minimum value among the given values.
     *
     * @param values the values whose minimum is to be computed.
     * @return the minimum value, or 0 if null or no values are given.
     */
    public static long minOf(long... values)
    {
        if (values == null || values.length == 0)
        {
            return 0L;
        }
        return min(values);
    }

    /**
     * Returns the minimum value among the given values.
     *
     * @param values the values whose minimum is to be computed.
     * @return the minimum value, or 0 if null or no values are given.
     */
    public static float minOf(float... values) 
    {
        if (values == null || values.length == 0)
        {
            return 0.0f;
        }
        float min = values[0];
        for (int i = 1; i < values.length; i++)
        {
            min = Math.min(min, values[i]);
        }
        return min;
    }
    
    /**
     * Returns the minimum value among the given values.
     *
     * @param values the values whose minimum is to be computed.
     * @return the minimum value, or 0 if null or no values are given.
     */
    public static double minOf(double... values)
    {
        if (values == null || values.length == 0)
        {
            return 0.0;
        }
        return min(values);
    }
    
    /**
     * Returns the maximum value in the given array.
     *
     * @param values the array whose maximum is to be computed.
     * @return the maximum value, or 0 if the array is empty.
     */
    public static byte max(byte[] values)
    {
        byte m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                byte item = values[i];
                if (item > m)
                {
                    m = item;
                }
            }
        }
        return m;
    }
    
    /**
     * Returns the maximum value in the given array.
     *
     * @param values the array whose maximum is to be computed.
     * @return the maximum value, or 0 if the array is empty.
     */
    public static short max(short[] values)
    {
        short m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                short item = values[i];
                if (item > m)
                {
                    m = item;
                }
            }
        }
        return m;
    }
    
    /**
     * Returns the maximum value in the given array.
     *
     * @param values the array whose maximum is to be computed.
     * @return the maximum value, or 0 if the array is empty.
     */
    public static char max(char[] values)
    {
        char m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                char item = values[i];
                if (item > m)
                {
                    m = item;
                }
            }
        }
        return m;
    }
    
    /**
     * Returns the maximum value in the given array.
     *
     * @param values the array whose maximum is to be computed.
     * @return the maximum value, or 0 if the array is empty.
     */
    public static int max(int[] values)
    {
        int m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                int item = values[i];
                if (item > m)
                {
                    m = item;
                }
            }
        }
        return m;
    }
    
    /**
     * Returns the maximum value in the given array.
     *
     * @param values the array whose maximum is to be computed.
     * @return the maximum value, or 0 if the array is empty.
     */
    public static long max(long[] values)
    {
        long m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                long item = values[i];
                if (item > m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    /**
     * Returns the maximum value in the given array.
     *
     * @param values the array whose maximum is to be computed.
     * @return the maximum value, or 0 if the array is empty.
     */
    public static float max(float[] values)
    {
        float m = 0;
        if (values.length > 0)
        {
            m = values[0];
            for (int i = 1; i < values.length; i++)
            {
                float item = values[i];
                if (item > m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    /**
     * Returns the maximum value in the given array.
     *
     * @param d the array whose maximum is to be computed.
     * @return the maximum value, or 0 if the array is empty.
     */
    public static double max(double[] d)
    {
        double m = 0;
        if (d.length > 0)
        {
            m = d[0];
            for (int i = 1; i < d.length; i++)
            {
                double item = d[i];
                if (item > m)
                {
                    m = item;
                }
            }
        }
        return m;
    }
    
    /**
     * Returns the maximum value among the given values.
     *
     * @param values the values whose maximum is to be computed.
     * @return the maximum value, or 0 if null or no values are given.
     */
    public static byte maxOf(byte... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        return max(values);
    }
    /**
     * Returns the maximum value among the given values.
     *
     * @param values the values whose maximum is to be computed.
     * @return the maximum value, or 0 if null or no values are given.
     */
    public static short maxOf(short... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        return max(values);
    }
    /**
     * Returns the maximum value among the given values.
     *
     * @param values the values whose maximum is to be computed.
     * @return the maximum value, or 0 if null or no values are given.
     */
    public static char maxOf(char... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        return max(values);
    }
    /**
     * Returns the maximum value among the given values.
     *
     * @param values the values whose maximum is to be computed.
     * @return the maximum value, or 0 if null or no values are given.
     */
    public static int maxOf(int... values)
    {
        if (values == null || values.length == 0)
        {
            return 0;
        }
        return max(values);
    }
    /**
     * Returns the maximum value among the given values.
     *
     * @param values the values whose maximum is to be computed.
     * @return the maximum value, or 0 if no values are given.
     */
    public static long maxOf(long... values) 
    {
        if (values == null || values.length == 0)
        {
            return 0L;
        }
        return max(values);
    }
    
    /**
     * Returns the maximum value among the given values.
     *
     * @param values the values whose maximum is to be computed.
     * @return the maximum value, or 0 if null or no values are given.
     */
    public static float maxOf(float... values) 
    {
        if (values == null || values.length == 0)
        {
            return 0.0f;
        }
        float max = values[0];
        for (int i = 1; i < values.length; i++)
        {
            max = Math.max(max, values[i]);
        }
        return max;
    }
    /**
     * Returns the maximum value among the given values.
     *
     * @param values the values whose maximum is to be computed.
     * @return the maximum value, or 0 if null or no values are given.
     */
    public static double maxOf(double... values) 
    {
        if (values == null || values.length == 0)
        {
            return 0.0;
        }
        return max(values);
    }
    
    /**
     * Finds the minimum and maximum values in the given byte array.
     *
     * @param d the byte array
     * @return a two-element array containing the minimum and maximum values, or null if the input array is null.
     *         If the array is empty, returns {0, 0}.
     */
    public static byte[] minMax(byte[] d)
    {
        if (d == null)
        {
            return null;
        }
        if (d.length == 0)
        {
            return new byte[]{0, 0};
        }
        byte min = d[0];
        byte max = d[0];
        for (int i = 1; i < d.length; i++)
        {
            byte val = d[i];
            if (val < min)
            {
                min = val;
            }
            else if (val > max)
            {
                max = val;
            }
        }
        return new byte[]{min, max};
    }

    /**
     * Finds the minimum and maximum values in the given short array.
     *
     * @param d the short array
     * @return a two-element array containing the minimum and maximum values, or null if the input array is null.
     *         If the array is empty, returns {0, 0}.
     */
    public static short[] minMax(short[] d)
    {
        if (d == null)
        {
            return null;
        }
        if (d.length == 0)
        {
            return new short[]{0, 0};
        }
        short min = d[0];
        short max = d[0];
        for (int i = 1; i < d.length; i++)
        {
            short val = d[i];
            if (val < min)
            {
                min = val;
            }
            else if (val > max)
            {
                max = val;
            }
        }
        return new short[]{min, max};
    }

    /**
     * Finds the minimum and maximum values in the given char array.
     *
     * @param d the char array
     * @return a two-element array containing the minimum and maximum values, or null if the input array is null.
     *         If the array is empty, returns {0, 0}.
     */
    public static char[] minMax(char[] d)
    {
        if (d == null)
        {
            return null;
        }
        if (d.length == 0)
        {
            return new char[]{0, 0};
        }
        char min = d[0];
        char max = d[0];
        for (int i = 1; i < d.length; i++)
        {
            char val = d[i];
            if (val < min)
            {
                min = val;
            }
            else if (val > max)
            {
                max = val;
            }
        }
        return new char[]{min, max};
    }

    /**
     * Finds the minimum and maximum values in the given int array.
     *
     * @param d the int array
     * @return a two-element array containing the minimum and maximum values, or null if the input array is null.
     *         If the array is empty, returns {0, 0}.
     */
    public static int[] minMax(int[] d)
    {
        if (d == null)
        {
            return null;
        }
        if (d.length == 0)
        {
            return new int[]{0, 0};
        }
        int min = d[0];
        int max = d[0];
        for (int i = 1; i < d.length; i++)
        {
            int val = d[i];
            if (val < min)
            {
                min = val;
            }
            else if (val > max)
            {
                max = val;
            }
        }
        return new int[]{min, max};
    }

    /**
     * Finds the minimum and maximum values in the given long array.
     *
     * @param d the long array
     * @return a two-element array containing the minimum and maximum values, or null if the input array is null.
     *         If the array is empty, returns {0L, 0L}.
     */
    public static long[] minMax(long[] d)
    {
        if (d == null)
        {
            return null;
        }
        if (d.length == 0)
        {
            return new long[]{0L, 0L};
        }
        long min = d[0];
        long max = d[0];
        for (int i = 1; i < d.length; i++)
        {
            long val = d[i];
            if (val < min)
            {
                min = val;
            }
            else if (val > max)
            {
                max = val;
            }
        }
        return new long[]{min, max};
    }

    /**
     * Finds the minimum and maximum values in the given float array.
     *
     * @param d the float array
     * @return a two-element array containing the minimum and maximum values, or null if the input array is null.
     *         If the array is empty, returns {0.0f, 0.0f}.
     */
    public static float[] minMax(float[] d)
    {
        if (d == null)
        {
            return null;
        }
        if (d.length == 0)
        {
            return new float[]{0.0f, 0.0f};
        }
        float min = d[0];
        float max = d[0];
        for (int i = 1; i < d.length; i++)
        {
            float val = d[i];
            if (val < min)
            {
                min = val;
            }
            else if (val > max)
            {
                max = val;
            }
        }
        return new float[]{min, max};
    }

    /**
     * Finds the minimum and maximum values in the given double array.
     *
     * @param d the double array
     * @return a two-element array containing the minimum and maximum values, or null if the input array is null.
     *         If the array is empty, returns {0.0, 0.0}.
     */
    public static double[] minMax(double[] d)
    {
        if (d == null)
        {
            return null;
        }
        if (d.length == 0)
        {
            return new double[]{0.0, 0.0};
        }
        double min = d[0];
        double max = d[0];
        for (int i = 1; i < d.length; i++)
        {
            double val = d[i];
            if (val < min)
            {
                min = val;
            }
            else if (val > max)
            {
                max = val;
            }
        }
        return new double[]{min, max};
    }
    
    /**
     * Returns whether all the given byte values are between a and b, inclusive, regardless of order.
     *
     * @param a the first bound.
     * @param b the second bound.
     * @param values the values to check.
     * @return true if every value is between a and b (inclusive), or true if no values are given; false otherwise.
     */
    public static boolean between(byte a, byte b, byte... values)
    {
        if (values == null || values.length == 0)
        {
            return true;
        }
        for (byte value : values)
        {
            if (!((a <= b) ? a <= value && value <= b : b <= value && value <= a))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether all the given int values are between a and b, inclusive, regardless of order.
     *
     * @param a the first bound.
     * @param b the second bound.
     * @param values the values to check.
     * @return true if every value is between a and b (inclusive), or true if no values are given; false otherwise.
     */
    public static boolean between(int a, int b, int... values)
    {
        if (values == null || values.length == 0)
        {
            return true;
        }
        for (int value : values)
        {
            if (!((a <= b) ? a <= value && value <= b : b <= value && value <= a))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether all the given long values are between a and b, inclusive, regardless of order.
     *
     * @param a the first bound.
     * @param b the second bound.
     * @param values the values to check.
     * @return true if every value is between a and b (inclusive), or true if no values are given; false otherwise.
     */
    public static boolean between(long a, long b, long... values)
    {
        if (values == null || values.length == 0)
        {
            return true;
        }
        for (long value : values)
        {
            if (!((a <= b) ? a <= value && value <= b : b <= value && value <= a))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether all the given float values are between a and b, inclusive, regardless of order.
     *
     * @param a the first bound.
     * @param b the second bound.
     * @param values the values to check.
     * @return true if every value is between a and b (inclusive), or true if no values are given; false otherwise.
     */
    public static boolean between(float a, float b, float... values)
    {
        if (values == null || values.length == 0)
        {
            return true;
        }
        for (float value : values)
        {
            if (!((a <= b) ? a <= value && value <= b : b <= value && value <= a))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether all the given double values are between a and b, inclusive, regardless of order.
     *
     * @param a the first bound.
     * @param b the second bound.
     * @param values the values to check.
     * @return true if every value is between a and b (inclusive), or true if no values are given; false otherwise.
     */
    public static boolean between(double a, double b, double... values)
    {
        if (values == null || values.length == 0)
        {
            return true;
        }
        for (double value : values)
        {
            if (!((a <= b) ? a <= value && value <= b : b <= value && value <= a))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Clamps the given byte {@code value} to be within the inclusive range of {@code [min, max]}.
     * <p>
     * If {@code min} is greater than {@code max}, the bounds are automatically swapped before clamping.
     *
     * @param min   the lower bound of the range (inclusive).
     * @param max   the upper bound of the range (inclusive).
     * @param value the byte value to clamp.
     * @return      the clamped value; {@code min} if {@code value < min}, {@code max} if {@code value > max},
     *              and {@code value} otherwise.
     */
    public static byte bound(byte min, byte max, byte value)
    {
        if(min>max)
        {
            return bound(max,min,value);
        }
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    /**
     * Clamps the given short {@code value} to be within the inclusive range of {@code [min, max]}.
     * <p>
     * If {@code min} is greater than {@code max}, the bounds are automatically swapped before clamping.
     *
     * @param min   the lower bound of the range (inclusive).
     * @param max   the upper bound of the range (inclusive).
     * @param value the short value to clamp.
     * @return      the clamped value; {@code min} if {@code value < min}, {@code max} if {@code value > max},
     *              and {@code value} otherwise.
     */
    public static short bound(short min, short max, short value)
    {
        if(min>max)
        {
            return bound(max,min,value);
        }
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    /**
     * Clamps the given integer {@code value} to be within the inclusive range of {@code [min, max]}.
     * <p>
     * If {@code min} is greater than {@code max}, the bounds are automatically swapped before clamping.
     *
     * @param min   the lower bound of the range (inclusive).
     * @param max   the upper bound of the range (inclusive).
     * @param value the integer value to clamp.
     * @return      the clamped value; {@code min} if {@code value < min}, {@code max} if {@code value > max},
     *              and {@code value} otherwise.
     */
    public static int bound(int min, int max, int value)
    {
        if(min>max)
        {
            return bound(max,min,value);
        }
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    /**
     * Clamps the given long {@code value} to be within the inclusive range of {@code [min, max]}.
     * <p>
     * If {@code min} is greater than {@code max}, the bounds are automatically swapped before clamping.
     *
     * @param min   the lower bound of the range (inclusive).
     * @param max   the upper bound of the range (inclusive).
     * @param value the long value to clamp.
     * @return      the clamped value; {@code min} if {@code value < min}, {@code max} if {@code value > max},
     *              and {@code value} otherwise.
     */
    public static long bound(long min, long max, long value)
    {
        if(min>max)
        {
            return bound(max,min,value);
        }
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    /**
     * Clamps the given float {@code value} to be within the inclusive range of {@code [min, max]}.
     * <p>
     * If {@code min} is greater than {@code max}, the bounds are automatically swapped before clamping.
     *
     * @param min   the lower bound of the range (inclusive).
     * @param max   the upper bound of the range (inclusive).
     * @param value the float value to clamp.
     * @return      the clamped value; {@code min} if {@code value < min}, {@code max} if {@code value > max},
     *              and {@code value} otherwise.
     */
    public static float bound(float min, float max, float value)
    {
        if(min>max)
        {
            return bound(max,min,value);
        }
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    /**
     * Clamps the given double {@code value} to be within the inclusive range of {@code [min, max]}.
     * <p>
     * If {@code min} is greater than {@code max}, the bounds are automatically swapped before clamping.
     *
     * @param min   the lower bound of the range (inclusive).
     * @param max   the upper bound of the range (inclusive).
     * @param value the double value to clamp.
     * @return      the clamped value; {@code min} if {@code value < min}, {@code max} if {@code value > max},
     *              and {@code value} otherwise.
     */
    public static double bound(double min, double max, double value)
    {
        if(min>max)
        {
            return bound(max,min,value);
        }
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

}
