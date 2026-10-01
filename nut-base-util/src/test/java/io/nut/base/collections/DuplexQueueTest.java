/*
 *  DuplexQueueTest.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class DuplexQueueTest
{
    private Queue<String> readQueue;
    private Queue<String> writeQueue;
    private DuplexQueue<String> duplexQueue;

    @BeforeEach
    void setUp()
    {
        readQueue = new LinkedList<>();
        writeQueue = new LinkedList<>();
        duplexQueue = new DuplexQueue<>(readQueue, writeQueue);
    }

    @Test
    void testOfferAndAddWriteToRightQueue()
    {
        duplexQueue.offer("A");
        duplexQueue.add("B");

        assertTrue(writeQueue.contains("A"));
        assertTrue(writeQueue.contains("B"));
        assertTrue(readQueue.isEmpty(), "Read queue should remain empty when adding to duplex");
    }

    @Test
    void testPollAndPeekReadFromRightQueue()
    {
        readQueue.add("ReadMe");
        
        assertEquals("ReadMe", duplexQueue.peek());
        assertEquals("ReadMe", duplexQueue.poll());
        assertTrue(readQueue.isEmpty());
        assertTrue(writeQueue.isEmpty());
    }

    @Test
    void testSizeAndIsEmptyReflectReadQueue()
    {
        readQueue.add("Item");
        writeQueue.add("Ignored");

        assertEquals(1, duplexQueue.size());
        assertFalse(duplexQueue.isEmpty());
        
        readQueue.clear();
        assertTrue(duplexQueue.isEmpty());
    }

    @Test
    void testAddAllDelegatesToWrite()
    {
        duplexQueue.addAll(Arrays.asList("X", "Y"));
        assertEquals(0, writeQueue.size());
        assertEquals(2, readQueue.size());
    }

    @Test
    void testClearOnlyClearsRead()
    {
        readQueue.add("A");
        writeQueue.add("B");
        
        duplexQueue.clear();
        
        assertTrue(readQueue.isEmpty());
        assertFalse(writeQueue.isEmpty());
    }
}