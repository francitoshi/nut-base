/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression guard for the batch actor's in-flight counter: when the
 * downstream {@code forward} consumer throws, the batch's counter entries must
 * still be released. Otherwise {@code counters.second} never returns to zero,
 * {@link BatchActor#isIdle()} never becomes {@code true} again and
 * {@link BatchActor#shutdown(boolean) shutdown(true)} never terminates.
 */
class BatchActorForwardExceptionTest
{
    /**
     * Polls {@code stage.isIdle()} until it reaches {@code expected} or dies
     * trying, so slow test-machine scheduling never turns into a false alarm.
     */
    private static void awaitIdle(boolean expected, String what, Linkable stage) throws InterruptedException
    {
        long end = System.nanoTime() + 5_000_000_000L;
        while (stage.isIdle() != expected)
        {
            if (System.nanoTime() > end)
            {
                fail("actor never became " + (expected ? "idle" : "busy") + ": " + what);
            }
            Thread.sleep(5);
        }
    }

    @Test
    void counterReleasesForeverWhenSinkThrows()
    throws Exception
    {
        BatchActor<String> batch = new BatchActor<>(1, 0);
        try
        {
            batch.linkTo(m ->
            {
                throw new IllegalStateException("sink boom");
            });

            // maxSize == 1: the batch drains, flushes to the throwing sink and
            // the in-flight counter entry must be released regardless
            batch.accept("a");
            awaitIdle(true, "sink threw on the only message", batch);

            // the actor must keep accepting and processing afterwards
            AtomicInteger delivered = new AtomicInteger();
            batch.linkTo(m -> delivered.incrementAndGet());
            batch.accept("b");
            awaitIdle(true, "second message", batch);
            assertEquals(1, delivered.get());

            Exception ex = batch.getException();
            assertNotNull(ex, "the sink failure has to be recorded by the stage");
        }
        finally
        {
            batch.shutdown();
        }
    }

    @Test
    void fullBatchFailureKeepsCountersConsistent()
    throws Exception
    {
        BatchActor<String> batch = new BatchActor<>(2, 0);
        try
        {
            AtomicInteger firstBatchFailed = new AtomicInteger(0);
            AtomicInteger delivered = new AtomicInteger();
            batch.linkTo(m ->
            {
                List<String> items = m;
                if (items.contains("a"))
                {
                    firstBatchFailed.incrementAndGet();
                    throw new IllegalStateException("boom " + items.size());
                }
                delivered.addAndGet(items.size());
            });

            batch.accept("a");
            batch.accept("b");  // batch [a,b]: fails
            awaitIdle(true, "first batch threw", batch);

            batch.accept("c");
            batch.accept("d");  // batch [c,d]: succeeds
            awaitIdle(true, "second batch", batch);

            assertEquals(1, firstBatchFailed.get());
            assertEquals(2, delivered.get());
            Exception ex = batch.getException();
            assertNotNull(ex);

            // shutdown(true) must be able to terminate now that the counters
            // are consistent again; with the bug isIdle() is permanently false
            // and the deferred shutdown never completes
            batch.shutdown(true);
            awaitTerminated(batch);
        }
        finally
        {
            batch.shutdown();
        }
    }

    /** Bounded wait out of the pool thread: terminates the actor's private hub cleanly. */
    private static void awaitTerminated(BatchActor<?> batch) throws InterruptedException
    {
        Thread closer = new Thread(() ->
        {
            // shutdown(true) marked the close-when-empty flag; the remaining
            // work is to wait for the inner actor's termination
            while (!batch.isShutdown())
            {
                Thread.yield();
            }
            batch.awaitTermination();
        });
        closer.setDaemon(true);
        closer.start();
        closer.join(3_000);
        assertFalse(closer.isAlive(), "deferred shutdown never terminated the batch actor");
        assertTrue(batch.isTerminated());
    }
}
