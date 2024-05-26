/*
 *  Sorts.java
 *
 *  Copyright (c) 2012-2024 francitoshi@gmail.com
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/**
 *
 * @author franci
 */
public class Sorts
{
    public static void reverse(byte[] a)
    {
        for(int h=0,t=a.length-1;h<t;h++,t--)
        {
            byte tmp = a[h];
            a[h]=a[t];
            a[t]=tmp;
        }
    }
    public static void reverse(short[] a)
    {
        for(int h=0,t=a.length-1;h<t;h++,t--)
        {
            short tmp = a[h];
            a[h]=a[t];
            a[t]=tmp;
        }
    }
    public static void reverse(char[] a)
    {
        for(int h=0,t=a.length-1;h<t;h++,t--)
        {
            char tmp = a[h];
            a[h]=a[t];
            a[t]=tmp;
        }
    }
    public static void reverse(int[] a)
    {
        for(int h=0,t=a.length-1;h<t;h++,t--)
        {
            int tmp = a[h];
            a[h]=a[t];
            a[t]=tmp;
        }
    }
    public static void reverse(long[] a)
    {
        for(int h=0,t=a.length-1;h<t;h++,t--)
        {
            long tmp = a[h];
            a[h]=a[t];
            a[t]=tmp;
        }
    }
    public static void reverse(float[] a)
    {
        for(int h=0,t=a.length-1;h<t;h++,t--)
        {
            float tmp = a[h];
            a[h]=a[t];
            a[t]=tmp;
        }
    }
    public static void reverse(double[] a)
    {
        for(int h=0,t=a.length-1;h<t;h++,t--)
        {
            double tmp = a[h];
            a[h]=a[t];
            a[t]=tmp;
        }
    }
    public static <T> void reverse(T[] a)
    {
        for(int h=0,t=a.length-1;h<t;h++,t--)
        {
            T tmp = a[h];
            a[h]=a[t];
            a[t]=tmp;
        }
    }
    
    public static byte[] reverseOf(byte[] data) 
    {
        byte[] copy = new byte[data.length];
        for (int i = 0, j = data.length - 1; i < data.length; i++, j--)
        {
            copy[i] = data[data.length - 1 - i];
        }
        return copy;
    }
    public static short[] reverseOf(short[] data) 
    {
        short[] copy = new short[data.length];
        for (int i = 0, j = data.length - 1; i < data.length; i++, j--)
        {
            copy[i] = data[data.length - 1 - i];
        }
        return copy;
    }
    public static char[] reverseOf(char[] data) 
    {
        char[] copy = new char[data.length];
        for (int i = 0, j = data.length - 1; i < data.length; i++, j--)
        {
            copy[i] = data[data.length - 1 - i];
        }
        return copy;
    }
    
    public static int[] reverseOf(int[] data) 
    {
        int[] copy = new int[data.length];
        for (int i = 0, j = data.length - 1; i < data.length; i++, j--)
        {
            copy[i] = data[data.length - 1 - i];
        }
        return copy;
    }
    
    public static long[] reverseOf(long[] data) 
    {
        long[] copy = new long[data.length];
        for (int i = 0, j = data.length - 1; i < data.length; i++, j--)
        {
            copy[i] = data[data.length - 1 - i];
        }
        return copy;
    }
    
    public static float[] reverseOf(float[] data) 
    {
        float[] copy = new float[data.length];
        for (int i = 0, j = data.length - 1; i < data.length; i++, j--)
        {
            copy[i] = data[data.length - 1 - i];
        }
        return copy;
    }
    public static double[] reverseOf(double[] data) 
    {
        double[] copy = new double[data.length];
        for (int i = 0, j = data.length - 1; i < data.length; i++, j--)
        {
            copy[i] = data[data.length - 1 - i];
        }
        return copy;
    }
    
    public static <T> T[] reverseOf(T[] data)
    {
        T[] copy = Arrays.copyOf(data, data.length);
        for (int i = 0, j = data.length - 1; i < data.length; i++, j--)
        {
            copy[i] = data[j];
        }
        return copy;
    }

    /**
     * Copies the specified array, eliminating duplicated objects and compacting
     * the arrary if necesary
     *
     * @param <T>
     * @param src
     * @return a copy of the src array, compated eliminating duplicated objects
     */
    public static <T> T[] uniqueCopyOf(T[] src)
    {
        ArrayList<T> list = new ArrayList<>(src.length);
        for (T item : src)
        {
            if (!list.contains(item))
            {
                list.add(item);
            }
        }
        return list.toArray(Arrays.copyOf(src, 0));
    }

    ///// byte,byte ////////////////////////////////////////////////////////////
    public static void sort(byte[] key, byte[] value, int fromIndex, int toIndex, boolean reverse)
    {
        class Entry
        {
            final byte key;
            final byte value;
            public Entry(byte key, byte value)
            {
                this.key = key;
                this.value = value;
            }            
        }
        class EntryComparator implements Comparator<Entry>
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                if(o1.key<o2.key)
                    return -1;
                if(o1.key>o2.key)
                    return +1;
                if(o1.value<o2.value)
                    return -1;
                if(o1.value>o2.value)
                    return +1;
                return 0;
            }
        }
        class ReverseEntryComparator extends EntryComparator
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                return super.compare(o2, o1);
            }
        }

        Entry[] entries = new Entry[toIndex-fromIndex];
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            entries[i]=new Entry(key[j], value[j]);
        }
        EntryComparator cmp = reverse?new ReverseEntryComparator():new EntryComparator();
        Arrays.sort(entries,cmp);
        
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            key[j]  =entries[i].key;
            value[j]=entries[i].value;
        }
    }
    public static void sort(byte[] key, byte[] value)
    {
        sort(key, value, 0, key.length, false);
    }
    public static void sort(byte[] key, byte[] value, boolean reverse)
    {
        sort(key, value, 0, key.length, reverse);
    }
    public static void sort(byte[] key, byte[] value, int fromIndex, int toIndex)
    {
        sort(key, value, fromIndex, toIndex, false);
    }

    ///// int,byte /////////////////////////////////////////////////////////////
    public static void sort(int[] key, byte[] value, int fromIndex, int toIndex, boolean reverse)
    {
        class Entry
        {
            final int key;
            final byte value;
            public Entry(int key, byte value)
            {
                this.key = key;
                this.value = value;
            }            
        }
        class EntryComparator implements Comparator<Entry>
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                if(o1.key<o2.key)
                    return -1;
                if(o1.key>o2.key)
                    return +1;
                if(o1.value<o2.value)
                    return -1;
                if(o1.value>o2.value)
                    return +1;
                return 0;
            }
        }
        class ReverseEntryComparator extends EntryComparator
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                return super.compare(o2, o1);
            }
        }

        Entry[] entries = new Entry[toIndex-fromIndex];
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            entries[i]=new Entry(key[j], value[j]);
        }
        EntryComparator cmp = reverse?new ReverseEntryComparator():new EntryComparator();
        Arrays.sort(entries,cmp);
        
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            key[j]  =entries[i].key;
            value[j]=entries[i].value;
        }
    }
    public static void sort(int[] key, byte[] value)
    {
        sort(key, value, 0, key.length, false);
    }
    public static void sort(int[] key, byte[] value, boolean reverse)
    {
        sort(key, value, 0, key.length, reverse);
    }
    public static void sort(int[] key, byte[] value, int fromIndex, int toIndex)
    {
        sort(key, value, fromIndex, toIndex, false);
    }
    
    ///// int,int //////////////////////////////////////////////////////////////
    public static void sort(int[] key, int[] value, int fromIndex, int toIndex, boolean reverse)
    {
        class Entry
        {
            final int key;
            final int value;
            public Entry(int key, int value)
            {
                this.key = key;
                this.value = value;
            }            
        }
        class EntryComparator implements Comparator<Entry>
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                if(o1.key<o2.key)
                    return -1;
                if(o1.key>o2.key)
                    return +1;
                if(o1.value<o2.value)
                    return -1;
                if(o1.value>o2.value)
                    return +1;
                return 0;
            }
        }
        class ReverseEntryComparator extends EntryComparator
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                return super.compare(o2, o1);
            }
        }

        Entry[] entries = new Entry[toIndex-fromIndex];
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            entries[i]=new Entry(key[j], value[j]);
        }
        EntryComparator cmp = reverse?new ReverseEntryComparator():new EntryComparator();
        Arrays.sort(entries,cmp);
        
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            key[j]  =entries[i].key;
            value[j]=entries[i].value;
        }
    }
    public static void sort(int[] key, int[] value)
    {
        sort(key, value, 0, key.length, false);
    }
    public static void sort(int[] key, int[] value, boolean reverse)
    {
        sort(key, value, 0, key.length, reverse);
    }
    public static void sort(int[] key, int[] value, int fromIndex, int toIndex)
    {
        sort(key, value, fromIndex, toIndex, false);
    }
    ///// long,long //////////////////////////////////////////////////////////////
    public static void sort(long[] key, long[] value, int fromIndex, int toIndex, boolean reverse)
    {
        class Entry
        {
            final long key;
            final long value;
            public Entry(long key, long value)
            {
                this.key = key;
                this.value = value;
            }            
        }
        class EntryComparator implements Comparator<Entry>
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                if(o1.key<o2.key)
                    return -1;
                if(o1.key>o2.key)
                    return +1;
                if(o1.value<o2.value)
                    return -1;
                if(o1.value>o2.value)
                    return +1;
                return 0;
            }
        }
        class ReverseEntryComparator extends EntryComparator
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                return super.compare(o2, o1);
            }
        }

        Entry[] entries = new Entry[toIndex-fromIndex];
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            entries[i]=new Entry(key[j], value[j]);
        }
        EntryComparator cmp = reverse?new ReverseEntryComparator():new EntryComparator();
        Arrays.sort(entries,cmp);
        
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            key[j]  =entries[i].key;
            value[j]=entries[i].value;
        }
    }
    public static void sort(long[] key, long[] value)
    {
        sort(key, value, 0, key.length, false);
    }
    public static void sort(long[] key, long[] value, boolean reverse)
    {
        sort(key, value, 0, key.length, reverse);
    }
    public static void sort(long[] key, long[] value, int fromIndex, int toIndex)
    {
        sort(key, value, fromIndex, toIndex, false);
    }
    ///// double,double //////////////////////////////////////////////////////////////
    public static void sort(double[] key, double[] value, int fromIndex, int toIndex, boolean reverse)
    {
        class Entry
        {
            final double key;
            final double value;
            public Entry(double key, double value)
            {
                this.key = key;
                this.value = value;
            }            
        }
        class EntryComparator implements Comparator<Entry>
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                if(o1.key<o2.key)
                    return -1;
                if(o1.key>o2.key)
                    return +1;
                if(o1.value<o2.value)
                    return -1;
                if(o1.value>o2.value)
                    return +1;
                return 0;
            }
        }
        class ReverseEntryComparator extends EntryComparator
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                return super.compare(o2, o1);
            }
        }

        Entry[] entries = new Entry[toIndex-fromIndex];
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            entries[i]=new Entry(key[j], value[j]);
        }
        EntryComparator cmp = reverse?new ReverseEntryComparator():new EntryComparator();
        Arrays.sort(entries,cmp);
        
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            key[j]  =entries[i].key;
            value[j]=entries[i].value;
        }
    }
    public static void sort(double[] key, double[] value)
    {
        sort(key, value, 0, key.length, false);
    }
    public static void sort(double[] key, double[] value, boolean reverse)
    {
        sort(key, value, 0, key.length, reverse);
    }
    public static void sort(double[] key, double[] value, int fromIndex, int toIndex)
    {
        sort(key, value, fromIndex, toIndex, false);
    }
    
    public static <V> void sort(int[] key, V[] value, int fromIndex, int toIndex, boolean reverse)
    {
        class Entry
        {
            final int key;
            final V value;
            public Entry(int key, V value)
            {
                this.key = key;
                this.value = value;
            }            
        }
        class EntryComparator implements Comparator<Entry>
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                if(o1.key<o2.key)
                    return -1;
                if(o1.key>o2.key)
                    return +1;
                if(o1.value instanceof Comparable)
                {
                    return ((Comparable)o1.value).compareTo(o2.value);
                }
                return 0;
            }
        }
        class ReverseEntryComparator extends EntryComparator
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                return super.compare(o2, o1);
            }
        }

        Entry[] entries = new Entry[toIndex-fromIndex];
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            entries[i]=new Entry(key[j], value[j]);
        }
        EntryComparator cmp = reverse?new ReverseEntryComparator():new EntryComparator();
        Arrays.sort(entries,cmp);
        
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            key[j]  =entries[i].key;
            value[j]=entries[i].value;
        }
    }
    public static <V> void sort(int[] key, V[] value)
    {
        sort(key, value, 0, key.length, false);
    }
    public static <V> void sort(int[] key, V[] value, boolean reverse)
    {
        sort(key, value, 0, key.length, reverse);
    }
    public static <V> void sort(int[] key, V[] value, int fromIndex, int toIndex)
    {
        sort(key, value, fromIndex, toIndex, false);
    }

    public static <K,V> void sort(K[] key, V[] value, int fromIndex, int toIndex, boolean reverse)
    {
        class Entry
        {
            final K key;
            final V value;
            public Entry(K key, V value)
            {
                this.key = key;
                this.value = value;
            }            
        }
        class EntryComparator implements Comparator<Entry>
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                if(o1.key instanceof Comparable)
                {
                    return ((Comparable)o1.key).compareTo(o2.key);
                }
                if(o1.value instanceof Comparable)
                {
                    return ((Comparable)o1.value).compareTo(o2.value);
                }
                return 0;
            }
        }
        class ReverseEntryComparator extends EntryComparator
        {
            @Override
            public int compare(Entry o1, Entry o2)
            {
                return super.compare(o2, o1);
            }
        }

        Entry[] entries = new Entry[toIndex-fromIndex];
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            entries[i]=new Entry(key[j], value[j]);
        }
        EntryComparator cmp = reverse?new ReverseEntryComparator():new EntryComparator();
        Arrays.sort(entries,cmp);
        
        for(int i=0,j=fromIndex;i<entries.length;i++,j++)
        {
            key[j]  =entries[i].key;
            value[j]=entries[i].value;
        }
    }
    public static <K,V> void sort(K[] key, V[] value)
    {
        sort(key, value, 0, key.length, false);
    }
    public static <K,V> void sort(K[] key, V[] value, boolean reverse)
    {
        sort(key, value, 0, key.length, reverse);
    }
    public static <K,V> void sort(K[] key, V[] value, int fromIndex, int toIndex)
    {
        sort(key, value, fromIndex, toIndex, false);
    }

    public static void sort(byte[][] items)
    {
        Arrays.sort(items, new Comparator<byte[]>()
        {
            @Override
            public int compare(byte[] b0, byte[] b1)
            {
                return Utils.compare(b0, b1);
            }
        });
    }
    public static void sort(int[][] items)
    {
        Arrays.sort(items, new Comparator<int[]>()
        {
            @Override
            public int compare(int[] i0, int[] i1)
            {
                return Utils.compare(i0, i1);
            }
        });
    }
    public static void sort(long[][] items)
    {
        Arrays.sort(items, new Comparator<long[]>()
        {
            @Override
            public int compare(long[] l0, long[] l1)
            {
                return Utils.compare(l0, l1);
            }
        });
    }
    public static void sort(double[][] items)
    {
        Arrays.sort(items, new Comparator<double[]>()
        {
            @Override
            public int compare(double[] d0, double[] d1)
            {
                return Utils.compare(d0, d1);
            }
        });
    }

    public static <T> Comparator<T> asStringComparator()
    {
        return new Comparator<T>()
        {
            @Override
            public int compare(T t1, T t2)
            {
                String s1 = t1.toString();
                String s2 = t2.toString();
                return s1.compareTo(s2);
            }
        };
    }
    public static <T> Comparator<T> asStringComparatorIgnoreCase()
    {
        return new Comparator<T>()
        {
            @Override
            public int compare(T t1, T t2)
            {
                String s1 = t1.toString();
                String s2 = t2.toString();
                return s1.compareToIgnoreCase(s2);
            }
        };
    }
    public static <T> Comparator<T> asStringComparatorReverse()
    {
        return new Comparator<T>()
        {
            @Override
            public int compare(T t1, T t2)
            {
                String s1 = t1.toString();
                String s2 = t2.toString();
                return s2.compareTo(s1);
            }
        };
    }
    public static <T> Comparator<T> asStringComparatorIgnoreCaseReverse()
    {
        return new Comparator<T>()
        {
            @Override
            public int compare(T t1, T t2)
            {
                String s1 = t1.toString();
                String s2 = t2.toString();
                return s2.compareToIgnoreCase(s1);
            }
        };
    }
    
    public static String[] sequence(String[] values, int from)
    {
        for(int i=0;i<values.length;i++, from++)
        {
            values[i] = Integer.toString(from);
        }
        return values;
    }
    public static String[] sequence(String[] values)
    {
        for(int i=0;i<values.length;i++)
        {
            values[i] = Integer.toString(i);
        }
        return values;
    }
    public static <T extends Comparable> int getSorted(T... t)
    {
        T ant = t.length>1 ? t[0] : null;
        for(int i=1;i<t.length;i++)
        {
            T cur = t[i];
            if(cur.compareTo(ant)<0)
            {
                return i;
            }
            ant = cur;
        }
        return t.length;
    }
    public static <T extends Comparable> boolean isSorted(T... t)
    {
        return getSorted(t)==t.length;
    }
    public static int getSortedIgnoreCase(Locale locale, String... t)
    {
        String ant = t.length>1 ? t[0].toLowerCase(locale) : null;
        for(int i=1;i<t.length;i++)
        {
            String cur = t[i].toLowerCase(locale);
            if(cur.compareTo(ant)<0)
            {
                return i;
            }
            ant = cur;
        }
        return t.length;
    }
    public static int getSortedIgnoreCase(String... t)
    {
        return getSortedIgnoreCase(Locale.getDefault(), t);
    }
    public static boolean isSortedIgnoreCase(Locale locale, String... t)
    {
        return getSortedIgnoreCase(locale,t)==t.length;
    }
    public static boolean isSortedIgnoreCase(String... t)
    {
        return getSortedIgnoreCase(Locale.getDefault(),t)==t.length;
    }
}
