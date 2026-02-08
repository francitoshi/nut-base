/*
 *  CircularQueueByteTest.java
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
class CircularQueueByteTest
{

    private CircularQueueByte queue;

    @BeforeEach
    void setUp()
    {
        queue = new CircularQueueByte(3);
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
        CircularQueueByte queue = new CircularQueueByte(3);
        queue.push((byte) 10);
        queue.push((byte) 20);
        assertEquals((byte) 10, queue.peek());
        assertEquals((byte) 10, queue.peek());
        assertEquals(2, queue.size());
    }

}
