/*
 *  Primes.java
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
package io.nut.base.math;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.BitSet;

/**
 *
 * @author gemini
 */
public class Primes
{

    private static final int CERTAINTY = 100; // probability error: 2^(-100)

    // Utilidad para generar lista de primos pequeños
    private static int[] getSmallPrimes(int limit)
    {
        boolean[] composite = new boolean[limit + 1];
        int count = 0;
        for (int i = 2; i <= limit; i++)
        {
            if (!composite[i])
            {
                count++;
                if ((long) i * i <= limit)
                {
                    for (int j = i * i; j <= limit; j += i)
                    {
                        composite[j] = true;
                    }
                }
            }
        }
        int[] primes = new int[count];
        int idx = 0;
        for (int i = 2; i <= limit; i++)
        {
            if (!composite[i])
            {
                primes[idx++] = i;
            }
        }
        return primes;
    }

    // Search window size.
    // A larger size offsets the cost of creating the initial BigInteger,
    // but consumes more CPU cache. 8*1024 is a good balance.
    private static final int SIEVE_SIZE = 8 * 1024;

    // Pre-calculated small primes for the sieve.
    // The more primes we use here, the fewer times we'll call Miller-Rabin,
    // but the initialization cost of the sieve increases.
    // 2000-3000 is usually the sweet spot.
    private static final int[] SMALL_PRIMES = getSmallPrimes(2000);

    private static final SecureRandom RAND = new SecureRandom();

    /**
     * Genera un primo p tal que p = 2q + 1.
     *
     * @param bits Longitud en bits del primo seguro p.
     * @return el primo seguro
     */
    public static BigInteger safePrime(int bits)
    {
        return safePrime(bits, CERTAINTY);
    }
    /**
     * Genera un primo p tal que p = 2q + 1.
     *
     * @param bits Longitud en bits del primo seguro p.
     * @return el primo seguro
     */
    public static BigInteger safePrime(int bits, int certainty)
    {
        int qBits = bits - 1;

        certainty = certainty!=0 ? certainty : CERTAINTY;
        int fastCertainty = Math.min(certainty/5+1, 15);
        int slowCertainty = Math.min(certainty, CERTAINTY);
        
        // Infinite loop until a match is found
        while (true)
        {
            // 1. Generate a random base candidate for q.
            // We force it to be odd and have the most significant bit active.
            BigInteger baseQ = new BigInteger(qBits, RAND).setBit(qBits - 1).setBit(0);

            // 2. Crear la criba (BitSet).
            // false = candidato potencial, true = descartado (compuesto o safe prime malo).
            // Representa los números: baseQ, baseQ+2, baseQ+4, ...
            BitSet sieve = new BitSet(SIEVE_SIZE);

            // 3. Fase de Criba (Aritmética rápida con int)
            for (int prime : SMALL_PRIMES)
            {
                // We get the remainder of baseQ against the small prime.
                // BigInteger.remainder is slow, but we only do it once per search window.
                int rem = baseQ.remainder(BigInteger.valueOf(prime)).intValue();

                // --- FILTER A: Discard if q is divisible by prime ---
                // We want to find the first offset k such that (baseQ + 2*k) % prime == 0
                // Solution: 2*k = -rem (mod prime)
                // The modular inverse of 2 mod prime is (prime+1)/2.
                long inv2 = (prime + 1) >> 1; // Quick division by 2
                long k1 = ((long) (prime - rem) * inv2) % prime;

                // Mark multiples on the screen
                for (int i = (int) k1; i < SIEVE_SIZE; i += prime)
                {
                    sieve.set(i);
                }

                // --- FILTER B: Discard if (2q+1) is divisible by prime ---
                // If 2q + 1 = 0 (mod prime) -> 2q = -1 (mod prime).
                // This occurs if q = (prime - 1) / 2 (mod prime).
                // We look for k such that (baseQ + 2k) % prime == targetRem
                int targetRem = (prime - 1) >> 1;
                int diff = targetRem - rem;
                if (diff < 0)
                {
                    diff += prime;
                }

                long k2 = ((long) diff * inv2) % prime;

                for (int i = (int) k2; i < SIEVE_SIZE; i += prime)
                {
                    sieve.set(i);
                }
            }

            // 4. Heavy Testing Phase (Miller-Rabin)
            // We iterate only over the bits that remained in 'false' (survivors)
            for (int i = sieve.nextClearBit(0); i >= 0 && i < SIEVE_SIZE; i = sieve.nextClearBit(i + 1))
            {
                // We reconstruct the real candidate q
                // q = baseQ + (2 * i)
                BigInteger q = baseQ.add(BigInteger.valueOf(2L * i));

                // Primality test for q (Sophie Germain)
                // We use low certainty (e.g., 15-20) first for speed,
                // or the standard 100 if immediate absolute certainty is required.
                if (q.isProbablePrime(fastCertainty))
                {
                    // Calculate p = 2q + 1. It is a very fast bit operation.
                    BigInteger p = q.shiftLeft(1).add(BigInteger.ONE);

                    // Test de primalidad para p (Safe Prime)
                    if (p.isProbablePrime(slowCertainty) && q.isProbablePrime(slowCertainty))
                    {
                        return p;
                    }
                }
            }
            // If the sieve runs out without success, the while(true) loop restarts with a new random number.
        }
    }

    /**
     * Generate a prime p such that p % 4 == 3 (required for sqrt via
     * a^{(p+1)/4}).
     *
     * @param bits size in bits (>= 512 recommended; 2048+ for production
     * security)
     * @return prime p
     */
    public static BigInteger primeMod4Equals3(int bits)
    {
        if (bits < 512)
        {
            throw new IllegalArgumentException("Use at least 512 bits; 2048+ recommended for production");
        }
        BigInteger four = BigInteger.valueOf(4);
        while (true)
        {
            BigInteger p = BigInteger.probablePrime(bits, RAND);
            if (p.mod(four).intValue() == 3)
            {
                return p;
            }
        }
    }
    
}
