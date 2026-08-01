/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

/**
 *
 * @author franci
 */
public class Bits
{
    public static int bitCount(byte b)
    {
        return Integer.bitCount(b & 0xFF);
    }

    public static int bitCount(short s)
    {
        return Integer.bitCount(s & 0xFFFF);
    }

    public static int bitCount(char c)
    {
        return Integer.bitCount(c & 0xFFFF);
    }
    
    public static byte bitSet(byte value, int index, boolean bitValue)
    {
        if (bitValue)
        {
            value |= 1 << index;
        }
        else
        {
            value &= ~(1 << index);
        }
        return value;
    }
    public static boolean bitGet(byte value, int index)
    {
        return ((value >> index) & 1) == 1;
    }    
    
}
