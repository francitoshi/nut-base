/*
 *  RsaTimeLock.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto.vdf;

import io.nut.base.math.Primes;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

public class RsaTimeLock
{
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final SecureRandom RANDOM = new SecureRandom();
    
    // NOTE: φ(N) is neither calculated nor needed. Only λ(N) is sufficient and more efficient.
    // Carmichael's theorem is used.
        
    public final BigInteger n;      // public RSA module
    public final BigInteger lambda; // precalculated
    
    public RsaTimeLock(BigInteger n, BigInteger lambda)
    {
        this.n = n;
        this.lambda = lambda;
    }
    
    public RsaTimeLock(BigInteger n)
    {
        this(n, null);
    }

    public static RsaTimeLock create(int bits)
    {
        BigInteger p = Primes.safePrime(bits);
        BigInteger q = Primes.safePrime(bits);

        BigInteger N = p.multiply(q);
        BigInteger lambda = p.subtract(BigInteger.ONE)
                   .divide(p.subtract(BigInteger.ONE).gcd(q.subtract(BigInteger.ONE)))
                   .multiply(q.subtract(BigInteger.ONE));
        
        return new RsaTimeLock(N, lambda);
    }


    public BigInteger createChallenge()
    {
        BigInteger x = new BigInteger(n.bitLength(), RANDOM).mod(n);
        if (x.compareTo(BigInteger.ONE) <= 0)
        {
            x = x.add(TWO);
        }
        return x;
    }
    
    /**
     *
     * @param x
     * @param t number of sequential squares (2^T effective exponent)
     * @return
     */
    public BigInteger solve(BigInteger x, int t)
    {
        BigInteger current = x;
        for (long i = 0; i < t; i++)
        {
            current = current.multiply(current).mod(n);
        }
        return current;
    }
    
    public boolean verify(BigInteger x, int t, BigInteger candidate) 
    {
        if(this.lambda!=null)
        {
            // fast - using lambda - Carmichael's theorem
            if (t == 0) return x.equals(candidate);
            BigInteger reducedExponent = TWO.modPow(BigInteger.valueOf(t), lambda);
            BigInteger expected = x.modPow(reducedExponent, n);
            return expected.equals(candidate);
        }
        else
        {
            // slow - without lambda
            BigInteger next = candidate.multiply(candidate).mod(n);
            BigInteger expectedNext = x.modPow(BigInteger.ONE.shiftLeft((int) (t + 1)), n);
            return next.equals(expectedNext);
        }
    } 
  
    public int delayUnitsPerMillisecond(int testMillis)
    {
        BigInteger x = createChallenge();

        final long testNanos = TimeUnit.MILLISECONDS.toNanos(testMillis);

        long startNanos = System.nanoTime();
        long stopWarmUp = startNanos + (testNanos / 10);
        long stopTester = startNanos + testNanos;
        long lastNanos = startNanos;

        int i=0;
        int t=1;
        
        for(;(lastNanos=System.nanoTime())<stopWarmUp && i<31;i++,t*=2)
        {
            solve(x, t);
        }

        long startTester = lastNanos;
        long count = 0;

        for(;(lastNanos=System.nanoTime())<stopTester && i<31 ;i++,t*=2)
        {
            solve(x, t);
            count += t;
        }

        long nanos = lastNanos-startTester;
        long millis = TimeUnit.NANOSECONDS.toMillis(nanos);

        return millis == 0 ? 1 : (int) (count / millis);
    }

}
