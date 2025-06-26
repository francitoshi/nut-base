/*
 *  Joins.java
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

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author franci
 */
public abstract class Joins
{
    public static byte[] join(byte[]... src)
    {
        int count = 0;
        for (byte[] item : src)
        {
            if (item != null)
            {
                count += item.length;
            }
        }
        ByteBuffer dst = ByteBuffer.allocate(count);
        for (byte[] item : src)
        {
            if (item != null && item.length > 0)
            {
                dst.put(item);
            }
        }
        return dst.array();
    }

    public static int[] join(int[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        int[] dst = new int[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    public static long[] join(long[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        long[] dst = new long[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    public static short[] join(short[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        short[] dst = new short[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    public static char[] join(char[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        char[] dst = new char[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    public static float[] join(float[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        float[] dst = new float[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    public static double[] join(double[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        double[] dst = new double[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    public static String[] join(String[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        String[] dst = new String[count];
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }
    @Deprecated
    public static <E> E[] join(Class<E> cls, E[]... src)
    {
        int count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                count += src[i].length;
            }
        }
        E[] dst = (E[]) Array.newInstance(cls, count);
        count = 0;
        for (int i = 0; i < src.length; i++)
        {
            if (src[i] != null && src[i].length > 0)
            {
                System.arraycopy(src[i], 0, dst, count, src[i].length);
                count += src[i].length;
            }
        }
        return dst;
    }

    public static <E> E[] join(E[] ... src)
    {
        if (src == null)
        {
            return null;
        }
        int size = 0;
        E[] dst = null;
        for (E[] item : src)
        {
            if (item != null)
            {
                size += item.length;
                if (dst==null)
                {
                    dst = Arrays.copyOf(item, 0);
                }
            }
        }
        if(dst ==null)
        {
            return null;
        }
        dst = Arrays.copyOf(dst,size);
        for (int i = 0, w = 0; i < src.length; i++)
        {
            if (src[i] != null)
            {
                for (E item : src[i])
                {
                    dst[w++] = item;
                }
            }
        }
        return dst;
    }
    
    public static String join(String... src)
    {
        StringBuilder dst = new StringBuilder();
        for (String src1 : src)
        {
            if (src1 != null)
            {
                dst.append(src1);
            }
        }
        return dst.toString();
    }

    public static <E> List<E> join(List<E>... src)
    {
        List<E> dst = new ArrayList<>();
        for (List<E> src1 : src)
        {
            if (src1 != null)
            {
                dst.addAll(src1);
            }
        }
        return dst;
    }
}
