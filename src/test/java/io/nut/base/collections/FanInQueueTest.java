/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FanInQueueTest
{
    @Test
    void testSubscribeReturnsThisForChaining()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();

        FanInQueue<Integer> fan = new FanInQueue<>();
        assertSame(fan, fan.subscribe(a));
        assertSame(fan, fan.subscribe(a, n -> true));
    }

    @Test
    void testUnsubscribeRemovesSource()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();
        a.add(1);

        FanInQueue<Integer> fan = new FanInQueue<>(a);
        fan.subscribe(b);

        assertTrue(fan.unsubscribe(a));
        assertFalse(fan.unsubscribe(a));
        assertEquals(1, a.size()); // element stays in the source

        b.add(2);
        assertEquals(Integer.valueOf(2), fan.poll());
        assertNull(fan.poll());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testTakeReadsFromAllSources() throws InterruptedException
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();
        a.add(1);
        b.add(2);

        FanInQueue<Integer> fan = new FanInQueue<>(a, b);

        assertEquals(Integer.valueOf(1), fan.take());
        // the next round starts at the next source
        assertEquals(Integer.valueOf(2), fan.take());
    }

    @Test
    void testPollReturnsFirstFoundFromStart()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();
        a.add(10);
        b.add(20);

        FanInQueue<Integer> fan = new FanInQueue<>(a, b);

        assertEquals(Integer.valueOf(10), fan.poll());
    }

    @Test
    void testPollRotatesStartToAvoidStarvation()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();
        a.add(10);
        b.add(20);

        FanInQueue<Integer> fan = new FanInQueue<>(a, b);

        assertEquals(Integer.valueOf(10), fan.poll());
        assertEquals(Integer.valueOf(20), fan.poll());
        assertNull(fan.poll());
    }

    @Test
    void testPollReturnsNullWhenAllSourcesEmpty()
    {
        FanInQueue<Integer> fan = new FanInQueue<>(new LinkedBlockingQueue<>());
        assertNull(fan.poll());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testPollWithTimeoutReturnsElement() throws InterruptedException
    {
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();
        b.add(7);

        FanInQueue<Integer> fan = new FanInQueue<>(new LinkedBlockingQueue<>(), b);

        assertEquals(Integer.valueOf(7), fan.poll(1, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testPollWithTimeoutExpires() throws InterruptedException
    {
        FanInQueue<Integer> fan = new FanInQueue<>(new LinkedBlockingQueue<>());

        long start = System.nanoTime();
        assertNull(fan.poll(20, TimeUnit.MILLISECONDS));
        assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(15));
    }

    @Test
    void testPredicateDropsRejectedElements()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        a.add(5);   // rejected
        a.add(20);  // accepted

        FanInQueue<Integer> fan = new FanInQueue<Integer>().subscribe(a, n -> n > 10);

        // the rejected element is consumed and discarded, not returned
        assertNull(fan.poll());
        assertEquals(Integer.valueOf(20), fan.poll());
        assertTrue(a.isEmpty());
    }

    @Test
    void testPredicateFiltersPerSource()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();
        a.add(1);
        b.add(2);

        FanInQueue<Integer> fan = new FanInQueue<Integer>(a).subscribe(b, n -> n == 2);

        assertEquals(Integer.valueOf(1), fan.poll());
        assertEquals(Integer.valueOf(2), fan.poll());
        assertNull(fan.poll());
    }

    @Test
    void testInsertionsAreNotSupported()
    {
        FanInQueue<Integer> fan = new FanInQueue<>(new LinkedBlockingQueue<>());

        assertThrows(UnsupportedOperationException.class, () -> fan.add(1));
        assertThrows(UnsupportedOperationException.class, () -> fan.put(1));
        assertThrows(UnsupportedOperationException.class, () -> fan.offer(1));
        assertThrows(UnsupportedOperationException.class,
            () -> fan.offer(1, 1, TimeUnit.SECONDS));
        assertThrows(UnsupportedOperationException.class, () -> fan.addAll(new ArrayList<>()));
    }

    @Test
    void testPeekDoesNotConsume()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        a.add(1);

        FanInQueue<Integer> fan = new FanInQueue<>(a);

        assertEquals(Integer.valueOf(1), fan.peek());
        assertEquals(1, a.size());
        assertEquals(Integer.valueOf(1), fan.poll());
    }

    @Test
    void testElementAndRemoveThrowWhenEmpty()
    {
        FanInQueue<Integer> fan = new FanInQueue<>(new LinkedBlockingQueue<>());

        assertThrows(NoSuchElementException.class, fan::element);
        assertThrows(NoSuchElementException.class, fan::remove);
    }

    @Test
    void testSizeContainsDrainClear()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();
        a.add(1);
        a.add(2);
        b.add(3);

        FanInQueue<Integer> fan = new FanInQueue<>(a, b);

        assertEquals(3, fan.size());
        assertFalse(fan.isEmpty());
        assertTrue(fan.contains(2));
        assertFalse(fan.contains(9));

        List<Integer> list = new ArrayList<>();
        assertEquals(3, fan.drainTo(list));
        assertTrue(list.containsAll(java.util.Arrays.asList(1, 2, 3)));
        assertTrue(fan.isEmpty());

        fan.clear();
        assertEquals(0, fan.size());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testTakeBlocksUntilElementAvailable() throws InterruptedException
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        FanInQueue<Integer> fan = new FanInQueue<>(a);

        AtomicReference<Object> result = new AtomicReference<>();
        Thread t = new Thread(() ->
        {
            try
            {
                result.set(fan.take());
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
        });
        t.start();

        Thread.sleep(300);
        assertNull(result.get());
        a.add(42);

        t.join(5000);
        assertFalse(t.isAlive());
        assertEquals(42, result.get());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testTakeBlocksUntilFirstSubscriberIsRegistered() throws InterruptedException
    {
        FanInQueue<Integer> fan = new FanInQueue<>();

        AtomicReference<Object> result = new AtomicReference<>();
        Thread t = new Thread(() ->
        {
            try
            {
                result.set(fan.take());
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
        });
        t.start();

        Thread.sleep(300);
        assertNull(result.get());

        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        fan.subscribe(a);
        a.add(42);

        t.join(5000);
        assertFalse(t.isAlive());
        assertEquals(42, result.get());
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testPollWithTimeoutExpiresWhenNoSubscribers() throws InterruptedException
    {
        FanInQueue<Integer> fan = new FanInQueue<>();
        assertNull(fan.poll(50, TimeUnit.MILLISECONDS));
    }

    @Test
    void testRemainingCapacitySumsSources()
    {
        BlockingQueue<Integer> bounded = new java.util.concurrent.ArrayBlockingQueue<>(3);
        FanInQueue<Integer> fan = new FanInQueue<>(bounded);

        assertEquals(3, fan.remainingCapacity());
        bounded.add(1);
        assertEquals(2, fan.remainingCapacity());
    }

    @Test
    void testRejectsInvalidLatency()
    {
        assertThrows(IllegalArgumentException.class,
            () -> new FanInQueue<Integer>(0, new LinkedBlockingQueue<>()));
        assertThrows(IllegalArgumentException.class,
            () -> new FanInQueue<Integer>(-5L, new LinkedBlockingQueue<>()));
    }

    @Test
    void testDefaultLatencyIsOneSecond() throws InterruptedException
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        a.add(1);
        FanInQueue<Integer> fan = new FanInQueue<>(a);

        // uses the default latency, still reads immediately
        assertEquals(Integer.valueOf(1), fan.poll(1, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testCustomLatencyBoundsTheRoundWait() throws InterruptedException
    {
        // latency 3 ms: rounds wait 1, 2, then 3 ms each
        FanInQueue<Integer> fan = new FanInQueue<>(3L, new LinkedBlockingQueue<>());

        long start = System.nanoTime();
        assertNull(fan.poll(20, TimeUnit.MILLISECONDS));
        long elapsed = System.nanoTime() - start;

        assertTrue(elapsed >= TimeUnit.MILLISECONDS.toNanos(10), "elapsed=" + elapsed);
        assertTrue(elapsed < TimeUnit.MILLISECONDS.toNanos(100), "elapsed=" + elapsed);
    }
}