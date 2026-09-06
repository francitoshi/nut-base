/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeSet;
import java.util.WeakHashMap;

/**
 * QuickSelect — gets the k-th element (0-indexed, ascending order) of an array
 * without fully sorting it, using the Quickselect (Hoare's selection) algorithm
 * with a random pivot to avoid the pathological O(n^2) case on already sorted
 * or adversarial arrays.
 *
 * WORK REUSE ACROSS CALLS
 * ----------------------- Each time Quickselect places a pivot into its final
 * position, that position becomes "confirmed": everything to its left is less
 * than or equal to it, and everything to its right is greater than or equal to
 * it (just like in a fully sorted array). This class remembers, for each array
 * instance (object identity, not content), the set of already confirmed indices
 * from previous calls.
 *
 * When a new k is requested on the same array:
 * <ul>
 *   <li>If k was already confirmed, a[k] is returned directly without touching
 *       the array.</li>
 *   <li>Otherwise, the nearest confirmed index immediately below and above k
 *       are looked up. Since the array is already consistently partitioned
 *       between those two indices, the Quickselect search is bounded to that
 *       range instead of traversing the whole array.</li>
 * </ul>
 *
 * This way successive calls on the same array (e.g. asking for the median and
 * then the 90th percentile) reuse the already done partition work instead of
 * repeating it from scratch.
 *
 * The array -&gt; confirmed indices association is kept in a WeakHashMap so that arrays
 * no longer used elsewhere are not retained in memory.
 *
 * IMPORTANT: if the array is modified outside this class (e.g. manually sorted,
 * new data inserted, etc.), the cached information must be invalidated by calling
 * {@link #invalidate(Object)}, otherwise results may be incorrect.
 *
 * CONVENIENCE METHODS
 * ------------------- Besides selecting a single position, this class offers
 * {@link #lowest(int[], int)} / {@link #highest(int[], int)} to retrieve the k
 * smallest / k largest values in no particular order, and the sorted variants
 * {@link #lowestSorted(int[], int)} / {@link #highestSorted(int[], int)}. The
 * unsorted variants only perform the partitions strictly needed to isolate the
 * k values (no full sort), while the sorted variants additionally sort those
 * k values only, never the whole array.
 *
 * @author franci
 */
public final class QuickSelect
{

    private QuickSelect()
    {
    }

    private static final Random RANDOM = new Random();

    /**
     * array (by identity) -&gt; ordered set of already confirmed indices.
     */
    private static final Map<Object,NavigableSet<Integer>> CONFIRMED = Collections.synchronizedMap(new WeakHashMap<Object,NavigableSet<Integer>>());

    private static NavigableSet<Integer> confirmedFor(Object array)
    {
        synchronized (CONFIRMED)
        {
            NavigableSet<Integer> set = CONFIRMED.get(array);
            if (set == null)
            {
                set = new TreeSet<>();
                CONFIRMED.put(array, set);
            }
            return set;
        }
    }

    /**
     * Computes the range [lo, hi] in which k still needs to be searched, based
     * on the indices already confirmed by previous calls.
     */
    private static int[] boundsFor(NavigableSet<Integer> confirmed, int k, int length)
    {
        Integer lowerConfirmed = confirmed.lower(k);   // last confirmed < k
        Integer upperConfirmed = confirmed.higher(k);  // first confirmed > k
        int lo = (lowerConfirmed == null) ? 0 : lowerConfirmed + 1;
        int hi = (upperConfirmed == null) ? length - 1 : upperConfirmed - 1;
        return new int[]{ lo, hi };
    }

    private static void checkIndex(int k, int length)
    {
        if (length == 0)
        {
            throw new IllegalArgumentException("The array is empty");
        }
        if (k < 0 || k >= length)
        {
            throw new IndexOutOfBoundsException("k=" + k + " is outside the range [0, " + (length - 1) + "]");
        }
    }

    private static void checkCount(int k, int length)
    {
        if (k < 0 || k > length)
        {
            throw new IllegalArgumentException("k=" + k + " is outside the range [0, " + length + "]");
        }
    }

    /**
     * Forgets any confirmed partitions information for this array.
     * It must be called if the array is modified externally to this class, to
     * force the next call to select() to partition from scratch again.
     *
     * @param array the array whose cached information must be discarded.
     */
    public static void invalidate(Object array)
    {
        CONFIRMED.remove(array);
    }

    // ================= int =================

    /**
     * Returns the k-th smallest int of the given array, in ascending order.
     *
     * @param a the array.
     * @param k the 0-based selection index.
     * @return the k-th smallest int of the array.
     * @throws IllegalArgumentException if a is empty.
     * @throws IndexOutOfBoundsException if k is outside [0, a.length-1].
     */
    public static int select(int[] a, int k)
    {
        checkIndex(k, a.length);
        NavigableSet<Integer> confirmed = confirmedFor(a);
        if (confirmed.contains(k))
        {
            return a[k];
        }
        int[] bounds = boundsFor(confirmed, k, a.length);
        int lo = bounds[0], hi = bounds[1];
        while (true)
        {
            if (lo == hi)
            {
                confirmed.add(lo);
                return a[lo];
            }
            int pivotIndex = lo + RANDOM.nextInt(hi - lo + 1);
            int p = partition(a, lo, hi, pivotIndex);
            confirmed.add(p);
            if (k == p)
            {
                return a[p];
            }
            if (k < p)
            {
                hi = p - 1;
            }
            else
            {
                lo = p + 1;
            }
        }
    }

    private static int partition(int[] a, int lo, int hi, int pivotIndex)
    {
        int pivotValue = a[pivotIndex];
        swap(a, pivotIndex, hi);
        int store = lo;
        for (int i = lo; i < hi; i++)
        {
            if (a[i] < pivotValue)
            {
                swap(a, i, store);
                store++;
            }
        }
        swap(a, store, hi);
        return store;
    }

    private static void swap(int[] a, int i, int j)
    {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    /**
     * Returns the k smallest ints of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest ints of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static int[] lowest(int[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new int[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, k - 1);
        return Arrays.copyOf(a, k);
    }

    /**
     * Returns the k largest ints of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest ints of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static int[] highest(int[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new int[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, a.length - k);
        return Arrays.copyOfRange(a, a.length - k, a.length);
    }

    /**
     * Returns the k smallest ints of the given array, sorted in ascending order.
     * The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest ints of a, ascending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static int[] lowestSorted(int[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new int[0];
        }
        int[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, k - 1);
            r = Arrays.copyOf(a, k);
        }
        Arrays.sort(r);
        return r;
    }

    /**
     * Returns the k largest ints of the given array, sorted in descending order
     * (largest first). The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest ints of a, descending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static int[] highestSorted(int[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new int[0];
        }
        int[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, a.length - k);
            r = Arrays.copyOfRange(a, a.length - k, a.length);
        }
        Arrays.sort(r);
        reverse(r);
        return r;
    }

    private static void reverse(int[] a)
    {
        for (int i = 0, j = a.length - 1; i < j; i++, j--)
        {
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    // ================= long =================

    /**
     * Returns the k-th smallest long of the given array, in ascending order.
     *
     * @param a the array.
     * @param k the 0-based selection index.
     * @return the k-th smallest long of the array.
     * @throws IllegalArgumentException if a is empty.
     * @throws IndexOutOfBoundsException if k is outside [0, a.length-1].
     */
    public static long select(long[] a, int k)
    {
        checkIndex(k, a.length);
        NavigableSet<Integer> confirmed = confirmedFor(a);
        if (confirmed.contains(k))
        {
            return a[k];
        }
        int[] bounds = boundsFor(confirmed, k, a.length);
        int lo = bounds[0], hi = bounds[1];
        while (true)
        {
            if (lo == hi)
            {
                confirmed.add(lo);
                return a[lo];
            }
            int pivotIndex = lo + RANDOM.nextInt(hi - lo + 1);
            int p = partition(a, lo, hi, pivotIndex);
            confirmed.add(p);
            if (k == p)
            {
                return a[p];
            }
            if (k < p)
            {
                hi = p - 1;
            }
            else
            {
                lo = p + 1;
            }
        }
    }

    private static int partition(long[] a, int lo, int hi, int pivotIndex)
    {
        long pivotValue = a[pivotIndex];
        swap(a, pivotIndex, hi);
        int store = lo;
        for (int i = lo; i < hi; i++)
        {
            if (a[i] < pivotValue)
            {
                swap(a, i, store);
                store++;
            }
        }
        swap(a, store, hi);
        return store;
    }

    private static void swap(long[] a, int i, int j)
    {
        long t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    /**
     * Returns the k smallest longs of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest longs of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static long[] lowest(long[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new long[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, k - 1);
        return Arrays.copyOf(a, k);
    }

    /**
     * Returns the k largest longs of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest longs of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static long[] highest(long[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new long[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, a.length - k);
        return Arrays.copyOfRange(a, a.length - k, a.length);
    }

    /**
     * Returns the k smallest longs of the given array, sorted in ascending
     * order. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest longs of a, ascending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static long[] lowestSorted(long[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new long[0];
        }
        long[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, k - 1);
            r = Arrays.copyOf(a, k);
        }
        Arrays.sort(r);
        return r;
    }

    /**
     * Returns the k largest longs of the given array, sorted in descending
     * order (largest first). The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest longs of a, descending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static long[] highestSorted(long[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new long[0];
        }
        long[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, a.length - k);
            r = Arrays.copyOfRange(a, a.length - k, a.length);
        }
        Arrays.sort(r);
        reverse(r);
        return r;
    }

    private static void reverse(long[] a)
    {
        for (int i = 0, j = a.length - 1; i < j; i++, j--)
        {
            long t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    // ================= double =================

    /**
     * Returns the k-th smallest double of the given array, in ascending order.
     *
     * @param a the array.
     * @param k the 0-based selection index.
     * @return the k-th smallest double of the array.
     * @throws IllegalArgumentException if a is empty.
     * @throws IndexOutOfBoundsException if k is outside [0, a.length-1].
     */
    public static double select(double[] a, int k)
    {
        checkIndex(k, a.length);
        NavigableSet<Integer> confirmed = confirmedFor(a);
        if (confirmed.contains(k))
        {
            return a[k];
        }
        int[] bounds = boundsFor(confirmed, k, a.length);
        int lo = bounds[0], hi = bounds[1];
        while (true)
        {
            if (lo == hi)
            {
                confirmed.add(lo);
                return a[lo];
            }
            int pivotIndex = lo + RANDOM.nextInt(hi - lo + 1);
            int p = partition(a, lo, hi, pivotIndex);
            confirmed.add(p);
            if (k == p)
            {
                return a[p];
            }
            if (k < p)
            {
                hi = p - 1;
            }
            else
            {
                lo = p + 1;
            }
        }
    }

    private static int partition(double[] a, int lo, int hi, int pivotIndex)
    {
        double pivotValue = a[pivotIndex];
        swap(a, pivotIndex, hi);
        int store = lo;
        for (int i = lo; i < hi; i++)
        {
            if (a[i] < pivotValue)
            {
                swap(a, i, store);
                store++;
            }
        }
        swap(a, store, hi);
        return store;
    }

    private static void swap(double[] a, int i, int j)
    {
        double t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    /**
     * Returns the k smallest doubles of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest doubles of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static double[] lowest(double[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new double[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, k - 1);
        return Arrays.copyOf(a, k);
    }

    /**
     * Returns the k largest doubles of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest doubles of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static double[] highest(double[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new double[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, a.length - k);
        return Arrays.copyOfRange(a, a.length - k, a.length);
    }

    /**
     * Returns the k smallest doubles of the given array, sorted in ascending
     * order. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest doubles of a, ascending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static double[] lowestSorted(double[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new double[0];
        }
        double[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, k - 1);
            r = Arrays.copyOf(a, k);
        }
        Arrays.sort(r);
        return r;
    }

    /**
     * Returns the k largest doubles of the given array, sorted in descending
     * order (largest first). The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest doubles of a, descending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static double[] highestSorted(double[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new double[0];
        }
        double[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, a.length - k);
            r = Arrays.copyOfRange(a, a.length - k, a.length);
        }
        Arrays.sort(r);
        reverse(r);
        return r;
    }

    private static void reverse(double[] a)
    {
        for (int i = 0, j = a.length - 1; i < j; i++, j--)
        {
            double t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    // ================= float =================

    /**
     * Returns the k-th smallest float of the given array, in ascending order.
     *
     * @param a the array.
     * @param k the 0-based selection index.
     * @return the k-th smallest float of the array.
     * @throws IllegalArgumentException if a is empty.
     * @throws IndexOutOfBoundsException if k is outside [0, a.length-1].
     */
    public static float select(float[] a, int k)
    {
        checkIndex(k, a.length);
        NavigableSet<Integer> confirmed = confirmedFor(a);
        if (confirmed.contains(k))
        {
            return a[k];
        }
        int[] bounds = boundsFor(confirmed, k, a.length);
        int lo = bounds[0], hi = bounds[1];
        while (true)
        {
            if (lo == hi)
            {
                confirmed.add(lo);
                return a[lo];
            }
            int pivotIndex = lo + RANDOM.nextInt(hi - lo + 1);
            int p = partition(a, lo, hi, pivotIndex);
            confirmed.add(p);
            if (k == p)
            {
                return a[p];
            }
            if (k < p)
            {
                hi = p - 1;
            }
            else
            {
                lo = p + 1;
            }
        }
    }

    private static int partition(float[] a, int lo, int hi, int pivotIndex)
    {
        float pivotValue = a[pivotIndex];
        swap(a, pivotIndex, hi);
        int store = lo;
        for (int i = lo; i < hi; i++)
        {
            if (a[i] < pivotValue)
            {
                swap(a, i, store);
                store++;
            }
        }
        swap(a, store, hi);
        return store;
    }

    private static void swap(float[] a, int i, int j)
    {
        float t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    /**
     * Returns the k smallest floats of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest floats of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static float[] lowest(float[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new float[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, k - 1);
        return Arrays.copyOf(a, k);
    }

    /**
     * Returns the k largest floats of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest floats of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static float[] highest(float[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new float[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, a.length - k);
        return Arrays.copyOfRange(a, a.length - k, a.length);
    }

    /**
     * Returns the k smallest floats of the given array, sorted in ascending
     * order. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest floats of a, ascending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static float[] lowestSorted(float[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new float[0];
        }
        float[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, k - 1);
            r = Arrays.copyOf(a, k);
        }
        Arrays.sort(r);
        return r;
    }

    /**
     * Returns the k largest floats of the given array, sorted in descending
     * order (largest first). The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest floats of a, descending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static float[] highestSorted(float[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new float[0];
        }
        float[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, a.length - k);
            r = Arrays.copyOfRange(a, a.length - k, a.length);
        }
        Arrays.sort(r);
        reverse(r);
        return r;
    }

    private static void reverse(float[] a)
    {
        for (int i = 0, j = a.length - 1; i < j; i++, j--)
        {
            float t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    // ================= short =================

    /**
     * Returns the k-th smallest short of the given array, in ascending order.
     *
     * @param a the array.
     * @param k the 0-based selection index.
     * @return the k-th smallest short of the array.
     * @throws IllegalArgumentException if a is empty.
     * @throws IndexOutOfBoundsException if k is outside [0, a.length-1].
     */
    public static short select(short[] a, int k)
    {
        checkIndex(k, a.length);
        NavigableSet<Integer> confirmed = confirmedFor(a);
        if (confirmed.contains(k))
        {
            return a[k];
        }
        int[] bounds = boundsFor(confirmed, k, a.length);
        int lo = bounds[0], hi = bounds[1];
        while (true)
        {
            if (lo == hi)
            {
                confirmed.add(lo);
                return a[lo];
            }
            int pivotIndex = lo + RANDOM.nextInt(hi - lo + 1);
            int p = partition(a, lo, hi, pivotIndex);
            confirmed.add(p);
            if (k == p)
            {
                return a[p];
            }
            if (k < p)
            {
                hi = p - 1;
            }
            else
            {
                lo = p + 1;
            }
        }
    }

    private static int partition(short[] a, int lo, int hi, int pivotIndex)
    {
        short pivotValue = a[pivotIndex];
        swap(a, pivotIndex, hi);
        int store = lo;
        for (int i = lo; i < hi; i++)
        {
            if (a[i] < pivotValue)
            {
                swap(a, i, store);
                store++;
            }
        }
        swap(a, store, hi);
        return store;
    }

    private static void swap(short[] a, int i, int j)
    {
        short t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    /**
     * Returns the k smallest shorts of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest shorts of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static short[] lowest(short[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new short[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, k - 1);
        return Arrays.copyOf(a, k);
    }

    /**
     * Returns the k largest shorts of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest shorts of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static short[] highest(short[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new short[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, a.length - k);
        return Arrays.copyOfRange(a, a.length - k, a.length);
    }

    /**
     * Returns the k smallest shorts of the given array, sorted in ascending
     * order. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest shorts of a, ascending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static short[] lowestSorted(short[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new short[0];
        }
        short[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, k - 1);
            r = Arrays.copyOf(a, k);
        }
        Arrays.sort(r);
        return r;
    }

    /**
     * Returns the k largest shorts of the given array, sorted in descending
     * order (largest first). The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest shorts of a, descending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static short[] highestSorted(short[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new short[0];
        }
        short[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, a.length - k);
            r = Arrays.copyOfRange(a, a.length - k, a.length);
        }
        Arrays.sort(r);
        reverse(r);
        return r;
    }

    private static void reverse(short[] a)
    {
        for (int i = 0, j = a.length - 1; i < j; i++, j--)
        {
            short t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    // ================= byte =================

    /**
     * Returns the k-th smallest byte of the given array, in ascending order.
     *
     * @param a the array.
     * @param k the 0-based selection index.
     * @return the k-th smallest byte of the array.
     * @throws IllegalArgumentException if a is empty.
     * @throws IndexOutOfBoundsException if k is outside [0, a.length-1].
     */
    public static byte select(byte[] a, int k)
    {
        checkIndex(k, a.length);
        NavigableSet<Integer> confirmed = confirmedFor(a);
        if (confirmed.contains(k))
        {
            return a[k];
        }
        int[] bounds = boundsFor(confirmed, k, a.length);
        int lo = bounds[0], hi = bounds[1];
        while (true)
        {
            if (lo == hi)
            {
                confirmed.add(lo);
                return a[lo];
            }
            int pivotIndex = lo + RANDOM.nextInt(hi - lo + 1);
            int p = partition(a, lo, hi, pivotIndex);
            confirmed.add(p);
            if (k == p)
            {
                return a[p];
            }
            if (k < p)
            {
                hi = p - 1;
            }
            else
            {
                lo = p + 1;
            }
        }
    }

    private static int partition(byte[] a, int lo, int hi, int pivotIndex)
    {
        byte pivotValue = a[pivotIndex];
        swap(a, pivotIndex, hi);
        int store = lo;
        for (int i = lo; i < hi; i++)
        {
            if (a[i] < pivotValue)
            {
                swap(a, i, store);
                store++;
            }
        }
        swap(a, store, hi);
        return store;
    }

    private static void swap(byte[] a, int i, int j)
    {
        byte t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    /**
     * Returns the k smallest bytes of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest bytes of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static byte[] lowest(byte[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new byte[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, k - 1);
        return Arrays.copyOf(a, k);
    }

    /**
     * Returns the k largest bytes of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest bytes of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static byte[] highest(byte[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new byte[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, a.length - k);
        return Arrays.copyOfRange(a, a.length - k, a.length);
    }

    /**
     * Returns the k smallest bytes of the given array, sorted in ascending
     * order. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest bytes of a, ascending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static byte[] lowestSorted(byte[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new byte[0];
        }
        byte[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, k - 1);
            r = Arrays.copyOf(a, k);
        }
        Arrays.sort(r);
        return r;
    }

    /**
     * Returns the k largest bytes of the given array, sorted in descending
     * order (largest first). The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest bytes of a, descending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static byte[] highestSorted(byte[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new byte[0];
        }
        byte[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, a.length - k);
            r = Arrays.copyOfRange(a, a.length - k, a.length);
        }
        Arrays.sort(r);
        reverse(r);
        return r;
    }

    private static void reverse(byte[] a)
    {
        for (int i = 0, j = a.length - 1; i < j; i++, j--)
        {
            byte t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    // ================= char =================

    /**
     * Returns the k-th smallest char of the given array, in ascending order.
     *
     * @param a the array.
     * @param k the 0-based selection index.
     * @return the k-th smallest char of the array.
     * @throws IllegalArgumentException if a is empty.
     * @throws IndexOutOfBoundsException if k is outside [0, a.length-1].
     */
    public static char select(char[] a, int k)
    {
        checkIndex(k, a.length);
        NavigableSet<Integer> confirmed = confirmedFor(a);
        if (confirmed.contains(k))
        {
            return a[k];
        }
        int[] bounds = boundsFor(confirmed, k, a.length);
        int lo = bounds[0], hi = bounds[1];
        while (true)
        {
            if (lo == hi)
            {
                confirmed.add(lo);
                return a[lo];
            }
            int pivotIndex = lo + RANDOM.nextInt(hi - lo + 1);
            int p = partition(a, lo, hi, pivotIndex);
            confirmed.add(p);
            if (k == p)
            {
                return a[p];
            }
            if (k < p)
            {
                hi = p - 1;
            }
            else
            {
                lo = p + 1;
            }
        }
    }

    private static int partition(char[] a, int lo, int hi, int pivotIndex)
    {
        char pivotValue = a[pivotIndex];
        swap(a, pivotIndex, hi);
        int store = lo;
        for (int i = lo; i < hi; i++)
        {
            if (a[i] < pivotValue)
            {
                swap(a, i, store);
                store++;
            }
        }
        swap(a, store, hi);
        return store;
    }

    private static void swap(char[] a, int i, int j)
    {
        char t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    /**
     * Returns the k smallest chars of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest chars of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static char[] lowest(char[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new char[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, k - 1);
        return Arrays.copyOf(a, k);
    }

    /**
     * Returns the k largest chars of the given array, in no particular order.
     * Only the partitions needed to isolate them are performed, the array is
     * never fully sorted. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest chars of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static char[] highest(char[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new char[0];
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, a.length - k);
        return Arrays.copyOfRange(a, a.length - k, a.length);
    }

    /**
     * Returns the k smallest chars of the given array, sorted in ascending
     * order. The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest chars of a, ascending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static char[] lowestSorted(char[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new char[0];
        }
        char[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, k - 1);
            r = Arrays.copyOf(a, k);
        }
        Arrays.sort(r);
        return r;
    }

    /**
     * Returns the k largest chars of the given array, sorted in descending
     * order (largest first). The given array may be rearranged.
     *
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest chars of a, descending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static char[] highestSorted(char[] a, int k)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return new char[0];
        }
        char[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, a.length - k);
            r = Arrays.copyOfRange(a, a.length - k, a.length);
        }
        Arrays.sort(r);
        reverse(r);
        return r;
    }

    private static void reverse(char[] a)
    {
        for (int i = 0, j = a.length - 1; i < j; i++, j--)
        {
            char t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    // ================= generic (Comparable) =================

    /**
     * Returns the k-th smallest element of the given array, in natural
     * ascending order.
     *
     * @param <T> the type of the elements, which must be comparable.
     * @param a the array.
     * @param k the 0-based selection index.
     * @return the k-th smallest element of the array.
     * @throws IllegalArgumentException if a is empty.
     * @throws IndexOutOfBoundsException if k is outside [0, a.length-1].
     */
    public static <T extends Comparable<? super T>> T select(T[] a, int k)
    {
        return select(a, k, Comparator.naturalOrder());
    }

    /**
     * Returns the k smallest elements of the given array, in natural order, in
     * no particular order. Only the partitions needed to isolate them are
     * performed, the array is never fully sorted. The given array may be
     * rearranged.
     *
     * @param <T> the type of the elements, which must be comparable.
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest elements of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static <T extends Comparable<? super T>> T[] lowest(T[] a, int k)
    {
        return lowest(a, k, Comparator.naturalOrder());
    }

    /**
     * Returns the k largest elements of the given array, in natural order, in
     * no particular order. Only the partitions needed to isolate them are
     * performed, the array is never fully sorted. The given array may be
     * rearranged.
     *
     * @param <T> the type of the elements, which must be comparable.
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest elements of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static <T extends Comparable<? super T>> T[] highest(T[] a, int k)
    {
        return highest(a, k, Comparator.naturalOrder());
    }

    /**
     * Returns the k smallest elements of the given array, in natural order,
     * sorted in ascending order. The given array may be rearranged.
     *
     * @param <T> the type of the elements, which must be comparable.
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k smallest elements of a, ascending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static <T extends Comparable<? super T>> T[] lowestSorted(T[] a, int k)
    {
        return lowestSorted(a, k, Comparator.naturalOrder());
    }

    /**
     * Returns the k largest elements of the given array, in natural order,
     * sorted in descending order (largest first). The given array may be
     * rearranged.
     *
     * @param <T> the type of the elements, which must be comparable.
     * @param a the array.
     * @param k the number of elements to return.
     * @return a new array with the k largest elements of a, descending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     */
    public static <T extends Comparable<? super T>> T[] highestSorted(T[] a, int k)
    {
        return highestSorted(a, k, Comparator.naturalOrder());
    }

    // ================= generic (Comparator) =================

    /**
     * Returns the k-th smallest element of the given array, according to the
     * given comparator.
     *
     * @param <T> the type of the elements.
     * @param a the array.
     * @param k the 0-based selection index.
     * @param comparator the comparator used to order the elements.
     * @return the k-th smallest element of the array.
     * @throws IllegalArgumentException if a is empty.
     * @throws IndexOutOfBoundsException if k is outside [0, a.length-1].
     * @throws NullPointerException if comparator is null.
     */
    public static <T> T select(T[] a, int k, Comparator<? super T> comparator)
    {
        checkIndex(k, a.length);
        NavigableSet<Integer> confirmed = confirmedFor(a);
        if (confirmed.contains(k))
        {
            return a[k];
        }
        int[] bounds = boundsFor(confirmed, k, a.length);
        int lo = bounds[0], hi = bounds[1];
        while (true)
        {
            if (lo == hi)
            {
                confirmed.add(lo);
                return a[lo];
            }
            int pivotIndex = lo + RANDOM.nextInt(hi - lo + 1);
            int p = partition(a, lo, hi, pivotIndex, comparator);
            confirmed.add(p);
            if (k == p)
            {
                return a[p];
            }
            if (k < p)
            {
                hi = p - 1;
            }
            else
            {
                lo = p + 1;
            }
        }
    }

    private static <T> int partition(T[] a, int lo, int hi, int pivotIndex,
            Comparator<? super T> comparator)
    {
        T pivotValue = a[pivotIndex];
        swap(a, pivotIndex, hi);
        int store = lo;
        for (int i = lo; i < hi; i++)
        {
            if (comparator.compare(a[i], pivotValue) < 0)
            {
                swap(a, i, store);
                store++;
            }
        }
        swap(a, store, hi);
        return store;
    }

    private static <T> void swap(T[] a, int i, int j)
    {
        T t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    /**
     * Returns the k smallest elements of the given array, according to the
     * given comparator, in no particular order. Only the partitions needed to
     * isolate them are performed, the array is never fully sorted. The given
     * array may be rearranged.
     *
     * @param <T> the type of the elements.
     * @param a the array.
     * @param k the number of elements to return.
     * @param comparator the comparator used to order the elements.
     * @return a new array with the k smallest elements of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     * @throws NullPointerException if comparator is null.
     */
    public static <T> T[] lowest(T[] a, int k, Comparator<? super T> comparator)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return Arrays.copyOf(a, 0);
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, k - 1, comparator);
        return Arrays.copyOfRange(a, 0, k);
    }

    /**
     * Returns the k largest elements of the given array, according to the
     * given comparator, in no particular order. Only the partitions needed to
     * isolate them are performed, the array is never fully sorted. The given
     * array may be rearranged.
     *
     * @param <T> the type of the elements.
     * @param a the array.
     * @param k the number of elements to return.
     * @param comparator the comparator used to order the elements.
     * @return a new array with the k largest elements of a.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     * @throws NullPointerException if comparator is null.
     */
    public static <T> T[] highest(T[] a, int k, Comparator<? super T> comparator)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return Arrays.copyOf(a, 0);
        }
        if (k == a.length)
        {
            return a.clone();
        }
        select(a, a.length - k, comparator);
        return Arrays.copyOfRange(a, a.length - k, a.length);
    }

    /**
     * Returns the k smallest elements of the given array, according to the
     * given comparator, sorted in ascending order. The given array may be
     * rearranged.
     *
     * @param <T> the type of the elements.
     * @param a the array.
     * @param k the number of elements to return.
     * @param comparator the comparator used to order the elements.
     * @return a new array with the k smallest elements of a, ascending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     * @throws NullPointerException if comparator is null.
     */
    public static <T> T[] lowestSorted(T[] a, int k, Comparator<? super T> comparator)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return Arrays.copyOf(a, 0);
        }
        T[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, k - 1, comparator);
            r = Arrays.copyOfRange(a, 0, k);
        }
        Arrays.sort(r, comparator);
        return r;
    }

    /**
     * Returns the k largest elements of the given array, according to the
     * given comparator, sorted in descending order (largest first). The given
     * array may be rearranged.
     *
     * @param <T> the type of the elements.
     * @param a the array.
     * @param k the number of elements to return.
     * @param comparator the comparator used to order the elements.
     * @return a new array with the k largest elements of a, descending.
     * @throws IllegalArgumentException if k is outside [0, a.length].
     * @throws NullPointerException if comparator is null.
     */
    public static <T> T[] highestSorted(T[] a, int k, Comparator<? super T> comparator)
    {
        checkCount(k, a.length);
        if (k == 0)
        {
            return Arrays.copyOf(a, 0);
        }
        T[] r;
        if (k == a.length)
        {
            r = a.clone();
        }
        else
        {
            select(a, a.length - k, comparator);
            r = Arrays.copyOfRange(a, a.length - k, a.length);
        }
        Arrays.sort(r, comparator);
        reverse(r);
        return r;
    }

    private static <T> void reverse(T[] a)
    {
        for (int i = 0, j = a.length - 1; i < j; i++, j--)
        {
            T t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }
}