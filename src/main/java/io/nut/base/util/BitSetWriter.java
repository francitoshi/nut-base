/*
 * BitSetWriter.java
 *
 * Copyright (c) 2012-2023 francitoshi@gmail.com
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

import java.util.BitSet;

/**
 *
 * @author franci
 */
public abstract class BitSetWriter
{
    private static final byte[] POW2 = {1,2,4,8,16,32,64,-128};
    public abstract void put(boolean val);
    public abstract void put(byte val,int bits);
    public abstract int count();

    static public BitSetWriter build(final BitSet src)
    {
        return new BitSetWriter()
        {
            private final BitSet bs=src;
            private int count=0;
            @Override
            public void put(boolean val)
            {
                bs.set(count++,val);
            }
            @Override
            public void put(byte val, int bits)
            {
                for (int i = 0; i < 8 && i<bits; i++)
                {
                    boolean bit = (POW2[i]&val)==POW2[i];
                    put(bit);
                }
            }
            @Override
            public int count()
            {
                return count;
            }
        };
    }
    static public BitSetWriter syncronized(final BitSetWriter src)
    {
        return new BitSetWriter()
        {
            private final BitSetWriter bq=src;
            private final Object lock=new Object();
            @Override
            public void put(boolean val)
            {
                synchronized(lock)
                {
                    bq.put(val);
                }
            }
            @Override
            public void put(byte val, int bits)
            {
                synchronized(lock)
                {
                    bq.put(val,bits);
                }
            }

            @Override
            public int count()
            {
                synchronized(lock)
                {
                    return bq.count();
                }
            }
        };
    }
}
