/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BufferedChannelTest
{
    private static final Object MISSING = new Object();

    @Test
    public void testPutGet() throws InterruptedException
    {
        BufferedChannel<String> channel = new BufferedChannel<>(4);
        channel.put("a");
        assertEquals("a", channel.get());
    }

    @Test
    public void testFifoOrder() throws InterruptedException
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(8);
        for (int i = 0; i < 8; i++)
        {
            channel.put(i);
        }
        for (int i = 0; i < 8; i++)
        {
            assertEquals(i, channel.get());
        }
    }

    @Test
    public void testInvalidCapacity()
    {
        assertThrows(IllegalArgumentException.class, () -> new BufferedChannel<>(0));
        assertThrows(IllegalArgumentException.class, () -> new BufferedChannel<>(-5));
    }

    @Test
    public void testPutNullThrows()
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(4);
        assertThrows(NullPointerException.class, () -> channel.put(null));
    }

    @Test
    public void testPutBlocksWhenFull() throws Exception
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(1);
        channel.put(1);

        CountDownLatch entered = new CountDownLatch(1);
        AtomicBoolean putReturned = new AtomicBoolean(false);
        Thread producer = new Thread(() ->
        {
            entered.countDown();
            channel.put(2);
            putReturned.set(true);
        });
        producer.start();

        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);
        assertFalse(putReturned.get());

        assertEquals(1, channel.get());
        producer.join(5000);
        assertTrue(putReturned.get());
        assertEquals(2, channel.get());
    }

    @Test
    public void testGetBlocksWhenEmpty() throws Exception
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(4);
        CountDownLatch entered = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>(MISSING);

        Thread consumer = new Thread(() ->
        {
            entered.countDown();
            result.set(channel.get());
        });
        consumer.start();

        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);
        assertSame(MISSING, result.get());

        channel.put(42);
        consumer.join(5000);
        assertEquals(42, result.get());
    }

    @Test
    public void testPutInterruptedWhenFull_marksInterruptedAndResumes() throws Exception
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(1);
        channel.put(1);

        CountDownLatch entered = new CountDownLatch(1);
        AtomicBoolean putReturned = new AtomicBoolean(false);
        Thread producer = new Thread(() ->
        {
            entered.countDown();
            channel.put(2);
            putReturned.set(true);
        });
        producer.start();

        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);
        producer.interrupt();
        Thread.sleep(200);

        // The channel recorded the interruption request, but the put did NOT
        // abort: it resumed and kept blocking until there is room.
        assertTrue(channel.isInterrupted());
        assertFalse(putReturned.get(), "put must resume and stay blocked after interrupt");

        // Freeing space lets the resumed put complete normally.
        assertEquals(1, channel.get());
        producer.join(5000);
        assertTrue(putReturned.get());
        assertEquals(2, channel.get());
    }

    @Test
    public void testGetInterruptedWhenEmpty_marksInterruptedAndResumes() throws Exception
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(4);
        CountDownLatch entered = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>(MISSING);

        Thread consumer = new Thread(() ->
        {
            entered.countDown();
            result.set(channel.get());
        });
        consumer.start();

        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);
        consumer.interrupt();
        Thread.sleep(200);

        // The channel recorded the interruption request, but the get did NOT
        // abort: it resumed and kept blocking until a value is available.
        assertTrue(channel.isInterrupted());
        assertSame(MISSING, result.get(), "get must resume and stay blocked after interrupt");

        channel.put(42);
        consumer.join(5000);
        assertEquals(42, result.get());
    }

    @Test
    public void testConcurrentPutGet() throws Exception
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(128);
        final int producers = 2;
        final int consumers = 2;
        final int perThread = 5000;
        final int total = producers * perThread;

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(producers + consumers);
        AtomicReference<Throwable> error = new AtomicReference<>();

        for (int p = 0; p < producers; p++)
        {
            final int base = p * perThread;
            Thread t = new Thread(() ->
            {
                try
                {
                    start.await();
                    for (int i = 0; i < perThread; i++)
                    {
                        channel.put(base + i);
                    }
                }
                catch (Throwable t2)
                {
                    error.compareAndSet(null, t2);
                }
                finally
                {
                    done.countDown();
                }
            });
            t.start();
        }
        for (int c = 0; c < consumers; c++)
        {
            Thread t = new Thread(() ->
            {
                try
                {
                    start.await();
                    for (int i = 0; i < total / consumers; i++)
                    {
                        assertNotNull(channel.get());
                    }
                }
                catch (Throwable t2)
                {
                    error.compareAndSet(null, t2);
                }
                finally
                {
                    done.countDown();
                }
            });
            t.start();
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        assertNull(error.get());
    }
}