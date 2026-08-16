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
 * Same scenario as CloseableBufferedChannelCloseTest, adapted to the
 * unbuffered (rendezvous) case. Here a put() blocks naturally whenever no
 * get() is currently waiting to take the value, since SynchronousQueue has
 * zero capacity: every put() must hand off directly to a taker.
 *
 * Before the fix, close(timeout, unit) set "closed = true" even on timeout,
 * and a later close() call would short-circuit to "return true" without
 * ever running the poisoning loop, leaving the blocked put() (and any
 * pending get()s) unresolved while callers believed the channel was
 * cleanly closed.
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
    void closeWithShortTimeout_returnsFalse_whenPutIsStillBlocked_andSucceedsOnceUnblocked() throws Exception
    {
        executor = Executors.newFixedThreadPool(3);

        CloseableUnbufferedChannel<String> channel = new CloseableUnbufferedChannel<>();

        // No get() is waiting yet, so this put() blocks inside the readLock
        // section, holding it until some get() rendezvous with it.
        CountDownLatch putStarted = new CountDownLatch(1);
        AtomicBoolean putReturned = new AtomicBoolean(false);
        Future<?> blockedPut = executor.submit(() ->
        {
            try
            {
                putStarted.countDown();
                channel.put("handshake"); // blocks until a get() takes it
                putReturned.set(true);
            }
            catch (InterruptedException ignored)
            {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(putStarted.await(2, TimeUnit.SECONDS), "put() thread did not start in time");
        Thread.sleep(200); // give it a moment to actually reach queue.put() and block

        // First close attempt: the writeLock barrier cannot be acquired
        // because the blocked put() is holding the readLock. Must be honest
        // about the failure, not silently succeed.
        boolean firstAttempt = channel.close(300, TimeUnit.MILLISECONDS);
        assertFalse(firstAttempt, "close() must return false while a put() is still blocked");
        assertTrue(channel.isClosed(), "isClosed() is true as soon as close() is invoked");

        // Retrying close() again before unblocking anything must still be
        // honest and return false (the old code used to short-circuit to
        // "return true" here instead).
        boolean secondAttemptStillBlocked = channel.close(200, TimeUnit.MILLISECONDS);
        assertFalse(secondAttemptStillBlocked, "retry must not lie about success while still blocked");

        // Since the channel is already marked closed, get() takes the
        // non-blocking drainAfterClose() path, which does a queue.poll():
        // for a SynchronousQueue this immediately rendezvous with the
        // producer that is currently parked waiting for a taker, releasing
        // it and its readLock.
        String handshakeValue = channel.get();
        assertEquals("handshake", handshakeValue);

        blockedPut.get(2, TimeUnit.SECONDS);
        assertTrue(putReturned.get(), "the blocked put() should have completed once a get() took it");

        // Now that nothing holds the readLock anymore, close() must be able
        // to acquire the barrier and finish normally.
        boolean finalAttempt = channel.close();
        assertTrue(finalAttempt, "close() must succeed once the blocked put() has released the lock");

        // Channel is closed and empty: get() must return null, not block.
        assertNull(channel.get());
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
