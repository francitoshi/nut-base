/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

/**
 *
 * @author franci
 */
public class Bitwise
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
        
    public static byte[] xor(byte[] x, byte[] y)
    {
        int min = Math.min(x.length, y.length);
        int max = Math.max(x.length, y.length);
        byte[] xy = new byte[max];
        for (int i = 0; i < min; i++)
        {
            xy[i] = (byte) (x[i] ^ y[i]);
        }
        System.arraycopy(x, min, xy, min, x.length - min);
        System.arraycopy(y, min, xy, min, y.length - min);
        return xy;
    }

    public static byte[] rollXor(byte[] data, long xor)
    {
        byte[] ret = new byte[data.length];
        for (int i = 0; i < data.length; i++)
        {
            ret[i] = (byte) (data[i] ^ xor);
            xor = Long.reverse(data[i] + ret[i] + xor + i);
        }
        return ret;
    }

}
