/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author franci
 */
public class Rand
{
    private final Random random;

    public Rand(Random random)
    {
        this.random = random;
    }

    /**
     * Returns a {@link Rand} backed by the {@link ThreadLocalRandom} of the
     * current thread.
     * <p>
     * This method is useful for high-throughput, non-cryptographic randomness
     * in concurrent code, since it avoids contention and the overhead of
     * allocating a new generator per call. The returned generator must not be
     * shared across threads, and must not be used for cryptographic purposes.
     *
     * @return a thread-local {@link Rand}
     */
    public static Rand getThreadLocalInstance()
    {
        return new Rand(ThreadLocalRandom.current());
    }
    
    public int nextInt()
    {
        return random.nextInt();
    }

    public int nextInt(int i)
    {
        return random.nextInt(i);
    }

    public long nextLong()
    {
        return random.nextLong();
    }

    public boolean nextBoolean()
    {
        return random.nextBoolean();
    }

    public float nextFloat()
    {
        return random.nextFloat();
    }

    public double nextDouble()
    {
        return random.nextDouble();
    }

    public synchronized double nextGaussian()
    {
        return random.nextGaussian();
    }

    public BigInteger nextBigInteger(int bitLength, int certainty)
    {
        return new BigInteger(bitLength, certainty, random);
    }
    
    public BigInteger nextBigInteger(int numBits)
    {
        return new BigInteger(numBits, random);
    }
    
    public BigInteger nextBigInteger(BigInteger bound)
    {
        BigInteger r;
        do 
        {
            r = new BigInteger(bound.bitLength(), random);
        } 
        while (r.compareTo(bound) >= 0);
        return r;
    }
    
    public byte[] nextBytes(byte[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        random.nextBytes(data);
        return data;
    }

    public boolean[] nextBoolean(boolean[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = random.nextBoolean();
        }
        return data;
    }

    public int[] nextInts(int[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = random.nextInt();
        }
        return data;
    }

    public int[] nextInts(int[] data, int bound)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = random.nextInt(bound);
        }
        return data;
    }

    public long[] nextLongs(long[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = random.nextLong();
        }
        return data;
    }

    public float[] nextFloats(float[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = random.nextFloat();
        }
        return data;
    }
    
    public double[] nextDoubles(double[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = random.nextDouble();
        }
        return data;
    }

    public BigInteger[] nextBigIntegers(BigInteger[] data, int numBits)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = nextBigInteger(numBits);
        }
        return data;
    }
    
    public BigInteger[] nextBigIntegers(BigInteger[] data, int bitLength, int certainty)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = nextBigInteger(bitLength, certainty);
        }
        return data;
    }
    
    public BigInteger[] nextBigIntegers(BigInteger[] data, BigInteger bound)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        for(int i=0;i<data.length;i++)
        {
            data[i] = nextBigInteger(bound);
        }
        return data;
    }
    
}
