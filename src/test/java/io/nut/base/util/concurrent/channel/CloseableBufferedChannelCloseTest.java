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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces the scenario where close(timeout, unit) times out while a put()
 * is still blocked (buffer full, nobody draining). Before the fix, the
 * "closed" flag was still set to true on timeout, and a *later* close() call
 * would short-circuit on "if (closed) return true" without ever running the
 * poisoning loop, silently lying about success and leaving pending get()
 * calls blocked forever.
 *
 * After the fix, close(timeout, unit) must keep returning false honestly for
 * as long as the barrier cannot be acquired, and must only return true once
 * the poisoning loop has actually run and unblocked all pending get()s.
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
    void closeWithShortTimeout_returnsFalse_whenPutIsStillBlocked_andSucceedsOnceUnblocked() throws Exception
    {
        executor = Executors.newFixedThreadPool(3);

        // Capacity 1: after filling it, any further put() blocks until
        // someone drains an item.
        CloseableBufferedChannel<String> channel = new CloseableBufferedChannel<>(1);

        // Fill the only slot in the buffer.
        channel.put("first");

        // This put() will block inside the readLock section because the
        // queue is already full and nothing is consuming yet.
        CountDownLatch putStarted = new CountDownLatch(1);
        AtomicBoolean putReturned = new AtomicBoolean(false);
        Future<?> blockedPut = executor.submit(() ->
        {
            try
            {
                putStarted.countDown();
                channel.put("second"); // blocks until "first" is taken
                putReturned.set(true);
            }
            catch (InterruptedException ignored)
            {
                Thread.currentThread().interrupt();
            }
        });

        // Make sure the blocking put() has actually entered the queue.put()
        // call (holding the readLock) before we attempt to close.
        assertTrue(putStarted.await(2, TimeUnit.SECONDS), "put() thread did not start in time");
        Thread.sleep(200); // give it a moment to actually reach queue.put() and block

        // First close attempt: the writeLock barrier cannot be acquired
        // because the blocked put() is holding the readLock. This must
        // honestly report failure, not silently succeed.
        boolean firstAttempt = channel.close(300, TimeUnit.MILLISECONDS);
        assertFalse(firstAttempt, "close() must return false while a put() is still blocked");

        // isClosed() is true from the moment close() is called: no new
        // put()s are accepted from here on, even though the poisoning
        // barrier has not been confirmed yet.
        assertTrue(channel.isClosed());

        // Retrying close() again before unblocking anything must still be
        // honest and return false (this is exactly what the old code got
        // wrong: it used to short-circuit to "return true" here).
        boolean secondAttemptStillBlocked = channel.close(200, TimeUnit.MILLISECONDS);
        assertFalse(secondAttemptStillBlocked, "retry must not lie about success while still blocked");

        // Now drain the buffered item, which frees a slot and lets the
        // pending put() finally complete and release the readLock.
        String firstValue = channel.get();
        assertEquals("first", firstValue);

        // Give the previously blocked put() a chance to finish.
        blockedPut.get(2, TimeUnit.SECONDS);
        assertTrue(putReturned.get(), "the blocked put() should have completed once space was freed");

        // Now that nothing holds the readLock anymore, close() must be able
        // to acquire the barrier and actually run the poisoning loop.
        boolean finalAttempt = channel.close();
        assertTrue(finalAttempt, "close() must succeed once the blocked put() has released the lock");

        // "second" was buffered by the previously blocked put() before the
        // channel got closed, so it must still be delivered to a reader.
        String secondValue = channel.get();
        assertEquals("second", secondValue);

        // After the real values are drained, get() must return null
        // (poison / closed-and-empty), not block forever.
        assertNull(channel.get());
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
