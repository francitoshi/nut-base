/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BatchActor}: size-based flush, time-based flush,
 * explicit {@link BatchActor#flush()}, {@link BatchActor#pending()} inspection,
 * termination cleanup, and the ActorHub factory methods.
 */
class BatchActorTest
{
    private ActorHub actorHub;

    @BeforeEach
    void setUp()
    {
        actorHub = ActorHub.hub(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException
    {
        actorHub.shutdown();
        actorHub.awaitTermination(2000);
    }

    @Test
    void sizeBasedFlushIsTriggeredWhenBatchReachesMaxSize()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchActor<Integer> batch = new BatchActor<>(3, 0L); // no time-based flush
        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        batch.accept(2);
        // Worker accumulates asynchronously: wait for both to be pending.
        assertTrue(awaitInt(() -> batch.pending(), 2, 2000));
        assertEquals(0, batches.size());

        batch.accept(3);
        // Batch should be full now, flushed immediately
        assertTrue(awaitInt(() -> batches.size(), 1, 2000));
        assertEquals(0, batch.pending());
        assertEquals(Arrays.asList(1, 2, 3), batches.get(0));
    }

    @Test
    void multipleBatchesAreForwardedSequentially()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchActor<Integer> batch = new BatchActor<>(2, 0L);
        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        batch.accept(2);
        assertTrue(awaitInt(() -> batches.size(), 1, 2000));

        batch.accept(3);
        batch.accept(4);
        assertTrue(awaitInt(() -> batches.size(), 2, 2000));

        assertEquals(Arrays.asList(1, 2), batches.get(0));
        assertEquals(Arrays.asList(3, 4), batches.get(1));
    }

    @Test
    void pendingCountReflectsTheCurrentBatchSize()
    {
        BatchActor<String> batch = new BatchActor<>(5, 0L);

        assertEquals(0, batch.pending());

        batch.accept("a");
        batch.accept("b");
        assertTrue(awaitInt(() -> batch.pending(), 2, 2000));

        batch.accept("c");
        batch.accept("d");
        batch.accept("e");
        assertTrue(awaitInt(() -> batch.pending(), 0, 2000)); // flushed when size reached 5

        batch.accept("f");
        assertTrue(awaitInt(() -> batch.pending(), 1, 2000));
    }

    @Test
    void explicitFlushEmitsTheCurrentPartialBatch()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchActor<Integer> batch = new BatchActor<>(10, 0L); // high threshold
        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        batch.accept(2);
        batch.accept(3);
        assertEquals(0, batches.size());

        batch.flush();

        assertEquals(1, batches.size());
        assertEquals(Arrays.asList(1, 2, 3), batches.get(0));
        assertEquals(0, batch.pending());
    }

    @Test
    void explicitFlushOnEmptyBatchDoesNothing()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchActor<Integer> batch = new BatchActor<>(10, 0L);
        batch.linkTo(m -> batches.add(m));

        batch.flush();

        assertEquals(0, batches.size());
        assertEquals(0, batch.pending());
    }

    @Test
    void timeBasedFlushIsTriggeredAfterTheConfiguredWindow()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchActor<Integer> batch = new BatchActor<>(100, 100L); // 100ms window, high size threshold

        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        assertEquals(0, batches.size());
        assertTrue(awaitInt(() -> batch.pending(), 1, 2000));

        // Wait for the time window to trigger
        assertTrue(awaitTrue(() -> batches.size() > 0, 500));

        assertEquals(1, batches.size());
        assertEquals(Arrays.asList(1), batches.get(0));
        assertEquals(0, batch.pending());
    }

    @Test
    void sizeFlushTakesPrecedenceOverTimeWindow()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchActor<Integer> batch = new BatchActor<>(2, 1000L); // 1s window, low size threshold

        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        batch.accept(2);

        // Size threshold is reached, no need to wait for the time window
        assertTrue(awaitInt(() -> batches.size(), 1, 2000));
        assertEquals(Arrays.asList(1, 2), batches.get(0));
    }

    @Test
    void disablingTimeWindowWithZeroOnlyTriggersOnSize()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchActor<Integer> batch = new BatchActor<>(5, 0L); // no time-based flush

        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        batch.accept(2);
        assertEquals(0, batches.size());

        // Time passes, but no flush should happen without size threshold
        try
        {
            Thread.sleep(50);
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
        }

        assertEquals(0, batches.size());
        assertEquals(2, batch.pending());
    }

    @Test
    void terminationFlushesPendingBatch()
    {
        List<List<String>> batches = new CopyOnWriteArrayList<>();
        BatchActor<String> batch = actorHub.batch(10, 0L); // high threshold

        batch.linkTo(m -> batches.add(m));

        batch.accept("a");
        batch.accept("b");

        batch.shutdown();
        batch.awaitTermination(1000);

        assertEquals(1, batches.size());
        assertEquals(Arrays.asList("a", "b"), batches.get(0));
    }

    @Test
    void noNextStageLinkedStillAcceptsAndBuffersMessages()
    {
        BatchActor<Integer> batch = new BatchActor<>(3, 0L);

        batch.accept(1);
        batch.accept(2);
        batch.accept(3);

        assertTrue(awaitInt(() -> batch.pending(), 0, 2000)); // should be flushed
    }

    @Test
    void constructorRejectsInvalidMaxSize()
    {
        assertThrows(IllegalArgumentException.class, () -> new BatchActor<>(0, 100L));
        assertThrows(IllegalArgumentException.class, () -> new BatchActor<>(-5, 100L));
    }

    @Test
    void linkToRejectsNull()
    {
        BatchActor<Integer> batch = new BatchActor<>(3, 100L);
        assertThrows(NullPointerException.class, () -> batch.linkTo(null));
    }

    @Test
    void linkToReturnsTheSameNextInstanceForFluentChaining()
    {
        BatchActor<Integer> batch = new BatchActor<>(3, 100L);
        java.util.function.Consumer<List<Integer>> consumer = m -> {};
        Consumer<List<Integer>> next = m -> consumer.accept(m);

        Consumer<List<Integer>> returned = batch.linkTo(next);

        assertEquals(next, returned);
    }

    @Test
    void hubBatchFactoryWithMaxSizeAndWindow() throws InterruptedException
    {
        List<List<String>> batches = new CopyOnWriteArrayList<>();
        BatchActor<String> batch = actorHub.batch(2, 0L);
        batch.linkTo(m -> batches.add(m));

        batch.accept("a");
        batch.accept("b");

        actorHub.close(true);

        assertEquals(1, batches.size());
        assertEquals(Arrays.asList("a", "b"), batches.get(0));
    }

    @Test
    void hubBatchFactoryWithQueueSizeParameter() throws InterruptedException
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchActor<Integer> batch = actorHub.batch(2, 10, 3, 0L);
        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        batch.accept(2);
        batch.accept(3);

        actorHub.close(true);

        assertEquals(1, batches.size());
        assertEquals(Arrays.asList(1, 2, 3), batches.get(0));
    }

    @Test
    void batchesAreIndependentListInstances()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchActor<Integer> batch = new BatchActor<>(2, 0L);
        batch.linkTo(m -> batches.add(new ArrayList<>(m)));

        batch.accept(1);
        batch.accept(2);
        batch.accept(3);
        batch.accept(4);

        assertTrue(awaitInt(() -> batches.size(), 2, 2000));
        assertTrue(batches.get(0) instanceof ArrayList);
        assertTrue(batches.get(1) instanceof ArrayList);
    }

    /**
     * Polls the given condition every 5ms until it becomes true or the
     * timeout elapses, returning the last observed value.
     */
    static boolean awaitTrue(BooleanSupplier condition, long timeoutMillis)
    {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline)
        {
            if (condition.getAsBoolean())
            {
                return true;
            }
            try
            {
                Thread.sleep(5);
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();
                return condition.getAsBoolean();
            }
        }
        return condition.getAsBoolean();
    }

    /**
     * Polls the given value supplier every 5ms until it returns the expected
     * value or the timeout elapses, returning the last observed result.
     */
    static boolean awaitInt(IntSupplier supplier, int expected, long timeoutMillis)
    {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline)
        {
            if (supplier.getAsInt() == expected)
            {
                return true;
            }
            try
            {
                Thread.sleep(5);
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();
                return supplier.getAsInt() == expected;
            }
        }
        return supplier.getAsInt() == expected;
    }
}
