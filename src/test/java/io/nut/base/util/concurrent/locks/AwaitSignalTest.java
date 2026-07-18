/*
 * Copyright (C) 2007-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.locks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AwaitSignal}.
 * <p>
 * These tests exercise a real wait/notify based primitive, so a few of
 * them coordinate threads with short, generous sleeps to make the race
 * between "thread B starts waiting" and "thread A signals" effectively
 * deterministic. The sleep durations are intentionally conservative to
 * keep the suite non-flaky on slow or single-core machines.
 *
 * @author franci
 */
class AwaitSignalTest
{
    /** Generous delay to let a thread actually enter Object.wait() before we act on it. */
    private static final long SETTLE_MS = 200;
    /** Upper bound used to detect "did not return in time" without hanging forever. */
    private static final long JOIN_TIMEOUT_MS = 5_000;

    @Test
    @Timeout(5)
    void signalWithNoWaitingThreadDoesNothing()
    {
        AwaitSignal awaitSignal = new AwaitSignal();

        assertDoesNotThrow(awaitSignal::signal);
        assertDoesNotThrow(awaitSignal::signalAll);
    }

    @Test
    @Timeout(5)
    void awaitBlocksUntilSignalIsCalled() throws Exception
    {
        AwaitSignal awaitSignal = new AwaitSignal();
        CountDownLatch aboutToWait = new CountDownLatch(1);
        AtomicBoolean returned = new AtomicBoolean(false);

        Thread waiter = new Thread(() ->
        {
            aboutToWait.countDown();
            try
            {
                awaitSignal.await();
                returned.set(true);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }, "waiter");
        waiter.start();

        aboutToWait.await();
        Thread.sleep(SETTLE_MS);
        assertFalse(returned.get(), "the waiting thread should still be blocked before signal() is called");

        awaitSignal.signal();
        waiter.join(JOIN_TIMEOUT_MS);

        assertFalse(waiter.isAlive(), "the waiting thread should have finished after signal()");
        assertTrue(returned.get(), "await() should have returned after signal()");
    }

    @Test
    @Timeout(5)
    void timedAwaitReturnsOnItsOwnWhenNoSignalArrives()
    {
        AwaitSignal awaitSignal = new AwaitSignal();
        long timeoutMs = 200;

        long start = System.nanoTime();
        assertDoesNotThrow(() -> awaitSignal.await(timeoutMs));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMs >= timeoutMs - 20, "await(timeout) should not return noticeably earlier than the requested timeout, took " + elapsedMs + " ms");
    }

    @Test
    @Timeout(5)
    void timedAwaitReturnsEarlyWhenSignalled() throws Exception
    {
        AwaitSignal awaitSignal = new AwaitSignal();
        CountDownLatch aboutToWait = new CountDownLatch(1);
        long longTimeoutMs = 4_000;
        AtomicReference<Long> elapsedMs = new AtomicReference<>();

        Thread waiter = new Thread(() ->
        {
            aboutToWait.countDown();
            long start = System.nanoTime();
            try
            {
                awaitSignal.await(longTimeoutMs);
                elapsedMs.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }, "waiter");
        waiter.start();

        aboutToWait.await();
        Thread.sleep(SETTLE_MS);
        awaitSignal.signal();
        waiter.join(JOIN_TIMEOUT_MS);

        assertFalse(waiter.isAlive());
        assertTrue(elapsedMs.get() < longTimeoutMs, "signal() should wake the waiter well before the " + longTimeoutMs + " ms timeout, took " + elapsedMs.get() + " ms");
    }

    @Test
    @Timeout(5)
    void signalWakesUpExactlyOneOfSeveralWaitingThreads() throws Exception
    {
        AwaitSignal awaitSignal = new AwaitSignal();
        int waiterCount = 3;
        CountDownLatch allAboutToWait = new CountDownLatch(waiterCount);
        AtomicInteger completed = new AtomicInteger(0);
        Thread[] waiters = new Thread[waiterCount];

        for (int i = 0; i < waiterCount; i++)
        {
            waiters[i] = new Thread(() ->
            {
                allAboutToWait.countDown();
                try
                {
                    awaitSignal.await();
                    completed.incrementAndGet();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }, "waiter");
            waiters[i].start();
        }

        allAboutToWait.await();
        Thread.sleep(SETTLE_MS);

        awaitSignal.signal();
        Thread.sleep(SETTLE_MS);

        assertEquals(1, completed.get(), "signal() should wake up exactly one waiting thread");

        // release the remaining waiters so the test does not leak threads
        awaitSignal.signalAll();
        for (Thread waiter : waiters)
        {
            waiter.join(JOIN_TIMEOUT_MS);
            assertFalse(waiter.isAlive());
        }
        assertEquals(waiterCount, completed.get());
    }

    @Test
    @Timeout(5)
    void signalAllWakesUpEveryWaitingThread() throws Exception
    {
        AwaitSignal awaitSignal = new AwaitSignal();
        int waiterCount = 5;
        CountDownLatch allAboutToWait = new CountDownLatch(waiterCount);
        AtomicInteger completed = new AtomicInteger(0);
        Thread[] waiters = new Thread[waiterCount];

        for (int i = 0; i < waiterCount; i++)
        {
            waiters[i] = new Thread(() ->
            {
                allAboutToWait.countDown();
                try
                {
                    awaitSignal.await();
                    completed.incrementAndGet();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }, "waiter");
            waiters[i].start();
        }

        allAboutToWait.await();
        Thread.sleep(SETTLE_MS);

        awaitSignal.signalAll();

        for (Thread waiter : waiters)
        {
            waiter.join(JOIN_TIMEOUT_MS);
            assertFalse(waiter.isAlive());
        }
        assertEquals(waiterCount, completed.get(), "signalAll() should wake up every waiting thread");
    }

    @Test
    @Timeout(5)
    void interruptingAWaitingThreadThrowsInterruptedException() throws Exception
    {
        AwaitSignal awaitSignal = new AwaitSignal();
        CountDownLatch aboutToWait = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);

        Thread waiter = new Thread(() ->
        {
            aboutToWait.countDown();
            try
            {
                awaitSignal.await();
            }
            catch (InterruptedException e)
            {
                interrupted.set(true);
            }
        }, "waiter");
        waiter.start();

        aboutToWait.await();
        Thread.sleep(SETTLE_MS);

        waiter.interrupt();
        waiter.join(JOIN_TIMEOUT_MS);

        assertFalse(waiter.isAlive());
        assertTrue(interrupted.get(), "await() should throw InterruptedException when the waiting thread is interrupted");
    }

    @Test
    @Timeout(5)
    void interruptedWaiterDoesNotLeaveAStaleWaitingCount() throws Exception
    {
        // Regression test: an earlier version of this class leaked the
        // internal waiting-thread counter when await() was interrupted,
        // because the counter was only decremented after lock.wait()
        // returned normally. If that counter leaks, signal() called
        // afterwards (with nobody actually waiting) would try to notify
        // a thread that no longer exists; while harmless in itself, it
        // is a symptom that a *real*, later waiter could then be skipped
        // if the leaked count masked the true state. This test checks
        // the observable behavior: after an interrupted await(), a
        // fresh waiter must still be reachable by signal().
        AwaitSignal awaitSignal = new AwaitSignal();

        CountDownLatch firstAboutToWait = new CountDownLatch(1);
        Thread firstWaiter = new Thread(() ->
        {
            firstAboutToWait.countDown();
            try
            {
                awaitSignal.await();
            }
            catch (InterruptedException ignored)
            {
                // expected
            }
        }, "first-waiter");
        firstWaiter.start();
        firstAboutToWait.await();
        Thread.sleep(SETTLE_MS);
        firstWaiter.interrupt();
        firstWaiter.join(JOIN_TIMEOUT_MS);
        assertFalse(firstWaiter.isAlive());

        CountDownLatch secondAboutToWait = new CountDownLatch(1);
        AtomicBoolean secondReturned = new AtomicBoolean(false);
        Thread secondWaiter = new Thread(() ->
        {
            secondAboutToWait.countDown();
            try
            {
                awaitSignal.await();
                secondReturned.set(true);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }, "second-waiter");
        secondWaiter.start();
        secondAboutToWait.await();
        Thread.sleep(SETTLE_MS);

        awaitSignal.signal();
        secondWaiter.join(JOIN_TIMEOUT_MS);

        assertFalse(secondWaiter.isAlive());
        assertTrue(secondReturned.get(), "a new waiter must still be woken up by signal() after a previous waiter was interrupted");
    }

    @Test
    @Timeout(5)
    void signalDoesNotStoreAPermitForALaterWaiter() throws Exception
    {
        // Documents the raw wait/notify semantics of this class: a
        // signal() with nobody currently waiting is simply dropped, it
        // is not remembered for whoever calls await() next (unlike a
        // Semaphore). A later await(timeout) must therefore still block
        // for close to the full timeout.
        AwaitSignal awaitSignal = new AwaitSignal();
        long timeoutMs = 200;

        awaitSignal.signal();
        awaitSignal.signalAll();

        long start = System.nanoTime();
        awaitSignal.await(timeoutMs);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue(elapsedMs >= timeoutMs - 20, "a stale signal() sent before await() must not be honored, took only " + elapsedMs + " ms");
    }
}
