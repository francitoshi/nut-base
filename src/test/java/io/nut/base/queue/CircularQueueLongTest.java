/*
 *  CircularQueueLongTest.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
import java.util.ArrayList;
import java.util.List;

// Claude Sonnet 4.5
class CircularQueueLongTest
{

    private CircularQueueLong queue;

    @BeforeEach
    void setUp()
    {
        queue = new CircularQueueLong(5);
    }

    @Test
    void testConstructorInvalidCapacity()
    {
        assertThrows(IllegalArgumentException.class, () -> new CircularQueueLong(0));
        assertThrows(IllegalArgumentException.class, () -> new CircularQueueLong(-1));
    }

    @Test
    void testPushAndSize()
    {
        assertEquals(0, queue.size());
        queue.push(10L);
        assertEquals(1, queue.size());
        queue.push(20L);
        assertEquals(2, queue.size());
    }

    @Test
    void testPushOverflow()
    {
        for (int i = 1; i <= 5; i++)
        {
            assertEquals(0, queue.push(i * 10L));
        }
        assertEquals(5, queue.size());

        long removed = queue.push(60L);
        assertEquals(10L, removed);
        assertEquals(5, queue.size());
    }

    @Test
    void testPop()
    {
        queue.push(10L);
        queue.push(20L);
        queue.push(30L);

        assertEquals(10L, queue.pop());
        assertEquals(2, queue.size());
        assertEquals(20L, queue.pop());
        assertEquals(1, queue.size());
    }

    @Test
    void testPopEmpty()
    {
        assertEquals(0L, queue.pop());
    }

    @Test
    void testForeach()
    {
        queue.push(10L);
        queue.push(20L);
        queue.push(30L);

        List<Long> values = new ArrayList<>();
        queue.foreach(values::add);

        assertArrayEquals(new Long[]
        {
            10L, 20L, 30L
        }, values.toArray());
    }

    @Test
    void testArray()
    {
        queue.push(10L);
        queue.push(20L);
        queue.push(30L);

        assertArrayEquals(new long[]
        {
            10L, 20L, 30L
        }, queue.array());
    }

    @Test
    void testSum()
    {
        queue.push(10L);
        queue.push(20L);
        queue.push(30L);

        assertEquals(60L, queue.sum());
    }

    @Test
    void testAverage()
    {
        queue.push(10L);
        queue.push(20L);
        queue.push(30L);

        assertEquals(20.0, queue.average(), 0.001);
    }

    @Test
    void testMin()
    {
        queue.push(30L);
        queue.push(10L);
        queue.push(20L);

        assertEquals(10L, queue.min());
    }

    @Test
    void testMax()
    {
        queue.push(30L);
        queue.push(10L);
        queue.push(50L);

        assertEquals(50L, queue.max());
    }

    @Test
    void testGet()
    {
        queue.push(10L);
        queue.push(20L);
        queue.push(30L);

        assertEquals(10L, queue.get(0));
        assertEquals(20L, queue.get(1));
        assertEquals(30L, queue.get(2));
        assertEquals(0L, queue.get(3));
        assertEquals(0L, queue.get(-1));
    }

    @Test
    void testCircularBehavior()
    {
        for (int i = 1; i <= 7; i++)
        {
            queue.push(i * 10L);
        }

        assertArrayEquals(new long[]
        {
            30L, 40L, 50L, 60L, 70L
        }, queue.array());
    }
}
