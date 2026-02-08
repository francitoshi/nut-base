/*
 *  CircularQueueDoubleTest.java
 *
 *  Copyright (c) 2025-2026 francitoshi@gmail.com
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
package io.nut.base.queue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// Claude Sonnet 4.5
class CircularQueueDoubleTest
{

    private CircularQueueDouble queue;

    @BeforeEach
    void setUp()
    {
        queue = new CircularQueueDouble(4);
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
        CircularQueueDouble queue = new CircularQueueDouble(3);
        queue.push(10);
        queue.push(20);
        assertEquals(10, queue.peek());
        assertEquals(10, queue.peek());
        assertEquals(2, queue.size());
    }
    
}
