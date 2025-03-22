/*
 *  Widener.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util;

/**
 *
 * @author franci
 */
public class Widener
{
    public static short[] shorts(byte... src)
    {
        if (src == null)
        {
            return null;
        }
        short[] dst = new short[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static int[] ints(byte... src)
    {
        if (src == null)
        {
            return null;
        }
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }
    public static int[] ints(short... src)
    {
        if (src == null)
        {
            return null;
        }
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }
    public static int[] ints(char... src)
    {
        if (src == null)
        {
            return null;
        }
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }
    
    public static long[] longs(byte... src)
    {
        if (src == null)
        {
            return null;
        }
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }
    public static long[] longs(short... src)
    {
        if (src == null)
        {
            return null;
        }
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }
    public static long[] longs(char... src)
    {
        if (src == null)
        {
            return null;
        }
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }
    public static long[] longs(int... src)
    {
        if (src == null)
        {
            return null;
        }
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

}
