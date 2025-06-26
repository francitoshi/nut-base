/*
 *  Concats.java
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
public abstract class Concats
{
    
    public static byte[] cat(byte[] src, byte... next)
    {
        return Joins.join(src, next);
    }
    /**
     * Concatenate a series of elements to an int[] array.
     * @param src the starting array
     * @param next the values to concatenate
     * @return a new array with the resulting array
     */
    public static int[] cat(int[] src, int... next)
    {
        return Joins.join(src, next);
    }
    public static short[] cat(short[] src, short... next)
    {
        return Joins.join(src, next);
    }
    public static char[] cat(char[] src, char... next)
    {
        return Joins.join(src, next);
    }
    public static long[] cat(long[] src, long... next)
    {
        return Joins.join(src, next);
    }
    public static float[] cat(float[] src, float... next)
    {
        return Joins.join(src, next);
    }
    public static double[] cat(double[] src, double... next)
    {
        return Joins.join(src, next);
    }
    public static <E> E[] cat(E[] src, E... next)
    {
        return Joins.join(src, next);
    }
}
