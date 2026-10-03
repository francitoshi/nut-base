/*
 *  SlothVDF.java
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Production-ready Sloth-style VDF implementation (modular square-root based).
 *
 * Notes and guarantees included in Javadoc below. This single-file
 * implementation provides: - prime generation with p % 4 == 3 - hashing to a
 * quadratic residue in Z_p* (safe mapping) - prover: iterative modular
 * square-root (sequential work) - verifier: iterative modular squaring (fast) -
 * utilities, timing/benchmark helpers and a CLI example
 *
 * Security notes (read carefully before production use): - Choose p >= 2048
 * bits for long-term security. - Protect the prime and rotation policy if you
 * rely on secrecy. - This implementation maps an arbitrary input to a quadratic
 * residue by hashing and squaring the hash value; this ensures the square-root
 * exists. - Use SecureRandom and a secure hash (SHA-256) as used here.
 *
 * Usage (example): run main() to generate a prime, create a challenge, compute
 * the VDF proof and verify it. The CLI demonstrates common calls.
 */
public final class SlothVDF
{
    private static final SecureRandom RAND = new SecureRandom();
    private static final String HASH = "SHA-256";

    public final BigInteger p;

    public SlothVDF(BigInteger p)
    {
        this.p = p;
    }
    
    
    public static SlothVDF create(int bits)
    {
        return new SlothVDF(Primes.primeMod4Equals3(bits));
    }
    
    public BigInteger createChallenge()
    {
        byte[] x = new byte[32];
        RAND.nextBytes(x);
        return hashToQuadraticResidue(x);
    }

    /**
     * Prover / Evaluator: computes t iterations of canonical modular square
     * root. Each iteration must be performed sequentially; this is the
     * time-consuming step.
     *
     * Input x must be a quadratic residue. Use hashToQuadraticResidue to
     * produce x.
     *
     * @param x quadratic residue in Z_p
     *
     * @param t number of iterations (t >= 1)
     * @return y = SlothEval(x, t) computed by t iterative canonical square
     * roots
     */
    public BigInteger solve(BigInteger x, int t)
    {
        Objects.requireNonNull(x, "x must not be null");
        if (t <= 0)
        {
            throw new IllegalArgumentException("t must be > 0");
        }
        BigInteger cur = x.mod(p);
        for (int i = 0; i < t; i++)
        {
            cur = canonicalSqrt(cur, p);
        }
        return cur;
    }

    /**
     * Verifier: checks that y is a correct t-iteration preimage of x by
     * squaring t times. That is, compute y^{2^t} (by repeated squaring) and
     * compare to x modulo p. Repeated squaring is cheap compared to modular
     * sqrt exponentiation.
     *
     * @param x original quadratic residue challenge
     * @param y candidate proof
     * @param t number of iterations
     * @return true if valid
     */
    public boolean verify(BigInteger x, BigInteger y, int t)
    {
        Objects.requireNonNull(x, "x must not be null");
        Objects.requireNonNull(y, "y must not be null");
        if (t < 0)
        {
            throw new IllegalArgumentException("t cannot be negative");
        }
        BigInteger cur = y.mod(p);
        for (int i = 0; i < t; i++)
        {
            cur = cur.multiply(cur).mod(p);
        }
        return cur.equals(x.mod(p));
    }
    
    /**
     * Hash arbitrary input to a field element in [1, p-1] and make it a
     * quadratic residue by squaring the hash-derived integer modulo p. This
     * guarantees the value has a modular square root.
     *
     * The mapping: x = HashToInt(data) mod p; return (x^2 mod p) ensuring x in
     * QR.
     *
     * @param data input bytes
     * @param p prime modulus
     * @return an integer in Z_p* that is a quadratic residue (has sqrt)
     */
    protected BigInteger hashToQuadraticResidue(byte[] data)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance(HASH);
            md.update(data);
            byte[] digest = md.digest();
            BigInteger x = new BigInteger(1, digest).mod(p);
            // ensure non-zero
            if (x.equals(BigInteger.ZERO))
            {
                x = BigInteger.ONE;
            }
            // square to guarantee a quadratic residue
            return x.multiply(x).mod(p);
        }
        catch (NoSuchAlgorithmException e)
        {
            // SHA-256 always exists in standard JRE
            throw new RuntimeException(e);
        }
    }

    /**
     * Compute the canonical modular square root for p ≡ 3 (mod 4). The
     * mathematical square root is r = a^{(p+1)/4} (mod p). We return a
     * canonical representative (the "even" root): if r is odd, return p - r.
     * This gives a unique deterministic root.
     *
     * WARNING: This method assumes input a is a quadratic residue (i.e., sqrt
     * exists). Use {@link #hashToQuadraticResidue(byte[], BigInteger)} to
     * produce such values.
     *
     * @param a value in Z_p* (quadratic residue)
     * @param p prime modulus with p%4==3
     * @return canonical square root in [0, p-1]
     */
    public static BigInteger canonicalSqrt(BigInteger a, BigInteger p)
    {
        if (!p.isProbablePrime(64) || p.mod(BigInteger.valueOf(4)).intValue() != 3)
        {
            throw new IllegalArgumentException("p must be prime and p % 4 == 3");
        }
        BigInteger exp = p.add(BigInteger.ONE).shiftRight(2); // (p+1)/4
        BigInteger r = a.modPow(exp, p);
        // choose canonical root: make the returned root even (LSB == 0)
        if (r.testBit(0))
        {
            r = p.subtract(r);
        }
        return r;
    }
    // ---------------------- Utilities and example CLI ----------------------

    public int delayUnitsPerMillisecond(int testMillis)
    {
        BigInteger x = createChallenge();
        long nanos = 0;
        long count = 0;
        
        long testNanos = TimeUnit.MILLISECONDS.toNanos(testMillis);
        for(int i=0,t=10;i<31 && nanos<testNanos;i++,t*=2)
        {
            long t0 = System.nanoTime();
            solve(x, t);
            long t1 = System.nanoTime();
            nanos += (t1-t0);
            count += t;
        }
        long millis = TimeUnit.NANOSECONDS.toMillis(nanos);
        return (int)(count/millis);
    }

}
