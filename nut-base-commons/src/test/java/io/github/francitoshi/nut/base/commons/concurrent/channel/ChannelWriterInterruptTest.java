/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.francitoshi.nut.base.commons.lang.Unchecked.sneakyThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A writer's own interruption aborts its put without delivering the value
 * and without closing the channel: the boolean variants return false and the
 * blocking variant returns, and the interruption is marked and routed to the
 * interruption listener. A reader's own interruption aborts its read (null)
 * the same way — the channel stays open and fully usable on both sides (see
 * {@code doc/channels.txt}).
 */
class ChannelWriterInterruptTest
{
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void bufferedPutAbortsOnWriterInterrupt() throws Exception
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(1);
        channel.setInterruptedExceptionListener(Channel::ignoreInterruptedException);
        channel.put(1); // the buffer is now full

        CountDownLatch waiting = new CountDownLatch(1);
        Thread writer = new Thread(() ->
        {
            waiting.countDown();
            try
            {
                channel.put(2); // the void put returns without delivering
            }
            catch (Exception ex)
            {
                throw sneakyThrow(ex);
            }
        });
        writer.start();
        assertTrue(waiting.await(5, TimeUnit.SECONDS));
        writer.interrupt();

        // the interrupted writer returns promptly: no delivery (the buffer
        // still holds only the first value), no exception, channel open
        writer.join(5_000);
        assertFalse(writer.isAlive(), "the interrupted put must return promptly");
        assertFalse(channel.isClosed(), "a writer interrupt must not close the channel");
        assertTrue(channel.isInterrupted(), "the interruption must be marked");

        // the channel stays fully usable and the interrupted value was not
        // enqueued: the only readable value is the first one
        assertEquals(1, (int) channel.get());
        channel.put(2);
        assertEquals(2, (int) channel.get());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void timedBufferedPutAbortsOnWriterInterrupt() throws Exception
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(1);
        channel.setInterruptedExceptionListener(Channel::ignoreInterruptedException);
        channel.put(1); // full

        AtomicBoolean delivered = new AtomicBoolean(false);
        CountDownLatch waiting = new CountDownLatch(1);
        Thread writer = new Thread(() ->
        {
            waiting.countDown();
            try
            {
                // a generous deadline: the interrupt must end the wait well
                // before it expires, returning false without delivering
                delivered.set(channel.put(2, 2, TimeUnit.SECONDS));
            }
            catch (Exception ignored)
            {
            }
        });
        writer.start();
        assertTrue(waiting.await(5, TimeUnit.SECONDS));
        writer.interrupt();
        writer.join(5_000);
        assertFalse(writer.isAlive(), "the interrupted timed put must return promptly");
        assertFalse(delivered.get(), "the interrupted timed put must return false");
        assertFalse(channel.isClosed(), "a writer interrupt must not close the channel");
        assertTrue(channel.isInterrupted());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void rendezvousPutAbortsOnWriterInterrupt() throws Exception
    {
        UnbufferedChannel<Integer> channel = new UnbufferedChannel<>();
        channel.setInterruptedExceptionListener(Channel::ignoreInterruptedException);

        CountDownLatch waiting = new CountDownLatch(1);
        Thread writer = new Thread(() ->
        {
            waiting.countDown();
            try
            {
                channel.put(7); // the void put returns without delivering
            }
            catch (Exception ex)
            {
                throw sneakyThrow(ex);
            }
        });
        writer.start();
        assertTrue(waiting.await(5, TimeUnit.SECONDS));
        writer.interrupt();

        // the interrupted writer returns promptly: no delivery, no exception,
        // channel open
        writer.join(5_000);
        assertFalse(writer.isAlive(), "the interrupted put must return promptly");
        assertFalse(channel.isClosed(), "a writer interrupt must not close the channel");
        assertTrue(channel.isInterrupted(), "the interruption must be marked");

        // the channel stays fully usable: a taker arrives and the value goes in
        AtomicReference<Integer> received = new AtomicReference<>();
        Thread taker = new Thread(() -> received.set(channel.get()));
        taker.start();
        channel.put(7);
        taker.join(5_000);
        assertEquals(7, received.get());
        taker.join(5_000);
        assertFalse(taker.isAlive());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void readerInterruptionAbortsTheReadWithoutClosing() throws Exception
    {
        // the reader side aborts too: an interrupted read yields null and the
        // channel stays open and fully usable (no abort-and-close)
        BufferedChannel<Integer> channel = ChannelTestSupport.quiet(new BufferedChannel<>(4));
        CountDownLatch waiting = new CountDownLatch(1);
        AtomicReference<Integer> received = new AtomicReference<>();
        Thread reader = new Thread(() ->
        {
            waiting.countDown();
            try
            {
                received.set(channel.get()); // parks: the channel is empty
            }
            catch (Throwable t)
            {
                throw sneakyThrow(t);
            }
        });
        reader.start();
        assertTrue(waiting.await(5, TimeUnit.SECONDS));
        Thread.sleep(100); // let the reader park on the empty channel
        reader.interrupt();
        reader.join(5_000);
        assertFalse(reader.isAlive(), "the interrupted read must return promptly");
        assertNull(received.get(), "the interrupted read yields null");
        assertFalse(channel.isClosed(), "a reader interrupt must not close the channel");
        assertTrue(channel.isInterrupted(), "the interruption must be marked");

        // the channel stays fully usable
        channel.put(1);
        assertEquals(1, (int) channel.get());
    }
}
