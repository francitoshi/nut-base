/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class Comparators
{    
    private static final Logger LOG = Logger.getLogger(Comparators.class.getName());

    public static final Comparator<Collection<?>> COLLECTION_SIZE_COMPARATOR = new Comparator<Collection<?>>() 
    {
        @Override
        public int compare(Collection<?> a, Collection<?> b) 
        {
            return Integer.compare(a.size(), b.size());
        }
    };
    
    public static final Comparator<Object[]> ARRAY_SIZE_COMPARATOR = new Comparator<Object[]>() 
    {
        @Override
        public int compare(Object[] a, Object[] b) 
        {
            return Integer.compare(a.length, b.length);
        }
    };
    
    public static final Comparator<byte[]> BYTE_ARRAY_SIZE_COMPARATOR = new Comparator<byte[]>() 
    {
        @Override
        public int compare(byte[] a, byte[] b) 
        {
            return Integer.compare(a.length, b.length);
        }
    };
    
    public static final Comparator<int[]> INT_ARRAY_SIZE_COMPARATOR = new Comparator<int[]>() 
    {
        @Override
        public int compare(int[] a, int[] b) 
        {
            return Integer.compare(a.length, b.length);
        }
    };
    
    public static final Comparator<long[]> LONG_ARRAY_SIZE_COMPARATOR = new Comparator<long[]>() 
    {
        @Override
        public int compare(long[] a, long[] b) 
        {
            return Integer.compare(a.length, b.length);
        }
    };
        
    //java9 Arrays.compare(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex)
    public static int compare(byte[] a, int aFrom, int aTo, byte[] b, int bFrom, int bTo)
    {
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");
        
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

    public static int compare(char[] a, char[] b)
    {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++)
        {
            int cmp = Character.compare(a[i], b[i]);
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

    public static int compare(short[] a, short[] b)
    {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++)
        {
            int cmp = Short.compare(a[i], b[i]);
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

    public static int compare(float[] a, float[] b)
    {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++)
        {
            int cmp = Float.compare(a[i], b[i]);
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

    public static int compare(boolean[] a, boolean[] b)
    {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++)
        {
            int cmp = Boolean.compare(a[i], b[i]);
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

    /**
     * Returns the value mapped to the given item, or the value supplied by
     * {@code defaultValueSupplier} when no key matches.
     *
     * <p>The supplier is only evaluated when there is no match, so an
     * expensive default value is not computed unless it is needed.</p>
     *
     * <p>This method behaves exactly like
     * {@link #equivalent(Object, Object[], Object[], Object)} except that the
     * default value is provided lazily. It is named {@code equivalentGet} to
     * avoid overloading ambiguity with {@link #equivalent(Object, Object[],
     * Object[], Object)} when the default value is a {@code null} literal.</p>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param item the value to look up, may be null
     * @param keys the array of keys, may not be null
     * @param values the array of values, may not be null
     * @param defaultValueSupplier the supplier of the value returned when no
     *  key matches, may not be null
     * @return the value mapped to the item, or {@code defaultValueSupplier.get()}
     */
    public static <K, V> V equivalent(K item, K[] keys, V[] values, Supplier<? extends V> defaultValueSupplier)
    {
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
        return defaultValueSupplier==null ? null : defaultValueSupplier.get();
    }

    public static <K, V> V equivalent(K item, K[] keys, V[] values)
    {
        return equivalent(item, keys, values, (V)null);
    }


}
