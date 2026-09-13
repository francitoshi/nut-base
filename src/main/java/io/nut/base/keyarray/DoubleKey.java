/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.keyarray;

import io.nut.base.util.Utils;
import java.util.Arrays;

/**
 * This class is intended for encapsulate a {@code double[]} and use it as a key in a Map,
 * because using {@code double[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class DoubleKey extends ArrayKey<double[]>
{
    public DoubleKey(double[] doubles)
    {
        super(doubles);
    }

    @Override
    protected int compareArrays(double[] a, double[] b)
    {
        return Utils.compare(a, b);
    }

    @Override
    protected int hashArray(double[] a)
    {
        return Arrays.hashCode(a);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(array);
    }

    public double[] getDoubles()
    {
        return array.clone();
    }
}