/*
 *  SlothVDFTest.java
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

import io.nut.base.util.Joins;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class SlothVDFTest
{
    static final SecureRandom RAND = new SecureRandom();
    static final BigInteger TWO = BigInteger.valueOf(2);
    
    static final int BITS = 512;//2048;
    static final int T = 100;//1000;
    @Test
    public void testAll()
    {
        SlothVDF instance = SlothVDF.create(BITS);
        
        // Create challenge
        BigInteger x = instance.createChallenge();

        // Evaluate (prover)
        BigInteger y = instance.solve(x, T);
        boolean ok = instance.verify(x, y, T);

        assertTrue(ok);
    }
    @Test
    public void testBasicEvaluateVerify()
    {
        SlothVDF instance = SlothVDF.create(BITS);
        
        BigInteger x = instance.createChallenge();
        BigInteger y = instance.solve(x, T);
        assertTrue(instance.verify(x, y, T));
    }

    @Test
    public void testDeterministicOutput()
    {
        SlothVDF instance = SlothVDF.create(BITS);
        
        BigInteger x = new BigInteger("1234567890");

        BigInteger y1 = instance.solve(x, T);
        BigInteger y2 = instance.solve(x, T);

        assertEquals(y1, y2);
    }

    @Test
    public void testVerificationFailsOnWrongY()
    {
        SlothVDF instance = SlothVDF.create(BITS);

        BigInteger x = new BigInteger("987654321");

        BigInteger y = instance.solve(x, T);
        BigInteger wrong = y.add(BigInteger.ONE).mod(instance.p);

        assertFalse(instance.verify(x, wrong, T));
    }

    @Test
    public void testMappingProducesQuadraticResidue()
    {
        SlothVDF instance = SlothVDF.create(BITS);
        byte[] message = new byte[64];
        RAND.nextBytes(message);

        BigInteger x = instance.hashToQuadraticResidue(message);

        // verify x is a quadratic residue: x^((p-1)/2) mod p == 1
        BigInteger check = x.modPow(instance.p.subtract(BigInteger.ONE).divide(TWO), instance.p);
        assertEquals(BigInteger.ONE, check);
    }

}
