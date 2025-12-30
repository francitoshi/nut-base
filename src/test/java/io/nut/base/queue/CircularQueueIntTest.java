/*
 *  CircularQueueIntTest.java
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

import io.nut.base.profile.Profiler;
import io.nut.base.time.JavaTime;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

// Claude Sonnet 4.5
class CircularQueueIntTest
{

    private CircularQueueInt queue;

    @BeforeEach
    void setUp()
    {
        queue = new CircularQueueInt(3);
    }

    @Test
    void testPushAndPop()
    {
        queue.push(100);
        queue.push(200);
        assertEquals(100, queue.pop());
        assertEquals(200, queue.pop());
        assertEquals(0, queue.pop());
    }

    @Test
    void testSum()
    {
        queue.push(5);
        queue.push(10);
        queue.push(15);
        assertEquals(30L, queue.sum());
    }

    @Test
    void testAverage()
    {
        queue.push(10);
        queue.push(20);
        queue.push(30);
        assertEquals(20.0, queue.average(), 0.001);
    }

    @Test
    void testMinMax()
    {
        queue.push(25);
        queue.push(10);
        queue.push(40);
        assertEquals(10, queue.min());
        assertEquals(40, queue.max());
    }
}
