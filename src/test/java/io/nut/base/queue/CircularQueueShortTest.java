/*
 *  CircularQueueShortTest.java
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
class CircularQueueShortTest
{

    private CircularQueueShort queue;

    @BeforeEach
    void setUp()
    {
        queue = new CircularQueueShort(3);
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
        CircularQueueShort queue = new CircularQueueShort(3);
        queue.push((short)10);
        queue.push((short)20);
        assertEquals((short)10, queue.peek());
        assertEquals((short)10, queue.peek());
        assertEquals(2, queue.size());
    }
        
}
