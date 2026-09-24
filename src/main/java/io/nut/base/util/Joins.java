/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author franci
 */
public abstract class Joins
{
    /**
     * Concatenates the given {@code byte} arrays into one new array, skipping
     * any {@code null} array.
     *
     * @param src the arrays to concatenate; may contain {@code null} entries
     * @return a new array with the concatenation of all non-null {@code src}
     */
    public static byte[] join(byte[]... src)
    {
        int count = 0;
        for (byte[] item : src)
        {
            if (item != null)
            {
                count += item.length;
            }
        }
        ByteBuffer dst = ByteBuffer.allocate(count);
        for (byte[] item : src)
        {
            if (item != null && item.length > 0)
            {
                dst.put(item);
            }
        }
        return dst.array();
    }

    /**
     * Concatenates the given {@code int} arrays into one new array, skipping
     * any {@code null} array.
     *
     * @param src the arrays to concatenate; may contain {@code null} entries
     * @return a new array with the concatenation of all non-null {@code src}
     */
    public static int[] join(int[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        int[] dst = new int[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    /**
     * Concatenates the given {@code long} arrays into one new array, skipping
     * any {@code null} array.
     *
     * @param src the arrays to concatenate; may contain {@code null} entries
     * @return a new array with the concatenation of all non-null {@code src}
     */
    public static long[] join(long[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        long[] dst = new long[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    /**
     * Concatenates the given {@code short} arrays into one new array, skipping
     * any {@code null} array.
     *
     * @param src the arrays to concatenate; may contain {@code null} entries
     * @return a new array with the concatenation of all non-null {@code src}
     */
    public static short[] join(short[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        short[] dst = new short[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    /**
     * Concatenates the given {@code char} arrays into one new array, skipping
     * any {@code null} array.
     *
     * @param src the arrays to concatenate; may contain {@code null} entries
     * @return a new array with the concatenation of all non-null {@code src}
     */
    public static char[] join(char[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        char[] dst = new char[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    /**
     * Concatenates the given {@code float} arrays into one new array, skipping
     * any {@code null} array.
     *
     * @param src the arrays to concatenate; may contain {@code null} entries
     * @return a new array with the concatenation of all non-null {@code src}
     */
    public static float[] join(float[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        float[] dst = new float[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    /**
     * Concatenates the given {@code double} arrays into one new array,
     * skipping any {@code null} array.
     *
     * @param src the arrays to concatenate; may contain {@code null} entries
     * @return a new array with the concatenation of all non-null {@code src}
     */
    public static double[] join(double[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        double[] dst = new double[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    /**
     * Concatenates the given {@link String} arrays into one new array,
     * skipping any {@code null} array.
     *
     * @param src the arrays to concatenate; may contain {@code null} entries
     * @return a new array with the concatenation of all non-null {@code src}
     */
    public static String[] join(String[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        String[] dst = new String[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }
    /**
     * Concatenates the given arrays of type {@code cls} into one new array,
     * skipping any {@code null} array.
     *
     * @deprecated prefer {@link #join(Object[]...)} when the component type can
     * be inferred from the given arrays
     * @param cls the component type of the resulting array
     * @param src the arrays to concatenate; may contain {@code null} entries
     * @return a new array with the concatenation of all non-null {@code src}
     */
    @Deprecated
    public static <E> E[] join(Class<E> cls, E[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        E[] dst = (E[]) Array.newInstance(cls, count);
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    /**
     * Concatenates the given arrays into one new array, skipping any
     * {@code null} array.
     *
     * @param src the arrays to concatenate; may contain {@code null} entries
     * @return a new array with the concatenation of all non-null {@code src},
     * or {@code null} if {@code src} is null or all the given arrays are null
     */
    public static <E> E[] join(E[] ... src)
    {
        if (src == null)
        {
            return null;
        }
        int size = 0;
        E[] dst = null;
        for (E[] item : src)
        {
            if (item != null)
            {
                size += item.length;
                if (dst==null)
                {
                    dst = Arrays.copyOf(item, 0);
                }
            }
        }
        if(dst ==null)
        {
            return null;
        }
        dst = Arrays.copyOf(dst,size);
        for (int i = 0, w = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                for (E item : src[i])
                {
                    dst[w++] = item;
                }
            }
        }
        return dst;
    }
    
    /**
     * Concatenates the given {@link String}s, skipping any {@code null}
     * string.
     *
     * @param src the strings to concatenate; may contain {@code null} entries
     * @return a new {@link String} with the concatenation of all non-null
     *         {@code src}
     */
    public static String join(String... src)
    {
        StringBuilder dst = new StringBuilder();
        for (String src1 : src)
        {
            if (src1 != null)
            {
                dst.append(src1);
            }
        }
        return dst.toString();
    }

    /**
     * Merges the given {@link List}s into one new list, skipping any
     * {@code null} list.
     *
     * @param src the lists to merge; may contain {@code null} entries
     * @return a new {@link List} with the elements of all non-null
     *         {@code src}, in order
     */
    public static <E> List<E> join(List<E>... src)
    {
        List<E> dst = new ArrayList<>();
        for (List<E> src1 : src)
        {
            if (src1 != null)
            {
                dst.addAll(src1);
            }
        }
        return dst;
    }
    
    /**
     * Concatenate a series of elements to a byte[] array.
     * @param src the starting array
     * @param next the values to concatenate
     * @return a new array with the resulting array
     */
    public static byte[] cat(byte[] src, byte... next)
    {
        return Joins.join(src, next);
    }
    /**
     * Concatenate a series of elements to an int[] array.
     * @param src the starting array
     * @param next the values to concatenate
     * @return a new array with the resulting array
     */
    public static int[] cat(int[] src, int... next)
    {
        return Joins.join(src, next);
    }
    /**
     * Concatenate a series of elements to a short[] array.
     * @param src the starting array
     * @param next the values to concatenate
     * @return a new array with the resulting array
     */
    public static short[] cat(short[] src, short... next)
    {
        return Joins.join(src, next);
    }
    /**
     * Concatenate a series of elements to a char[] array.
     * @param src the starting array
     * @param next the values to concatenate
     * @return a new array with the resulting array
     */
    public static char[] cat(char[] src, char... next)
    {
        return Joins.join(src, next);
    }
    /**
     * Concatenate a series of elements to a long[] array.
     * @param src the starting array
     * @param next the values to concatenate
     * @return a new array with the resulting array
     */
    public static long[] cat(long[] src, long... next)
    {
        return Joins.join(src, next);
    }
    /**
     * Concatenate a series of elements to a float[] array.
     * @param src the starting array
     * @param next the values to concatenate
     * @return a new array with the resulting array
     */
    public static float[] cat(float[] src, float... next)
    {
        return Joins.join(src, next);
    }
    /**
     * Concatenate a series of elements to a double[] array.
     * @param src the starting array
     * @param next the values to concatenate
     * @return a new array with the resulting array
     */
    public static double[] cat(double[] src, double... next)
    {
        return Joins.join(src, next);
    }
    /**
     * Concatenate a series of elements to an E[] array.
     * @param src the starting array
     * @param next the values to concatenate
     * @return a new array with the resulting array
     */
    public static <E> E[] cat(E[] src, E... next)
    {
        return Joins.join(src, next);
    }
    
}
