/*
 *  Utils.java
 *
 *  Copyright (c) 2023-2024 francitoshi@gmail.com
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

import io.nut.base.compat.ByteBufferCompat;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Adler32;

/**
 *
 * @author franci
 */
public class Utils
{
    public static final long NANOS_PER_MILLIS = 1_000_000L;
    public static final long NANOS_PER_SECOND = 1_000_000_000L;

    // shorthand for some units of time
    public static final long SECOND_MILLIS = 1000L;
    public static final long MINUTE_MILLIS = 60 * SECOND_MILLIS;
    public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
    public static final long DAY_MILLIS = 24 * HOUR_MILLIS;
    public static final long WEEK_MILLIS = 7 * DAY_MILLIS;

    public static final long SECOND_NANOS = 1000 * NANOS_PER_MILLIS;
    public static final long MINUTE_NANOS = 60 * SECOND_NANOS;
    public static final long HOUR_NANOS = 60 * MINUTE_NANOS;
    public static final long DAY_NANOS = 24 * HOUR_NANOS;
    public static final long WEEK_NANOS = 7 * DAY_NANOS;   

    /**
     * An empty immutable {@code boolean} array.
     */
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = {};

    /**
     * An empty immutable {@link Boolean} array.
     */
    public static final Boolean[] EMPTY_BOOLEAN_OBJECT_ARRAY = {};

    /**
     * An empty immutable {@code byte} array.
     */
    public static final byte[] EMPTY_BYTE_ARRAY = {};

    /**
     * An empty immutable {@link Byte} array.
     */
    public static final Byte[] EMPTY_BYTE_OBJECT_ARRAY = {};

    /**
     * An empty immutable {@code char} array.
     */
    public static final char[] EMPTY_CHAR_ARRAY = {};

    /**
     * An empty immutable {@link Character} array.
     */
    public static final Character[] EMPTY_CHARACTER_OBJECT_ARRAY = {};

    /**
     * An empty immutable {@code double} array.
     */
    public static final double[] EMPTY_DOUBLE_ARRAY = {};

    /**
     * An empty immutable {@link Double} array.
     */
    public static final Double[] EMPTY_DOUBLE_OBJECT_ARRAY = {};

    /**
     * An empty immutable {@code float} array.
     */
    public static final float[] EMPTY_FLOAT_ARRAY = {};

    /**
     * An empty immutable {@link Float} array.
     */
    public static final Float[] EMPTY_FLOAT_OBJECT_ARRAY = {};

    /**
     * An empty immutable {@code int} array.
     */
    public static final int[] EMPTY_INT_ARRAY = {};

    /**
     * An empty immutable {@link Integer} array.
     */
    public static final Integer[] EMPTY_INTEGER_OBJECT_ARRAY = {};

    /**
     * An empty immutable {@code long} array.
     */
    public static final long[] EMPTY_LONG_ARRAY = {};

    /**
     * An empty immutable {@link Long} array.
     */
    public static final Long[] EMPTY_LONG_OBJECT_ARRAY = {};

    /**
     * An empty immutable {@link Object} array.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = {};

    /**
     * An empty immutable {@code short} array.
     */
    public static final short[] EMPTY_SHORT_ARRAY = {};

    /**
     * An empty immutable {@link Short} array.
     */
    public static final Short[] EMPTY_SHORT_OBJECT_ARRAY = {};

    /**
     * An empty immutable {@link String} array.
     */
    public static final String[] EMPTY_STRING_ARRAY = {};

    public static byte[] asBytes(InputStream in) throws IOException
    {
        if (in == null)
        {
            return null;
        }
        int r;
        byte[] buf = new byte[64 * 1024];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = in.read(buf)) > 0)
        {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

    public static byte[] asBytes(byte... src)
    {
        return src;
    }

    public static byte[] asBytes(BigInteger n, int minBytes)
    {
        if(n==null)
        {
            return null;
        }
        byte[] ret = n.toByteArray();
        if(ret.length<minBytes)
        {
           
            ret = new ByteBufferCompat(ByteBuffer.allocate(minBytes)).put(minBytes-ret.length, ret).array();
        }
        return ret;
    }

    public static short[] asShorts(byte... src)
    {
        if (src == null)
            return null;
        short[] dst = new short[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    //----------------------------------------------------------------------------------------------
    public static int[] asInts(byte... src)
    {
        if (src == null)
            return null;
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static int[] asInts(short... src)
    {
        if (src == null)
            return null;
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    /**
     * Converts an array of {@link Integer} objects to an array of primitive {@code int}s.
     *
     * <p>If an element in the source array is {@code null}, the specified {@code whenNull} value will be used 
     * in the destination array at the corresponding index.
     *
     * @param src the source array of {@link Integer} objects. If {@code null}, the method returns {@code null}.
     * @param whenNull the value to be used in the destination array if a source array element is {@code null}.
     * @return an array of primitive {@code int}s with the same length as the source array. If the source 
     *         array is {@code null}, this method returns {@code null}.
     */
    public static int[] asInts(Integer[] src, int whenNull)
    {
        if (src == null)
        {
            return null;
        }
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++)
        {
            Integer item = src[i];
            dst[i] = (item != null) ? item : whenNull;
        }
        return dst;
    }

    public static int[] asInts(int... items)
    {
        return items;
    }

    public static int[] asInts(String[] src, int defaultValue, int radix)
    {
        if (src == null)
            return null;
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++)
        {
            if(src[i]!=null && !src[i].trim().isEmpty())
            {
                try
                {
                    dst[i] = Integer.parseInt(src[i], radix);
                }
                catch(NumberFormatException ex)
                {
                    dst[i] = defaultValue;
                }
            }
            else
            {
                dst[i] = defaultValue;
            }
        }
        return dst;
    }
    public static int[] asInts(String[] src, int defaultValue)
    {
        return asInts(src, defaultValue, 10);
    }

    public static int[] asInts(String[] src)
    {
        return asInts(src, 0, 10);
    }

    //----------------------------------------------------------------------------------------------

    public static long[] asLongs(byte... src)
    {
        if (src == null)
            return null;
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static long[] asLongs(short... src)
    {
        if (src == null)
            return null;
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static long[] asLongs(int... src)
    {
        if (src == null)
            return null;
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static long[] asLongs(Long[] src)
    {
        if (src == null)
            return null;
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static long[] asLongs(long... items)
    {
        return items;
    }

    public static long[] asLongs(String[] src, long defaultValue, int radix)
    {
        if (src == null)
            return null;
        long[] dst = new long[src.length];
        for (int i = 0; i < src.length; i++)
        {
            if(src[i]!=null && !src[i].trim().isEmpty())
            {
                try
                {
                    dst[i] = Long.parseLong(src[i], radix);
                }
                catch(NumberFormatException ex)
                {
                    dst[i] = defaultValue;
                }
            }
            else
            {
                dst[i] = defaultValue;
            }
        }
        return dst;
    }

    public static long[] asLongs(String[] src, long defaultValue)
    {
        return asLongs(src, defaultValue, 10);
    }

    public static long[] asLongs(String[] src)
    {
        return asLongs(src, 0, 10);
    }

    //----------------------------------------------------------------------------------------------
    public static float[] asFloats(byte... src)
    {
        if (src == null)
            return null;
        float[] dst = new float[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static float[] asFloats(short... src)
    {
        if (src == null)
            return null;
        float[] dst = new float[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static float[] asFloats(int... src)
    {
        if (src == null)
            return null;
        float[] dst = new float[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static float[] asFloats(long... values)
    {
        float[] ret = new float[values.length];
        for (int i = 0; i < values.length; i++)
        {
            ret[i] = values[i];
        }
        return ret;
    }

    public static float[] asFloats(float... items)
    {
        return items;
    }

    //----------------------------------------------------------------------------------------------
    public static double[] asDoubles(byte... src)
    {
        if (src == null)
            return null;
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static double[] asDoubles(short... src)
    {
        if (src == null)
            return null;
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static double[] asDoubles(int... src)
    {
        if (src == null)
            return null;
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static double[] asDoubles(long... src)
    {
        if (src == null)
            return null;
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static double[] asDoubles(float... src)
    {
        if (src == null)
            return null;
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = src[i];
        }
        return dst;
    }

    public static double[] asDoubles(double... items)
    {
        return items;
    }

    public static double[] asDoubles(String[] src, double defaultValue)
    {
        if (src == null)
            return null;
        double[] dst = new double[src.length];
        for (int i = 0; i < src.length; i++)
        {
            if(src[i]!=null && !src[i].trim().isEmpty())
            {
                try
                {
                    dst[i] = Double.parseDouble(src[i]);
                }
                catch(NumberFormatException ex)
                {
                    dst[i] = defaultValue;
                }
            }
            else
            {
                dst[i] = defaultValue;
            }
        }
        return dst;
    }

    public static double[] asDoubles(String[] src)
    {
        return asDoubles(src, 0.0);
    }

    //----------------------------------------------------------------------------------------------
    public static <T> String[] asStrings(T... src)
    {
        if (src == null)
            return null;
        String[] dst = new String[src.length];
        for (int i = 0; i < src.length; i++)
        {
            T item = src[i];
            dst[i] = item != null ? item.toString() : null;
        }
        return dst;
    }

    public static <T> String[] asStrings(List<T> src)
    {
        if (src == null)
            return null;
        String[] dst = new String[src.size()];
        for (int i = 0; i < dst.length; i++)
        {
            T item = src.get(i);
            dst[i] = item != null ? item.toString() : null;
        }
        return dst;
    }

    //----------------------------------------------------------------------------------------------

    public static Object[] asObjects(Object... values)
    {
        return values;
    }

    //----------------------------------------------------------------------------------------------

    public static BigInteger asBigInteger(byte[] b) 
    {
        return new BigInteger(1, b);
    }
    
    //----------------------------------------------------------------------------------------------
    
    public static BigInteger[] asBigIntegers(BigInteger... values)
    {
        return values;
    }
    
    public static BigInteger[] asBigIntegers(byte[] values, int start, int end)
    {
        BigInteger[] bi = new BigInteger[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigInteger.valueOf(values[j]);
}
        return bi;
    }
    public static BigInteger[] asBigIntegers(byte[] values)
    {
        return asBigIntegers(values, 0, values.length);
    }
    public static BigInteger[] asBigIntegers(byte[][] values, int start, int end)
    {
        BigInteger[] bi = new BigInteger[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = new BigInteger(values[j]);
        }
        return bi;
    }
    public static BigInteger[] asBigIntegers(byte[][] values)
    {
        return asBigIntegers(values, 0, values.length);
    }
    public static BigInteger[] asBigIntegers(char[] values, int start, int end)
    {
        BigInteger[] bi = new BigInteger[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigInteger.valueOf(values[j]);
        }
        return bi;
    }
    public static BigInteger[] asBigIntegers(char[] values)
    {
        return asBigIntegers(values, 0, values.length);
    }
    public static BigInteger[] asBigIntegers(int[] values, int start, int end)
    {
        BigInteger[] bi = new BigInteger[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigInteger.valueOf(values[j]);
        }
        return bi;
    }
    public static BigInteger[] asBigIntegers(int[] values)
    {
        return asBigIntegers(values, 0, values.length);
    }
    public static BigInteger[] asBigIntegers(long[] values, int start, int end)
    {
        BigInteger[] bi = new BigInteger[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigInteger.valueOf(values[j]);
        }
        return bi;
    }
    public static BigInteger[] asBigIntegers(long[] values)
    {
        return asBigIntegers(values, 0, values.length);
    }

    public static BigDecimal[] asBigDecimals(BigDecimal... values)
    {
        return values;
    }
    
    //----------------------------------------------------------------------------------------------

    public static BigDecimal[] asBigDecimals(byte[] values, int start, int end)
    {
        BigDecimal[] bi = new BigDecimal[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigDecimal.valueOf(values[j]);
}
        return bi;
    }
    public static BigDecimal[] asBigDecimals(byte[] values)
    {
        return asBigDecimals(values, 0, values.length);
    }
    public static BigDecimal[] asBigDecimals(char[] values, int start, int end)
    {
        BigDecimal[] bi = new BigDecimal[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigDecimal.valueOf(values[j]);
        }
        return bi;
    }
    public static BigDecimal[] asBigDecimals(char[] values)
    {
        return asBigDecimals(values, 0, values.length);
    }
    public static BigDecimal[] asBigDecimals(int[] values, int start, int end)
    {
        BigDecimal[] bi = new BigDecimal[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigDecimal.valueOf(values[j]);
        }
        return bi;
    }
    public static BigDecimal[] asBigDecimals(int[] values)
    {
        return asBigDecimals(values, 0, values.length);
    }
    public static BigDecimal[] asBigDecimals(long[] values, int start, int end)
    {
        BigDecimal[] bi = new BigDecimal[end-start];
        for(int i=0,j=start;j<end;i++,j++)
        {
            bi[i] = BigDecimal.valueOf(values[j]);
        }
        return bi;
    }
    public static BigDecimal[] asBigDecimals(long[] values)
    {
        return asBigDecimals(values, 0, values.length);
    }

    //----------------------------------------------------------------------------------------------

    public static <T> Set<T> asSet(T... items)
    {
        return new HashSet<>(Arrays.asList(items));
    }
    
    public static <T> List<T> asList(T... items)
    {
        return Arrays.asList(items);
    }
    
    public static <T> Queue<T> asQueue(T... items)
    {
        return new ArrayDeque<>(Arrays.asList(items));
    }
    
    public static <T> Deque<T> asDeque(T... items)
    {
        return new ArrayDeque<>(Arrays.asList(items));
    }
    
    public static <T> BlockingQueue<T> asBlockingQueue(T... items)
    {
        return new ArrayBlockingQueue<>(items.length, true, Arrays.asList(items));
    }

    public static <T> BlockingDeque<T> asBlockingDeque(T... items)
    {
        return new LinkedBlockingDeque<>(Arrays.asList(items));
    }

    public static <K,V> Map<K,V> asMap(K[] keys, V[] values)
    {
        assert keys.length>=values.length;

        HashMap<K,V> map = new HashMap<>();

        for(int i=0;i<values.length;i++)
        {
            map.put(keys[i], values[i]);
        }
        for(int i=values.length;i<keys.length;i++)
        {
            map.put(keys[i], null);
        }
        return map;
    }
    
    //----------------------------------------------------------------------------------------------
    
    //java9 Arrays.compare(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex)
    public static int compare(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo)
    {
        if(a==null||b==null)
        {
            throw new NullPointerException();
        }
        if(aFrom > aTo || bFrom > bTo)
        {
            throw new IllegalArgumentException();
        }
        if(aFrom < 0 || aTo > a.length || bFrom < 0 || bTo > b.length)
        {
            throw new ArrayIndexOutOfBoundsException();
        }
        
        int n = Math.min(aTo-aFrom, bTo-bFrom);
        for (int i = 0; i < n; i++)
        {
            int cmp = Byte.compare(a[aFrom+i], b[bFrom+i]);
            if (cmp != 0)
            {
                return cmp;
            }
        }
        if (a.length < b.length && a.length < n)
        {
            return -1;
        }
        if (a.length > b.length && b.length < n)
        {
            return +1;
        }
        return 0;
    }
    public static int compare(byte[] a, byte[] b)
    {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++)
        {
            int cmp = Byte.compare(a[i], b[i]);
            if (cmp != 0)
            {
                return cmp;
            }
        }
        if (a.length < b.length)
        {
            return -1;
        }
        if (a.length > b.length)
        {
            return +1;
        }
        return 0;
    }

    public static int compare(int[] a, int[] b)
    {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++)
        {
            int cmp = Integer.compare(a[i], b[i]);
            if (cmp != 0)
            {
                return cmp;
            }
        }
        if (a.length < b.length)
        {
            return -1;
        }
        if (a.length > b.length)
        {
            return +1;
        }
        return 0;
    }

    public static int compare(long[] a, long[] b)
    {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++)
        {
            int cmp = Long.compare(a[i], b[i]);
            if (cmp != 0)
            {
                return cmp;
            }
        }
        if (a.length < b.length)
        {
            return -1;
        }
        if (a.length > b.length)
        {
            return +1;
        }
        return 0;
    }

    public static int compare(double[] a, double[] b)
    {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++)
        {
            int cmp = Double.compare(a[i], b[i]);
            if (cmp != 0)
            {
                return cmp;
            }
        }
        if (a.length < b.length)
        {
            return -1;
        }
        if (a.length > b.length)
        {
            return +1;
        }
        return 0;
    }

    @Deprecated
    public static String getJavaHome()
    {
        return Java.JAVA_HOME;
    }
    @Deprecated
    public static String getTmpDir()
    {
        return Java.JAVA_IO_TMPDIR;
    }
    @Deprecated
    public static String getOsName()
    {
        return Java.OS_NAME;
    }
    @Deprecated
    public static String getOsArch()
    {
        return Java.OS_ARCH;
    }
    @Deprecated
    public static String getOsVersion()
    {
        return Java.OS_VERSION;
    }
    @Deprecated
    public static String getUserName()
    {
        return Java.USER_NAME;
    }
    @Deprecated
    public static String getUserHome()
    {
        return Java.USER_HOME;
    }

    public static String getJavaClassPathCommon()
    {
        String[] cp = Java.JAVA_CLASS_PATH.split(File.pathSeparator);
        if (cp.length == 0)
        {
            return null;
        }
        if (cp.length == 1)
        {
            return new File(cp[0]).getAbsoluteFile().getParent();
        }
        int min = Integer.MAX_VALUE;
        for (String item : cp)
        {
            min = Math.min(min, item.length());
        }
        int common = 0;
        boolean eq = true;
        for (int i = 0; i < min && eq; i++)
        {
            int c = cp[0].charAt(i);
            for (int j = 1; j < cp.length && eq; j++)
            {
                eq = c == cp[j].charAt(i);
            }
            common = i;
        }
        return cp[0].substring(0, common);
    }

    public static <T> T firstNonNull(T... t)
    {
        for (T item : t)
        {
            if (item != null)
            {
                return item;
            }
        }
        return null;
    }

    public static <T> String firstNonNullOrEmpty(T... t)
    {
        for (T item : t)
        {
            String value;
            if (item != null && !(value = item.toString()).isEmpty())
            {
                return value;
            }
        }
        return "";
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds, subject to the
     * precision and accuracy of system timers and schedulers. The thread does
     * not lose ownership of any monitors.
     *
     * @param millis the length of time to sleep in milliseconds.
     * @return true if thread was interrupted; false otherwise.
     */
    public static boolean sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
            return false;
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return true;
        }
    }

    /**
     * Causes the currently executing thread to sleep (cease execution) for the
     * specified number of milliseconds plus the specified number of
     * nanoseconds, subject to the precision and accuracy of system timers and
     * schedulers. The thread does not lose ownership of any monitors.
     *
     * @param millis the length of time to sleep in milliseconds.
     * @param nanos 0-999999 additional nanoseconds to sleep.
     * @return true if thread was interrupted; false otherwise.
     */
    public static boolean sleep(long millis, int nanos)
    {
        try
        {
            Thread.sleep(millis, nanos);
            return false;
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return true;
        }
    }

    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds counted from a
     * System.nanoTime() moment, subject to the precision and accuracy of system
     * timers and schedulers. The thread does not lose ownership of any
     * monitors.
     *
     * @param fromNanos nanos returned from System.nanoTime() to start counting
     * from
     * @param sleepMillis the length of time to sleep in milliseconds.
     * @return true if thread was interrupted; false otherwise.
     */
    public static boolean sleepFrom(long fromNanos, long sleepMillis)
    {
        long diffNanos = fromNanos + TimeUnit.MILLISECONDS.toNanos(sleepMillis) - System.nanoTime();
        sleepMillis = (diffNanos > 0) ? TimeUnit.NANOSECONDS.toMillis(diffNanos) : 0;
        if (sleepMillis > 0)
        {
            return sleep(sleepMillis, 0);
        }
        return false;
    }
    
    public static byte[] cat(byte[] src, byte... next)
    {
        return join(src, next);
    }
    /**
     * Concatenate a series of elements to an int[] array.
     * @param src the starting array
     * @param next the values to concatenate
     * @return a new array with the resulting array
     */
    public static int[] cat(int[] src, int... next)
    {
        return join(src, next);
    }
    public static short[] cat(short[] src, short... next)
    {
        return join(src, next);
    }
    public static char[] cat(char[] src, char... next)
    {
        return join(src, next);
    }
    public static long[] cat(long[] src, long... next)
    {
        return join(src, next);
    }
    public static float[] cat(float[] src, float... next)
    {
        return join(src, next);
    }
    public static double[] cat(double[] src, double... next)
    {
        return join(src, next);
    }
    
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

    public static long adler32(byte[] bytes)
    {
        return adler32(bytes,0,bytes.length);
    }
    public static long adler32(byte[] bytes, int off, int len)
    {
        Adler32 adler32 = new Adler32();
        adler32.update(bytes, off, len);
        return adler32.getValue();
    }

    public static <T extends Comparable<T>> T max(T... t)
    {
        T m = null;
        if (t.length > 0)
        {
            for (int i = 0; i < t.length; i++)
            {
                T item = t[i];
                if (m == null || (item != null && item.compareTo(m) > 0))
                {
                    m = item;
                }
            }
        }
        return m;
    }

    public static <T extends Comparable<T>> T min(T... t)
    {
        T m = null;
        if (t.length > 0)
        {
            for (int i = 0; i < t.length; i++)
            {
                T item = t[i];
                if (m == null || (item != null && item.compareTo(m) < 0))
                {
                    m = item;
                }
            }
        }
        return m;
    }

    public static int min(int[] d)
    {
        int m = 0;
        if (d.length > 0)
        {
            m = d[0];
            for (int i = 1; i < d.length; i++)
            {
                int item = d[i];
                if (item < m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    public static int max(int[] d)
    {
        int m = 0;
        if (d.length > 0)
        {
            m = d[0];
            for (int i = 1; i < d.length; i++)
            {
                int item = d[i];
                if (item > m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    public static long min(long[] d)
    {
        long m = 0;
        if (d.length > 0)
        {
            m = d[0];
            for (int i = 1; i < d.length; i++)
            {
                long item = d[i];
                if (item < m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    public static long max(long[] d)
    {
        long m = 0;
        if (d.length > 0)
        {
            m = d[0];
            for (int i = 1; i < d.length; i++)
            {
                long item = d[i];
                if (item > m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    public static double min(double[] d)
    {
        double m = 0;
        if (d.length > 0)
        {
            m = d[0];
            for (int i = 1; i < d.length; i++)
            {
                double item = d[i];
                if (item < m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    public static double max(double[] d)
    {
        double m = 0;
        if (d.length > 0)
        {
            m = d[0];
            for (int i = 1; i < d.length; i++)
            {
                double item = d[i];
                if (item > m)
                {
                    m = item;
                }
            }
        }
        return m;
    }

    public static int minOf(int... d)
    {
        return min(d);
    }

    public static int maxOf(int... d)
    {
        return max(d);
    }

    public static long minOf(long... d)
    {
        return min(d);
    }

    public static long maxOf(long... d)
    {
        return max(d);
    }

    public static double minOf(double... d)
    {
        return min(d);
    }

    public static double maxOf(double... d)
    {
        return max(d);
    }

    public static byte[] sequence(byte[] items, byte first, byte step)
    {
        for (int i = 0; i < items.length; i++, first += step)
        {
            items[i] = first;
        }
        return items;
    }

    public static short[] sequence(short[] items, short first, short step)
    {
        for (int i = 0; i < items.length; i++, first += step)
        {
            items[i] = first;
        }
        return items;
    }

    public static char[] sequence(char[] items, char first, int step)
    {
        for (int i = 0; i < items.length; i++, first += step)
        {
            items[i] = first;
        }
        return items;
    }

    public static int[] sequence(int[] items, int first, int step)
    {
        for (int i = 0; i < items.length; i++, first += step)
        {
            items[i] = first;
        }
        return items;
    }

    public static long[] sequence(long[] items, long first, long step)
    {
        for (int i = 0; i < items.length; i++, first += step)
        {
            items[i] = first;
        }
        return items;
    }

    public static double[] sequence(double[] items, double first, double step)
    {
        for (int i = 0; i < items.length; i++, first += step)
        {
            items[i] = first;
        }
        return items;
    }
    
    public static boolean isPositive(BigInteger n)
    {
        return n.compareTo(BigInteger.ZERO) > 0;
    }

    public static boolean isPositive(BigDecimal n)
    {
        return n.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isPositiveOrZero(BigInteger n)
    {
        return n.compareTo(BigInteger.ZERO) >= 0;
    }

    public static boolean isPositiveOrZero(BigDecimal n)
    {
        return n.compareTo(BigDecimal.ZERO) >= 0;
    }

    public static boolean isNegative(BigInteger n)
    {
        return n.compareTo(BigInteger.ZERO) < 0;
    }

    public static boolean isNegative(BigDecimal n)
    {
        return n.compareTo(BigDecimal.ZERO) < 0;
    }

    public static boolean isNegativeOrZero(BigInteger n)
    {
        return n.compareTo(BigInteger.ZERO) <= 0;
    }

    public static boolean isNegativeOrZero(BigDecimal n)
    {
        return n.compareTo(BigDecimal.ZERO) <= 0;
    }

    public static boolean isZero(BigDecimal n)
    {
        return n.compareTo(BigDecimal.ZERO) == 0;
    }

    public static boolean isZero(BigDecimal n, BigDecimal delta)
    {
        return n.abs().compareTo(delta.abs()) <= 0;
    }

    public static boolean isNullOrZero(BigDecimal n)
    {
        return n == null || isZero(n);
    }

    public static boolean isNullOrZero(BigDecimal n, BigDecimal delta)
    {
        return n == null || isZero(n, delta);
    }

    public static BigDecimal nextGaussian(double gaussian, BigDecimal mean, BigDecimal deviation, BigDecimal min, BigDecimal max, boolean half)
    {
        assert min.compareTo(mean) <= 0;
        assert mean.compareTo(max) <= 0;

        BigDecimal value = half ? BigDecimal.valueOf(-Math.abs(gaussian)) : BigDecimal.valueOf(gaussian);

        value = value.multiply(deviation);
        value = value.add(mean);
        value = Utils.min(value, max);
        value = Utils.max(value, min);

        return value;
    }
    public static double nextGaussian(double gaussian, double mean, double deviation, double min, double max, boolean half)
    {
        assert min <= mean;
        assert mean <= max;

        double value = half ? Math.abs(gaussian) : gaussian;

        value *= deviation;
        value += mean;
        value = Math.min(value, max);
        value = Math.max(value, min);

        return value;
    }

    public static int nextGaussian(double gaussian, int mean, int deviation, int min, int max, boolean half)
    {
        assert min <= mean;
        assert mean <= max;

        double value = half ? Math.abs(gaussian) : gaussian;

        value *= deviation;
        value += mean;
        value = Math.min(value, max);
        value = Math.max(value, min);

        return (int) Math.round(value);
    }

    public static long nextGaussian(double gaussian, long mean, long deviation, long min, long max, boolean half)
    {
        assert min <= mean;
        assert mean <= max;

        double value = half ? Math.abs(gaussian) : gaussian;

        value *= deviation;
        value += mean;
        value = Math.min(value, max);
        value = Math.max(value, min);

        return Math.round(value);
    }
    public static BigDecimal nextGaussian(Random random, BigDecimal mean, BigDecimal deviation, BigDecimal min, BigDecimal max, boolean half)
    {
        return nextGaussian(random.nextGaussian(), mean, deviation, min, max, half);
    }
    public static double nextGaussian(Random random, double mean, double deviation, double min, double max, boolean half)
    {
        return nextGaussian(random.nextGaussian(), mean, deviation, min, max, half);
    }

    public static int nextGaussian(Random random, int mean, int deviation, int min, int max, boolean half)
    {
        return nextGaussian(random.nextGaussian(), mean, deviation, min, max, half);
    }

    public static long nextGaussian(Random random, long mean, long deviation, long min, long max, boolean half)
    {
        return nextGaussian(random.nextGaussian(), mean, deviation, min, max, half);
    }

    public static boolean between(byte a, byte b, byte value)
    {
        return (a <= b) ? a <= value && value <= b : b <= value && value <= a;
    }

    public static boolean between(int a, int b, int value)
    {
        return (a <= b) ? a <= value && value <= b : b <= value && value <= a;
    }

    public static boolean between(long a, long b, long value)
    {
        return (a <= b) ? a <= value && value <= b : b <= value && value <= a;
    }

    public static boolean between(float a, float b, float value)
    {
        return (a <= b) ? a <= value && value <= b : b <= value && value <= a;
    }

    public static boolean between(double a, double b, double value)
    {
        return (a <= b) ? a <= value && value <= b : b <= value && value <= a;
    }

    public static boolean between(BigDecimal a, BigDecimal b, BigDecimal value)
    {
        return (a.compareTo(b) <= 0) ? a.compareTo(value) <= 0 && value.compareTo(b) <= 0 : b.compareTo(value) <= 0 && value.compareTo(a) <= 0;
    }
    public static <T extends Comparable<T>> boolean between(T a, T b, T value)
    {
        return (a.compareTo(b) <= 0) ? a.compareTo(value) <= 0 && value.compareTo(b) <= 0 : b.compareTo(value) <= 0 && value.compareTo(a) <= 0;
    }

    public static byte bound(byte min, byte max, byte value)
    {
        assert (min <= max);
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    public static short bound(short min, short max, short value)
    {
        assert (min <= max);
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    public static int bound(int min, int max, int value)
    {
        assert (min <= max);
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    public static long bound(long min, long max, long value)
    {
        assert (min <= max);
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    public static float bound(float min, float max, float value)
    {
        assert (min <= max);
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    public static BigInteger bound(BigInteger min, BigInteger max, BigInteger value)
    {
        assert (min.compareTo(max) <= 0);
        if (value.compareTo(min) < 0)
        {
            return min;
        }
        if (value.compareTo(max) > 0)
        {
            return max;
        }
        return value;
    }

    public static BigDecimal bound(BigDecimal min, BigDecimal max, BigDecimal value)
    {
        assert (min.compareTo(max) <= 0);
        if (value.compareTo(min) < 0)
        {
            return min;
        }
        if (value.compareTo(max) > 0)
        {
            return max;
        }
        return value;
    }

    public static double bound(double min, double max, double value)
    {
        assert (min <= max);
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    public static int add(int... values)
    {
        int s = 0;
        for (int v : values)
        {
            s += v;
        }
        return s;
    }

    public static long add(long... values)
    {
        long s = 0;
        for (long v : values)
        {
            s += v;
        }
        return s;
    }

    public static double add(double... values)
    {
        double s = 0;
        for (double v : values)
        {
            s += v;
        }
        return s;
    }

    public static BigInteger add(BigInteger... values)
    {
        BigInteger s = BigInteger.ZERO;
        for (BigInteger v : values)
        {
            if (v != null)
            {
                s = s.add(v);
            }
        }
        return s;
    }

    public static BigDecimal add(BigDecimal... values)
    {
        BigDecimal s = BigDecimal.ZERO;
        for (BigDecimal v : values)
        {
            if (v != null)
            {
                s = s.add(v);
            }
        }
        return s;
    }

    public static BigDecimal add(MathContext mc, BigDecimal... values)
    {
        BigDecimal s = BigDecimal.ZERO;
        for (BigDecimal v : values)
        {
            if (v != null)
            {
                s = s.add(v, mc);
            }
        }
        return s;
    }

    /**
     * Returns {@code true} if the two arrays are equal to one another. When the
     * two arrays differ in length, trivially returns {@code false}. When the
     * two arrays are equal in length, does a constant-time comparison of the
     * two, i.e. does not abort the comparison when the first differing element
     * is found.
     *
     * <p>
     * NOTE: This is a copy of
     * {@code java/com/google/math/crypto/ConstantTime#arrayEquals}.
     *
     * @param a An array to compare
     * @param b Another array to compare
     * @return {@code true} if these arrays are both null or if they have equal
     * length and equal bytes in all elements
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b)
    {
        if (a == null || b == null)
        {
            return (a == b);
        }
        if (a.length != b.length)
        {
            return false;
        }
        byte result = 0;
        for (int i = 0; i < b.length; i++)
        {
            result = (byte) (result | a[i] ^ b[i]);
        }
        return (result == 0);
    }

    public static boolean constantTimeEquals(int[] a, int[] b)
    {
        if (a == null || b == null)
        {
            return (a == b);
        }
        if (a.length != b.length)
        {
            return false;
        }
        int result = 0;
        for (int i = 0; i < b.length; i++)
        {
            result = (int) (result | a[i] ^ b[i]);
        }
        return (result == 0);
    }

    public static byte[] safeCopy(byte[] src)
    {
        return src != null ? src.clone() : src;
    }

    public static int[] safeCopy(int[] src)
    {
        return src != null ? src.clone() : src;
    }

    public static byte[][] safeDeepCopy(byte[][] src)
    {
        if (src != null)
        {
            src = src.clone();
            for (int i = 0; i < src.length; i++)
            {
                src[i] = safeCopy(src[i]);
            }
        }
        return src;
    }

    public static int[][] safeDeepCopy(int[][] src)
    {
        if (src != null)
        {
            src = src.clone();
            for (int i = 0; i < src.length; i++)
            {
                src[i] = safeCopy(src[i]);
            }
        }
        return src;
    }


    public static byte[][] transpose(byte[][] a, byte defaultValue)
    {
        if (a == null)
        {
            return null;
        }
        if (a.length == 0)
        {
            return a;
        }
        int cols = 0;
        for (int i = 0; i < a.length; i++)
        {
            cols = Math.max(a[i].length, cols);
        }
        byte[][] b = new byte[cols][a.length];
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[i].length; j++)
            {
                b[j][i] = a[i][j];
            }
            if (defaultValue != 0)
            {
                for (int j = a[i].length; j < cols; j++)
                {
                    b[j][i] = defaultValue;
                }
            }
        }
        return b;
    }

    public static int[][] transpose(int[][] a, int defaultValue)
    {
        if (a == null)
        {
            return null;
        }
        if (a.length == 0)
        {
            return a;
        }
        int cols = 0;
        for (int i = 0; i < a.length; i++)
        {
            cols = Math.max(a[i].length, cols);
        }
        int[][] b = new int[cols][a.length];
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[i].length; j++)
            {
                b[j][i] = a[i][j];
            }
            if (defaultValue != 0)
            {
                for (int j = a[i].length; j < cols; j++)
                {
                    b[j][i] = defaultValue;
                }
            }
        }
        return b;
    }

    public static long[][] transpose(long[][] a, long defaultValue)
    {
        if (a == null)
        {
            return null;
        }
        if (a.length == 0)
        {
            return a;
        }
        int cols = 0;
        for (int i = 0; i < a.length; i++)
        {
            cols = Math.max(a[i].length, cols);
        }
        long[][] b = new long[cols][a.length];
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[i].length; j++)
            {
                b[j][i] = a[i][j];
            }
            if (defaultValue != 0)
            {
                for (int j = a[i].length; j < cols; j++)
                {
                    b[j][i] = defaultValue;
                }
            }
        }
        return b;
    }

    public static float[][] transpose(float[][] a, float defaultValue)
    {
        if (a == null)
        {
            return null;
        }
        if (a.length == 0)
        {
            return a;
        }
        int cols = 0;
        for (int i = 0; i < a.length; i++)
        {
            cols = Math.max(a[i].length, cols);
        }
        float[][] b = new float[cols][a.length];
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[i].length; j++)
            {
                b[j][i] = a[i][j];
            }
            if (defaultValue != 0)
            {
                for (int j = a[i].length; j < cols; j++)
                {
                    b[j][i] = defaultValue;
                }
            }
        }
        return b;
    }

    public static double[][] transpose(double[][] a, double defaultValue)
    {
        if (a == null)
        {
            return null;
        }
        if (a.length == 0)
        {
            return a;
        }
        int cols = 0;
        for (int i = 0; i < a.length; i++)
        {
            cols = Math.max(a[i].length, cols);
        }
        double[][] b = new double[cols][a.length];
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[i].length; j++)
            {
                b[j][i] = a[i][j];
            }
            if (defaultValue != 0)
            {
                for (int j = a[i].length; j < cols; j++)
                {
                    b[j][i] = defaultValue;
                }
            }
        }
        return b;
    }

    public static <T> T[][] transpose(T[][] a, T defaultValue)
    {
        if (a == null)
        {
            return null;
        }
        if (a.length == 0)
        {
            return a;
        }
        int cols = 0;
        T[] witness = null;
        for (int i = 0; i < a.length; i++)
        {
            if (a[i] != null)
            {
                cols = Math.max(a[i].length, cols);
                witness = witness != null && a[i].length > 0 ? witness : a[i];
            }
        }
        if (witness == null)
        {
            return Arrays.copyOf(a, 0);
        }
        T[][] b = Arrays.copyOf(a, cols);
        for (int i = 0; i < cols; i++)
        {
            b[i] = Arrays.copyOf(witness, a.length);
        }

        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[i].length; j++)
            {
                b[j][i] = a[i][j];
            }
            if (defaultValue != null)
            {
                for (int j = a[i].length; j < cols; j++)
                {
                    b[j][i] = defaultValue;
                }
            }
        }
        return b;
    }

    public static byte[] bigEndian(short value)
    {
        return ByteBuffer.allocate(Short.BYTES).putShort(value).order(ByteOrder.BIG_ENDIAN).array();
    }

    public static byte[] littleEndian(short value)
    {
        return ByteBuffer.allocate(Short.BYTES).putShort(value).order(ByteOrder.LITTLE_ENDIAN).array();
    }

    public static byte[] bigEndian(int value)
    {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).order(ByteOrder.BIG_ENDIAN).array();
    }

    public static byte[] littleEndian(int value)
    {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).order(ByteOrder.LITTLE_ENDIAN).array();
    }

    public static byte[] bigEndian(long value)
    {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).order(ByteOrder.BIG_ENDIAN).array();
    }

    public static byte[] littleEndian(long value)
    {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).order(ByteOrder.LITTLE_ENDIAN).array();
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static <T> T[] unique(T... items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<T> set = new HashSet<>(items.length);
        int count = 0;
        for (T item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static byte[] unique(byte[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Byte> set = new HashSet<>(items.length);
        int count = 0;
        for (byte item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static short[] unique(short[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Short> set = new HashSet<>(items.length);
        int count = 0;
        for (short item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static char[] unique(char[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Character> set = new HashSet<>(items.length);
        int count = 0;
        for (char item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static int[] unique(int[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Integer> set = new HashSet<>(items.length);
        int count = 0;
        for (int item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static long[] unique(long[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Long> set = new HashSet<>(items.length);
        int count = 0;
        for (long item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    /**
     * Returns the array passed as parameter removing repeated items and keeping
     * the order. It will return the original array if no item is removed.
     *
     * @param items the array with the items
     * @return an array with only a copy of each item of the array
     */
    public static double[] unique(double[] items)
    {
        if (items == null || items.length == 0)
        {
            return items;
        }
        items = items.clone();

        HashSet<Double> set = new HashSet<>(items.length);
        int count = 0;
        for (double item : items)
        {
            if (set.add(item))
            {
                items[count++] = item;
            }
        }
        return count < items.length ? Arrays.copyOf(items, count) : items;
    }

    public static String getStringClipboard()
    {
        try
        {
            // Get the system clipboard
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            // Get the contents of the clipboard
            Transferable contents = clipboard.getContents(null);
            return (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) ? (String) contents.getTransferData(DataFlavor.stringFlavor) : null;
        }
        catch (IOException | UnsupportedFlavorException ex)
        {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    public static void setStringClipboard(String data)
    {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(data), null);
    }

    /**
     * Creates a copy of the items passed as parameter removing the null values
     * and keeping the order.
     *
     * @param <E>
     * @param e
     * @return an array with the non-null items
     */
    public static <E> E[] nonNull(E... e)
    {
        if (e != null && e.length > 0)
        {
            e = e.clone();
            int count = 0;
            for (int i = 0; i < e.length; i++)
            {
                if (e[i] != null)
                {
                    if (count != i)
                    {
                        e[count++] = e[i];
                    }
                    else
                    {
                        count++;
                    }
                }
            }
            e = count < e.length ? Arrays.copyOf(e, count) : e;
        }
        return e;
    }

    public static <T> void foreach(Consumer<? super T> action, T... items)
    {
        foreach(1, action, items);
    }

    public static <T> void foreach(int threads, Consumer<? super T> action, T... items)
    {
        if (threads == 1)
        {
            for (T t : items)
            {
                action.accept(t);
            }
        }
        else
        {
            try
            {
                int nth = threads <= 0 ? Runtime.getRuntime().availableProcessors() * 2 : threads;
                ExecutorService executor = Executors.newFixedThreadPool​(Math.min(nth, items.length));
                for (T t : items)
                {
                    executor.execute(() -> action.accept(t));
                }
                executor.shutdown();
                executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Creates a new Map with the inverse relation key-map
     * @param <K> Key
     * @param <V> Value
     * @param map source Map
     * @return the inverse Map
     */
    public static <K, V> Map<V, K> inverse(Map<K, V> map)
    {
        Map<V, K> inverse = new HashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet())
        {
            inverse.put(entry.getValue(), entry.getKey());
        }
        return inverse;
    }
    

    public static String toJava(byte[] data)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("byte[] data = {");
        String sep = " ";

        for (int i = 0; i < data.length; i++)
        {
            sb.append(sep).append(data[i]);
            sep = ", ";
        }
        sb.append("}");
        return sb.toString();
    }

    public static String toJavaXor(String name, byte[] data)
    {
        StringBuilder sb = new StringBuilder();
        long xor = Math.abs(new Random().nextLong());
        data = rollXor(data, xor);
        sb.append("long ").append(name).append("Xor = ").append(xor).append("L;\n");
        sb.append("byte[] ").append(name).append(" = {");
        String sep = " ";
        for (int i = 0; i < data.length; i++)
        {
            sb.append(sep).append(data[i]);
            sep = ", ";
        }
        sb.append("};").append("\n");
        return sb.toString();
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

    /**
     * Close the {@link Closeable} ignoring any {@link IOException}
     *
     * @param closeable The {@link Closeable} to close
     */
    public static void closeQuietly(Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch (final IOException ignored)
            {
                // Nothing to do
            }
        }
    }

    public static URL parseURL(String url)
    {
        if (url == null)
        {
            return null;
        }
        try
        {
            return new URL(url);
        }
        catch (final MalformedURLException e)
        {
            // This should not happen.
        }
        return null;
    }

    public static <E> boolean equals(E e1, E e2)
    {
        if (e1 == e2)
        {
            return true;
        }
        if (e1 != null)
        {
            return e1.equals(e2);
        }
        if (e2 != null)
        {
            return e2.equals(e1);
        }
        return false;
    }

    public static <E extends Enum<?>> boolean equals(E e1, E e2)
    {
        return (e1 == e2);
    }


    public static String getLocalHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException ex)
        {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static String getCanonicalHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (UnknownHostException ex)
        {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    //----------------------------------------------------------------------------------------------

//    public static <T extends Comparable<T>> T min(T a, T b)
//    {
//        return a.compareTo(b) <= 0 ? a : b;
//    }
//    public static <T extends Comparable<T>> T max(T a, T b)
//    {
//        return a.compareTo(b) >= 0 ? a : b;
//    }
    public static String format(double value, int decimals)
    {
        return String.format(Locale.ROOT, ("%." + decimals + "f"), value);
    }

    public static String format(double value, int decimals, char group)
    {
        String txt = format(value, decimals);
        if (value > 10000.0 || value < -10000.0)
        {
            int first = value < 0.0 ? 1 : 0;
            int index = txt.indexOf('.');
            StringBuilder sb = new StringBuilder(txt);
            for (int i = index - 3; i > first; i -= 3)
            {
                sb.insert(i, group);
            }
            txt = sb.toString();
        }
        return txt;
    }

    public static int[] exclude(int poison, int... values)
    {
        int[] val = new int[values.length];
        int count = 0;
        for (int i = 0; i < values.length; i++)
        {
            if (values[i] != poison)
            {
                val[count++] = values[i];
            }
        }
        return Arrays.copyOf(val, count);
    }

    public static double[] exclude(double poison, double... values)
    {
        double[] val = new double[values.length];
        int count = 0;
        for (int i = 0; i < values.length; i++)
        {
            if (values[i] != poison)
            {
                val[count++] = values[i];
            }
        }
        return Arrays.copyOf(val, count);
    }

    public static <T> T[] excludeEquals(T poison, T... values)
    {
        T[] val = values.clone();
        int count = 0;
        for (int i = 0; i < values.length; i++)
        {
            if (!Utils.equals(values[i], poison))
            {
                val[count++] = values[i];
            }
        }
        return Arrays.copyOf(val, count);
    }

    public static <T> T[] excludeSame(T poison, T... values)
    {
        T[] val = values.clone();
        int count = 0;
        for (int i = 0; i < values.length; i++)
        {
            if ((values[i] != poison))
            {
                val[count++] = values[i];
            }
        }
        return Arrays.copyOf(val, count);
    }

    public static BigDecimal[] exclude(BigDecimal poison, BigDecimal... values)
    {
        BigDecimal[] val = new BigDecimal[values.length];
        int count = 0;
        for (int i = 0; i < values.length; i++)
        {
            if (values[i] == poison)
            {
                continue;
            }
            if (values[i] != null && values[i].compareTo(poison) == 0)
            {
                continue;
            }
            val[count++] = values[i];
        }
        return Arrays.copyOf(val, count);
    }

    public static <K, V> V equivalent(K item, K[] keys, V[] values, V defaultValue)
    {
        //enums do not need to use equals method
        boolean equals = !(item instanceof Enum);

        for (int i = 0; i < keys.length && i < values.length; i++)
        {
            if (keys[i] == item)
            {
                return values[i];
            }
            if (equals && keys[i] != null && keys[i].equals(item))
            {
                return values[i];
            }
        }
        return defaultValue;
    }

    public static <K, V> V equivalent(K item, K[] keys, V[] values)
    {
        return equivalent(item, keys, values, null);
    }


    public static void log(Logger logger, Level level, Supplier<String> msg)
    {
        if (logger.isLoggable(level))
        {
            logger.log(level, msg);
        }
    }

    public static void log(Logger logger, Level level, Throwable thrown, Supplier<String> msg)
    {
        if (logger.isLoggable(level))
        {
            logger.log(level, thrown, msg);
        }
    }

    public static int findFirst(byte[] items, byte value, int defaultValue)
    {
        for(int i=0;i<items.length;i++)
        {
            if(items[i]==value)
            {
                return i;
            }
        }
        return defaultValue;
    }
    public static int findFirst(int[] items, int value, int defaultValue)
    {
        for(int i=0;i<items.length;i++)
        {
            if(items[i]==value)
            {
                return i;
            }
        }
        return defaultValue;
    }

    public static int mismatch(byte[] a, byte[] b)
    {
        if (a == b)
        {
            return -1;
        }
        if (a == null || b == null)
        {
            throw new NullPointerException();
        }
        int i;
        for (i = 0; i < a.length && i < b.length; i++)
        {
            if (a[i] != b[i])
            {
                return i;
            }
        }
        if (i < a.length || i < b.length)
        {
            return i;
        }
        return -1;
    }
    public static byte[] fill(byte[] buffer, byte value)
    {
        for(int i=0;i<buffer.length;i++)
        {
            buffer[i] = value;
        }
        return buffer;
    }
    public static int[] fill(int[] buffer, int value)
    {
        for(int i=0;i<buffer.length;i++)
        {
            buffer[i] = value;
        }
        return buffer;
    }
    public static void checkArgument(boolean condition, String message)
    {
        if(condition==false)
        {
            throw new IllegalArgumentException(message);
        }
    }
    public static void checkKey(boolean condition, String message) throws InvalidKeyException
    {
        if(condition==false)
        {
            throw new InvalidKeyException(message);
        }
    }
    
    /**
     * Create a type-safe generic array.
     * <p>
     * <pre>
    String[] array = Utils.toArray("1", "2");
    String[] emptyArray = Utils.&lt;String&gt;toArray();
     * </pre>
     *
     * @param  <T>   the array's element type
     * @param  items  the varargs array items, null allowed
     * @return the array, not null unless a null array is passed in
     */
    public static <T> T[] toArray(final T... items) 
    {
        return items;
    }       
                              
    //java9 new BigInteger​​(int signum, byte[] magnitude, int off, int len)
    public static BigInteger newBigInteger(int signum, byte[] magnitude, int off, int len)
    {
        return new BigInteger(signum, Arrays.copyOfRange(magnitude, off, off+len));
    }
    
    //java9 new BigInteger​​(byte[] val, int off, int len)
    public static BigInteger newBigInteger​(byte[] val, int off, int len)
    {
        return new BigInteger(Arrays.copyOfRange(val, off, off+len));
    }
    
    //java9 List.of(E... e)
    public static <E> List<E> listOf(E... e)
    {
        ArrayList<E> list = new ArrayList<>(e.length);
        Collections.addAll(list, e);
        return Collections.unmodifiableList(list);
    }
    
    
}
