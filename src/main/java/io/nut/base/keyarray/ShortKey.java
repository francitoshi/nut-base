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
 * This class is intended for encapsulate a {@code short[]} and use it as a key in a Map,
 * because using {@code short[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class ShortKey implements Comparable<ShortKey>, Serializable
{
    protected final short[] shorts;

    public ShortKey(short[] shorts)
    {
        this.shorts = shorts;
    }

    @Override
    public int compareTo(ShortKey other)
    {
        return Utils.compare(this.shorts, other.shorts);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + Arrays.hashCode(this.shorts);
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
        final ShortKey other = (ShortKey) obj;
        return Arrays.equals(this.shorts, other.shorts);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(shorts);
    }

    public short[] getShorts()
    {
        return shorts.clone();
    }

}
