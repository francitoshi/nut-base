/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.util;

import io.github.francitoshi.nut.base.commons.lang.Comparators;
import io.github.francitoshi.nut.base.commons.lang.Exceptions;
import io.github.francitoshi.nut.base.commons.lang.Java;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public abstract class Utils
{    
    private static final Logger LOG = Logger.getLogger(Utils.class.getName());

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
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

    
    public static <E> List<E> append(List<E> list, E... items)
    {
        list.addAll(Arrays.asList(items));
        return list;
    }

    public static <E> Set<E> append(Set<E> set, E... items)
    {
        set.addAll(Arrays.asList(items));
        return set;
    }

    public static <E> Queue<E> append(Queue<E> queue, E... items)
    {
        queue.addAll(Arrays.asList(items));
        return queue;
    }
    
    /**
     * Returns the minimum value among the given elements, or null if none are given.
     * <p>
     * Null elements are skipped.
     *
     * @param t the values whose minimum is to be computed.
     * @return the minimum value, or null if t is empty or contains only nulls.
     */
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

    /**
     * Finds the minimum and maximum values in the given byte array.
     *
     * @param d the byte array
     * @return a two-element array containing the minimum and maximum values, or null if the input array is null.
     *         If the array is empty, returns {0, 0}.
     */
    /**
     * Finds the minimum and maximum values in the given boolean array.
     * False is considered less than true.
     *
     * @param d the boolean array
     * @return a two-element array containing the minimum and maximum values, or null if the input array is null.
     *         If the array is empty, returns {false, false}.
     */
    public static boolean[] minMax(boolean[] d)
    {
        if (d == null)
        {
            return null;
        }
        if (d.length == 0)
        {
            return new boolean[]{false, false};
        }
        boolean min = d[0];
        boolean max = d[0];
        for (int i = 1; i < d.length; i++)
        {
            if (!min && max)
            {
                break;
            }
            boolean val = d[i];
            if (!val)
            {
                min = false;
            }
            else
            {
                max = true;
            }
        }
        return new boolean[]{min, max};
    }

    /**
     * Finds the minimum and maximum values in the given array of Comparable objects.
     * Null elements in the array are ignored.
     *
     * @param <T> the type of elements in the array, which must implement Comparable
     * @param d the array of Comparable elements
     * @return a two-element array containing the minimum and maximum values, or null if the input array is null.
     *         If the array is empty, returns {null, null}.
     */
    public static <T extends Comparable<T>> T[] minMax(T[] d)
    {
        if (d == null)
        {
            return null;
        }
        T[] result = java.util.Arrays.copyOf(d, 2);
        T min = null;
        T max = null;
        for (int i = 0; i < d.length; i++)
        {
            T val = d[i];
            if (val != null)
            {
                if (min == null || val.compareTo(min) < 0)
                {
                    min = val;
                }
                if (max == null || val.compareTo(max) > 0)
                {
                    max = val;
                }
            }
        }
        result[0] = min;
        result[1] = max;
        return result;
    }

    /**
     * Finds the minimum and maximum values in the given Iterable of Comparable objects.
     * Null elements in the Iterable are ignored.
     *
     * @param <T> the type of elements in the Iterable, which must implement Comparable
     * @param d the Iterable of Comparable elements
     * @return a two-element array containing the minimum and maximum values, or null if the input Iterable is null.
     *         If the Iterable is empty or contains only null elements, returns a two-element array with null values.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> T[] minMax(Iterable<T> d)
    {
        if (d == null)
        {
            return null;
        }
        T min = null;
        T max = null;
        for (T val : d)
        {
            if (val != null)
            {
                if (min == null || val.compareTo(min) < 0)
                {
                    min = val;
                }
                if (max == null || val.compareTo(max) > 0)
                {
                    max = val;
                }
            }
        }
        
        Class<?> clazz = null;
        if (min != null)
        {
            clazz = min.getClass();
        }
        else
        {
            for (T val : d)
            {
                if (val != null)
                {
                    clazz = val.getClass();
                    break;
                }
            }
        }
        
        if (clazz == null)
        {
            clazz = Comparable.class;
        }
        
        T[] result = (T[]) java.lang.reflect.Array.newInstance(clazz, 2);
        result[0] = min;
        result[1] = max;
        return result;
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

    /**
     * Returns whether all the given values are between a and b, inclusive, regardless of order.
     *
     * @param <T> the type of the values, which must be comparable.
     * @param a the first bound.
     * @param b the second bound.
     * @param values the values to check.
     * @return true if every value is between a and b (inclusive), or true if no values are given; false otherwise.
     */
    public static <T extends Comparable<T>> boolean between(T a, T b, T... values)
    {
        if (values == null || values.length == 0)
        {
            return true;
        }
        for (T value : values)
        {
            if (!((a.compareTo(b) <= 0) ? a.compareTo(value) <= 0 && value.compareTo(b) <= 0 : b.compareTo(value) <= 0 && value.compareTo(a) <= 0))
            {
                return false;
            }
        }
        return true;
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

    public static <T> Set<T> union(Set<T>... sets)
    {
        HashSet<T> ret = new HashSet<>();
        for(Set<T> item : sets)
        {
            ret.addAll(item);
        }
        return ret;
    }
    
    public static <T> Set<T> intersection(Set<T>... sets)
    {
        HashSet<T> ret = new HashSet<>();
        if(sets.length==0)
        {
            return ret;
        }

        sets = sets.clone();

        Arrays.sort(sets, Comparators.COLLECTION_SIZE_COMPARATOR);
        
        ret.addAll(sets[0]);

        for(T item : sets[0])
        {
            for(int i=1;i<sets.length;i++)
            {
                if(!sets[i].contains(item))
                {
                    ret.remove(item);
                    break;
                }
            }
        }
        return ret;
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
            Exceptions.severe(LOG, ex);
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

    /**
     * Checks whether the given array is {@code null} or contains {@code null} 
     * elements.
     *
     * @param e the array to check, may be {@code null}
     * @param <E> the type of elements in the array
     * @return {@code true} if the array is {@code null} or any element is 
     * {@code null}; {@code false} otherwise
     */
    public static <E> boolean hasNulls(E... e)
    {
        if (e == null)
        {
            return true;
        }
        for (E item : e)
        {
            if (item == null)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the given array is {@code null}, is empty or contains {@code null} 
     * elements.
     *
     * @param e the array to check, may be {@code null}
     * @param <E> the type of elements in the array
     * @return {@code true} if the array is {@code null}, is empty or any element is 
     * {@code null}; {@code false} otherwise
     */
    public static <E> boolean hasNullsOrEmpty(E... e)
    {
        if (e == null || e.length==0)
        {
            return true;
        }
        for (E item : e)
        {
            if (item == null)
            {
                return true;
            }
        }
        return false;
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

    public static String getLocalHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException ex)
        {
            Exceptions.severe(LOG, ex);
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
            Exceptions.severe(LOG, ex);
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
            if (!Comparators.equals(values[i], poison))
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

    public static int mismatch(byte[] a, byte[] b)
    {
        if (a == b)
        {
            return -1;
        }
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");

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
                              
    public static <T> int countColums(T[][] cells)
    {
        int cols = 0;
        if (cells != null)
        {
            for (T[] row : cells)
            {
                cols = Math.max(cols, row != null ? row.length : 0);
            }
        }
        return cols;
    }

    public static int countColums(byte[][] cells)
    {
        int cols = 0;
        if (cells != null)
        {
            for (byte[] row : cells)
            {
                cols = Math.max(cols, row != null ? row.length : 0);
            }
        }
        return cols;
    }

    public static int countColums(short[][] cells)
    {
        int cols = 0;
        if (cells != null)
        {
            for (short[] row : cells)
            {
                cols = Math.max(cols, row != null ? row.length : 0);
            }
        }
        return cols;
    }

    public static int countColums(char[][] cells)
    {
        int cols = 0;
        if (cells != null)
        {
            for (char[] row : cells)
            {
                cols = Math.max(cols, row != null ? row.length : 0);
            }
        }
        return cols;
    }

    public static int countColums(int[][] cells)
    {
        int cols = 0;
        if (cells != null)
        {
            for (int[] row : cells)
            {
                cols = Math.max(cols, row != null ? row.length : 0);
            }
        }
        return cols;
    }

    public static int countColums(long[][] cells)
    {
        int cols = 0;
        if (cells != null)
        {
            for (long[] row : cells)
            {
                cols = Math.max(cols, row != null ? row.length : 0);
            }
        }
        return cols;
    }

    public static int countColums(float[][] cells)
    {
        int cols = 0;
        if (cells != null)
        {
            for (float[] row : cells)
            {
                cols = Math.max(cols, row != null ? row.length : 0);
            }
        }
        return cols;
    }

    public static int countColums(double[][] cells)
    {
        int cols = 0;
        if (cells != null)
        {
            for (double[] row : cells)
            {
                cols = Math.max(cols, row != null ? row.length : 0);
            }
        }
        return cols;
    }
    
    /**
     * Creates, configures, and starts a new {@link Thread} to execute the given
     * task.
     *
     * <p>
     * This method simplifies thread creation by allowing the caller to specify
     * the task, thread name, and daemon status. The thread is immediately
     * started after creation.</p>
     *
     * @param action the {@link Runnable} task to be executed in the new thread
     * @param name the name to assign to the new thread
     * @param daemon {@code true} if the new thread should be a daemon thread;
     * {@code false} otherwise
     * @return the {@link Thread} that was created and started
     */
    public static Thread execute(Runnable action, String name, boolean daemon)
    {
        Thread thread = new Thread(action, name);
        thread.setDaemon(daemon);
        thread.start();
        return thread;
    }
    
    /**
     * Creates and starts a new {@link Thread} to execute the given task.
     *
     * <p>
     * This method simplifies thread creation by allowing the caller to specify
     * the task. The thread is immediately started after creation.</p>
     *
     * @param action the {@link Runnable} task to be executed in the new thread
     * @return the {@link Thread} that was created and started
     */
    public static Thread execute(Runnable action)
    {
        Thread thread = new Thread(action);
        thread.start();
        return thread;
    }

    public static void ansiTitle(String s)
    {
        ansiTitle(System.out, s);
    }
    public static void ansiTitle(PrintStream out, String s)
    {
        out.printf("\033]0;%s\007",s);
    }
    

    /**
     * Executes the given {@link Runnable} asynchronously using the {@link ForkJoinPool#commonPool()}.
     * The task starts immediately upon calling this method.
     *
     * <p>This is the static utility variant of
     * {@link YourClass#async(Runnable) async(Runnable)}: it behaves identically
     * but uses the JVM-wide common pool instead of a dedicated {@link java.util.concurrent.ThreadPoolExecutor}.
     * Prefer this when no custom pool configuration is needed.</p>
     *
     * <pre>{@code
     * Future<Void> task = Utils.async(() -> { sort(a); sort(b); });
     * task.get(); // blocks until done
     * }</pre>
     *
     * @param runnable the task to execute asynchronously
     * @return a {@link Future} that completes with {@code null} when the task finishes
     * @see ForkJoinPool#commonPool()
     */    
    public static Future<Void> async(Runnable runnable)
    {
        return CompletableFuture.runAsync(runnable, ForkJoinPool.commonPool());
    }

    /**
     * Executes the given {@link Supplier} asynchronously using the {@link ForkJoinPool#commonPool()},
     * returning its result through a {@link Future}.
     * The task starts immediately upon calling this method.
     *
     * <p>This is the static utility variant of
     * {@link Utils#async(Supplier) async(Supplier)}: it behaves identically
     * but uses the JVM-wide common pool instead of a dedicated {@link java.util.concurrent.ThreadPoolExecutor}.
     * Prefer this when no custom pool configuration is needed.</p>
     *
     * <pre>{@code
     * Future<List<Integer>> task = Utils.async(() -> join(sort(a), sort(b)));
     * List<Integer> result = task.get(); // blocks until done
     * }</pre>
     *
     * @param <U>      the type of the result produced by the supplier
     * @param supplier the task to execute asynchronously
     * @return a {@link Future} that will hold the supplier's return value upon completion
     * @see ForkJoinPool#commonPool()
     */
    public static <U> Future<U> async(Supplier<U> supplier)
    {
        return CompletableFuture.supplyAsync(supplier, ForkJoinPool.commonPool());
    }

    /**
     * Wraps the given {@link Runnable} in a lazy {@link Future} that only executes
     * when {@link Future#get()} is called. The task runs on the calling thread at that point,
     * not on the internal thread pool.
     *
     * <pre>{@code
     * Future<Void> task = Utils.lazy(() -> { sort(a); sort(b); });
     * // nothing runs yet...
     * task.get(); // executes and blocks here
     * }</pre>
     *
     * @param runnable the task to execute lazily
     * @return a {@link Future} that executes the task on demand and completes with {@code null}
     */
    public static Future<Void> lazy(Runnable runnable)
    {
        return new FutureTask<Void>(runnable, null)
        {
            @Override
            public Void get() throws InterruptedException, ExecutionException
            {
                run();
                return super.get();
            }
            @Override
            public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
            {
                run(); // Run the process only when the result is requested
                return super.get(timeout, unit);
            }
        };
    }

    /**
     * Wraps the given {@link Supplier} in a lazy {@link Future} that only executes
     * when {@link Future#get()} or {@link Future#get(long, TimeUnit)} is called.
     * The task runs on the calling thread at that point, not on the internal thread pool.
     *
     * <pre>{@code
     * Future<List<Integer>> task = Utils.lazy(() -> join(sort(a), sort(b)));
     * // nothing runs yet...
     * List<Integer> result = task.get();                  // executes and returns here
     * List<Integer> result = task.get(5, TimeUnit.SECONDS); // with timeout
     * }</pre>
     *
     * @param <U>      the type of the result produced by the supplier
     * @param supplier the task to execute lazily
     * @return a {@link Future} that executes the supplier on demand and returns its value
     */
    public static <U> Future<U> lazy(Supplier<U> supplier)
    {
        // We created a FutureTask that involves the supplier.
        // We use supplier::get to adapt it to the Callable interface that FutureTask requires.
        return new FutureTask<U>(supplier::get)
        {
            @Override
            public U get() throws InterruptedException, ExecutionException
            {
                run(); //Run the process only when the result is requested
                return super.get();
            }

            @Override
            public U get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
            {
                run(); // Run the process only when the result is requested
                return super.get(timeout, unit);
            }
        };
    }
    
    public static void arraycopy(byte[] src, int srcPos, byte[] dst, int dstPos, int length)
    {
        System.arraycopy(src, srcPos, dst, dstPos, length); 
    }
    public static void arraycopy(int[] src, int srcPos, int[] dst, int dstPos, int length)
    {
        System.arraycopy(src, srcPos, dst, dstPos, length); 
    }
    public static void arraycopy(long[] src, int srcPos, long[] dst, int dstPos, int length)
    {
        System.arraycopy(src, srcPos, dst, dstPos, length); 
    }
    public static void arraycopy(short[] src, int srcPos, short[] dst, int dstPos, int length)
    {
        System.arraycopy(src, srcPos, dst, dstPos, length); 
    }
    public static void arraycopy(char[] src, int srcPos, char[] dst, int dstPos, int length)
    {
        System.arraycopy(src, srcPos, dst, dstPos, length); 
    }
    public static void arraycopy(float[] src, int srcPos, float[] dst, int dstPos, int length)
    {
        System.arraycopy(src, srcPos, dst, dstPos, length); 
    }
    public static void arraycopy(double[] src, int srcPos, double[] dst, int dstPos, int length)
    {
        System.arraycopy(src, srcPos, dst, dstPos, length); 
    }

}
