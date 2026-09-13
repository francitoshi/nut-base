/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.keyarray;

import io.nut.base.util.Utils;
import java.util.Arrays;

/**
 * This class is intended for encapsulate an {@code int[]} and use it as a key in a Map,
 * because using {@code int[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class IntKey extends ArrayKey<int[]>
{
    public IntKey(int[] ints)
    {
        super(ints);
    }

    @Override
    protected int compareArrays(int[] a, int[] b)
    {
        return Utils.compare(a, b);
    }

    @Override
    protected int hashArray(int[] a)
    {
        return Arrays.hashCode(a);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(array);
    }

    public int[] getInts()
    {
        return array.clone();
    }
}