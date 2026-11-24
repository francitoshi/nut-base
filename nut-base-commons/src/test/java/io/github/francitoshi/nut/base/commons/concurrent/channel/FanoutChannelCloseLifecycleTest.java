/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FanoutChannelCloseLifecycleTest
{
    @Test
    void broadcastAndClose() throws InterruptedException
    {
        Channel<String> dest1 = Channel.of(4);
        Channel<String> dest2 = Channel.of(4);

        FanoutChannel<String> fan = Channel.fanout(dest1, dest2);
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
        Channel<String> dest = Channel.of(4);
        FanoutChannel<String> fan = Channel.fanout(dest);

        fan.close();
        assertThrows(IllegalStateException.class, () -> fan.put("nope"));
    }

    @Test
    void closeIsIdempotent()
    {
        @SuppressWarnings("unchecked")
        Channel<String> dest = Channel.of(4);
        FanoutChannel<String> fan = Channel.fanout(dest);

        assertTrue(fan.close());
        assertTrue(fan.close());
        assertTrue(fan.isClosed());
    }

    @Test
    void closeAbortsBlockedPutAndDoesNotDeadlock() throws Exception
    {
        // Unbuffered target: with no consumer waiting, the broadcast blocks in
        // target.put(). close() must abort it in bounded time. Regression test
        // for the old deadlock between an in-flight put() and close().
        Channel<String> dest = ChannelTestSupport.quiet(new UnbufferedChannel<>());
        FanoutChannel<String> fan = Channel.fanout(dest);

        CountDownLatch putStarted = new CountDownLatch(1);
        AtomicBoolean putReturned = new AtomicBoolean(false);
        AtomicReference<Throwable> putError = new AtomicReference<>();
        Thread sender = new Thread(() ->
        {
            putStarted.countDown();
            try
            {
                fan.put("hello");
                putReturned.set(true);
            }
            catch (Throwable t)
            {
                putError.set(t);
            }
        });
        sender.start();

        assertTrue(putStarted.await(2, TimeUnit.SECONDS));
        Thread.sleep(200); // let the sender reach the blocking target.put()

        assertTrue(fan.close(), "close() must abort the blocked put and complete in bounded time");
        assertTrue(fan.isClosed());

        // the blocked put() ends promptly: the target's own close aborted it
        // and the broadcast returned without delivering to it (a writer's
        // interruption is routed to the listener, never thrown).
        sender.join(2000);
        assertFalse(sender.isAlive(), "the blocked put() must end once the targets are closed");
        assertNull(putError.get(), "the writer's own interruption must not throw");
        assertTrue(putReturned.get(), "the broadcast must return once the targets are closed");

        // the target was closed by close(): drained end-of-data.
        assertNull(dest.get());
    }

    @Test
    void closePropagatesToCloseableTargetsOnly() throws InterruptedException
    {
        Channel<String> closeable = Channel.of(4);
        BufferedChannel<String> plain = new BufferedChannel<>(4);

        FanoutChannel<String> fan = Channel.fanout(closeable, plain);
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
        Channel<String> dest = Channel.of(4);
        FanoutChannel<String> fan = Channel.fanout(dest);

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
        Channel<String> dest = Channel.of(4);
        FanoutChannel<String> fan = Channel.fanout(dest);

        fan.close();
        fan.join(); // must not block
    }

    @Test
    void factoryMethod() throws InterruptedException
    {
        Channel<String> dest1 = Channel.of(4);
        Channel<String> dest2 = Channel.of(4);

        FanoutChannel<String> fan = Channel.fanout(dest1, dest2);
        fan.put("test");
        fan.close();

        assertEquals("test", dest1.get());
        assertNull(dest1.get());
        assertEquals("test", dest2.get());
        assertNull(dest2.get());
    }

    @Test
    void putTimeoutThrowsAfterClose() throws InterruptedException
    {
        Channel<String> dest = Channel.of(4);
        FanoutChannel<String> fan = Channel.fanout(dest);

        fan.close();
        assertThrows(IllegalStateException.class, () -> fan.put("nope", 10, TimeUnit.MILLISECONDS));
    }

    @Test
    void emptyCloseableFanout() throws InterruptedException
    {
        @SuppressWarnings("unchecked")
        FanoutChannel<String> fan = Channel.fanout();
        fan.put("nothing");
        assertTrue(fan.close());
        assertTrue(fan.isClosed());
    }
}
