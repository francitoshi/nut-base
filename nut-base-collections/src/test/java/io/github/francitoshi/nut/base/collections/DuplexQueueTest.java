/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.collections;

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