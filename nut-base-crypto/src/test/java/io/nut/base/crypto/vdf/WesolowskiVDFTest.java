/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.vdf;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class WesolowskiVDFTest
{

    /**
     * Test of create method, of class WesolowskiVDF.
     */
    @Test
    public void testCreate()
    {
        // ALICE CREATES THE PUZZLE
        WesolowskiVDF alice = WesolowskiVDF.create(512);

        assertNotNull(alice);
        
        BigInteger x = alice.createChallenge();
        assertNotNull(x);
        assertTrue(x.compareTo(alice.n)<0);
        
        int spm = alice.delayUnitsPerMillisecond(100);
        assertTrue(spm>0);
        int t = spm*100;

        BigInteger[] y = alice.solve(x, t);
        assertNotNull(y);
        assertTrue(alice.verify(x, t, y));
        
        // BOB SOLVE THE PUZZLE
        
        WesolowskiVDF bob = new WesolowskiVDF(alice.n);

        long t0 = System.nanoTime();
        BigInteger[] y2 = bob.solve(x, t);
        long t1 = System.nanoTime();

        assertNotNull(y2);
        assertEquals(2, y2.length);
        assertTrue(alice.verify(x, t, y2));

        // ALICE VERIFY THE SOLUTION
        
        boolean isValid = alice.verify(x, t, y2);
        assertTrue(isValid);
        
        long ms = TimeUnit.NANOSECONDS.toMillis(t1-t0);
        assertTrue(ms>57 && ms < 133, "ms must be 100 aprox but is "+ms);
        
    }
}
