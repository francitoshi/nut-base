/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Same scenario as CloseableBufferedChannelCloseTest and
 * CloseableUnbufferedChannelCloseTest, adapted to the unlimited channel.
 *
 * Unlike the buffered/unbuffered cases, put() on an unlimited channel never
 * blocks for lack of room (LinkedBlockingQueue has no capacity limit), so
 * there is no natural way to catch a put() "in flight" inside the readLock
 * section for long enough to race a close() against it.
 *
 * To still exercise the close(timeout, unit) barrier under contention, this
 * test grabs the channel's internal readLock directly via reflection from a
 * background thread, simulating a put() that is momentarily suspended
 * inside its critical section (e.g. due to a scheduling delay or GC pause)
 * right when close() is called. This is exactly the situation the barrier
 * in close() is meant to detect.
 *
 * Before the fix, close(timeout, unit) set "closed = true" even on timeout,
 * and a later close() call would short-circuit to "return true" without
 * ever running the poisoning loop, leaving pending get()s unresolved while
 * callers believed the channel was cleanly closed.
 */
class CloseableUnlimitedChannelCloseTest
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

    @SuppressWarnings("unchecked")
    private static ReentrantReadWriteLock extractRwLock(CloseableUnlimitedChannel<?> channel) throws Exception
    {
        Field field = CloseableUnlimitedChannel.class.getDeclaredField("rwLock");
        field.setAccessible(true);
        return (ReentrantReadWriteLock) field.get(channel);
    }

    @Test
    void closeWithShortTimeout_returnsFalse_whenReadLockIsHeld_andSucceedsOnceReleased() throws Exception
    {
        executor = Executors.newFixedThreadPool(3);

        CloseableUnlimitedChannel<String> channel = new CloseableUnlimitedChannel<>();
        ReentrantReadWriteLock rwLock = extractRwLock(channel);

        // Simulate a put() that is suspended mid-flight, still holding the
        // readLock, right before close() runs its barrier.
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseSignal = new CountDownLatch(1);
        Future<?> holder = executor.submit(() ->
        {
            rwLock.readLock().lock();
            try
            {
                lockAcquired.countDown();
                releaseSignal.await();
            }
            catch (InterruptedException ignored)
            {
                Thread.currentThread().interrupt();
            }
            finally
            {
                rwLock.readLock().unlock();
            }
        });

        assertTrue(lockAcquired.await(2, TimeUnit.SECONDS), "reader lock was not acquired in time");

        // A real value that was already fully enqueued before the "stuck"
        // put() simulation started, so we can confirm it survives close().
        channel.put("already committed");

        // First close attempt: the writeLock barrier cannot be acquired
        // because the readLock is being held. Must be honest about the
        // failure, not silently succeed.
        boolean firstAttempt = channel.close(300, TimeUnit.MILLISECONDS);
        assertFalse(firstAttempt, "close() must return false while the readLock is still held");
        assertTrue(channel.isClosed(), "isClosed() is true as soon as close() is invoked");

        // Retrying close() again before releasing the lock must still be
        // honest and return false (the old code used to short-circuit to
        // "return true" here instead).
        boolean secondAttemptStillBlocked = channel.close(200, TimeUnit.MILLISECONDS);
        assertFalse(secondAttemptStillBlocked, "retry must not lie about success while still blocked");

        // Release the simulated in-flight put().
        releaseSignal.countDown();
        holder.get(2, TimeUnit.SECONDS);

        // Now that nothing holds the readLock anymore, close() must be able
        // to acquire the barrier and finish normally.
        boolean finalAttempt = channel.close();
        assertTrue(finalAttempt, "close() must succeed once the readLock has been released");

        // The value committed before close() must still be delivered.
        assertEqualsFirstThenNull(channel, "already committed");
    }

    private static void assertEqualsFirstThenNull(CloseableUnlimitedChannel<String> channel, String expected)
            throws InterruptedException
    {
        String value = channel.get();
        assertTrue(expected.equals(value), "expected '" + expected + "' but got '" + value + "'");
        assertNull(channel.get(), "channel should be drained and closed, get() must return null");
    }

    @Test
    void close_isIdempotent_andReturnsTrueOnSubsequentCalls_whenNothingIsBlocked() throws Exception
    {
        CloseableUnlimitedChannel<String> channel = new CloseableUnlimitedChannel<>();
        channel.put("only");

        assertTrue(channel.close(), "first close() with nothing blocked must succeed");
        assertTrue(channel.close(), "close() must be idempotent and keep returning true");
        assertTrue(channel.isClosed());

        assertEqualsFirstThenNull(channel, "only");
    }
}
