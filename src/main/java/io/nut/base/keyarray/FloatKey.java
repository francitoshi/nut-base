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
 * This class is intended for encapsulate a {@code float[]} and use it as a key in a Map,
 * because using {@code float[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class FloatKey implements Comparable<FloatKey>, Serializable
{
    protected final float[] floats;

    public FloatKey(float[] floats)
    {
        this.floats = floats;
    }

    @Override
    public int compareTo(FloatKey other)
    {
        return Utils.compare(this.floats, other.floats);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + Arrays.hashCode(this.floats);
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
        final FloatKey other = (FloatKey) obj;
        return Arrays.equals(this.floats, other.floats);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(floats);
    }

    public float[] getFloats()
    {
        return floats.clone();
    }

}
