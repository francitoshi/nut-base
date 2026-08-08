/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.ring;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigInteger;

// Claude Sonnet 4.5
class RingQueueBigIntegerTest
{

    private RingQueueBigInteger queue;

    @BeforeEach
    void setUp()
    {
        queue = new RingQueueBigInteger(3);
    }

    @Test
    void testPushAndPop()
    {
        queue.push(new BigInteger("100"));
        queue.push(new BigInteger("200"));
        assertEquals(new BigInteger("100"), queue.pop());
        assertEquals(new BigInteger("200"), queue.pop());
        assertNull(queue.pop());
    }

    @Test
    void testSum()
    {
        queue.push(new BigInteger("1000000000000"));
        queue.push(new BigInteger("2000000000000"));
        queue.push(new BigInteger("3000000000000"));
        assertEquals(new BigInteger("6000000000000"), queue.sum());
    }

    @Test
    void testAverage()
    {
        queue.push(new BigInteger("30"));
        queue.push(new BigInteger("60"));
        queue.push(new BigInteger("90"));
        assertEquals(new BigDecimal("60"), queue.average());
    }

    @Test
    void testMinMax()
    {
        queue.push(new BigInteger("500"));
        queue.push(new BigInteger("100"));
        queue.push(new BigInteger("300"));
        assertEquals(new BigInteger("100"), queue.min());
        assertEquals(new BigInteger("500"), queue.max());
    }

    @Test
    void testPushOverflow()
    {
        queue.push(new BigInteger("10"));
        queue.push(new BigInteger("20"));
        queue.push(new BigInteger("30"));
        BigInteger removed = queue.push(new BigInteger("40"));
        assertEquals(new BigInteger("10"), removed);
    }
    
   @Test
    void testPeek()
    {
        RingQueueBigInteger queue = new RingQueueBigInteger(3);
        queue.push(BigInteger.ONE);
        queue.push(BigInteger.TEN);
        assertEquals(BigInteger.ONE, queue.peek());
        assertEquals(BigInteger.ONE, queue.peek());
        assertEquals(2, queue.size());
    }
        
}
