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
 * This class is intended for encapsulate a {@code char[]} and use it as a key in a Map,
 * because using {@code char[]} as key compares array memory address and not the content.
 *
 * @author franci
 */
public class CharKey implements Comparable<CharKey>, Serializable
{
    protected final char[] chars;

    public CharKey(char[] chars)
    {
        this.chars = chars;
    }

    public CharKey(String value)
    {
        this.chars = value.toCharArray();
    }

    @Override
    public int compareTo(CharKey other)
    {
        return Utils.compare(this.chars, other.chars);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + Arrays.hashCode(this.chars);
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
        final CharKey other = (CharKey) obj;
        return Arrays.equals(this.chars, other.chars);
    }

    @Override
    public String toString()
    {
        return new String(chars);
    }

    public char[] getChars()
    {
        return chars.clone();
    }

}
