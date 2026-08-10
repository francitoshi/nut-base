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
 * This class is intended for encapsulate a {@code long[]} and use it as a key in a Map,
 * because using {@code long[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class LongKey implements Comparable<LongKey>, Serializable
{
    protected final long[] longs;

    public LongKey(long[] longs)
    {
        this.longs = longs;
    }

    @Override
    public int compareTo(LongKey other)
    {
        return Utils.compare(this.longs, other.longs);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + Arrays.hashCode(this.longs);
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
        final LongKey other = (LongKey) obj;
        return Arrays.equals(this.longs, other.longs);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(longs);
    }

    public long[] getLongs()
    {
        return longs.clone();
    }

}
