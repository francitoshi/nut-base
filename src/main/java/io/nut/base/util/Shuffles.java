/*
 * Shuffles.java
 *
 * Copyright (c) 2012-2024 francitoshi@gmail.com
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

import io.nut.base.crypto.Crypto;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 *
 * @author franci
 */
public class Shuffles
{
    enum Holder
    {
        INSTANCE;
        private final SecureRandom secureRandom = Crypto.getSecureRandomStrong();
    }

    private static SecureRandom getSecureRandom()
    {
        return Holder.INSTANCE.secureRandom;
    }
    
    public static boolean[] shuffle(boolean[] values, Random random)
    {
        ArrayList<Boolean> list = new ArrayList<>(values.length);
        
        for(boolean item : values)
        {
            list.add(item);
        }
        Collections.shuffle(list, random);
        for(int i=0;i<values.length;i++)
        {
            values[i] = list.get(i);
        }
        return values;
    }
    public static boolean[] shuffle(boolean[] values)
    {
        return shuffle(values, getSecureRandom());
    }
    public static byte[] shuffle(byte[] values, Random random)
    {
        ArrayList<Byte> list = new ArrayList<>(values.length);
        
        for(byte item : values)
        {
            list.add(item);
        }
        Collections.shuffle(list, random);
        for(int i=0;i<values.length;i++)
        {
            values[i] = list.get(i);
        }
        return values;
    }
    public static byte[] shuffle(byte[] values)
    {
        return shuffle(values, getSecureRandom());
    }
    public static short[] shuffle(short[] values, Random random)
    {
        ArrayList<Short> list = new ArrayList<>(values.length);
        
        for(short item : values)
        {
            list.add(item);
        }
        Collections.shuffle(list, random);
        for(int i=0;i<values.length;i++)
        {
            values[i] = list.get(i);
        }
        return values;
    }
    public static short[] shuffle(short[] values)
    {
        return shuffle(values, getSecureRandom());
    }
    public static char[] shuffle(char[] values, Random random)
    {
        ArrayList<Character> list = new ArrayList<>(values.length);
        
        for(char item : values)
        {
            list.add(item);
        }
        Collections.shuffle(list, random);
        for(int i=0;i<values.length;i++)
        {
            values[i] = list.get(i);
        }
        return values;
    }
    public static char[] shuffle(char[] values)
    {
        return shuffle(values, getSecureRandom());
    }
    public static int[] shuffle(int[] values, Random random)
    {
        ArrayList<Integer> list = new ArrayList<>(values.length);
        
        for(int item : values)
        {
            list.add(item);
        }
        Collections.shuffle(list, random);
        for(int i=0;i<values.length;i++)
        {
            values[i] = list.get(i);
        }
        return values;
    }
    public static int[] shuffle(int[] values)
    {
        return shuffle(values, getSecureRandom());
    }
    public static long[] shuffle(long[] values, Random random)
    {
        ArrayList<Long> list = new ArrayList<>(values.length);
        
        for(long item : values)
        {
            list.add(item);
        }
        Collections.shuffle(list, random);
        for(int i=0;i<values.length;i++)
        {
            values[i] = list.get(i);
        }
        return values;
    }
    public static long[] shuffle(long[] values)
    {
        return shuffle(values, getSecureRandom());
    }
    public static double[] shuffle(double[] values, Random random)
    {
        ArrayList<Double> list = new ArrayList<>(values.length);
        
        for(double item : values)
        {
            list.add(item);
        }
        Collections.shuffle(list, random);
        for(int i=0;i<values.length;i++)
        {
            values[i] = list.get(i);
        }
        return values;
    }
    public static double[] shuffle(double[] values)
    {
        return shuffle(values, getSecureRandom());
    }
    
    public static <T> T[] shuffle(T[] values, Random random)
    {
        ArrayList<T> list = new ArrayList<>(values.length);
        
        for(T item : values)
        {
            list.add(item);
        }
        Collections.shuffle(list, random);
        for(int i=0;i<values.length;i++)
        {
            values[i] = list.get(i);
        }
        return values;
    }
    public static <T> T[] shuffle(T[] values)
    {
        return shuffle(values, getSecureRandom());
    }

}
