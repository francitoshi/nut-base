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
class RingQueueShortTest
{

    private RingQueueShort queue;

    @BeforeEach
    void setUp()
    {
        queue = new RingQueueShort(3);
    }

    @Test
    void testPushAndSize()
    {
        queue.push((short) 10);
        queue.push((short) 20);
        assertEquals(2, queue.size());
    }

    @Test
    void testSum()
    {
        queue.push((short) 5);
        queue.push((short) 10);
        queue.push((short) 15);
        assertEquals(30L, queue.sum());
    }

    @Test
    void testMinMax()
    {
        queue.push((short) 100);
        queue.push((short) 50);
        queue.push((short) 150);
        assertEquals((short) 50, queue.min());
        assertEquals((short) 150, queue.max());
    }

   @Test
    void testPeek()
    {
        RingQueueShort queue = new RingQueueShort(3);
        queue.push((short)10);
        queue.push((short)20);
        assertEquals((short)10, queue.peek());
        assertEquals((short)10, queue.peek());
        assertEquals(2, queue.size());
    }
        
}
