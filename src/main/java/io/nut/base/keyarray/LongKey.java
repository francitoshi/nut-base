/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.keyarray;

import io.nut.base.util.Utils;
import java.util.Arrays;

/**
 * This class is intended for encapsulate a {@code long[]} and use it as a key in a Map,
 * because using {@code long[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class LongKey extends ArrayKey<long[]>
{
    public LongKey(long[] longs)
    {
        super(longs);
    }

    @Override
    protected int compareArrays(long[] a, long[] b)
    {
        return Utils.compare(a, b);
    }

    @Override
    protected int hashArray(long[] a)
    {
        return Arrays.hashCode(a);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(array);
    }

    public long[] getLongs()
    {
        return array.clone();
    }
}