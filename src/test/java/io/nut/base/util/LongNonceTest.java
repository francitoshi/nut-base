/*
 * LongNonceTest.java
 *
 * Copyright (c) 2021-2025 francitoshi@gmail.com
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
package io.nut.base.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class LongNonceTest
{    
    /**
     * Test of getSequentialInstance method, of class LongNonce.
     */
    @Test
    public void testGetSequentialInstance_long()
    {
        LongNonce nonce = LongNonce.getSequentialInstance(0L);
        
        assertEquals(0L, nonce.peek());
        assertEquals(1L, nonce.get());
        assertEquals(1L, nonce.peek());
    }

    /**
     * Test of getSequentialInstance method, of class LongNonce.
     */
    @Test
    public void testGetSequentialInstance_0args()
    {
        LongNonce nonce = LongNonce.getSequentialInstance();
        
        assertEquals(0L, nonce.peek());
        assertEquals(1L, nonce.get());
        assertEquals(1L, nonce.peek());
    }

    /**
     * Test of getCurrentMillisInstance method, of class LongNonce.
     */
    @Test
    public void testGetCurrentMillisInstance_long()
    {
        LongNonce nonce = LongNonce.getCurrentMillisInstance(0L);
        
        assertEquals(0L, nonce.peek());
        
        long value1 = nonce.get();
        assertTrue(value1>=System.currentTimeMillis());
        assertTrue(value1>0);
        
        long value2 = nonce.get();
        assertTrue(value2>=System.currentTimeMillis());
        assertTrue(value2>value1);
        
    }

    /**
     * Test of getCurrentMillisInstance method, of class LongNonce.
     */
    @Test
    public void testGetCurrentMillisInstance_0args()
    {
        LongNonce nonce = LongNonce.getCurrentMillisInstance();
        
        long t0 = System.currentTimeMillis();
        long value0 = nonce.peek();
        long t1 = System.currentTimeMillis();
        assertTrue(value0>=t0);
        assertTrue(value0<=t1);
        
        long value1 = nonce.get();
        assertTrue(value1>=System.currentTimeMillis());
        assertTrue(value1>value0);
        
        long value2 = nonce.get();
        assertTrue(value2>=System.currentTimeMillis());
        assertTrue(value2>value1);
    }
    
}
