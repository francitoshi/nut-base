/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.keyarray;

import io.nut.base.util.Utils;
import java.io.Serializable;
import java.util.Arrays;

/**
 * This class is intended for encapsulate a {@code double[]} and use it as a key in a Map,
 * because using {@code double[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class DoubleKey implements Comparable<DoubleKey>, Serializable
{
    protected final double[] doubles;

    public DoubleKey(double[] doubles)
    {
        this.doubles = doubles;
    }

    @Override
    public int compareTo(DoubleKey other)
    {
        return Utils.compare(this.doubles, other.doubles);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + Arrays.hashCode(this.doubles);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DoubleKey other = (DoubleKey) obj;
        return Arrays.equals(this.doubles, other.doubles);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(doubles);
    }

    public double[] getDoubles()
    {
        return doubles.clone();
    }

}
