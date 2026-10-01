/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.security.SecureRandom;
import java.util.BitSet;

/**
 *
 * @author franci
 */
public abstract class BitSetReader
{
    private static final byte[] POW2 = {1,2,4,8,16,32,64,-128};
    public abstract boolean get();
    public abstract byte get(int bits);
    public abstract int count();
    public abstract void reset();
    
    final SecureRandom secureRandom;

    public BitSetReader()
    {
        this.secureRandom = new SecureRandom();
    }
   
    static public BitSetReader build(final BitSet src)
    {
        return new BitSetReader()
        {
            private final BitSet bs=src;
            private int count=0;
            private final int size = src.size();
            @Override
            public boolean get()
            {
                if(count>=size)
                {
                    count++;
                    return secureRandom.nextBoolean();
                }
                return bs.get(count++);
            }
            @Override
            public byte get(int bits)
            {
                byte val = 0;
                
                for (int i = 0; i<bits; i++)
                {
                    if(get())
                    {
                        val |= POW2[i];
                    }
                }
                return val;
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
    static public BitSetReader syncronized(final BitSetReader src)
    {
        return new BitSetReader()
        {
            private final BitSetReader bq=src;
            private final Object lock=new Object();
            @Override
            public boolean get()
            {
                synchronized(lock)
                {
                    return bq.get();
                }
            }
            @Override
            public byte get(int bits)
            {
                synchronized(lock)
                {
                    return bq.get(bits);
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
