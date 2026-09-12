/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DuplexChannelTest
{

    @Test
    public void testDuplexSameType() throws Exception
    {
        Channel<String> inChan = Channel.unbuffered();
        Channel<String> outChan = Channel.unbuffered();

        DuplexChannel<String> duplex = Channel.duplex(inChan, outChan);

        // Test writing delegates to outChan
        Thread writer = new Thread(() -> 
        {
            duplex.put("hello");
        });
        writer.start();

        assertEquals("hello", outChan.get());
        writer.join(2000);
        assertFalse(writer.isAlive());

        // Test reading delegates to inChan
        Thread reader = new Thread(() -> 
        {
            inChan.put("world");
        });
        reader.start();

        assertEquals("world", duplex.get());
        reader.join(2000);
        assertFalse(reader.isAlive());
    }

    @Test
    public void testDuplexNullValidation()
    {
        Channel<String> outChan = Channel.unbuffered();
        Channel<String> inChan = Channel.unbuffered();

        assertThrows(NullPointerException.class, () -> Channel.duplex(null, inChan));
        assertThrows(NullPointerException.class, () -> Channel.duplex(outChan, null));
    }

    @Test
    public void testIsInterruptedPropagatesFromDelegates() throws Exception
    {
        CloseableUnbufferedChannel<String> inChan = new CloseableUnbufferedChannel<>();
        CloseableUnbufferedChannel<String> outChan = new CloseableUnbufferedChannel<>();
        DuplexChannel<String> duplex = Channel.duplex(inChan, outChan);
        assertFalse(duplex.isInterrupted());

        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Thread> consumerThread = new AtomicReference<>();
        AtomicReference<String> result = new AtomicReference<>();

        // interrupt a get() blocked on the in() delegate → inChan is marked
        Thread consumer = new Thread(() ->
        {
            consumerThread.set(Thread.currentThread());
            started.countDown();
            result.set(duplex.get());
        });
        consumer.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertNull(result.get());

        consumerThread.get().interrupt();
        consumer.join(5000);

        assertNull(result.get());
        assertTrue(inChan.isInterrupted());
        assertTrue(duplex.isInterrupted(), "duplex must expose the interrupted state of its delegates");
    }
}
