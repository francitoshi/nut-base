/*
 * Rand.java
 *
 * Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 *
 * @author franci
 */
public class Rand
{
    private final SecureRandom secureRandom;

    public Rand(SecureRandom random)
    {
        this.secureRandom = random;
    }
    
    public int nextInt()
    {
        return secureRandom.nextInt();
    }

    public int nextInt(int i)
    {
        return secureRandom.nextInt(i);
    }

    public long nextLong()
    {
        return secureRandom.nextLong();
    }

    public boolean nextBoolean()
    {
        return secureRandom.nextBoolean();
    }

    public float nextFloat()
    {
        return secureRandom.nextFloat();
    }

    public double nextDouble()
    {
        return secureRandom.nextDouble();
    }

    public synchronized double nextGaussian()
    {
        return secureRandom.nextGaussian();
    }

    public BigInteger nextBigInteger(int bitLength, int certainty)
    {
        return new BigInteger(bitLength, certainty, secureRandom);
    }
    
    public BigInteger nextBigInteger(int numBits)
    {
        return new BigInteger(numBits, secureRandom);
    }
    
    public BigInteger nextBigInteger(BigInteger bound)
    {
        BigInteger r;
        do 
        {
            r = new BigInteger(bound.bitLength(), secureRandom);
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
        secureRandom.nextBytes(data);
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
            data[i] = secureRandom.nextBoolean();
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
            data[i] = secureRandom.nextInt();
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
            data[i] = secureRandom.nextInt(bound);
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
            data[i] = secureRandom.nextLong();
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
            data[i] = secureRandom.nextFloat();
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
            data[i] = secureRandom.nextDouble();
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
