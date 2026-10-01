/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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
    public abstract void reset();

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

            @Override
            public void reset()
            {
                count = 0;
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
            @Override
            public void reset()
            {
                synchronized(lock)
                {
                    bq.reset();
                }
            }
        };
    }
}
