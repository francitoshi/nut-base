/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.ring;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// Claude Sonnet 4.5
class RingQueueDoubleTest
{

    private RingQueueDouble queue;

    @BeforeEach
    void setUp()
    {
        queue = new RingQueueDouble(4);
    }

    @Test
    void testPushDouble()
    {
        queue.push(1.5);
        queue.push(2.7);
        queue.push(3.9);
        assertEquals(3, queue.size());
    }

    @Test
    void testSum()
    {
        queue.push(1.5);
        queue.push(2.5);
        queue.push(3.0);
        assertEquals(7.0, queue.sum(), 0.001);
    }

    @Test
    void testAverage()
    {
        queue.push(2.0);
        queue.push(4.0);
        queue.push(6.0);
        assertEquals(4.0, queue.average(), 0.001);
    }

    @Test
    void testMinMax()
    {
        queue.push(5.5);
        queue.push(2.3);
        queue.push(8.1);
        assertEquals(2.3, queue.min(), 0.001);
        assertEquals(8.1, queue.max(), 0.001);
    }
    
   @Test
    void testPeek()
    {
        RingQueueDouble queue = new RingQueueDouble(3);
        queue.push(10);
        queue.push(20);
        assertEquals(10, queue.peek());
        assertEquals(10, queue.peek());
        assertEquals(2, queue.size());
    }
    
    @Test
    void testGetSynchronized()
    {
        RingQueueDouble queue = new RingQueueDouble(5);
        queue.push(1.5);
        queue.push(2.5);
        queue.push(3.0);

        RingQueueDouble sync = RingQueueDouble.getSynchronized(queue);

        assertEquals(3, sync.size());
        assertEquals(1.5, sync.get(0), 0.001);
        assertEquals(2.5, sync.get(1), 0.001);
        assertEquals(3.0, sync.get(2), 0.001);

        sync.push(4.0);

        assertEquals(4, queue.size());
        assertEquals(4.0, queue.get(3), 0.001);
    }

}
