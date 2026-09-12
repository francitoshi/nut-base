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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scenario where close() is invoked while a put() is still blocked (buffer
 * full, nobody draining). Closing aborts the pending put: the blocked put()
 * fails with {@link IllegalStateException} instead of blocking forever, so
 * close() can complete in bounded time. Buffered elements are still delivered.
 */
class CloseableBufferedChannelCloseTest
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
    void close_abortsBlockedPut_andStillDrainsBufferedElements() throws Exception
    {
        executor = Executors.newFixedThreadPool(2);

        // Capacity 1: after filling it, any further put() blocks until
        // someone drains an item.
        CloseableBufferedChannel<String> channel = new CloseableBufferedChannel<>(1);

        // Fill the only slot in the buffer.
        channel.put("first");

        // This put() will block inside queue.put() because the queue is
        // already full and nothing is consuming yet.
        CountDownLatch putStarted = new CountDownLatch(1);
        AtomicReference<Throwable> putError = new AtomicReference<>();
        AtomicBoolean putReturned = new AtomicBoolean(false);
        Future<?> blockedPut = executor.submit(() ->
        {
            putStarted.countDown();
            try
            {
                channel.put("second");
                putReturned.set(true);
            }
            catch (Throwable t)
            {
                putError.set(t);
            }
        });

        // Make sure the blocking put() has actually entered queue.put()
        // before we attempt to close.
        assertTrue(putStarted.await(2, TimeUnit.SECONDS), "put() thread did not start in time");
        Thread.sleep(200); // give it a moment to actually reach queue.put() and block

        // close() aborts the blocked put() and must complete in bounded time.
        assertTrue(channel.close(), "close() must abort the pending put() and succeed");
        assertTrue(channel.isClosed());

        // the pending put() aborts per the interruption contract: it returns
        // without throwing and without delivering its value.
        blockedPut.get(2, TimeUnit.SECONDS);
        assertNull(putError.get(), "blocked put() must abort without throwing");
        assertTrue(putReturned.get(), "blocked put() must return after abort");
        assertTrue(channel.isInterrupted());

        // "first" was buffered before close, so it must still be delivered.
        assertEquals("first", channel.get());

        // After the real values are drained, get() must return null, not block.
        assertNull(channel.get());

        // puts after close are rejected.
        assertThrows(IllegalStateException.class, () -> channel.put("later"));
    }

    @Test
    void close_isIdempotent_andReturnsTrueOnSubsequentCalls_whenNothingIsBlocked() throws Exception
    {
        CloseableBufferedChannel<String> channel = new CloseableBufferedChannel<>(2);
        channel.put("only");

        assertTrue(channel.close(), "first close() with nothing blocked must succeed");
        assertTrue(channel.close(), "close() must be idempotent and keep returning true");
        assertTrue(channel.isClosed());

        assertEquals("only", channel.get());
        assertNull(channel.get());
    }
}