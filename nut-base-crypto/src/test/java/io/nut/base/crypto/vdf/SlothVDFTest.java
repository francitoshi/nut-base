/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.vdf;

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
