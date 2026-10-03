/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class PrimesTest
{
    /**
     * Test of safePrime method, of class Primes.
     */
    @Test
    public void testSafePrime()
    {
        long t0 = System.nanoTime();
        for(int bits=32;bits<=1024;bits*=2)
        {
            BigInteger p = Primes.safePrime(bits, 20);
            assertEquals(bits, p.bitLength());
            BigInteger q = p.divide(BigInteger.valueOf(2));
            assertEquals(p, q.multiply(BigInteger.valueOf(2)).add(BigInteger.ONE));
            long t1 = System.nanoTime();
            System.out.println("bits="+bits+" ms="+TimeUnit.NANOSECONDS.toMillis(t1-t0));
        }
    }

}
