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
