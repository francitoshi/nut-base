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

public class UnbufferedChannelTest
{
    private static final Object MISSING = new Object();

    @Test
    public void testPutGet() throws Exception
    {
        UnbufferedChannel<String> channel = new UnbufferedChannel<>();
        Thread producer = new Thread(() ->
        {
            channel.put("a");
        });
        producer.start();
        assertEquals("a", channel.get());
        producer.join(5000);
        assertFalse(producer.isAlive());
    }

    @Test
    public void testFifoOrder() throws Exception
    {
        UnbufferedChannel<Integer> channel = new UnbufferedChannel<>();
        final int n = 1000;
        Thread producer = new Thread(() ->
        {
            for (int i = 0; i < n; i++)
            {
                channel.put(i);
            }
        });
        producer.start();

        for (int i = 0; i < n; i++)
        {
            assertEquals(i, channel.get());
        }
        producer.join(5000);
        assertFalse(producer.isAlive());
    }

    @Test
    public void testPutBlocksUntilGet() throws Exception
    {
        UnbufferedChannel<Integer> channel = new UnbufferedChannel<>();
        CountDownLatch entered = new CountDownLatch(1);
        AtomicBoolean putReturned = new AtomicBoolean(false);

        Thread producer = new Thread(() ->
        {
            entered.countDown();
            channel.put(1);
            putReturned.set(true);
        });
        producer.start();

        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);
        assertFalse(putReturned.get());

        assertEquals(1, channel.get());
        producer.join(5000);
        assertTrue(putReturned.get());
    }

    @Test
    public void testGetBlocksUntilPut() throws Exception
    {
        UnbufferedChannel<Integer> channel = new UnbufferedChannel<>();
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
    public void testPutNullThrows()
    {
        UnbufferedChannel<Integer> channel = new UnbufferedChannel<>();
        assertThrows(NullPointerException.class, () -> channel.put(null));
    }

    @Test
    public void testPutInterrupted_marksInterruptedAndResumes() throws Exception
    {
        UnbufferedChannel<Integer> channel = new UnbufferedChannel<>();
        CountDownLatch entered = new CountDownLatch(1);
        AtomicBoolean putReturned = new AtomicBoolean(false);

        Thread producer = new Thread(() ->
        {
            entered.countDown();
            channel.put(1);
            putReturned.set(true);
        });
        producer.start();

        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);
        producer.interrupt();
        Thread.sleep(200);

        // The channel recorded the interruption request, but the rendezvous
        // put did NOT abort: it resumed and kept blocking until a get takes it.
        assertTrue(channel.isInterrupted());
        assertFalse(putReturned.get(), "put must resume and stay blocked after interrupt");

        assertEquals(1, channel.get());
        producer.join(5000);
        assertTrue(putReturned.get());
    }

    @Test
    public void testGetInterrupted_marksInterruptedAndResumes() throws Exception
    {
        UnbufferedChannel<Integer> channel = new UnbufferedChannel<>();
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

        // The channel recorded the interruption request, but the rendezvous
        // get did NOT abort: it resumed and kept blocking until a put arrives.
        assertTrue(channel.isInterrupted());
        assertSame(MISSING, result.get(), "get must resume and stay blocked after interrupt");

        channel.put(42);
        consumer.join(5000);
        assertEquals(42, result.get());
    }

    @Test
    public void testConcurrentPutGet() throws Exception
    {
        UnbufferedChannel<Integer> channel = new UnbufferedChannel<>();
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