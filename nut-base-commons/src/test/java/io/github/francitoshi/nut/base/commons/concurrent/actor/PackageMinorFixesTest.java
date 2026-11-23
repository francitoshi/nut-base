/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import io.github.francitoshi.nut.base.commons.concurrent.channel.CloseableConflatedChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the smaller fixes: the unlinked-forward logging, the
 * daemon threads of the owned hub of a BatchActor, the iterative deep-chain
 * traversal, the processed-count of failed synchronous messages, the loud
 * abort of the closeable conflated channel and the rejection of work on a
 * shut-down pool (no Phaser leak, no hanging futures).
 */
class PackageMinorFixesTest
{
    /**
     * A forward without a linked downstream is silently ignored: no
     * exception, no logging — a forgotten link loses the message without
     * noise (the drop logging was removed with the logging rework).
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void unlinkedForwardIsSilentlyIgnored()
    {
        PipeActor<String, String> pipe = new PipeActor<>(s -> s); // no hub, no link

        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler()
        {
            @Override
            public void publish(LogRecord record)
            {
                records.add(record);
            }

            @Override
            public void flush()
            {
            }

            @Override
            public void close()
            {
            }
        };
        Logger log = Logger.getLogger(LinkableActor.class.getName());
        boolean useParentHandlers = log.getUseParentHandlers();
        log.setUseParentHandlers(false);
        log.addHandler(handler);
        try
        {
            pipe.accept("dropped"); // must not throw and must not log
            assertEquals(Collections.emptyList(), records, "an unlinked forward is silently ignored");
        }
        finally
        {
            log.removeHandler(handler);
            log.setUseParentHandlers(useParentHandlers);
        }
    }

    /**
     * The worker threads of the hub owned by a stand-alone BatchActor must be
     * daemon: a forgotten close() must never keep the JVM alive.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void batchActorOwnHubWorkerIsDaemon() throws Exception
    {
        BatchActor<Integer> batch = new BatchActor<>(null, 1, 0, 2, 0);
        try
        {
            batch.accept(1); // starts the permanent worker on the owned hub

            Thread worker = null;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (worker == null && System.nanoTime() < deadline)
            {
                for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet())
                {
                    for (StackTraceElement ste : entry.getValue())
                    {
                        if (ste.getClassName().endsWith("BatchActor$BatchDrainer")
                                && ste.getMethodName().equals("drainBatches"))
                        {
                            worker = entry.getKey();
                            break;
                        }
                    }
                    if (worker != null)
                    {
                        break;
                    }
                }
                if (worker == null)
                {
                    Thread.sleep(20);
                }
            }
            assertNotNull(worker, "the owned hub's permanent worker must be running");
            assertTrue(worker.isDaemon(), "the owned hub's worker threads must be daemon");
        }
        finally
        {
            batch.close();
        }
    }

    /** Counting Linkable stub used to exercise the deep-chain traversal. */
    private static final class CountingStage implements Linkable, Consumer<Object>
    {
        private Consumer<?> next;
        private final AtomicInteger waits;

        CountingStage(Consumer<?> next, AtomicInteger waits)
        {
            this.next = next;
            this.waits = waits;
        }

        void linkTo(Consumer<?> next)
        {
            this.next = next;
        }

        @Override
        public void accept(Object o)
        {
        }

        @Override
        public Linkable waitForIdle()
        {
            waits.incrementAndGet();
            return this;
        }

        @Override
        public Linkable shutdown()
        {
            return this;
        }

        @Override
        public Linkable shutdown(boolean whenIdle)
        {
            return this;
        }

        @Override
        public Collection<Consumer<?>> getLinkedTargets()
        {
            return next == null ? Collections.emptyList() : Collections.singletonList(next);
        }

        @Override
        public boolean awaitTerminationUntilNanos(long untilNanos)
        {
            return true;
        }

        @Override
        public boolean isIdle()
        {
            return true;
        }

        @Override
        public boolean isShutdown()
        {
            return true;
        }

        @Override
        public boolean isTerminated()
        {
            return true;
        }

        @Override
        public ActorLifecycle awaitTermination()
        {
            return this;
        }

        @Override
        public void close()
        {
        }
    }

    /**
     * The hub's chain traversal is iterative: a deep chain (here 20 000
     * stages, deep enough to overflow a recursive implementation) must be
     * traversed completely and cycles must remain safe.
     */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void deepChainTraversalDoesNotOverflowAndHandlesCycles()
    {
        AtomicInteger waits = new AtomicInteger();
        int depth = 20_000;

        // linear chain, built tail-first
        CountingStage current = new CountingStage(null, waits);
        for (int i = 1; i < depth; i++)
        {
            current = new CountingStage(current, waits);
        }
        CountingStage head = current;

        ActorHub.waitForIdle(head);
        assertEquals(depth, waits.get(), "every stage of the deep chain must be waited on");

        // cycle: the tail points back to the head
        CountingStage last = null;
        current = head;
        waits.set(0);
        while (true)
        {
            CountingStage nextStage = null;
            for (Consumer<?> target : current.getLinkedTargets())
            {
                nextStage = (CountingStage) target;
            }
            if (nextStage == null)
            {
                last = current;
                break;
            }
            current = nextStage;
        }
        last.linkTo(head);

        ActorHub.waitForIdle(head);
        assertEquals(depth, waits.get(), "each stage must be waited on exactly once despite the cycle");
    }

    /**
     * A failed synchronous receive must not advance the hub-wide processed
     * counter (it would confuse the quiescence detection of shutdown(true)).
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void syncActorCountsOnlyProcessedMessages()
    {
        ActorHub syncHub = new ActorHub(0, 0, 0, false, false);
        try
        {
            Actor<Integer> actor = syncHub.actor(m ->
            {
                if (m == 0)
                {
                    throw new RuntimeException("boom");
                }
            });

            actor.accept(0); // fails, recorded, not processed
            assertEquals(0, syncHub.processedCount().sum(), "a failed message must not count as processed");

            actor.accept(1);
            assertEquals(1, syncHub.processedCount().sum(), "a processed message must count");
        }
        finally
        {
            syncHub.shutdown();
        }
    }

    /**
     * The closeable conflated channel must fail loudly when its put is
     * aborted by an interruption, never return as if the value was stored.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void conflatedPutAbortsLoudlyOnInterrupt()
    {
        CloseableConflatedChannel<String> channel = new CloseableConflatedChannel<>();
        Thread.currentThread().interrupt();
        try
        {
            assertThrows(IllegalStateException.class, () -> channel.put("x"));
            assertTrue(channel.isClosed(), "the aborted put must have closed the channel");
            assertTrue(channel.isInterrupted());
        }
        finally
        {
            Thread.interrupted(); // clear the flag
        }
    }

    /**
     * Executing work on a shut-down pool must be rejected loudly: with the
     * default CallerRunsPolicy the task would silently vanish, leaking a
     * Phaser registration (waitForIdle hangs forever) or hanging
     * submit()/spawn() on a never-completing handshake.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void rejectedExecutionAfterShutdownSurfacesLoudly()
    {
        ActorPool pool = new ActorPool(1, 2, 1_000);
        pool.shutdown();

        assertThrows(RejectedExecutionException.class, () -> pool.execute(() ->
        {
        }));
        assertThrows(RejectedExecutionException.class, () -> pool.spawn(() ->
        {
        }));
        assertThrows(RejectedExecutionException.class, () -> pool.submit(() ->
        {
        }));
        assertThrows(RejectedExecutionException.class, () -> pool.submit((Supplier<Object>) () -> "x"));

        assertTrue(pool.isIdle(), "no Phaser party may leak from the rejections");
    }
}
