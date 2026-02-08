/*
 *  CircularQueueBooleanTest.java
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
    
    @Test
    void testPeek()
    {
        CircularQueueBoolean queue = new CircularQueueBoolean(3);
        queue.push(true);
        queue.push(false);
        assertTrue(queue.peek());
        assertTrue(queue.peek());
        assertEquals(2, queue.size());
    }

    @Test
    public void testAll()
    {
        // Test 1: Basic push and pop
        CircularQueueBoolean queue = new CircularQueueBoolean(10);
        queue.push(true);
        queue.push(false);
        queue.push(true);
        queue.push(true);
        assertEquals(4,queue.size());
        assertTrue(queue.pop());
        assertFalse(queue.pop());
        assertEquals(2,queue.size());
        
        // Test 2: Array and get
        queue = new CircularQueueBoolean(5);
        queue.push(true);
        queue.push(false);
        queue.push(true);
        assertArrayEquals(new boolean[]{true, false, true}, queue.array());
        assertTrue(queue.get(0));
        assertFalse(queue.get(1));
        assertTrue(queue.get(2));
        
        // Test 3: push(byte[])
        queue = new CircularQueueBoolean(20);
        byte[] data = new byte[2];
        data[0] = (byte)0b10101010; // alternating pattern
        data[1] = (byte)0b11110000; // 4 zeros, 4 ones
        queue.push(data);
        assertEquals(16,queue.size());
        assertFalse(queue.get(0));
        assertTrue(queue.get(1));

        // Test 4: peek(byte[])
        queue = new CircularQueueBoolean(20);
        queue.push(true);
        queue.push(false);
        queue.push(true);
        queue.push(true);
        queue.push(false);
        queue.push(false);
        queue.push(true);
        queue.push(false);
        assertEquals(8,queue.size());
        assertArrayEquals(new boolean[]{true, false, true, true, false, false, true, false}, queue.array());
        
        byte[] peeked = new byte[1];
        queue.peek(peeked);
        assertEquals(77, peeked[0]);
        assertEquals(8, queue.size());
        
        //Peeked byte again, verify there is no modification
        queue.peek(peeked);
        assertEquals(77, peeked[0]);
        assertEquals(8, queue.size());
        
        // Test 5: pop(byte[])
        queue = new CircularQueueBoolean(20);
        for (int i = 0; i < 16; i++)
        {
            queue.push(i % 2 == 0);
        }
        assertEquals(16, queue.size());
        
        byte[] popped = new byte[2];
        queue.pop(popped);
        assertEquals(0, queue.size());
        
        // Test 6: Sum and average
        queue = new CircularQueueBoolean(10);
        queue.push(true);
        queue.push(true);
        queue.push(false);
        queue.push(true);
        queue.push(false);
        assertEquals(3, queue.sum());
        assertEquals(0.6, queue.average(), 0.001);
        
        // Test 7: Min and max
        queue = new CircularQueueBoolean(5);
        queue.push(true);
        queue.push(true);
        queue.push(true);
        assertTrue(queue.min());
        assertTrue(queue.max());
        
        queue = new CircularQueueBoolean(5);
        queue.push(false);
        queue.push(false);
        queue.push(false);
        assertFalse(queue.min());
        assertFalse(queue.max());
        
        queue = new CircularQueueBoolean(5);
        queue.push(true);
        queue.push(false);
        queue.push(true);
        assertFalse(queue.min());
        assertTrue(queue.max());
        
        // Test 8: Circular behavior (overflow)
        queue = new CircularQueueBoolean(3);
        queue.push(true);
        queue.push(false);
        queue.push(true);
        assertEquals(3, queue.size());
        
        boolean removed = queue.push(false);
        assertTrue(removed);
        assertEquals(3, queue.size());
        
        // Test 9: Constructor with boolean array
        boolean[] initData = {true, false, true, true, false};
        queue = new CircularQueueBoolean(initData);
        assertEquals(5, queue.size());
        
        assertTrue(queue.pop());
        assertFalse(queue.pop());
        assertTrue(queue.pop());
        assertTrue(queue.pop());
        assertFalse(queue.pop());
        
        // Test 10: foreach
        queue = new CircularQueueBoolean(5);
        queue.push(true);
        queue.push(false);
        queue.push(true);
        StringBuilder sb = new StringBuilder();
        queue.foreach(val -> sb.append(val?1:0));
        assertEquals("101", sb.toString());
    }    
}
