/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.keyarray;

import io.nut.base.util.Utils;
import java.util.Arrays;

/**
 * This class is intended for encapsulate a {@code float[]} and use it as a key in a Map,
 * because using {@code float[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class FloatKey extends ArrayKey<float[]>
{
    public FloatKey(float[] floats)
    {
        super(floats);
    }

    @Override
    protected int compareArrays(float[] a, float[] b)
    {
        return Utils.compare(a, b);
    }

    @Override
    protected int hashArray(float[] a)
    {
        return Arrays.hashCode(a);
    }

    @Override
    public String toString()
    {
        return Arrays.toString(array);
    }

    public float[] getFloats()
    {
        return array.clone();
    }
}