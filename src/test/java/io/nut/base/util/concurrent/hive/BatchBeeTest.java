/*
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent.hive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BatchBee}: size-based flush, time-based flush,
 * explicit {@link BatchBee#flush()}, {@link BatchBee#pending()} inspection,
 * termination cleanup, and the Hive factory methods.
 */
class BatchBeeTest
{
    private Hive hive;

    @BeforeEach
    void setUp()
    {
        hive = Hive.hive(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException
    {
        hive.shutdown();
        hive.awaitTermination(2000);
    }

    @Test
    void sizeBasedFlushIsTriggeredWhenBatchReachesMaxSize()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchBee<Integer> batch = new BatchBee<>(3, 0L); // no time-based flush
        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        batch.accept(2);
        assertEquals(0, batches.size());
        assertEquals(2, batch.pending());

        batch.accept(3);
        // Batch should be full now, flushed immediately
        assertEquals(1, batches.size());
        assertEquals(0, batch.pending());
        assertEquals(Arrays.asList(1, 2, 3), batches.get(0));
    }

    @Test
    void multipleBatchesAreForwardedSequentially()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchBee<Integer> batch = new BatchBee<>(2, 0L);
        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        batch.accept(2);
        assertEquals(1, batches.size());

        batch.accept(3);
        batch.accept(4);
        assertEquals(2, batches.size());

        assertEquals(Arrays.asList(1, 2), batches.get(0));
        assertEquals(Arrays.asList(3, 4), batches.get(1));
    }

    @Test
    void pendingCountReflectsTheCurrentBatchSize()
    {
        BatchBee<String> batch = new BatchBee<>(5, 0L);

        assertEquals(0, batch.pending());

        batch.accept("a");
        batch.accept("b");
        assertEquals(2, batch.pending());

        batch.accept("c");
        batch.accept("d");
        batch.accept("e");
        assertEquals(0, batch.pending()); // flushed when size reached 5

        batch.accept("f");
        assertEquals(1, batch.pending());
    }

    @Test
    void explicitFlushEmitsTheCurrentPartialBatch()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchBee<Integer> batch = new BatchBee<>(10, 0L); // high threshold
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
        BatchBee<Integer> batch = new BatchBee<>(10, 0L);
        batch.linkTo(m -> batches.add(m));

        batch.flush();

        assertEquals(0, batches.size());
        assertEquals(0, batch.pending());
    }

    @Test
    void timeBasedFlushIsTriggeredAfterTheConfiguredWindow()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchBee<Integer> batch = new BatchBee<>(100, 100L); // 100ms window, high size threshold

        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        assertEquals(0, batches.size());
        assertEquals(1, batch.pending());

        // Wait for the time window to trigger
        assertTrue(TestUtil.awaitTrue(() -> batches.size() > 0, 500));

        assertEquals(1, batches.size());
        assertEquals(Arrays.asList(1), batches.get(0));
        assertEquals(0, batch.pending());
    }

    @Test
    void sizeFlushTakesPrecedenceOverTimeWindow()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchBee<Integer> batch = new BatchBee<>(2, 1000L); // 1s window, low size threshold

        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        batch.accept(2);

        // Size threshold is reached, no need to wait for the time window
        assertEquals(1, batches.size());
        assertEquals(Arrays.asList(1, 2), batches.get(0));
    }

    @Test
    void disablingTimeWindowWithZeroOnlyTriggersOnSize()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchBee<Integer> batch = new BatchBee<>(5, 0L); // no time-based flush

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
        BatchBee<String> batch = hive.batch(10, 0L); // high threshold

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
        BatchBee<Integer> batch = new BatchBee<>(3, 0L);

        batch.accept(1);
        batch.accept(2);
        batch.accept(3);

        assertEquals(0, batch.pending()); // should be flushed
    }

    @Test
    void constructorRejectsInvalidMaxSize()
    {
        assertThrows(IllegalArgumentException.class, () -> new BatchBee<>(0, 100L));
        assertThrows(IllegalArgumentException.class, () -> new BatchBee<>(-5, 100L));
    }

    @Test
    void linkToRejectsNull()
    {
        BatchBee<Integer> batch = new BatchBee<>(3, 100L);
        assertThrows(NullPointerException.class, () -> batch.linkTo(null));
    }

    @Test
    void linkToReturnsTheSameNextInstanceForFluentChaining()
    {
        BatchBee<Integer> batch = new BatchBee<>(3, 100L);
        java.util.function.Consumer<List<Integer>> consumer = m -> {};
        Consumer<List<Integer>> next = m -> consumer.accept(m);

        Consumer<List<Integer>> returned = batch.linkTo(next);

        assertEquals(next, returned);
    }

    @Test
    void hiveBatchFactoryWithMaxSizeAndWindow() throws InterruptedException
    {
        List<List<String>> batches = new CopyOnWriteArrayList<>();
        BatchBee<String> batch = hive.batch(2, 0L);
        batch.linkTo(m -> batches.add(m));

        batch.accept("a");
        batch.accept("b");

        Hive.shutdownAndAwaitTermination(true, batch);

        assertEquals(1, batches.size());
        assertEquals(Arrays.asList("a", "b"), batches.get(0));
    }

    @Test
    void hiveBatchFactoryWithThreadsParameter() throws InterruptedException
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchBee<Integer> batch = hive.batch(2, 3, 0L);
        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        batch.accept(2);
        batch.accept(3);

        Hive.shutdownAndAwaitTermination(true, batch);

        assertEquals(1, batches.size());
    }

    @Test
    void hiveBatchFactoryWithQueueSizeParameter() throws InterruptedException
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchBee<Integer> batch = hive.batch(2, 10, 3, 0L);
        batch.linkTo(m -> batches.add(m));

        batch.accept(1);
        batch.accept(2);
        batch.accept(3);

        Hive.shutdownAndAwaitTermination(true, batch);

        assertEquals(1, batches.size());
        assertEquals(Arrays.asList(1, 2, 3), batches.get(0));
    }

    @Test
    void batchesAreIndependentListInstances()
    {
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        BatchBee<Integer> batch = new BatchBee<>(2, 0L);
        batch.linkTo(m -> batches.add(new ArrayList<>(m)));

        batch.accept(1);
        batch.accept(2);
        batch.accept(3);
        batch.accept(4);

        assertEquals(2, batches.size());
        assertTrue(batches.get(0) instanceof ArrayList);
        assertTrue(batches.get(1) instanceof ArrayList);
    }
}
