/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConflatedQueueTest
{
    @Test
    public void testBasicOfferAndConflation()
    {
        ConflatedQueue<String> queue = new ConflatedQueue<>();

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertEquals(1, queue.remainingCapacity());

        // First offer
        assertTrue(queue.offer("A"));
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.size());
        assertEquals(0, queue.remainingCapacity());
        assertEquals("A", queue.peek());

        // Second offer overwrites "A" (conflation)
        assertTrue(queue.offer("B"));
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.size());
        assertEquals(0, queue.remainingCapacity());
        assertEquals("B", queue.peek());

        // Retrieve the conflated element
        assertEquals("B", queue.poll());
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertNull(queue.peek());
    }

    @Test
    public void testNullProhibited()
    {
        ConflatedQueue<String> queue = new ConflatedQueue<>();
        assertThrows(NullPointerException.class, () -> queue.offer(null));
        assertThrows(NullPointerException.class, () -> queue.put(null));
    }

    @Test
    public void testBlockingTake() throws InterruptedException
    {
        ConflatedQueue<Integer> queue = new ConflatedQueue<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);
        AtomicReference<Integer> retrieved = new AtomicReference<>();

        Thread consumer = new Thread(() -> {
            try
            {
                startLatch.countDown();
                retrieved.set(queue.take());
                finishLatch.countDown();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();

        // Wait for consumer to start
        startLatch.await();
        Thread.sleep(50); // Small sleep to ensure take() is blocked

        // Offer element
        queue.put(42);

        // Verify consumer unblocks and retrieves 42
        assertTrue(finishLatch.await(2, TimeUnit.SECONDS));
        assertEquals(42, retrieved.get());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testTimeoutPoll() throws InterruptedException
    {
        ConflatedQueue<Integer> queue = new ConflatedQueue<>();

        // Poll with immediate timeout should return null
        assertNull(queue.poll(10, TimeUnit.MILLISECONDS));

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);
        AtomicReference<Integer> retrieved = new AtomicReference<>();

        Thread consumer = new Thread(() -> {
            try
            {
                startLatch.countDown();
                retrieved.set(queue.poll(500, TimeUnit.MILLISECONDS));
                finishLatch.countDown();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();

        startLatch.await();
        Thread.sleep(50);

        queue.offer(100);

        assertTrue(finishLatch.await(2, TimeUnit.SECONDS));
        assertEquals(100, retrieved.get());
    }

    @Test
    public void testClearAndRemove()
    {
        ConflatedQueue<String> queue = new ConflatedQueue<>();
        queue.offer("A");

        assertTrue(queue.contains("A"));
        assertFalse(queue.contains("B"));

        assertTrue(queue.remove("A"));
        assertFalse(queue.contains("A"));
        assertTrue(queue.isEmpty());

        queue.offer("B");
        queue.clear();
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testDrainTo()
    {
        ConflatedQueue<String> queue = new ConflatedQueue<>();
        queue.offer("A");

        List<String> list = new ArrayList<>();
        assertEquals(1, queue.drainTo(list));
        assertEquals(Arrays.asList("A"), list);
        assertTrue(queue.isEmpty());

        // Drain empty queue
        assertEquals(0, queue.drainTo(list));
    }

    @Test
    public void testIterator()
    {
        ConflatedQueue<String> queue = new ConflatedQueue<>();
        Iterator<String> itEmpty = queue.iterator();
        assertFalse(itEmpty.hasNext());
        assertThrows(NoSuchElementException.class, itEmpty::next);

        queue.offer("A");
        Iterator<String> it = queue.iterator();
        assertTrue(it.hasNext());

        // Conflate while iterator is created to verify snapshot behavior
        queue.offer("B");

        assertEquals("A", it.next());
        assertFalse(it.hasNext());

        // Test iterator remove
        Iterator<String> itRemove = queue.iterator();
        assertTrue(itRemove.hasNext());
        assertEquals("B", itRemove.next());
        itRemove.remove();
        assertTrue(queue.isEmpty());
    }
}
