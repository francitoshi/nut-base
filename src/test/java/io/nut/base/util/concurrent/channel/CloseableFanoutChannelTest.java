/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CloseableFanoutChannelTest
{
    @Test
    void broadcastAndClose() throws InterruptedException
    {
        CloseableChannel<String> dest1 = Channel.closeableOf(4);
        CloseableChannel<String> dest2 = Channel.closeableOf(4);

        CloseableFanoutChannel<String> fan = Channel.closeableFanout(dest1, dest2);
        fan.put("hello");
        fan.put("world");

        assertTrue(fan.close());
        assertTrue(fan.isClosed());

        assertEquals("hello", dest1.get());
        assertEquals("world", dest1.get());
        assertNull(dest1.get());

        assertEquals("hello", dest2.get());
        assertEquals("world", dest2.get());
        assertNull(dest2.get());
    }

    @Test
    void putAfterCloseThrows() throws InterruptedException
    {
        CloseableChannel<String> dest = Channel.closeableOf(4);
        CloseableFanoutChannel<String> fan = Channel.closeableFanout(dest);

        fan.close();
        assertThrows(IllegalStateException.class, () -> fan.put("nope"));
    }

    @Test
    void closeIsIdempotent()
    {
        @SuppressWarnings("unchecked")
        CloseableChannel<String> dest = Channel.closeableOf(4);
        CloseableFanoutChannel<String> fan = Channel.closeableFanout(dest);

        assertTrue(fan.close());
        assertTrue(fan.close());
        assertTrue(fan.isClosed());
    }

    @Test
    void closePropagatesToCloseableTargetsOnly() throws InterruptedException
    {
        CloseableChannel<String> closeable = Channel.closeableOf(4);
        BufferedChannel<String> plain = new BufferedChannel<>(4);

        CloseableFanoutChannel<String> fan = Channel.closeableFanout(closeable, plain);
        fan.put("msg");
        fan.close();

        // closeable target: drained and returns null
        assertEquals("msg", closeable.get());
        assertNull(closeable.get());

        // plain target: not closed by fanout, still has the value
        assertEquals("msg", plain.get());
    }

    @Test
    void joinBlocksUntilClose() throws Exception
    {
        CloseableChannel<String> dest = Channel.closeableOf(4);
        CloseableFanoutChannel<String> fan = Channel.closeableFanout(dest);

        CountDownLatch joined = new CountDownLatch(1);
        AtomicBoolean joinReturned = new AtomicBoolean(false);

        Thread waiter = new Thread(() ->
        {
            try
            {
                fan.join();
                joinReturned.set(true);
                joined.countDown();
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
        });
        waiter.start();

        Thread.sleep(200);
        assertFalse(joinReturned.get());

        fan.close();
        assertTrue(joined.await(5, TimeUnit.SECONDS));
        assertTrue(joinReturned.get());
    }

    @Test
    void joinReturnsImmediatelyWhenAlreadyClosed() throws InterruptedException
    {
        CloseableChannel<String> dest = Channel.closeableOf(4);
        CloseableFanoutChannel<String> fan = Channel.closeableFanout(dest);

        fan.close();
        fan.join(); // must not block
    }

    @Test
    void factoryMethod() throws InterruptedException
    {
        CloseableChannel<String> dest1 = Channel.closeableOf(4);
        CloseableChannel<String> dest2 = Channel.closeableOf(4);

        CloseableFanoutChannel<String> fan = Channel.closeableFanout(dest1, dest2);
        fan.put("test");
        fan.close();

        assertEquals("test", dest1.get());
        assertNull(dest1.get());
        assertEquals("test", dest2.get());
        assertNull(dest2.get());
    }

    @Test
    void putTimeoutReturnsFalseAfterClose() throws InterruptedException
    {
        CloseableChannel<String> dest = Channel.closeableOf(4);
        CloseableFanoutChannel<String> fan = Channel.closeableFanout(dest);

        fan.close();
        assertFalse(fan.put("nope", 10, TimeUnit.MILLISECONDS));
    }

    @Test
    void emptyCloseableFanout() throws InterruptedException
    {
        @SuppressWarnings("unchecked")
        CloseableFanoutChannel<String> fan = Channel.closeableFanout();
        fan.put("nothing");
        assertTrue(fan.close());
        assertTrue(fan.isClosed());
    }
}
