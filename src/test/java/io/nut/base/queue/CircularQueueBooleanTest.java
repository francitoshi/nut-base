/*
 *  CircularQueueBooleanTest.java
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

// Claude Sonnet 4.5
class CircularQueueBooleanTest
{
    @Test
    void testPushAndPop()
    {
        CircularQueueBoolean queue = new CircularQueueBoolean(3);
        queue.push((boolean) true);
        queue.push((boolean) false);
        assertEquals((boolean) true, queue.pop());
        assertEquals(1, queue.size());
    }

    @Test
    void testSum()
    {
        CircularQueueBoolean queue = new CircularQueueBoolean(3);
        queue.push((boolean) true);
        queue.push((boolean) true);
        queue.push((boolean) false);
        assertEquals(2, queue.sum());
    }

    @Test
    void testAverage()
    {
        CircularQueueBoolean queue = new CircularQueueBoolean(4);
        
        assertEquals(0.00, queue.average(), 0.001);

        queue.push((boolean) true);
        assertEquals(1.00, queue.average(), 0.001);

        queue.push((boolean) true);
        assertEquals(1.00, queue.average(), 0.001);
        
        queue.push((boolean) true);
        assertEquals(1.00, queue.average(), 0.001);
        
        queue.push((boolean) false);
        assertEquals(0.75, queue.average(), 0.001);
        
        queue.push((boolean) false);
        assertEquals(0.50, queue.average(), 0.001);
        
        queue.push((boolean) false);
        assertEquals(0.25, queue.average(), 0.001);
        
        queue.push((boolean) false);
        assertEquals(0.00, queue.average(), 0.001);
    }
}
