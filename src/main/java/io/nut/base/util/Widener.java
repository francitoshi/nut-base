/*
 *  Widener.java
 *
 *  Copyright (c) 2025-2026 francitoshi@gmail.com
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
 * Utility class providing methods to widen primitive arrays.
 * <p>
 * This class facilitates the conversion of smaller primitive type arrays (or varargs) 
 * into larger primitive type arrays (e.g., {@code byte[]} to {@code int[]}) 
 * without manual casting loops in the calling code.
 */
public class Widener
{
    /**
     * Widens an array of bytes to an array of shorts.
     *
     * @param src the source byte array or varargs sequence.
     * @return a new short array containing the widened values, 
     *         or {@code null} if the input is {@code null}.
     */
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

    /**
     * Widens an array of bytes to an array of ints.
     *
     * @param src the source byte array or varargs sequence.
     * @return a new int array containing the widened values, 
     *         or {@code null} if the input is {@code null}.
     */
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

    /**
     * Widens an array of shorts to an array of ints.
     *
     * @param src the source short array or varargs sequence.
     * @return a new int array containing the widened values, 
     *         or {@code null} if the input is {@code null}.
     */
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

    /**
     * Widens an array of characters to an array of ints.
     *
     * @param src the source char array or varargs sequence.
     * @return a new int array containing the widened values, 
     *         or {@code null} if the input is {@code null}.
     */
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
    
    /**
     * Widens an array of bytes to an array of longs.
     *
     * @param src the source byte array or varargs sequence.
     * @return a new long array containing the widened values, 
     *         or {@code null} if the input is {@code null}.
     */
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

    /**
     * Widens an array of shorts to an array of longs.
     *
     * @param src the source short array or varargs sequence.
     * @return a new long array containing the widened values, 
     *         or {@code null} if the input is {@code null}.
     */
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

    /**
     * Widens an array of characters to an array of longs.
     *
     * @param src the source char array or varargs sequence.
     * @return a new long array containing the widened values, 
     *         or {@code null} if the input is {@code null}.
     */
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

    /**
     * Widens an array of ints to an array of longs.
     *
     * @param src the source int array or varargs sequence.
     * @return a new long array containing the widened values, 
     *         or {@code null} if the input is {@code null}.
     */
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
