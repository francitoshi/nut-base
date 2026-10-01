/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

/**
 * Arithmetic operations that saturate at the bounds of the numeric type
 * instead of overflowing and wrapping around.
 * <p>
 * Used for deadline arithmetic on {@link System#nanoTime()}, where a wrapped
 * value would turn an almost infinite wait into an already expired one.
 *
 * @author franci
 */
public class Saturated
{
    private Saturated()
    {
    }

    /**
     * Returns the sum of a and b, saturating at Integer.MAX_VALUE or Integer.MIN_VALUE on overflow.
     *
     * @param a the first addend.
     * @param b the second addend.
     * @return the sum of a and b, or Integer.MAX_VALUE if the result overflows, or Integer.MIN_VALUE if it underflows.
     */
    public static int saturatedAdd(int a, int b)
    {
        int r = a + b;

        if (b > 0 && r < a)
        {
            return Integer.MAX_VALUE;
        }
        if (b < 0 && r > a)
        {
            return Integer.MIN_VALUE;
        }

        return r;
    }

    /**
     * Returns the sum of a and b, saturating at Long.MAX_VALUE or Long.MIN_VALUE on overflow.
     *
     * @param a the first addend.
     * @param b the second addend.
     * @return the sum of a and b, or Long.MAX_VALUE if the result overflows, or Long.MIN_VALUE if it underflows.
     */
    public static long saturatedAdd(long a, long b)
    {
        long r = a + b;

        if (b > 0 && r < a)
        {
            return Long.MAX_VALUE;
        }
        if (b < 0 && r > a)
        {
            return Long.MIN_VALUE;
        }

        return r;
    }

    /**
     * Returns the difference of a and b, saturating at Integer.MAX_VALUE or Integer.MIN_VALUE on overflow.
     *
     * @param a the minuend.
     * @param b the subtrahend.
     * @return the difference of a and b, or Integer.MAX_VALUE if the result overflows, or Integer.MIN_VALUE if it underflows.
     */
    public static int saturatedSubtract(int a, int b)
    {
        int r = a - b;

        if (b < 0 && r < a)
        {
            return Integer.MAX_VALUE;
        }
        if (b > 0 && r > a)
        {
            return Integer.MIN_VALUE;
        }

        return r;
    }

    /**
     * Returns the difference of a and b, saturating at Long.MAX_VALUE or Long.MIN_VALUE on overflow.
     *
     * @param a the minuend.
     * @param b the subtrahend.
     * @return the difference of a and b, or Long.MAX_VALUE if the result overflows, or Long.MIN_VALUE if it underflows.
     */
    public static long saturatedSubtract(long a, long b)
    {
        long r = a - b;

        if (b < 0 && r < a)
        {
            return Long.MAX_VALUE;
        }
        if (b > 0 && r > a)
        {
            return Long.MIN_VALUE;
        }

        return r;
    }

    /**
     * Returns the product of a and b, saturating at Integer.MAX_VALUE or Integer.MIN_VALUE on overflow.
     *
     * @param a the first factor.
     * @param b the second factor.
     * @return the product of a and b, or Integer.MAX_VALUE if the result overflows, or Integer.MIN_VALUE if it underflows.
     */
    public static int saturatedMultiply(int a, int b)
    {
        if (a == 0 || b == 0)
        {
            return 0;
        }

        if (a == -1 && b == Integer.MIN_VALUE)
        {
            return Integer.MAX_VALUE;
        }
        if (b == -1 && a == Integer.MIN_VALUE)
        {
            return Integer.MAX_VALUE;
        }

        int r = a * b;

        if (r / b != a)
        {
            return ((a ^ b) < 0) ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }

        return r;
    }

    /**
     * Returns the product of a and b, saturating at Long.MAX_VALUE or Long.MIN_VALUE on overflow.
     *
     * @param a the first factor.
     * @param b the second factor.
     * @return the product of a and b, or Long.MAX_VALUE if the result overflows, or Long.MIN_VALUE if it underflows.
     */
    public static long saturatedMultiply(long a, long b)
    {
        if (a == 0 || b == 0)
        {
            return 0;
        }

        if (a == -1 && b == Long.MIN_VALUE)
        {
            return Long.MAX_VALUE;
        }
        if (b == -1 && a == Long.MIN_VALUE)
        {
            return Long.MAX_VALUE;
        }

        long r = a * b;

        if (r / b != a)
        {
            return ((a ^ b) < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
        }

        return r;
    }

    /**
     * Returns the negation of a, saturating at Integer.MAX_VALUE if a is Integer.MIN_VALUE.
     *
     * @param a the value to negate.
     * @return the negation of a, or Integer.MAX_VALUE if a is Integer.MIN_VALUE.
     */
    public static int saturatedNegate(int a)
    {
        return a == Integer.MIN_VALUE ? Integer.MAX_VALUE : -a;
    }

    /**
     * Returns the absolute value of a, saturating at Integer.MAX_VALUE if a is Integer.MIN_VALUE.
     *
     * @param a the value.
     * @return the absolute value of a, or Integer.MAX_VALUE if a is Integer.MIN_VALUE.
     */
    public static int saturatedAbs(int a)
    {
        return a == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(a);
    }

    /**
     * Returns the negation of a, saturating at Long.MAX_VALUE if a is Long.MIN_VALUE.
     *
     * @param a the value to negate.
     * @return the negation of a, or Long.MAX_VALUE if a is Long.MIN_VALUE.
     */
    public static long saturatedNegate(long a)
    {
        return a == Long.MIN_VALUE ? Long.MAX_VALUE : -a;
    }

    /**
     * Returns the absolute value of a, saturating at Long.MAX_VALUE if a is Long.MIN_VALUE.
     *
     * @param a the value.
     * @return the absolute value of a, or Long.MAX_VALUE if a is Long.MIN_VALUE.
     */
    public static long saturatedAbs(long a)
    {
        return a == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(a);
    }
}
