/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A collection of static factory and conversion helpers for building common
 * data structures and converting between array types.
 *
 * <p>This class provides concise factory methods for {@link Map}s,
 * {@link List}s, {@link Queue}s, {@link Deque}s and {@link Set}s, as well as
 * converters that transform primitive and boxed arrays into other primitive
 * array types. No instance of this class is ever needed; all methods are
 * {@code static}.</p>
 *
 * @author franci
 */
public class As
{

    /**
     * Builds a map from a key array and a value array.
     *
     * <p>Each key in {@code keys} is paired with the value at the same index in
     * {@code values}. Keys beyond the length of {@code values} are mapped to
     * {@code null}.</p>
     *
     * @param keys the array of keys; must not be shorter than {@code values}
     * @param values the array of values; must not be longer than {@code keys}
     * @return a new {@link HashMap} pairing {@code keys} with {@code values}
     */
    public static <K, V> Map<K, V> map(K[] keys, V[] values)
    {
        assert keys.length >= values.length;

        HashMap<K, V> map = new HashMap<>();

        for (int i = 0; i < values.length; i++)
        {
            map.put(keys[i], values[i]);
        }
        for (int i = values.length; i < keys.length; i++)
        {
            map.put(keys[i], null);
        }
        return map;
    }

    /**
     * Builds a singleton map containing a single key-value pair.
     *
     * @param k1 the key
     * @param v1 the value
     * @return a new {@link HashMap} containing {@code k1 -> v1}
     */
    public static <K, V> Map<K, V> map(K k1, V v1)
    {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    /**
     * Builds a map from two key-value pairs.
     *
     * @param k1 the first key
     * @param v1 the value for {@code k1}
     * @param k2 the second key
     * @param v2 the value for {@code k2}
     * @return a new {@link HashMap} containing the two given pairs
     */
    public static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2)
    {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    /**
     * Builds a map from three key-value pairs.
     *
     * @param k1 the first key
     * @param v1 the value for {@code k1}
     * @param k2 the second key
     * @param v2 the value for {@code k2}
     * @param k3 the third key
     * @param v3 the value for {@code k3}
     * @return a new {@link HashMap} containing the three given pairs
     */
    public static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3)
    {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    /**
     * Builds a map from four key-value pairs.
     *
     * @param k1 the first key
     * @param v1 the value for {@code k1}
     * @param k2 the second key
     * @param v2 the value for {@code k2}
     * @param k3 the third key
     * @param v3 the value for {@code k3}
     * @param k4 the fourth key
     * @param v4 the value for {@code k4}
     * @return a new {@link HashMap} containing the four given pairs
     */
    public static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4)
    {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }

    /**
     * Builds a map from five key-value pairs.
     *
     * @param k1 the first key
     * @param v1 the value for {@code k1}
     * @param k2 the second key
     * @param v2 the value for {@code k2}
     * @param k3 the third key
     * @param v3 the value for {@code k3}
     * @param k4 the fourth key
     * @param v4 the value for {@code k4}
     * @param k5 the fifth key
     * @param v5 the value for {@code k5}
     * @return a new {@link HashMap} containing the five given pairs
     */
    public static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5)
    {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }

    /**
     * Builds a map from six key-value pairs.
     *
     * @param k1 the first key
     * @param v1 the value for {@code k1}
     * @param k2 the second key
     * @param v2 the value for {@code k2}
     * @param k3 the third key
     * @param v3 the value for {@code k3}
     * @param k4 the fourth key
     * @param v4 the value for {@code k4}
     * @param k5 the fifth key
     * @param v5 the value for {@code k5}
     * @param k6 the sixth key
     * @param v6 the value for {@code k6}
     * @return a new {@link HashMap} containing the six given pairs
     */
    public static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6)
    {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        return map;
    }

    /**
     * Builds a map from seven key-value pairs.
     *
     * @param k1 the first key
     * @param v1 the value for {@code k1}
     * @param k2 the second key
     * @param v2 the value for {@code k2}
     * @param k3 the third key
     * @param v3 the value for {@code k3}
     * @param k4 the fourth key
     * @param v4 the value for {@code k4}
     * @param k5 the fifth key
     * @param v5 the value for {@code k5}
     * @param k6 the sixth key
     * @param v6 the value for {@code k6}
     * @param k7 the seventh key
     * @param v7 the value for {@code k7}
     * @return a new {@link HashMap} containing the seven given pairs
     */
    public static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7)
    {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        return map;
    }

    /**
     * Builds a map from eight key-value pairs.
     *
     * @param k1 the first key
     * @param v1 the value for {@code k1}
     * @param k2 the second key
     * @param v2 the value for {@code k2}
     * @param k3 the third key
     * @param v3 the value for {@code k3}
     * @param k4 the fourth key
     * @param v4 the value for {@code k4}
     * @param k5 the fifth key
     * @param v5 the value for {@code k5}
     * @param k6 the sixth key
     * @param v6 the value for {@code k6}
     * @param k7 the seventh key
     * @param v7 the value for {@code k7}
     * @param k8 the eighth key
     * @param v8 the value for {@code k8}
     * @return a new {@link HashMap} containing the eight given pairs
     */
    public static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8)
    {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        return map;
    }

    /**
     * Builds a map from nine key-value pairs.
     *
     * @param k1 the first key
     * @param v1 the value for {@code k1}
     * @param k2 the second key
     * @param v2 the value for {@code k2}
     * @param k3 the third key
     * @param v3 the value for {@code k3}
     * @param k4 the fourth key
     * @param v4 the value for {@code k4}
     * @param k5 the fifth key
     * @param v5 the value for {@code k5}
     * @param k6 the sixth key
     * @param v6 the value for {@code k6}
     * @param k7 the seventh key
     * @param v7 the value for {@code k7}
     * @param k8 the eighth key
     * @param v8 the value for {@code k8}
     * @param k9 the ninth key
     * @param v9 the value for {@code k9}
     * @return a new {@link HashMap} containing the nine given pairs
     */
    public static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9)
    {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        map.put(k9, v9);
        return map;
    }

    /**
     * Wraps the given items into a fixed, array-backed list.
     *
     * @param items the items to place in the list; must not be {@code null}
     * @return a new array-backed {@link List} in the order of {@code items}
     */
    public static <T> List<T> list(T... items)
    {
        return Arrays.asList(items);
    }

    /**
     * Wraps the given items into a FIFO queue.
     *
     * <p>The returned queue is backed by an {@link ArrayDeque}.</p>
     *
     * @param items the items to enqueue; must not be {@code null}
     * @return a new {@link Queue} containing {@code items}
     */
    public static <T> Queue<T> queue(T... items)
    {
        return new ArrayDeque<>(Arrays.asList(items));
    }

    /**
     * Wraps the given items into a double-ended queue.
     *
     * <p>The returned deque is backed by an {@link ArrayDeque}.</p>
     *
     * @param items the items to add; must not be {@code null}
     * @return a new {@link Deque} containing {@code items}
     */
    public static <T> Deque<T> deque(T... items)
    {
        return new ArrayDeque<>(Arrays.asList(items));
    }

    /**
     * Wraps the given items into a bounded, FIFO blocking queue.
     *
     * <p>The returned queue is backed by an {@link ArrayBlockingQueue} whose
     * capacity matches the number of items and which uses a fair access
     * policy.</p>
     *
     * @param items the items to enqueue; must not be {@code null}
     * @return a new {@link BlockingQueue} containing {@code items}
     */
    public static <T> BlockingQueue<T> blockingQueue(T... items)
    {
        return new ArrayBlockingQueue<>(items.length, true, Arrays.asList(items));
    }

    /**
     * Wraps the given items into a blocking double-ended queue.
     *
     * <p>The returned deque is backed by a {@link LinkedBlockingDeque}.</p>
     *
     * @param items the items to add; must not be {@code null}
     * @return a new {@link BlockingDeque} containing {@code items}
     */
    public static <T> BlockingDeque<T> blockingDeque(T... items)
    {
        return new LinkedBlockingDeque<>(Arrays.asList(items));
    }

    /**
     * Wraps the given items into a set.
     *
     * <p>Duplicate items are collapsed since the returned set is backed by a
     * {@link HashSet}.</p>
     *
     * @param items the items to add; must not be {@code null}
     * @return a new {@link Set} containing {@code items}
     */
    public static <T> Set<T> set(T... items)
    {
        return new HashSet<>(Arrays.asList(items));
    }

    /**
     * Converts an array of {@code byte}s to an array of {@code int}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code int} array holding the same values, or {@code null}
     *         if {@code src} is {@code null}
     */
    public static int[] ints(byte[] src)
    {
        if (src == null)
        {
            return null;
        }
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Converts an array of {@code short}s to an array of {@code int}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code int} array holding the same values, or {@code null}
     *         if {@code src} is {@code null}
     */
    public static int[] ints(short[] src)
    {
        if (src == null)
        {
            return null;
        }
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Unboxes an array of {@link Integer}s to an array of {@code int}s.
     *
     * <p>A {@code null} element in {@code src} is replaced by
     * {@code whenNull}.</p>
     *
     * @param src the source array; may be {@code null}
     * @param whenNull the value substituted for each {@code null} element
     * @return a new {@code int} array holding the unboxed values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static int[] ints(Integer[] src, int whenNull)
    {
        if (src == null)
        {
            return null;
        }
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++)
        {
            Integer item = src[i];
            dst[i] = (item != null) ? item : whenNull;
        }
        return dst;
    }

    /**
     * Returns the given {@code int} values unchanged.
     *
     * @param items the values to return
     * @return {@code items} itself
     */
    public static int[] ints(int... items)
    {
        return items;
    }

    /**
     * Parses an array of strings to an array of {@code int}s using the given
     * radix.
     *
     * <p>Empty, {@code null} or unparsable elements are replaced by
     * {@code defaultValue}.</p>
     *
     * @param src the source array; may be {@code null}
     * @param defaultValue the value substituted for unparsable elements
     * @param radix the radix used for {@link Integer#parseInt(String, int)}
     * @return a new {@code int} array holding the parsed values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static int[] ints(String[] src, int defaultValue, int radix)
    {
        if (src == null)
        {
            return null;
        }
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && !src[i].trim().isEmpty())
            {
                try
                {
                    dst[i] = Integer.parseInt(src[i], radix);
                }
                catch (NumberFormatException ex)
                {
                    dst[i] = defaultValue;
                }
            }
            else
            {
                dst[i] = defaultValue;
            }
        }
        return dst;
    }

    /**
     * Parses an array of strings to an array of {@code int}s using radix 10.
     *
     * <p>Empty, {@code null} or unparsable elements are replaced by
     * {@code defaultValue}.</p>
     *
     * @param src the source array; may be {@code null}
     * @param defaultValue the value substituted for unparsable elements
     * @return a new {@code int} array holding the parsed values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static int[] ints(String[] src, int defaultValue)
    {
        return ints(src, defaultValue, 10);
    }

    /**
     * Parses an array of strings to an array of {@code int}s using radix 10
     * and {@code 0} as the default value.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code int} array holding the parsed values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static int[] ints(String[] src)
    {
        return ints(src, 0, 10);
    }

    //----------------------------------------------------------------------------------------------
    /**
     * Converts an array of {@code byte}s to an array of {@code long}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code long} array holding the same values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static long[] longs(byte[] src)
    {
        if (src == null)
        {
            return null;
        }
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Converts an array of {@code short}s to an array of {@code long}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code long} array holding the same values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static long[] longs(short[] src)
    {
        if (src == null)
        {
            return null;
        }
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Converts an array of {@code int}s to an array of {@code long}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code long} array holding the same values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static long[] longs(int[] src)
    {
        if (src == null)
        {
            return null;
        }
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Unboxes an array of {@link Long}s to an array of {@code long}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code long} array holding the unboxed values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static long[] longs(Long[] src)
    {
        if (src == null)
        {
            return null;
        }
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Returns the given {@code long} values unchanged.
     *
     * @param items the values to return
     * @return {@code items} itself
     */
    public static long[] longs(long... items)
    {
        return items;
    }

    /**
     * Parses an array of strings to an array of {@code long}s using the given
     * radix.
     *
     * <p>Empty, {@code null} or unparsable elements are replaced by
     * {@code defaultValue}.</p>
     *
     * @param src the source array; may be {@code null}
     * @param defaultValue the value substituted for unparsable elements
     * @param radix the radix used for {@link Long#parseLong(String, int)}
     * @return a new {@code long} array holding the parsed values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static long[] longs(String[] src, long defaultValue, int radix)
    {
        if (src == null)
        {
            return null;
        }
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && !src[i].trim().isEmpty())
            {
                try
                {
                    dst[i] = Long.parseLong(src[i], radix);
                }
                catch (NumberFormatException ex)
                {
                    dst[i] = defaultValue;
                }
            }
            else
            {
                dst[i] = defaultValue;
            }
        }
        return dst;
    }

    /**
     * Parses an array of strings to an array of {@code long}s using radix 10.
     *
     * <p>Empty, {@code null} or unparsable elements are replaced by
     * {@code defaultValue}.</p>
     *
     * @param src the source array; may be {@code null}
     * @param defaultValue the value substituted for unparsable elements
     * @return a new {@code long} array holding the parsed values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static long[] longs(String[] src, long defaultValue)
    {
        return longs(src, defaultValue, 10);
    }

    /**
     * Parses an array of strings to an array of {@code long}s using radix 10
     * and {@code 0} as the default value.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code long} array holding the parsed values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static long[] longs(String[] src)
    {
        return longs(src, 0, 10);
    }

    //----------------------------------------------------------------------------------------------
    /**
     * Converts an array of {@code byte}s to an array of {@code float}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code float} array holding the same values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static float[] floats(byte[] src)
    {
        if (src == null)
        {
            return null;
        }
        float[] dst = new float[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Converts an array of {@code short}s to an array of {@code float}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code float} array holding the same values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static float[] floats(short[] src)
    {
        if (src == null)
        {
            return null;
        }
        float[] dst = new float[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Converts an array of {@code int}s to an array of {@code float}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code float} array holding the same values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static float[] floats(int[] src)
    {
        if (src == null)
        {
            return null;
        }
        float[] dst = new float[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Converts an array of {@code long}s to an array of {@code float}s.
     *
     * @param values the source array; must not be {@code null}
     * @return a new {@code float} array holding the same values
     */
    public static float[] floats(long[] values)
    {
        float[] ret = new float[values.length];
        for (int i = 0; i < values.length; i++)
        {
            ret[i] = values[i];
        }
        return ret;
    }

    //----------------------------------------------------------------------------------------------
    /**
     * Converts an array of {@code byte}s to an array of {@code double}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code double} array holding the same values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static double[] doubles(byte[] src)
    {
        if (src == null)
        {
            return null;
        }
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Converts an array of {@code short}s to an array of {@code double}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code double} array holding the same values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static double[] doubles(short[] src)
    {
        if (src == null)
        {
            return null;
        }
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Converts an array of {@code int}s to an array of {@code double}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code double} array holding the same values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static double[] doubles(int[] src)
    {
        if (src == null)
        {
            return null;
        }
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Converts an array of {@code long}s to an array of {@code double}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code double} array holding the same values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static double[] doubles(long[] src)
    {
        if (src == null)
        {
            return null;
        }
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Converts an array of {@code float}s to an array of {@code double}s.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code double} array holding the same values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static double[] doubles(float[] src)
    {
        if (src == null)
        {
            return null;
        }
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Returns the given {@code double} values unchanged.
     *
     * @param items the values to return
     * @return {@code items} itself
     */
    public static double[] doubles(double... items)
    {
        return items;
    }

    /**
     * Parses an array of strings to an array of {@code double}s.
     *
     * <p>Empty, {@code null} or unparsable elements are replaced by
     * {@code defaultValue}.</p>
     *
     * @param src the source array; may be {@code null}
     * @param defaultValue the value substituted for unparsable elements
     * @return a new {@code double} array holding the parsed values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static double[] doubles(String[] src, double defaultValue)
    {
        if (src == null)
        {
            return null;
        }
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && !src[i].trim().isEmpty())
            {
                try
                {
                    dst[i] = Double.parseDouble(src[i]);
                }
                catch (NumberFormatException ex)
                {
                    dst[i] = defaultValue;
                }
            }
            else
            {
                dst[i] = defaultValue;
            }
        }
        return dst;
    }

    /**
     * Parses an array of strings to an array of {@code double}s using
     * {@code 0.0} as the default value.
     *
     * @param src the source array; may be {@code null}
     * @return a new {@code double} array holding the parsed values, or
     *         {@code null} if {@code src} is {@code null}
     */
    public static double[] doubles(String[] src)
    {
        return doubles(src, 0.0);
    }

    //----------------------------------------------------------------------------------------------
    /**
     * Converts the given items to an array of {@link String}s via
     * {@link Object#toString()}.
     *
     * <p>A {@code null} element maps to {@code null}.</p>
     *
     * @param src the source array; may be {@code null}
     * @return a new {@link String} array of equal length, or {@code null} if
     *         {@code src} is {@code null}
     */
    public static <T> String[] strings(T... src)
    {
        if (src == null)
            return null;
        String[] dst = new String[src.length];
        for (int i = 0; i < src.length; i++)
        {
            T item = src[i];
            dst[i] = item != null ? item.toString() : null;
        }
        return dst;
    }

    /**
     * Converts the given list of items to an array of {@link String}s via
     * {@link Object#toString()}.
     *
     * <p>A {@code null} element maps to {@code null}.</p>
     *
     * @param src the source list; may be {@code null}
     * @return a new {@link String} array of equal size, or {@code null} if
     *         {@code src} is {@code null}
     */
    public static <T> String[] strings(List<T> src)
    {
        if (src == null)
            return null;
        String[] dst = new String[src.size()];
        for (int i = 0; i < dst.length; i++)
        {
            T item = src.get(i);
            dst[i] = item != null ? item.toString() : null;
        }
        return dst;
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Wraps the given values into an {@link Object} array.
     *
     * @param values the values to return
     * @return {@code values} itself
     */
    public static Object[] asObjects(Object... values)
    {
        return values;
    }

    //----------------------------------------------------------------------------------------------

    /**
     * Converts a two's-complement byte array to a {@link BigInteger}.
     *
     * <p>The array is interpreted as an unsigned magnitude with the sign forced
     * positive.</p>
     *
     * @param b the two's-complement representation; must not be {@code null}
     * @return a non-negative {@link BigInteger} equivalent to {@code b}
     */
    public static BigInteger bigInteger(byte[] b) 
    {
        return new BigInteger(1, b);
    }
    
    //----------------------------------------------------------------------------------------------
    
    /**
     * Returns the given {@link BigInteger} values unchanged.
     *
     * @param values the values to return
     * @return {@code values} itself
     */
    public static BigInteger[] bigIntegers(BigInteger... values)
    {
        return values;
    }
    
    /**
     * Converts a range of {@code byte} values to {@link BigInteger}s by
     * value.
     *
     * @param values the source array; must not be {@code null}
     * @param start the first index, inclusive
     * @param end the last index, exclusive
     * @return a new {@link BigInteger} array holding the values of the range
     */
    public static BigInteger[] bigIntegers(byte[] values, int start, int end)
    {
        BigInteger[] bi = new BigInteger[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigInteger.valueOf(values[j]);
}
        return bi;
    }
    /**
     * Converts all {@code byte} values to {@link BigInteger}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @return a new {@link BigInteger} array holding all the values
     */
    public static BigInteger[] bigIntegers(byte[] values)
    {
        return bigIntegers(values, 0, values.length);
    }
    /**
     * Converts a range of byte arrays to {@link BigInteger}s, interpreting
     * each array as an unsigned magnitude.
     *
     * @param values the source array; must not be {@code null}
     * @param start the first index, inclusive
     * @param end the last index, exclusive
     * @return a new {@link BigInteger} array holding the values of the range
     */
    public static BigInteger[] bigIntegers(byte[][] values, int start, int end)
    {
        BigInteger[] bi = new BigInteger[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = new BigInteger(values[j]);
        }
        return bi;
    }
    /**
     * Converts all byte arrays to {@link BigInteger}s, interpreting each array
     * as an unsigned magnitude.
     *
     * @param values the source array; must not be {@code null}
     * @return a new {@link BigInteger} array holding all the values
     */
    public static BigInteger[] bigIntegers(byte[][] values)
    {
        return bigIntegers(values, 0, values.length);
    }
    /**
     * Converts a range of {@code char} values to {@link BigInteger}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @param start the first index, inclusive
     * @param end the last index, exclusive
     * @return a new {@link BigInteger} array holding the values of the range
     */
    public static BigInteger[] bigIntegers(char[] values, int start, int end)
    {
        BigInteger[] bi = new BigInteger[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigInteger.valueOf(values[j]);
        }
        return bi;
    }
    /**
     * Converts all {@code char} values to {@link BigInteger}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @return a new {@link BigInteger} array holding all the values
     */
    public static BigInteger[] bigIntegers(char[] values)
    {
        return bigIntegers(values, 0, values.length);
    }
    /**
     * Converts a range of {@code int} values to {@link BigInteger}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @param start the first index, inclusive
     * @param end the last index, exclusive
     * @return a new {@link BigInteger} array holding the values of the range
     */
    public static BigInteger[] bigIntegers(int[] values, int start, int end)
    {
        BigInteger[] bi = new BigInteger[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigInteger.valueOf(values[j]);
        }
        return bi;
    }
    /**
     * Converts all {@code int} values to {@link BigInteger}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @return a new {@link BigInteger} array holding all the values
     */
    public static BigInteger[] bigIntegers(int[] values)
    {
        return bigIntegers(values, 0, values.length);
    }
    /**
     * Converts a range of {@code long} values to {@link BigInteger}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @param start the first index, inclusive
     * @param end the last index, exclusive
     * @return a new {@link BigInteger} array holding the values of the range
     */
    public static BigInteger[] bigIntegers(long[] values, int start, int end)
    {
        BigInteger[] bi = new BigInteger[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigInteger.valueOf(values[j]);
        }
        return bi;
    }
    /**
     * Converts all {@code long} values to {@link BigInteger}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @return a new {@link BigInteger} array holding all the values
     */
    public static BigInteger[] bigIntegers(long[] values)
    {
        return bigIntegers(values, 0, values.length);
    }

    /**
     * Returns the given {@link BigDecimal} values unchanged.
     *
     * @param values the values to return
     * @return {@code values} itself
     */
    public static BigDecimal[] bigDecimals(BigDecimal... values)
    {
        return values;
    }
    
    //----------------------------------------------------------------------------------------------

    /**
     * Converts a range of {@code byte} values to {@link BigDecimal}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @param start the first index, inclusive
     * @param end the last index, exclusive
     * @return a new {@link BigDecimal} array holding the values of the range
     */
    public static BigDecimal[] bigDecimals(byte[] values, int start, int end)
    {
        BigDecimal[] bi = new BigDecimal[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigDecimal.valueOf(values[j]);
        }
        return bi;
    }
    /**
     * Converts all {@code byte} values to {@link BigDecimal}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @return a new {@link BigDecimal} array holding all the values
     */
    public static BigDecimal[] bigDecimals(byte[] values)
    {
        return bigDecimals(values, 0, values.length);
    }
    /**
     * Converts a range of {@code char} values to {@link BigDecimal}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @param start the first index, inclusive
     * @param end the last index, exclusive
     * @return a new {@link BigDecimal} array holding the values of the range
     */
    public static BigDecimal[] bigDecimals(char[] values, int start, int end)
    {
        BigDecimal[] bi = new BigDecimal[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigDecimal.valueOf(values[j]);
        }
        return bi;
    }
    /**
     * Converts all {@code char} values to {@link BigDecimal}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @return a new {@link BigDecimal} array holding all the values
     */
    public static BigDecimal[] bigDecimals(char[] values)
    {
        return bigDecimals(values, 0, values.length);
    }
    /**
     * Converts a range of {@code int} values to {@link BigDecimal}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @param start the first index, inclusive
     * @param end the last index, exclusive
     * @return a new {@link BigDecimal} array holding the values of the range
     */
    public static BigDecimal[] bigDecimals(int[] values, int start, int end)
    {
        BigDecimal[] bi = new BigDecimal[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigDecimal.valueOf(values[j]);
        }
        return bi;
    }
    /**
     * Converts all {@code int} values to {@link BigDecimal}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @return a new {@link BigDecimal} array holding all the values
     */
    public static BigDecimal[] bigDecimals(int[] values)
    {
        return bigDecimals(values, 0, values.length);
    }
    /**
     * Converts a range of {@code long} values to {@link BigDecimal}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @param start the first index, inclusive
     * @param end the last index, exclusive
     * @return a new {@link BigDecimal} array holding the values of the range
     */
    public static BigDecimal[] bigDecimals(long[] values, int start, int end)
    {
        BigDecimal[] bi = new BigDecimal[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigDecimal.valueOf(values[j]);
        }
        return bi;
    }
    /**
     * Converts all {@code long} values to {@link BigDecimal}s by value.
     *
     * @param values the source array; must not be {@code null}
     * @return a new {@link BigDecimal} array holding all the values
     */
    public static BigDecimal[] bigDecimals(long[] values)
    {
        return bigDecimals(values, 0, values.length);
    }
}
