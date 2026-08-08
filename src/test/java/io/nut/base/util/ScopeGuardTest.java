/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScopeGuardTest
{
    // ---------------------------------------------------------------
    // Basic behavior
    // ---------------------------------------------------------------

    @Test
    void runsActionOnClose()
    {
        AtomicInteger counter = new AtomicInteger(0);

        try (ScopeGuard guard = ScopeGuard.create())
        {
            guard.defer(counter::incrementAndGet);
        }

        assertEquals(1, counter.get());
    }

    @Test
    void doesNotRunActionBeforeClose()
    {
        AtomicInteger counter = new AtomicInteger(0);

        ScopeGuard guard = ScopeGuard.create();
        guard.defer(counter::incrementAndGet);

        assertEquals(0, counter.get());

        guard.close();
        assertEquals(1, counter.get());
    }

    @Test
    void runsActionsInReverseOrderLikeDefer()
    {
        List<Integer> executionOrder = new ArrayList<>();

        try (ScopeGuard guard = ScopeGuard.create())
        {
            guard.defer(() -> executionOrder.add(1));
            guard.defer(() -> executionOrder.add(2));
            guard.defer(() -> executionOrder.add(3));
        }

        assertEquals(As.list(3, 2, 1), executionOrder);
    }

    @Test
    void onExitReturnsSameInstanceForChaining()
    {
        ScopeGuard guard = ScopeGuard.create();
        ScopeGuard returned = guard.defer(() -> {});
        assertSame(guard, returned);
    }

    // ---------------------------------------------------------------
    // dismiss()
    // ---------------------------------------------------------------

    @Test
    void dismissPreventsAllPendingActionsFromRunning()
    {
        AtomicInteger counter = new AtomicInteger(0);

        try (ScopeGuard guard = ScopeGuard.create())
        {
            guard.defer(counter::incrementAndGet);
            guard.defer(counter::incrementAndGet);
            guard.dismiss();
        }

        assertEquals(0, counter.get());
    }

    @Test
    void onExitAfterDismissIsDiscarded()
    {
        AtomicInteger counter = new AtomicInteger(0);

        ScopeGuard guard = ScopeGuard.create();
        guard.dismiss();
        guard.defer(counter::incrementAndGet);
        guard.close();

        assertEquals(0, counter.get());
    }

    // ---------------------------------------------------------------
    // close() idempotency / post-close behavior
    // ---------------------------------------------------------------

    @Test
    void closeIsIdempotent()
    {
        AtomicInteger counter = new AtomicInteger(0);

        ScopeGuard guard = ScopeGuard.create();
        guard.defer(counter::incrementAndGet);

        guard.close();
        guard.close();
        guard.close();

        assertEquals(1, counter.get());
    }

    @Test
    void onExitAfterCloseIsDiscardedAndNeverRuns()
    {
        AtomicInteger counter = new AtomicInteger(0);

        ScopeGuard guard = ScopeGuard.create();
        guard.close();
        guard.defer(counter::incrementAndGet);
        guard.close();

        assertEquals(0, counter.get());
    }

    // ---------------------------------------------------------------
    // Exception handling
    // ---------------------------------------------------------------

    @Test
    void exceptionInActionIsRethrownWrappedInRuntimeException()
    {
        ScopeGuard guard = ScopeGuard.create();
        guard.defer(() -> { throw new IllegalStateException("boom"); });

        RuntimeException thrown = assertThrows(RuntimeException.class, guard::close);
        assertEquals(IllegalStateException.class, thrown.getCause().getClass());
        assertEquals("boom", thrown.getCause().getMessage());
    }

    @Test
    void allActionsRunEvenIfEarlierOnesThrow()
    {
        AtomicInteger counter = new AtomicInteger(0);

        ScopeGuard guard = ScopeGuard.create();
        guard.defer(counter::incrementAndGet);
        guard.defer(() -> { throw new RuntimeException("mid-stack failure"); });
        guard.defer(counter::incrementAndGet);

        assertThrows(RuntimeException.class, guard::close);

        // Both non-throwing actions ran despite the failure registered between them.
        assertEquals(2, counter.get());
    }

    @Test
    void secondAndLaterFailuresAreAttachedAsSuppressed()
    {
        ScopeGuard guard = ScopeGuard.create();
        guard.defer(() -> { throw new RuntimeException("failure A"); });
        guard.defer(() -> { throw new RuntimeException("failure B"); });

        RuntimeException thrown = assertThrows(RuntimeException.class, guard::close);

        // The last-registered action runs first (LIFO), so "failure B" surfaces
        // as the main cause and "failure A" ends up suppressed.
        assertEquals("failure B", thrown.getCause().getMessage());
        assertEquals(1, thrown.getSuppressed().length);
        assertEquals("failure A", thrown.getSuppressed()[0].getMessage());
    }

    @Test
    void suppressingGuardSwallowsAllExceptions()
    {
        AtomicInteger counter = new AtomicInteger(0);

        ScopeGuard guard = ScopeGuard.createSuppressing();
        guard.defer(counter::incrementAndGet);
        guard.defer(() -> { throw new RuntimeException("ignored"); });
        guard.defer(counter::incrementAndGet);

        guard.close(); // must not throw

        assertEquals(2, counter.get());
    }

    // ---------------------------------------------------------------
    // threadSafe()
    // ---------------------------------------------------------------

    @Test
    void threadSafeReturnsWorkingWrapper()
    {
        AtomicInteger counter = new AtomicInteger(0);

        try (ScopeGuard guard = ScopeGuard.create().threadSafe())
        {
            guard.defer(counter::incrementAndGet);
            guard.defer(counter::incrementAndGet);
        }

        assertEquals(2, counter.get());
    }

    @Test
    void threadSafeCalledTwiceDoesNotDoubleWrap()
    {
        ScopeGuard guard = ScopeGuard.create().threadSafe();
        ScopeGuard wrappedAgain = guard.threadSafe();

        assertSame(guard, wrappedAgain);
    }

    @Test
    void threadSafeWrapperHandlesConcurrentOnExitCalls() throws InterruptedException
    {
        int threadCount = 16;
        int actionsPerThread = 100;

        ScopeGuard guard = ScopeGuard.create().threadSafe();
        AtomicInteger counter = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++)
        {
            pool.submit(() ->
            {
                try
                {
                    startLatch.await();
                    for (int i = 0; i < actionsPerThread; i++)
                    {
                        guard.defer(counter::incrementAndGet);
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                finally
                {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(0, counter.get()); // nothing runs until close()

        guard.close();

        assertEquals(threadCount * actionsPerThread, counter.get());
    }

    @Test
    void threadSafeWrapperOnlyRunsActionsOnce() throws InterruptedException
    {
        ScopeGuard guard = ScopeGuard.create().threadSafe();
        AtomicInteger counter = new AtomicInteger(0);
        guard.defer(counter::incrementAndGet);

        int threadCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++)
        {
            pool.submit(() ->
            {
                try
                {
                    startLatch.await();
                    guard.close(); // all threads race to close the same guard
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                finally
                {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(1, counter.get());
    }

    @Test
    void dismissOnThreadSafeWrapperPreventsExecution()
    {
        AtomicInteger counter = new AtomicInteger(0);

        ScopeGuard guard = ScopeGuard.create().threadSafe();
        guard.defer(counter::incrementAndGet);
        guard.dismiss();
        guard.close();

        assertFalse(counter.get() == 1);
        assertEquals(0, counter.get());
    }

    private static class TestResource implements AutoCloseable
    {
        int closeCount = 0;

        @Override
        public void close()
        {
            closeCount++;
        }
    }

    @Test
    void useRegistersAndClosesResource()
    {
        TestResource resource = new TestResource();
        try (ScopeGuard guard = ScopeGuard.create())
        {
            TestResource returned = guard.use(resource);
            assertSame(resource, returned);
            assertEquals(0, resource.closeCount);
        }
        assertEquals(1, resource.closeCount);
    }

    @Test
    void releasePreventsResourceFromClosing()
    {
        TestResource resource = new TestResource();
        try (ScopeGuard guard = ScopeGuard.create())
        {
            guard.use(resource);
            guard.release(resource);
        }
        assertEquals(0, resource.closeCount);
    }

    @Test
    void dismissResourcePreventsResourceFromClosing()
    {
        TestResource resource = new TestResource();
        try (ScopeGuard guard = ScopeGuard.create())
        {
            guard.use(resource);
            guard.dismiss(resource);
        }
        assertEquals(0, resource.closeCount);
    }

    @Test
    void threadSafeUseReleaseAndDismiss()
    {
        TestResource resource1 = new TestResource();
        TestResource resource2 = new TestResource();
        try (ScopeGuard guard = ScopeGuard.create().threadSafe())
        {
            guard.use(resource1);
            guard.use(resource2);
            guard.release(resource1);
            guard.dismiss(resource2);
        }
        assertEquals(0, resource1.closeCount);
        assertEquals(0, resource2.closeCount);
    }
}

