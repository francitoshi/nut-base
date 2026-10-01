/*
 *  DuplexBlockingQueueTest.java
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