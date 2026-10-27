/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.lang;

import java.util.Arrays;
import java.util.HashSet;

/**
 *
 * @author franci
 */
public class Unique
{
    
    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static <T> T[] objects(T... items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<T> set = new HashSet<>(items.length);
        int count = 0;
        for (T item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static byte[] bytes(byte[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Byte> set = new HashSet<>(items.length);
        int count = 0;
        for (byte item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static short[] shorts(short[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Short> set = new HashSet<>(items.length);
        int count = 0;
        for (short item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static char[] chars(char[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Character> set = new HashSet<>(items.length);
        int count = 0;
        for (char item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static int[] ints(int[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Integer> set = new HashSet<>(items.length);
        int count = 0;
        for (int item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static long[] longs(long[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Long> set = new HashSet<>(items.length);
        int count = 0;
        for (long item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static double[] doubles(double[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Double> set = new HashSet<>(items.length);
        int count = 0;
        for (double item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static float[] floats(float[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Float> set = new HashSet<>(items.length);
        int count = 0;
        for (float item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

}
