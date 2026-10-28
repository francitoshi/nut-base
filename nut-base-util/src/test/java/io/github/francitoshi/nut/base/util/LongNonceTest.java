/*
 * Copyright (C) 2021-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.util;

import io.github.francitoshi.nut.base.commons.time.JavaTime;
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
    
    /**
     * Test of getEpochSecondInstance method, of class LongNonce.
     */
    @Test
    public void testGetEpochSecondInstance_long()
    {
        LongNonce nonce = LongNonce.getEpochSecondInstance(0L);
        
        assertEquals(0L, nonce.peek());
        
        long value1 = nonce.get();
        assertTrue(value1>=JavaTime.epochSecond());
        assertTrue(value1>0);
        
        long value2 = nonce.get();
        assertTrue(value2>=JavaTime.epochSecond());
        assertTrue(value2>value1);
        
    }

    /**
     * Test of getEpochSecondInstance method, of class LongNonce.
     */
    @Test
    public void testGetEpochSecondInstance_0args()
    {
        LongNonce nonce = LongNonce.getEpochSecondInstance();
        
        long t0 = JavaTime.epochSecond();
        long value0 = nonce.peek();
        long t1 = JavaTime.epochSecond();
        assertTrue(value0>=t0);
        assertTrue(value0<=t1);
        
        long value1 = nonce.get();
        assertTrue(value1>=JavaTime.epochSecond());
        assertTrue(value1>value0);
        
        long value2 = nonce.get();
        assertTrue(value2>=JavaTime.epochSecond());
        assertTrue(value2>value1);
    }
    
}
