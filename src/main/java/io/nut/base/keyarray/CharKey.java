/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.keyarray;

import io.nut.base.util.Utils;
import java.util.Arrays;

/**
 * This class is intended for encapsulate a {@code char[]} and use it as a key in a Map,
 * because using {@code char[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class CharKey extends ArrayKey<char[]>
{
    public CharKey(char[] chars)
    {
        super(chars);
    }

    public CharKey(String value)
    {
        super(value.toCharArray());
    }

    @Override
    protected int compareArrays(char[] a, char[] b)
    {
        return Utils.compare(a, b);
    }

    @Override
    protected int hashArray(char[] a)
    {
        return Arrays.hashCode(a);
    }

    @Override
    public String toString()
    {
        return new String(array);
    }

    public char[] getChars()
    {
        return array.clone();
    }
}