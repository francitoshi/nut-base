/*
 *  WesolowskiVDFTest.java
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
        assertTrue(ms>66 && ms < 133, "ms must be 100 aprox but is "+ms);
        
    }
}
