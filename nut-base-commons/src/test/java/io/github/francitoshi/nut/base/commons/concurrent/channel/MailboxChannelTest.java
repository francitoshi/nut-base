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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mailbox flavour of the closeable channels: a producer's own
 * interruption does not abort its put and does not close the channel — the
 * wait for room (or for the rendezvous hand-off) survives the interrupt, so
 * {@code Actor.accept()} can wait permanently. Reader interruptions keep the
 * regular abort-and-close semantics.
 */
class MailboxChannelTest
{
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void bufferedMailboxPutSurvivesProducerInterrupt() throws Exception
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(1, true);
        channel.setInterruptedExceptionListener(Channel::ignoreInterruptedException);
        channel.put(1); // the buffer is now full

        AtomicInteger delivered = new AtomicInteger(0);
        AtomicReference<Exception> failure = new AtomicReference<>();
        CountDownLatch waiting = new CountDownLatch(1);
        Thread producer = new Thread(() ->
        {
            waiting.countDown();
            try
            {
                channel.put(2);
                delivered.incrementAndGet();
            }
            catch (Exception ex)
            {
                failure.set(ex);
            }
        });
        producer.start();
        assertTrue(waiting.await(5, TimeUnit.SECONDS));
        producer.interrupt();

        // the interrupted producer keeps waiting and the channel stays open
        Thread.sleep(200);
        assertEquals(0, delivered.get(), "the put must not complete before there is room");
        assertFalse(channel.isClosed(), "a producer interrupt must not close the channel");
        assertNull(failure.get());

        assertEquals(1, (int) channel.get()); // room appears
        producer.join(5_000);
        assertFalse(producer.isAlive(), "the interrupted put must complete once there is room");
        assertEquals(1, delivered.get());
        assertNull(failure.get());
        assertEquals(2, (int) channel.get());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void rendezvousMailboxPutSurvivesProducerInterrupt() throws Exception
    {
        CloseableUnbufferedChannel<Integer> channel = new CloseableUnbufferedChannel<>(true);
        channel.setInterruptedExceptionListener(Channel::ignoreInterruptedException);

        AtomicInteger delivered = new AtomicInteger(0);
        AtomicReference<Integer> received = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        CountDownLatch waiting = new CountDownLatch(1);
        Thread producer = new Thread(() ->
        {
            waiting.countDown();
            try
            {
                channel.put(7);
                delivered.incrementAndGet();
            }
            catch (Exception ex)
            {
                failure.set(ex);
            }
        });
        producer.start();
        assertTrue(waiting.await(5, TimeUnit.SECONDS));
        producer.interrupt();

        // no taker yet: the interrupted producer keeps waiting, channel open
        Thread.sleep(200);
        assertEquals(0, delivered.get(), "the put must not complete without a taker");
        assertFalse(channel.isClosed(), "a producer interrupt must not close the channel");
        assertNull(failure.get());

        Thread taker = new Thread(() -> received.set(channel.get()));
        taker.start();
        producer.join(5_000);
        assertFalse(producer.isAlive(), "the interrupted put must complete once a taker arrives");
        assertEquals(1, delivered.get());
        assertNull(failure.get());
        assertEquals(7, received.get());
        taker.join(5_000);
        assertFalse(taker.isAlive());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void plainChannelsKeepAbortAndCloseOnProducerInterrupt() throws Exception
    {
        // control: the regular closeable flavour keeps its documented
        // interrupt-abort semantics
        CloseableBufferedChannel<Integer> channel = ChannelTestSupport.quiet(new CloseableBufferedChannel<>(1));
        channel.put(1);
        CountDownLatch waiting = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);
        Thread producer = new Thread(() ->
        {
            waiting.countDown();
            try
            {
                channel.put(2);
            }
            catch (IllegalStateException expected)
            {
                failed.set(true);
            }
        });
        producer.start();
        assertTrue(waiting.await(5, TimeUnit.SECONDS));
        producer.interrupt();
        producer.join(5_000);
        assertFalse(producer.isAlive());
        assertTrue(channel.isClosed(), "the regular flavour closes on a producer interrupt");
        assertTrue(failed.get(), "the regular flavour aborts the interrupted put loudly");
    }
}
