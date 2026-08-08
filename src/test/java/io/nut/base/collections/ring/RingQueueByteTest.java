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
class RingQueueByteTest
{

    private RingQueueByte queue;

    @BeforeEach
    void setUp()
    {
        queue = new RingQueueByte(3);
    }

    @Test
    void testPushAndPop()
    {
        queue.push((byte) 10);
        queue.push((byte) 20);
        assertEquals((byte) 10, queue.pop());
        assertEquals(1, queue.size());
    }

    @Test
    void testSum()
    {
        queue.push((byte) 1);
        queue.push((byte) 2);
        queue.push((byte) 3);
        assertEquals(6L, queue.sum());
    }

    @Test
    void testAverage()
    {
        queue.push((byte) 10);
        queue.push((byte) 20);
        queue.push((byte) 30);
        assertEquals(20.0, queue.average(), 0.001);
    }

    @Test
    void testPeek()
    {
        RingQueueByte queue = new RingQueueByte(3);
        queue.push((byte) 10);
        queue.push((byte) 20);
        assertEquals((byte) 10, queue.peek());
        assertEquals((byte) 10, queue.peek());
        assertEquals(2, queue.size());
    }

}
