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
class RingQueueFloatTest
{

    private RingQueueFloat queue;

    @BeforeEach
    void setUp()
    {
        queue = new RingQueueFloat(4);
    }

    @Test
    void testPushFloat()
    {
        queue.push(1.5f);
        queue.push(2.5f);
        assertEquals(2, queue.size());
    }

    @Test
    void testSum()
    {
        queue.push(1.5f);
        queue.push(2.5f);
        queue.push(3.0f);
        assertEquals(7.0, queue.sum(), 0.001);
    }

    @Test
    void testMinMax()
    {
        queue.push(5.5f);
        queue.push(2.2f);
        queue.push(8.8f);
        assertEquals(2.2f, queue.min(), 0.001);
        assertEquals(8.8f, queue.max(), 0.001);
    }

    @Test
    void testCircularOverwrite()
    {
        queue.push(1.0f);
        queue.push(2.0f);
        queue.push(3.0f);
        queue.push(4.0f);
        queue.push(5.0f); // Sobrescribe el primero

        float[] arr = queue.array();
        assertEquals(4, arr.length);
        assertEquals(2.0f, arr[0], 0.001);
    }
    
   @Test
    void testPeek()
    {
        RingQueueFloat queue = new RingQueueFloat(3);
        queue.push(10);
        queue.push(20);
        assertEquals(10, queue.peek());
        assertEquals(10, queue.peek());
        assertEquals(2, queue.size());
    }
    
}
