/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.ring;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;

// Claude Sonnet 4.5
class RingQueueBigDecimalTest
{
    private RingQueueBigDecimal queue;

    @BeforeEach
    void setUp()
    {
        queue = new RingQueueBigDecimal(3);
    }

    @Test
    void testPushAndPop()
    {
        queue.push(new BigDecimal("10.5"));
        queue.push(new BigDecimal("20.7"));
        assertEquals(new BigDecimal("10.5"), queue.pop());
        assertEquals(new BigDecimal("20.7"), queue.pop());
        assertNull(queue.pop());
    }

    @Test
    void testSum()
    {
        queue.push(new BigDecimal("1.5"));
        queue.push(new BigDecimal("2.5"));
        queue.push(new BigDecimal("3.0"));
        assertEquals(new BigDecimal("7.0"), queue.sum());
    }

    @Test
    void testAverage()
    {
        queue.push(new BigDecimal("10"));
        queue.push(new BigDecimal("20"));
        queue.push(new BigDecimal("30"));
        assertEquals(0, new BigDecimal("20").compareTo(queue.average()));
    }

    @Test
    void testMinMax()
    {
        queue.push(new BigDecimal("15.5"));
        queue.push(new BigDecimal("5.2"));
        queue.push(new BigDecimal("25.8"));
        assertEquals(new BigDecimal("5.2"), queue.min());
        assertEquals(new BigDecimal("25.8"), queue.max());
    }

    @Test
    void testGet()
    {
        queue.push(new BigDecimal("100"));
        queue.push(new BigDecimal("200"));
        assertEquals(new BigDecimal("100"), queue.get(0));
        assertEquals(new BigDecimal("200"), queue.get(1));
        assertNull(queue.get(5));
    }
    
   @Test
    void testPeek()
    {
        RingQueueBigDecimal queue = new RingQueueBigDecimal(3);
        queue.push(BigDecimal.ONE);
        queue.push(BigDecimal.TEN);
        assertEquals(BigDecimal.ONE, queue.peek());
        assertEquals(BigDecimal.ONE, queue.peek());
        assertEquals(2, queue.size());
    }
    
}
