/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.ring;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

// Claude Sonnet 4.5
class RingQueueIntTest
{

    @Test
    void testConstructorWithData()
    {
        RingQueueInt queue = new RingQueueInt(new int[]{100,200});

        assertEquals(2, queue.size());

        assertEquals(100, queue.push(300));
        assertEquals(200, queue.push(400));
        
        assertEquals(2, queue.size());
        
        assertEquals(300, queue.push(500));
        assertEquals(400, queue.push(600));
        
        assertEquals(2, queue.size());
        
        assertEquals(500, queue.pop());
        assertEquals(600, queue.pop());

        assertEquals(0, queue.size());
    }

    @Test
    void testPushAndPop()
    {
        RingQueueInt queue = new RingQueueInt(3);

        assertEquals(0, queue.size());

        queue.push(100);
        queue.push(200);

        assertEquals(100, queue.pop());
        assertEquals(200, queue.pop());
        assertEquals(0, queue.size());
        assertEquals(0, queue.pop());


    }

    @Test
    void testSum()
    {
        RingQueueInt queue = new RingQueueInt(3);
        queue.push(5);
        queue.push(10);
        queue.push(15);
        assertEquals(30L, queue.sum());
    }

    @Test
    void testAverage()
    {
        RingQueueInt queue = new RingQueueInt(3);
        queue.push(10);
        queue.push(20);
        queue.push(30);
        assertEquals(20.0, queue.average(), 0.001);
    }

    @Test
    void testMinMax()
    {
        RingQueueInt queue = new RingQueueInt(3);
        queue.push(25);
        queue.push(10);
        queue.push(40);
        assertEquals(10, queue.min());
        assertEquals(40, queue.max());
    }
    
    @Test
    void testPeek()
    {
        RingQueueInt queue = new RingQueueInt(3);
        queue.push(10);
        queue.push(20);
        assertEquals(10, queue.peek());
        assertEquals(10, queue.peek());
        assertEquals(2, queue.size());
    }
    
}
