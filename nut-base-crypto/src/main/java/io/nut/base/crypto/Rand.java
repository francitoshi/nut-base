/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import io.nut.base.jca.Kr;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A thin wrapper around {@link java.util.Random} that adds convenience methods
 * for filling arrays and for generating {@link BigInteger} values.
 * <p>
 * The instance inherits the properties of the {@code Random} implementation it
 * is built with: a plain {@code Random} or {@link ThreadLocalRandom} gives
 * fast, non-cryptographic pseudo-randomness, while a
 * {@link java.security.SecureRandom} gives cryptographically strong randomness.
 * Use this class only with a strong source when the output must not be
 * predictable (keys, nonces, salts, tokens).
 * <p>
 * All {@code next*(array)} methods fill the given array <b>in place</b> and
 * return the very same reference, so they can be used in a chain or passed
 * directly to another method. If the argument is {@code null} or empty it is
 * returned unchanged and nothing is drawn from the generator.
 * <p>
 * <b>Thread Safety:</b> instances are as thread-safe as the wrapped
 * {@code Random}: shared instances must be synchronized externally (only
 * {@link #nextGaussian()} synchronizes internally), whereas instances obtained
 * from {@link #getThreadLocalInstance()} are confined to their thread.
 *
 * @see java.util.Random
 * @see java.security.SecureRandom
 *
 * @author franci
 */
public class Rand
{
    private final Random random;

    /**
     * Creates a Rand that delegates to the given generator.
     *
     * @param random the underlying generator; its quality determines whether
     * this instance is suitable for cryptographic use.
     */
    private Rand(Random random)
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

    public static Rand getInstance()
    {
        return new Rand(Kr.getSecureRandom());
    }
        
    public static Rand getStrongInstance()
    {
        return new Rand(Kr.getSecureRandomStrong());
    }
     
    public static Rand getStrongFastInstance()
    {
        return new Rand(Kr.getSecureRandomStrongFast());
    }
     

        
    /**
     * Returns the next pseudorandom, uniformly distributed {@code int} value.
     *
     * @return the next pseudorandom {@code int} value.
     */
    public int nextInt()
    {
        return random.nextInt();
    }

    /**
     * Returns the next pseudorandom, uniformly distributed {@code int} value
     * between 0 (inclusive) and the specified bound (exclusive).
     *
     * @param i the upper bound (exclusive).
     * @return the next pseudorandom {@code int} value between 0 (inclusive) and
     * {@code i} (exclusive).
     * @throws IllegalArgumentException if {@code i} is not positive.
     */
    public int nextInt(int i)
    {
        return random.nextInt(i);
    }

    /**
     * Returns the next pseudorandom, uniformly distributed {@code long} value.
     *
     * @return the next pseudorandom {@code long} value.
     */
    public long nextLong()
    {
        return random.nextLong();
    }

    /**
     * Returns the next pseudorandom, uniformly distributed {@code boolean}
     * value.
     *
     * @return the next pseudorandom {@code boolean} value.
     */
    public boolean nextBoolean()
    {
        return random.nextBoolean();
    }

    /**
     * Returns the next pseudorandom, uniformly distributed {@code float} value
     * between 0.0 (inclusive) and 1.0 (exclusive).
     *
     * @return the next pseudorandom {@code float} value in the range
     * {@code [0.0, 1.0)}.
     */
    public float nextFloat()
    {
        return random.nextFloat();
    }

    /**
     * Returns the next pseudorandom, uniformly distributed {@code double} value
     * between 0.0 (inclusive) and 1.0 (exclusive).
     *
     * @return the next pseudorandom {@code double} value in the range
     * {@code [0.0, 1.0)}.
     */
    public double nextDouble()
    {
        return random.nextDouble();
    }

    /**
     * Returns the next pseudorandom, uniformly distributed Gaussian
     * ("normally") distributed {@code double} value with mean 0.0 and standard
     * deviation 1.0.
     * <p>
     * Synchronized, because {@link Random#nextGaussian()} keeps internal state
     * (the cached second value) that must not be interleaved between threads.
     *
     * @return the next pseudorandom Gaussian {@code double} value.
     */
    public synchronized double nextGaussian()
    {
        return random.nextGaussian();
    }

    /**
     * Returns a uniformly distributed pseudorandom BigInteger that is probably
     * prime with the requested certainty.
     * <p>
     * Equivalent to {@code new BigInteger(bitLength, certainty, random)}.
     *
     * @param bitLength the bit length of the returned BigInteger.
     * @param certainty the maximum error probability of the primality test.
     * @return a BigInteger of {@code bitLength} bits that is probably prime.
     * @throws IllegalArgumentException if {@code bitLength < 2} or
     * {@code certainty < 0}.
     */
    public BigInteger nextBigInteger(int bitLength, int certainty)
    {
        return new BigInteger(bitLength, certainty, random);
    }

    /**
     * Returns a uniformly distributed pseudorandom BigInteger with exactly
     * {@code numBits} bits.
     * <p>
     * Equivalent to {@code new BigInteger(numBits, random)}.
     *
     * @param numBits the maximum bit length of the returned BigInteger.
     * @return a BigInteger with a value in the range {@code [0, 2^numBits)}.
     * @throws IllegalArgumentException if {@code numBits < 0}.
     */
    public BigInteger nextBigInteger(int numBits)
    {
        return new BigInteger(numBits, random);
    }

    /**
     * Returns a uniformly distributed pseudorandom BigInteger in the range
     * {@code [0, bound)}.
     * <p>
     * Values are drawn with the bit length of {@code bound} and retried until
     * they fall below the bound, so very skewed bounds may need more than one
     * draw.
     *
     * @param bound the exclusive upper bound (must be positive).
     * @return a pseudorandom BigInteger in {@code [0, bound)}.
     * @throws IllegalArgumentException if {@code bound} is not positive.
     */
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
    
    /**
     * Fills the given byte array with pseudorandom bytes.
     *
     * @param data the array to fill; if {@code null} or empty it is returned
     * unchanged.
     * @return the same array reference that was passed in.
     */
    public byte[] nextBytes(byte[] data)
    {
        if(data==null || data.length==0)
        {
            return data;
        }
        random.nextBytes(data);
        return data;
    }

    /**
     * Fills the given boolean array with pseudorandom values.
     *
     * @param data the array to fill; if {@code null} or empty it is returned
     * unchanged.
     * @return the same array reference that was passed in.
     */
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

    /**
     * Fills the given int array with pseudorandom values.
     *
     * @param data the array to fill; if {@code null} or empty it is returned
     * unchanged.
     * @return the same array reference that was passed in.
     */
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

    /**
     * Fills the given int array with pseudorandom values between 0 (inclusive)
     * and the specified bound (exclusive).
     *
     * @param data the array to fill; if {@code null} or empty it is returned
     * unchanged.
     * @param bound the upper bound (exclusive) of each value.
     * @return the same array reference that was passed in.
     * @throws IllegalArgumentException if {@code bound} is not positive.
     */
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

    /**
     * Fills the given long array with pseudorandom values.
     *
     * @param data the array to fill; if {@code null} or empty it is returned
     * unchanged.
     * @return the same array reference that was passed in.
     */
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

    /**
     * Fills the given float array with pseudorandom values in
     * {@code [0.0, 1.0)}.
     *
     * @param data the array to fill; if {@code null} or empty it is returned
     * unchanged.
     * @return the same array reference that was passed in.
     */
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
    
    /**
     * Fills the given double array with pseudorandom values in
     * {@code [0.0, 1.0)}.
     *
     * @param data the array to fill; if {@code null} or empty it is returned
     * unchanged.
     * @return the same array reference that was passed in.
     */
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

    /**
     * Fills the given BigInteger array with pseudorandom values of exactly
     * {@code numBits} bits.
     *
     * @param data the array to fill; if {@code null} or empty it is returned
     * unchanged.
     * @param numBits the maximum bit length of each value.
     * @return the same array reference that was passed in.
     * @throws IllegalArgumentException if {@code numBits < 0}.
     */
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
    
    /**
     * Fills the given BigInteger array with pseudorandom values that are
     * probably prime.
     *
     * @param data the array to fill; if {@code null} or empty it is returned
     * unchanged.
     * @param bitLength the bit length of each value.
     * @param certainty the maximum error probability of the primality test.
     * @return the same array reference that was passed in.
     * @throws IllegalArgumentException if {@code bitLength < 2} or
     * {@code certainty < 0}.
     */
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
    
    /**
     * Fills the given BigInteger array with pseudorandom values in the range
     * {@code [0, bound)}.
     *
     * @param data the array to fill; if {@code null} or empty it is returned
     * unchanged.
     * @param bound the exclusive upper bound of each value.
     * @return the same array reference that was passed in.
     * @throws IllegalArgumentException if {@code bound} is not positive.
     */
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
