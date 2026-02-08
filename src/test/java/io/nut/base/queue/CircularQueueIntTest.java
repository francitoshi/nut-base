/*
 *  CircularQueueIntTest.java
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

// Claude Sonnet 4.5
class CircularQueueIntTest
{

    @Test
    void testConstructorWithData()
    {
        CircularQueueInt queue = new CircularQueueInt(new int[]{100,200});

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
        CircularQueueInt queue = new CircularQueueInt(3);

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
        CircularQueueInt queue = new CircularQueueInt(3);
        queue.push(5);
        queue.push(10);
        queue.push(15);
        assertEquals(30L, queue.sum());
    }

    @Test
    void testAverage()
    {
        CircularQueueInt queue = new CircularQueueInt(3);
        queue.push(10);
        queue.push(20);
        queue.push(30);
        assertEquals(20.0, queue.average(), 0.001);
    }

    @Test
    void testMinMax()
    {
        CircularQueueInt queue = new CircularQueueInt(3);
        queue.push(25);
        queue.push(10);
        queue.push(40);
        assertEquals(10, queue.min());
        assertEquals(40, queue.max());
    }
    
    @Test
    void testPeek()
    {
        CircularQueueInt queue = new CircularQueueInt(3);
        queue.push(10);
        queue.push(20);
        assertEquals(10, queue.peek());
        assertEquals(10, queue.peek());
        assertEquals(2, queue.size());
    }
    
}
