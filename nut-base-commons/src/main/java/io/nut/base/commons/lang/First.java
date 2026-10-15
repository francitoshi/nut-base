/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.lang;

import java.util.function.Supplier;

/**
 *
 * @author franci
 */
public class First
{
    
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

    /**
     * Returns the value supplied by the first supplier whose result is
     * non-<code>null</code>, or <code>null</code> if none of them is.
     *
     * <p>Suppliers are evaluated lazily and in order: a supplier is only
     * invoked when every previous supplier has returned <code>null</code>,
     * and no supplier after the first non-<code>null</code> result is ever
     * invoked.</p>
     *
     * <p>A <code>null</code> supplier is skipped without being invoked.</p>
     *
     * @param <T> the type of the supplied values
     * @param suppliers the suppliers to evaluate, may be empty or contain <code>null</code>s
     * @return the first non-<code>null</code> value supplied, or <code>null</code> if none
     */
    @SafeVarargs
    public static <T> T firstNonNull(Supplier<T>... suppliers)
    {
        for (Supplier<T> supplier : suppliers)
        {
            if (supplier != null)
            {
                T value = supplier.get();
                if (value != null)
                {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Returns the {@code toString()} of the first value that is neither
     * <code>null</code> nor empty, or {@code ""} if none of them is.
     *
     * @param <T> the type of the values
     * @param t the values to check, may be empty or contain <code>null</code>s
     * @return the {@code toString()} of the first non-<code>null</code>,
     *         non-empty value, or {@code ""} if none
     */
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
     * Returns the {@code toString()} of the first supplier whose result is
     * neither <code>null</code> nor empty, or {@code ""} if none of them is.
     *
     * <p>Suppliers are evaluated lazily and in order: a supplier is only
     * invoked when every previous supplier has returned <code>null</code> or
     * an empty string, and no supplier after the first acceptable result is
     * ever invoked.</p>
     *
     * <p>A <code>null</code> supplier is skipped without being invoked.</p>
     *
     * @param <T> the type of the supplied values
     * @param suppliers the suppliers to evaluate, may be empty or contain <code>null</code>s
     * @return the {@code toString()} of the first non-<code>null</code>,
     *         non-empty value supplied, or {@code ""} if none
     */
    @SafeVarargs
    public static <T> String firstNonNullOrEmpty(Supplier<T>... suppliers)
    {
        for (Supplier<T> supplier : suppliers)
        {
            if (supplier != null)
            {
                T value = supplier.get();
                if (value != null)
                {
                    String str = value.toString();
                    if (!str.isEmpty())
                    {
                        return str;
                    }
                }
            }
        }
        return "";
    }

    public static <T> T firstNonNullOrPoison(T poison, T... t)
    {
        for (T item : t)
        {
            if (item != null && !item.equals(poison))
            {
                return item;
            }
        }
        return null;
    }
    public static <T> T firstNonNullOrPoison(T[] poison, T... t)
    {
        for (T item : t)
        {
            if (item != null)
            {
                boolean poisoned = false;
                for(T p : poison)
                {
                    if(item.equals(p))
                    {
                        poisoned = true;
                        break;
                    }
                }
                if(!poisoned)
                {
                    return item;
                }
            }
        }
        return null;
    }
    public static <T> T firstNonNullOrPoisonLenient(T poison, T... t)
    {
        T ret = firstNonNullOrPoison(poison, t);
        return ret==null ? firstNonNull(t) : ret;
    }
    public static <T> T firstNonNullOrPoisonLenient(T[] poison, T... t)
    {
        T ret = firstNonNullOrPoison(poison, t);
        return ret==null ? firstNonNull(t) : ret;
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
         
}
