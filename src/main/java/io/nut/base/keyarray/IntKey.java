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
 * This class is intended for encapsulate an {@code int[]} and use it as a key in a Map,
 * because using {@code int[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class IntKey implements Comparable<IntKey>, Serializable
{
    protected final int[] ints;

    public IntKey(int[] ints)
    {
        this.ints = ints;
    }

    @Override
    public int compareTo(IntKey other)
    {
        return Utils.compare(this.ints, other.ints);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + Arrays.hashCode(this.ints);
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
        final IntKey other = (IntKey) obj;
        return Arrays.equals(this.ints, other.ints);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(ints);
    }

    public int[] getInts()
    {
        return ints.clone();
    }

}
