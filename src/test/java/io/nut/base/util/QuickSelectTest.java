/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.Arrays;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class QuickSelectTest
{
    /**
     * Test of select method, of class QuickSelect, for int arrays.
     */
    @Test
    public void testSelect_int()
    {
        int[] a = {4, 2, 7, 1, 9, 3};
        assertEquals(1, QuickSelect.select(a, 0));
        assertEquals(2, QuickSelect.select(a, 1));
        assertEquals(3, QuickSelect.select(a, 2));
        assertEquals(4, QuickSelect.select(a, 3));
        assertEquals(7, QuickSelect.select(a, 4));
        assertEquals(9, QuickSelect.select(a, 5));

        int[] sorted = {1, 2, 3, 4, 5};
        assertEquals(5, QuickSelect.select(sorted, 4));

        int[] dup = {5, 5, 5, 7, 7, 9};
        assertEquals(5, QuickSelect.select(dup, 0));
        assertEquals(7, QuickSelect.select(dup, 3));
        assertEquals(9, QuickSelect.select(dup, 5));

        int[] single = {42};
        assertEquals(42, QuickSelect.select(single, 0));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.select(new int[0], 0));
        assertThrows(IndexOutOfBoundsException.class, () -> QuickSelect.select(a, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> QuickSelect.select(a, a.length));
    }

    /**
     * Test of select method, of class QuickSelect, for long arrays.
     */
    @Test
    public void testSelect_long()
    {
        long[] a = {4L, 2L, 7L, 1L, 9L, 3L};
        assertEquals(1L, QuickSelect.select(a, 0));
        assertEquals(4L, QuickSelect.select(a, 3));
        assertEquals(9L, QuickSelect.select(a, 5));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.select(new long[0], 0));
        assertThrows(IndexOutOfBoundsException.class, () -> QuickSelect.select(a, 6));
    }

    /**
     * Test of select method, of class QuickSelect, for double arrays.
     */
    @Test
    public void testSelect_double()
    {
        double[] a = {4.5, 2.0, 7.0, 1.5, 9.0, 3.0};
        assertEquals(1.5, QuickSelect.select(a, 0));
        assertEquals(4.5, QuickSelect.select(a, 3));
        assertEquals(9.0, QuickSelect.select(a, 5));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.select(new double[0], 0));
    }

    /**
     * Test of select method, of class QuickSelect, for float arrays.
     */
    @Test
    public void testSelect_float()
    {
        float[] a = {4.5f, 2.0f, 7.0f, 1.5f, 9.0f, 3.0f};
        assertEquals(1.5f, QuickSelect.select(a, 0));
        assertEquals(4.5f, QuickSelect.select(a, 3));
        assertEquals(9.0f, QuickSelect.select(a, 5));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.select(new float[0], 0));
    }

    /**
     * Test of select method, of class QuickSelect, for short arrays.
     */
    @Test
    public void testSelect_short()
    {
        short[] a = {4, 2, 7, 1, 9, 3};
        assertEquals((short)1, QuickSelect.select(a, 0));
        assertEquals((short)4, QuickSelect.select(a, 3));
        assertEquals((short)9, QuickSelect.select(a, 5));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.select(new short[0], 0));
    }

    /**
     * Test of select method, of class QuickSelect, for byte arrays.
     */
    @Test
    public void testSelect_byte()
    {
        byte[] a = {4, 2, 7, 1, 9, 3};
        assertEquals((byte)1, QuickSelect.select(a, 0));
        assertEquals((byte)4, QuickSelect.select(a, 3));
        assertEquals((byte)9, QuickSelect.select(a, 5));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.select(new byte[0], 0));
    }

    /**
     * Test of select method, of class QuickSelect, for char arrays.
     */
    @Test
    public void testSelect_char()
    {
        char[] a = {'d', 'b', 'g', 'a', 'i', 'c'};
        assertEquals('a', QuickSelect.select(a, 0));
        assertEquals('d', QuickSelect.select(a, 3));
        assertEquals('i', QuickSelect.select(a, 5));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.select(new char[0], 0));
    }

    /**
     * Test of select method, of class QuickSelect, for generic arrays.
     */
    @Test
    public void testSelect_GenericType()
    {
        Integer[] a = {4, 2, 7, 1, 9, 3};
        assertEquals(Integer.valueOf(1), QuickSelect.select(a, 0));
        assertEquals(Integer.valueOf(4), QuickSelect.select(a, 3));
        assertEquals(Integer.valueOf(9), QuickSelect.select(a, 5));

        String[] s = {"pear", "apple", "banana", "cherry"};
        assertEquals("apple", QuickSelect.select(s, 0));
        assertEquals("pear", QuickSelect.select(s, 3));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.select(new Integer[0], 0));
    }

    /**
     * Test of select method, of class QuickSelect, with a Comparator.
     */
    @Test
    public void testSelect_GenericType_Comparator()
    {
        Integer[] a = {4, 2, 7, 1, 9, 3};
        Comparator<Integer> rev = Comparator.reverseOrder();
        assertEquals(Integer.valueOf(9), QuickSelect.select(a, 0, rev));
        assertEquals(Integer.valueOf(3), QuickSelect.select(a, 3, rev));
        assertEquals(Integer.valueOf(1), QuickSelect.select(a, 5, rev));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.select(new Integer[0], 0, rev));

        assertThrows(NullPointerException.class, () -> QuickSelect.select(new Integer[]{1, 2, 3}, 0, (Comparator<Integer>)null));
    }

    /**
     * Test of lowest method, of class QuickSelect, for int arrays.
     */
    @Test
    public void testLowest_int()
    {
        int[] a = {4, 2, 7, 1, 9, 3};
        assertArrayEquals(new int[]{1, 2, 3, 4}, sorted(QuickSelect.lowest(a, 4)));
        assertArrayEquals(new int[]{1, 2, 3, 4, 7}, sorted(QuickSelect.lowest(a, 5)));

        assertArrayEquals(new int[]{}, QuickSelect.lowest(a, 0));

        int[] all = {4, 2, 7};
        int[] allLowest = QuickSelect.lowest(all, all.length);
        assertNotSame(all, allLowest);
        assertArrayEquals(all, allLowest);

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.lowest(a, -1));
        assertThrows(IllegalArgumentException.class, () -> QuickSelect.lowest(a, a.length + 1));
    }

    /**
     * Test of highest method, of class QuickSelect, for int arrays.
     */
    @Test
    public void testHighest_int()
    {
        int[] a = {4, 2, 7, 1, 9, 3};
        assertArrayEquals(new int[]{4, 7, 9}, sorted(QuickSelect.highest(a, 3)));
        assertArrayEquals(new int[]{7, 9}, sorted(QuickSelect.highest(a, 2)));

        assertArrayEquals(new int[]{}, QuickSelect.highest(a, 0));

        int[] all = {4, 2, 7};
        int[] allHighest = QuickSelect.highest(all, all.length);
        assertNotSame(all, allHighest);
        assertArrayEquals(all, allHighest);

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.highest(a, -1));
        assertThrows(IllegalArgumentException.class, () -> QuickSelect.highest(a, a.length + 1));
    }

    /**
     * Test of lowestSorted method, of class QuickSelect, for int arrays.
     */
    @Test
    public void testLowestSorted_int()
    {
        int[] a = {4, 2, 7, 1, 9, 3};
        assertArrayEquals(new int[]{1, 2, 3}, QuickSelect.lowestSorted(a, 3));
        assertArrayEquals(new int[]{1, 2, 3, 4, 7}, QuickSelect.lowestSorted(a, 5));

        assertArrayEquals(new int[]{}, QuickSelect.lowestSorted(a, 0));

        int[] all = {4, 2, 7};
        assertArrayEquals(new int[]{2, 4, 7}, QuickSelect.lowestSorted(all, all.length));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.lowestSorted(a, -1));
        assertThrows(IllegalArgumentException.class, () -> QuickSelect.lowestSorted(a, a.length + 1));
    }

    /**
     * Test of highestSorted method, of class QuickSelect, for int arrays.
     */
    @Test
    public void testHighestSorted_int()
    {
        int[] a = {4, 2, 7, 1, 9, 3};
        assertArrayEquals(new int[]{9, 7, 4}, QuickSelect.highestSorted(a, 3));
        assertArrayEquals(new int[]{9, 7}, QuickSelect.highestSorted(a, 2));

        assertArrayEquals(new int[]{}, QuickSelect.highestSorted(a, 0));

        int[] all = {4, 2, 7};
        assertArrayEquals(new int[]{7, 4, 2}, QuickSelect.highestSorted(all, all.length));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.highestSorted(a, -1));
        assertThrows(IllegalArgumentException.class, () -> QuickSelect.highestSorted(a, a.length + 1));
    }

    /**
     * Test of lowest/highest methods, of class QuickSelect, for a generic array
     * with a Comparator.
     */
    @Test
    public void testLowestHighest_GenericType()
    {
        Integer[] a = {4, 2, 7, 1, 9, 3};
        assertArrayEquals(new Integer[]{1, 2}, sorted(QuickSelect.lowest(a, 2)));
        assertArrayEquals(new Integer[]{4, 7, 9}, sorted(QuickSelect.highest(a, 3)));

        assertArrayEquals(new Integer[]{1, 2, 3}, QuickSelect.lowestSorted(a, 3));
        assertArrayEquals(new Integer[]{9, 7, 4}, QuickSelect.highestSorted(a, 3));

        Comparator<Integer> rev = Comparator.reverseOrder();
        Integer[] b = {4, 2, 7, 1, 9, 3};
        assertArrayEquals(new Integer[]{7, 9}, sorted(QuickSelect.lowest(b, 2, rev)));
        assertArrayEquals(new Integer[]{1, 2, 3}, sorted(QuickSelect.highest(b, 3, rev)));
        assertArrayEquals(new Integer[]{9, 7, 4}, QuickSelect.lowestSorted(b, 3, rev));
        assertArrayEquals(new Integer[]{1, 2, 3}, QuickSelect.highestSorted(b, 3, rev));

        assertArrayEquals(new Integer[]{}, QuickSelect.lowest(a, 0));
        assertArrayEquals(new Integer[]{}, QuickSelect.highest(a, 0));
        assertArrayEquals(new Integer[]{}, QuickSelect.lowestSorted(a, 0));
        assertArrayEquals(new Integer[]{}, QuickSelect.highestSorted(a, 0));

        assertThrows(IllegalArgumentException.class, () -> QuickSelect.lowest(a, a.length + 1));
        assertThrows(IllegalArgumentException.class, () -> QuickSelect.highest(a, a.length + 1));
        assertThrows(IllegalArgumentException.class, () -> QuickSelect.lowestSorted(a, a.length + 1));
        assertThrows(IllegalArgumentException.class, () -> QuickSelect.highestSorted(a, a.length + 1));
    }

    /**
     * Test of lowest/highest methods, of class QuickSelect, with duplicated
     * values and negative numbers.
     */
    @Test
    public void testLowestHighest_deduplicated()
    {
        int[] a = {-3, 0, 5, -3, 7, 0};
        assertArrayEquals(new int[]{-3, -3, 0}, QuickSelect.lowestSorted(a, 3));
        assertArrayEquals(new int[]{7, 5, 0}, QuickSelect.highestSorted(a, 3));

        int[] b = {5, 5, 5, 2, 2, 9};
        assertArrayEquals(new int[]{2, 2, 5}, QuickSelect.lowestSorted(b, 3));
        assertArrayEquals(new int[]{9, 5, 5}, QuickSelect.highestSorted(b, 3));
    }

    private static int[] sorted(int[] a)
    {
        Arrays.sort(a);
        return a;
    }

    private static <T extends Comparable<? super T>> T[] sorted(T[] a)
    {
        Arrays.sort(a);
        return a;
    }
}