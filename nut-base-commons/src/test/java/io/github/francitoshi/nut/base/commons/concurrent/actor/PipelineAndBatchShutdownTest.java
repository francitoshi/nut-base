/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the pipeline/batch shutdown semantics: the synchronous
 * BatchActor must flush its partial batch on shutdown, the pipeline shutdown
 * must cascade (in-flight forwards land on still-open downstream stages), a
 * stale pipeline reference must only shut down its own stages, and the
 * terminal created by {@code sink} must be closed with the pipeline.
 */
class PipelineAndBatchShutdownTest
{
    private ActorHub hub;

    @BeforeEach
    void setUp()
    {
        hub = new ActorHub(2, 4, 1_000);
    }

    @AfterEach
    void tearDown()
    {
        hub.shutdown();
    }

    /**
     * The synchronous BatchActor has no read loop to flush on close: a partial
     * batch must be emitted by the shutdown instead of being dropped.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void syncBatchFlushesThePartialBatchOnShutdown()
    {
        List<List<String>> batches = new CopyOnWriteArrayList<>();
        BatchActor<String> batch = new BatchActor<>(null, 0, 0, 5, 0);
        batch.linkTo(batches::add);

        batch.accept("a");
        batch.accept("b");
        assertEquals(0, batches.size(), "the partial batch must still be pending");

        batch.shutdown();

        assertEquals(List.of(List.of("a", "b")), batches, "shutdown must flush the partial batch");
        assertTrue(batch.isTerminated());
    }

    /**
     * The pipeline shutdown cascades: a message accepted while the first stage
     * is busy (and the shutdown already running) is processed and forwarded
     * while the downstream stages are still open, instead of being dropped on
     * an already-closed stage.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void pipelineShutdownCascadeDeliversInFlightMessages() throws Exception
    {
        List<Integer> received = new CopyOnWriteArrayList<>();
        CountDownLatch firstInFlight = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        PipelineActor<Integer, Integer> pipeline = hub.pipeline(1, 4, (Integer i) ->
        {
            firstInFlight.countDown();
            try
            {
                release.await();
            }
            catch (InterruptedException ignored)
            {
                // the release below always arrives before the assertions
            }
            return i;
        });
        pipeline.then((Integer i) -> i).sink(received::add);

        pipeline.accept(1); // stage 1 parks inside its function
        assertTrue(firstInFlight.await(5, TimeUnit.SECONDS));

        Thread shutdowner = new Thread(() -> pipeline.shutdown(true));
        shutdowner.start();

        pipeline.accept(2); // queued while the shutdown is already cascading
        release.countDown();
        shutdowner.join(20_000);
        // the cascade only returns once every stage has terminated, so the
        // join above already proves the chain is closed (the hub's pool stays
        // open: the pipeline owns its stages, not the hub)
        assertFalse(shutdowner.isAlive(), "the cascading shutdown did not finish");

        assertEquals(List.of(1, 2), received);
    }

    /**
     * Each pipeline instance owns its own stage snapshot: a stale reference to
     * the shorter pipeline shuts down only the stages it was built from, not
     * the whole extended chain.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void stalePipelineReferenceShutsDownOnlyItsOwnStages() throws Exception
    {
        Actor<Integer> sink = hub.actor(1, 0, m ->
        {
        });
        PipelineActor<Integer, Integer> first = hub.pipeline((Integer i) -> i);
        PipelineActor<Integer, Integer> second = first.then((Integer i) -> i);
        second.to(sink);

        first.shutdown(); // stale reference: only stage 1 belongs to it

        assertTrue(first.head().isTerminated(), "stage 1 must be terminated");
        assertEquals(2, hub.actors().size(),
                "stage 2 and the sink must still be alive after the stale shutdown");
    }

    /**
     * The terminal created by {@code sink} is owned by the pipeline: shutting
     * the pipeline down must terminate it too.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void sinkTerminalIsClosedWithThePipeline() throws Exception
    {
        PipelineActor<Integer, Integer> pipeline = hub.pipeline((Integer i) -> i);
        PipeActor<Integer, ?> head = pipeline.sink(m ->
        {
        });

        pipeline.shutdown();

        assertTrue(head.isTerminated(), "the head must be terminated");
        assertTrue(hub.actors().isEmpty(),
                "the sink terminal must be terminated and unregistered with the pipeline");
    }
}
