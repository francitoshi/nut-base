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
 * This class is intended for encapsulate a {@code boolean[]} and use it as a key in a Map,
 * because using {@code boolean[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class BooleanKey implements Comparable<BooleanKey>, Serializable
{
    protected final boolean[] booleans;

    public BooleanKey(boolean[] booleans)
    {
        this.booleans = booleans;
    }

    @Override
    public int compareTo(BooleanKey other)
    {
        return Utils.compare(this.booleans, other.booleans);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + Arrays.hashCode(this.booleans);
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
        final BooleanKey other = (BooleanKey) obj;
        return Arrays.equals(this.booleans, other.booleans);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(booleans);
    }

    public boolean[] getBooleans()
    {
        return booleans.clone();
    }

}
