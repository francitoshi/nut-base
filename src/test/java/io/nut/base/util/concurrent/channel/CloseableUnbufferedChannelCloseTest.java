/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scenario where close() is invoked while a put() is still blocked (rendezvous:
 * no get() is waiting, so the put() blocks inside the SynchronousQueue).
 * Closing aborts the pending put: the blocked put() returns without throwing
 * per the interruption contract, so close() can complete in bounded time
 * without requiring a consumer.
 */
class CloseableUnbufferedChannelCloseTest
{
    private ExecutorService executor;

    @AfterEach
    void tearDown()
    {
        if (executor != null)
        {
            executor.shutdownNow();
        }
    }

    @Test
    void close_abortsBlockedPut_andReturnsNullAfterwards() throws Exception
    {
        executor = Executors.newFixedThreadPool(2);

        CloseableUnbufferedChannel<String> channel = new CloseableUnbufferedChannel<>();

        // No get() is waiting yet, so this put() blocks inside the
        // SynchronousQueue until some get() rendezvous with it.
        CountDownLatch putStarted = new CountDownLatch(1);
        AtomicReference<Throwable> putError = new AtomicReference<>();
        AtomicBoolean putReturned = new AtomicBoolean(false);
        Future<?> blockedPut = executor.submit(() ->
        {
            putStarted.countDown();
            try
            {
                channel.put("handshake");
                putReturned.set(true);
            }
            catch (Throwable t)
            {
                putError.set(t);
            }
        });

        assertTrue(putStarted.await(2, TimeUnit.SECONDS), "put() thread did not start in time");
        Thread.sleep(200); // give it a moment to actually reach queue.put() and block

        // close() aborts the blocked put() and completes in bounded time even
        // though no consumer will ever rendezvous with the producer.
        assertTrue(channel.close(), "close() must abort the pending put() and succeed");
        assertTrue(channel.isClosed());

        // the pending put() aborts per the interruption contract: it returns
        // without throwing and without delivering its value.
        blockedPut.get(2, TimeUnit.SECONDS);
        assertNull(putError.get(), "blocked put() must abort without throwing");
        assertTrue(putReturned.get(), "blocked put() must return after abort");
        assertTrue(channel.isInterrupted());

        // Channel is closed and empty: get() must return null, not block.
        assertNull(channel.get());

        // puts after close are rejected.
        assertThrows(IllegalStateException.class, () -> channel.put("later"));
    }

    @Test
    void close_isIdempotent_andReturnsTrueOnSubsequentCalls_whenNothingIsBlocked() throws Exception
    {
        CloseableUnbufferedChannel<String> channel = new CloseableUnbufferedChannel<>();

        assertTrue(channel.close(), "first close() with nothing blocked must succeed");
        assertTrue(channel.close(), "close() must be idempotent and keep returning true");
        assertTrue(channel.isClosed());

        assertNull(channel.get());
    }
}