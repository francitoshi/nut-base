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

public class CloseableBufferedChannelTest
{
    private static final Object MISSING = new Object();

    @Test
    public void testPutGet() throws InterruptedException
    {
        CloseableBufferedChannel<String> channel = new CloseableBufferedChannel<>(4);
        channel.put("a");
        assertEquals("a", channel.get());
    }

    @Test
    public void testFifoOrder() throws InterruptedException
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(8);
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
        assertThrows(IllegalArgumentException.class, () -> new CloseableBufferedChannel<>(0));
        assertThrows(IllegalArgumentException.class, () -> new CloseableBufferedChannel<>(-5));
    }

    @Test
    public void testPutNullThrows()
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(4);
        assertThrows(NullPointerException.class, () -> channel.put(null));
    }

    @Test
    public void testInitialStateNotClosed()
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(4);
        assertFalse(channel.isClosed());
    }

    @Test
    public void testCloseEmptyChannel() throws InterruptedException
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(4);
        assertTrue(channel.close());
        assertTrue(channel.isClosed());
        assertNull(channel.get());
        assertThrows(IllegalStateException.class, () -> channel.put(1));
    }

    @Test
    public void testDoubleCloseReturnsTrue()
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(4);
        assertTrue(channel.close());
        assertTrue(channel.close());
    }

    @Test
    public void testPutAfterCloseThrows() throws Exception
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(4);
        assertTrue(channel.close());
        assertThrows(IllegalStateException.class, () -> channel.put(1));
    }

    @Test
    public void testGetAfterCloseReturnsNull() throws Exception
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(4);
        channel.put(1);
        channel.put(2);
        assertEquals(1, channel.get());
        assertEquals(2, channel.get());
        assertTrue(channel.close());
        assertNull(channel.get());
    }

    @Test
    public void testCloseWithTimeoutReturnsTrue() throws Exception
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(4);
        channel.put(1);
        assertEquals(1, channel.get());
        assertTrue(channel.close(500, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCloseUnblocksBlockedGet() throws Exception
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(4);
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

        assertTrue(channel.close());
        consumer.join(5000);
        assertNull(result.get());
    }

    @Test
    public void testCloseTimeoutExpiresWithBlockedPut() throws Exception
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(1);
        channel.put(1);

        CountDownLatch entered = new CountDownLatch(1);
        AtomicBoolean putReturned = new AtomicBoolean(false);
        AtomicReference<Throwable> putError = new AtomicReference<>();

        Thread producer = new Thread(() ->
        {
            try
            {
                entered.countDown();
                channel.put(2);
                putReturned.set(true);
            }
            catch (Throwable t2)
            {
                putError.set(t2);
            }
        });
        producer.start();

        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);
        assertFalse(channel.close(100, TimeUnit.MILLISECONDS));
        assertTrue(channel.isClosed());

        // The channel is now closed, so the resumed put (after the interrupt)
        // cannot complete: it gives up with IllegalStateException.
        producer.interrupt();
        producer.join(5000);
        assertFalse(putReturned.get());
        assertTrue(putError.get() instanceof IllegalStateException);
        assertTrue(channel.isInterrupted());
    }

    @Test
    public void testConcurrentPutGetClose() throws Exception
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(128);
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
                        Integer v = channel.get();
                        if (v == null)
                        {
                            throw new IllegalStateException("channel closed before consuming all messages");
                        }
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
        assertTrue(channel.close());
    }
}