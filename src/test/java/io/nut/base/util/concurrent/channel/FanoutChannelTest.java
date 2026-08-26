/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FanoutChannelTest
{
    @Test
    void broadcastToMultipleTargets() throws InterruptedException
    {
        BufferedChannel<String> dest1 = new BufferedChannel<>(4);
        BufferedChannel<String> dest2 = new BufferedChannel<>(4);
        BufferedChannel<String> dest3 = new BufferedChannel<>(4);

        FanoutChannel<String> fan = new FanoutChannel<>(dest1, dest2, dest3);
        fan.put("hello");

        assertEquals("hello", dest1.get());
        assertEquals("hello", dest2.get());
        assertEquals("hello", dest3.get());
    }

    @Test
    void broadcastPreservesOrder() throws InterruptedException
    {
        BufferedChannel<Integer> dest1 = new BufferedChannel<>(8);
        BufferedChannel<Integer> dest2 = new BufferedChannel<>(8);

        FanoutChannel<Integer> fan = new FanoutChannel<>(dest1, dest2);
        for (int i = 0; i < 5; i++)
        {
            fan.put(i);
        }

        for (int i = 0; i < 5; i++)
        {
            assertEquals(i, dest1.get());
            assertEquals(i, dest2.get());
        }
    }

    @Test
    void addTargetReceivesSubsequentMessages() throws InterruptedException
    {
        BufferedChannel<String> dest1 = new BufferedChannel<>(4);

        FanoutChannel<String> fan = new FanoutChannel<>(dest1);
        fan.put("before");

        BufferedChannel<String> dest2 = new BufferedChannel<>(4);
        fan.addTarget(dest2);
        fan.put("after");

        assertEquals("before", dest1.get());
        assertEquals("after", dest1.get());
        assertEquals("after", dest2.get());
    }

    @Test
    void removeTargetStopsReceiving() throws InterruptedException
    {
        BufferedChannel<String> dest1 = new BufferedChannel<>(4);
        BufferedChannel<String> dest2 = new BufferedChannel<>(4);

        FanoutChannel<String> fan = new FanoutChannel<>(dest1, dest2);
        fan.put("first");

        assertTrue(fan.removeTarget(dest2));
        fan.put("second");

        assertEquals("first", dest1.get());
        assertEquals("second", dest1.get());
        assertEquals("first", dest2.get());
        assertNull(dest2.get(0, TimeUnit.MILLISECONDS));
    }

    @Test
    void factoryMethod() throws InterruptedException
    {
        BufferedChannel<String> dest1 = new BufferedChannel<>(4);
        BufferedChannel<String> dest2 = new BufferedChannel<>(4);

        FanoutChannel<String> fan = Channel.fanout(dest1, dest2);
        fan.put("test");

        assertEquals("test", dest1.get());
        assertEquals("test", dest2.get());
    }

    @Test
    void emptyFanoutDoesNothing() throws InterruptedException
    {
        @SuppressWarnings("unchecked")
        FanoutChannel<String> fan = new FanoutChannel<>();
        fan.put("nothing"); // must not throw
    }

    @Test
    void removeNonexistentTargetReturnsFalse()
    {
        BufferedChannel<String> dest = new BufferedChannel<>(4);
        FanoutChannel<String> fan = new FanoutChannel<>(dest);

        BufferedChannel<String> other = new BufferedChannel<>(4);
        assertFalse(fan.removeTarget(other));
    }

    @Test
    void putTimeoutSuccess() throws InterruptedException
    {
        BufferedChannel<String> dest1 = new BufferedChannel<>(4);
        BufferedChannel<String> dest2 = new BufferedChannel<>(4);

        FanoutChannel<String> fan = new FanoutChannel<>(dest1, dest2);
        assertTrue(fan.put("msg", 100, TimeUnit.MILLISECONDS));

        assertEquals("msg", dest1.get());
        assertEquals("msg", dest2.get());
    }

    @Test
    void putTimeoutReturnsFalseWhenTargetFull() throws InterruptedException
    {
        BufferedChannel<String> dest = new BufferedChannel<>(1);
        dest.put("full");

        FanoutChannel<String> fan = new FanoutChannel<>(dest);
        assertFalse(fan.put("overflow", 10, TimeUnit.MILLISECONDS));
    }

    @Test
    void concurrentPutBroadcastsAllValues() throws Exception
    {
        BufferedChannel<Integer> dest1 = new BufferedChannel<>(64);
        BufferedChannel<Integer> dest2 = new BufferedChannel<>(64);

        FanoutChannel<Integer> fan = new FanoutChannel<>(dest1, dest2);

        final int count = 1000;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger();

        // consumer threads to drain dest1 and dest2
        Thread consumer1 = new Thread(() ->
        {
            try
            {
                start.await();
                for (int i = 0; i < count; i++)
                {
                    assertNotNull(dest1.get());
                }
            }
            catch (Throwable t)
            {
                errors.incrementAndGet();
            }
        });
        Thread consumer2 = new Thread(() ->
        {
            try
            {
                start.await();
                for (int i = 0; i < count; i++)
                {
                    assertNotNull(dest2.get());
                }
            }
            catch (Throwable t)
            {
                errors.incrementAndGet();
            }
        });

        Thread producer = new Thread(() ->
        {
            try
            {
                start.await();
                for (int i = 0; i < count; i++)
                {
                    fan.put(i);
                }
            }
            catch (Throwable t)
            {
                errors.incrementAndGet();
            }
        });

        consumer1.start();
        consumer2.start();
        producer.start();

        start.countDown();
        producer.join(10_000);
        consumer1.join(10_000);
        consumer2.join(10_000);
        assertEquals(0, errors.get());
    }
}
