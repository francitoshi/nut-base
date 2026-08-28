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

public class UnlimitedChannelTest
{
    private static final Object MISSING = new Object();

    @Test
    public void testPutGet() throws InterruptedException
    {
        UnlimitedChannel<String> channel = new UnlimitedChannel<>();
        channel.put("a");
        assertEquals("a", channel.get());
    }

    @Test
    public void testFifoOrder() throws InterruptedException
    {
        UnlimitedChannel<Integer> channel = new UnlimitedChannel<>();
        final int n = 100_000;
        for (int i = 0; i < n; i++)
        {
            channel.put(i);
        }
        for (int i = 0; i < n; i++)
        {
            assertEquals(i, channel.get());
        }
    }

    @Test
    public void testManyPutsDoNotBlock() throws InterruptedException
    {
        UnlimitedChannel<Integer> channel = new UnlimitedChannel<>();
        final int n = 1_000_000;
        for (int i = 0; i < n; i++)
        {
            channel.put(i);
        }
        for (int i = 0; i < n; i++)
        {
            assertEquals(i, channel.get());
        }
    }

    @Test
    public void testPutNullThrows()
    {
        UnlimitedChannel<Integer> channel = new UnlimitedChannel<>();
        assertThrows(NullPointerException.class, () -> channel.put(null));
    }

    @Test
    public void testGetBlocksWhenEmpty() throws Exception
    {
        UnlimitedChannel<Integer> channel = new UnlimitedChannel<>();
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
    public void testGetInterruptedWhenEmpty_marksInterruptedAndResumes() throws Exception
    {
        UnlimitedChannel<Integer> channel = new UnlimitedChannel<>();
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
        UnlimitedChannel<Integer> channel = new UnlimitedChannel<>();
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