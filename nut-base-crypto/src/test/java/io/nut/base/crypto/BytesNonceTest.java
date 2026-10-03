/*
 *  BytesNonceTest.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class BytesNonceTest
{
    static final int LOOPS = 100_000;
    
    @Test
    public void testGetRandomInstance()
    {
        BytesNonce instance = BytesNonce.getRandomInstance();
        
        HashSet<byte[]> set = new HashSet();
        for(int i=0;i<LOOPS;i++)
        {
            assertTrue(set.add(instance.next()));
        }
        assertEquals(LOOPS, set.size());
    }

    @Test
    public void testGetRandomCounterInstance()
    {
        BytesNonce instance = BytesNonce.getRandomCounterInstance();
        HashSet<byte[]> set = new HashSet();
        for(int i=0;i<LOOPS;i++)
        {
            assertTrue(set.add(instance.next()));
        }
        assertEquals(LOOPS, set.size());
    }
    
}
