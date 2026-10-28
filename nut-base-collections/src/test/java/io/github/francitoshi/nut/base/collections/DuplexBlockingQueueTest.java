/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.collections;

import io.github.francitoshi.nut.base.collections.DuplexBlockingQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DuplexBlockingQueueTest 
{

    private BlockingQueue<Integer> readBQ;
    private BlockingQueue<Integer> writeBQ;
    private DuplexBlockingQueue<Integer> duplexBQ;

    @BeforeEach
    void setUp() 
    {
        readBQ = new LinkedBlockingQueue<>();
        writeBQ = new ArrayBlockingQueue<>(10);
        duplexBQ = new DuplexBlockingQueue<>(readBQ, writeBQ);
    }

    @Test
    void testPutDelegatesToWrite() throws InterruptedException 
    {
        duplexBQ.put(100);
        assertEquals(100, writeBQ.peek());
        assertTrue(readBQ.isEmpty());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testTakeDelegatesToRead() throws InterruptedException 
    {
        readBQ.put(500);
        Integer result = duplexBQ.take();
        assertEquals(500, result);
    }

    @Test
    void testRemainingCapacityReflectsWrite()
    {
        assertEquals(10, duplexBQ.remainingCapacity());
        writeBQ.add(1);
        assertEquals(9, duplexBQ.remainingCapacity());
    }

    @Test
    void testDrainToDelegatesToRead()
    {
        readBQ.add(1);
        readBQ.add(2);
        
        List<Integer> list = new ArrayList<>();
        int drained = duplexBQ.drainTo(list);
        
        assertEquals(2, drained);
        assertEquals(2, list.size());
        assertTrue(readBQ.isEmpty());
    }

    @Test
    void testOfferWithTimeout() throws InterruptedException
    {
        boolean success = duplexBQ.offer(1, 100, TimeUnit.MILLISECONDS);
        assertTrue(success);
        assertEquals(1, writeBQ.size());
    }

    @Test
    void testPollWithTimeout() throws InterruptedException
    {
        readBQ.add(99);
        Integer result = duplexBQ.poll(100, TimeUnit.MILLISECONDS);
        assertEquals(99, result);
    }
}