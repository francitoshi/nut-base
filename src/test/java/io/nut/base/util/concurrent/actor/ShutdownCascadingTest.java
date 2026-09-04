/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for cascading shutdown: when one stage in a chain is shut down
 * with shutdownCascading(), the shutdown propagates through all linked
 * downstream stages until reaching a plain Actor or a disconnected stage.
 */
class ShutdownCascadingTest
{
    private ActorHub actorHub;

    @BeforeEach
    void setUp()
    {
        actorHub = ActorHub.actorHub(2);
    }

    @AfterEach
    void tearDown()
    {
        actorHub.shutdown();
        try
        {
            actorHub.awaitTermination(2000);
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void shutdownCascadingPropagatesThroughLinearPipeActorChain() throws InterruptedException
    {
        PipeActor<Integer,Integer> pipe1 = actorHub.pipe(i -> i * 2);
        PipeActor<Integer,String> pipe2 = actorHub.pipe(i -> "v=" + i);
        RecordingActor<String> sink = new RecordingActor<>(actorHub);

        pipe1.linkTo(pipe2).linkTo(sink);

        actorHub.close(true);

        assertTrue(pipe1.isTerminated());
        assertTrue(pipe2.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void shutdownCascadingPropagatesThroughFilterActor() throws InterruptedException
    {
        PipeActor<Integer,Integer> pipe = actorHub.pipe(i -> i);
        FilterActor<Integer> filter = actorHub.filter(i -> i > 0);
        RecordingActor<Integer> sink = new RecordingActor<>(actorHub);

        pipe.linkTo(filter).linkTo(sink);

        actorHub.close(true);

        assertTrue(pipe.isTerminated());
        assertTrue(filter.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void shutdownCascadingPropagatesThroughBatchActor() throws InterruptedException
    {
        PipeActor<Integer,Integer> pipe = actorHub.pipe(i -> i);
        BatchActor<Integer> batch = actorHub.batch(5, 0L);
        RecordingActor<java.util.List<Integer>> sink = new RecordingActor<>(actorHub);

        pipe.linkTo(batch).linkTo(sink);

        actorHub.close(true);

        assertTrue(pipe.isTerminated());
        assertTrue(batch.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void shutdownCascadingFromFanOutActorShutdownsAllTargets() throws InterruptedException
    {
        PipeActor<Integer,Integer> pipe = actorHub.pipe(i -> i);
        RecordingActor<Integer> target1 = new RecordingActor<>(actorHub);
        RecordingActor<Integer> target2 = new RecordingActor<>(actorHub);
        RecordingActor<Integer> target3 = new RecordingActor<>(actorHub);

        FanOutActor<Integer> broadcast = actorHub.broadcast(target1, target2, target3);
        pipe.linkTo(broadcast);

        actorHub.close(true);

        assertTrue(pipe.isTerminated());
        assertTrue(broadcast.isTerminated());
        assertTrue(target1.isTerminated());
        assertTrue(target2.isTerminated());
        assertTrue(target3.isTerminated());
    }

    @Test
    void shutdownCascadingFromFanOutActorDirectly() throws InterruptedException
    {
        RecordingActor<String> t1 = new RecordingActor<>(actorHub);
        RecordingActor<String> t2 = new RecordingActor<>(actorHub);
        FanOutActor<String> bc = actorHub.broadcast(t1, t2);

        actorHub.close(true);

        assertTrue(bc.isTerminated());
        assertTrue(t1.isTerminated());
        assertTrue(t2.isTerminated());
    }

    @Test
    void shutdownCascadingWithComplexTopology() throws InterruptedException
    {
        // pipe1 -> filter -> batch -> (pipe2, pipe3)
        PipeActor<Integer,Integer> pipe1 = actorHub.pipe(i -> i * 2);
        FilterActor<Integer> filter = actorHub.filter(i -> i > 0);
        BatchActor<Integer> batch = actorHub.batch(3, 0L);
        PipeActor<java.util.List<Integer>,String> pipe2 = actorHub.pipe(lst -> "batch=" + lst);
        RecordingActor<String> sink = new RecordingActor<>(actorHub);

        pipe1.linkTo(filter).linkTo(batch).linkTo(pipe2).linkTo(sink);

        actorHub.close(true);

        assertTrue(pipe1.isTerminated());
        assertTrue(filter.isTerminated());
        assertTrue(batch.isTerminated());
        assertTrue(pipe2.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void shutdownCascadingWithOnlyWhenEmptyFlag() throws InterruptedException
    {
        PipeActor<Integer,Integer> pipe1 = actorHub.pipe(i -> i);
        RecordingActor<Integer> sink = new RecordingActor<>(actorHub);
        pipe1.linkTo(sink);

        actorHub.close(true);

        assertTrue(pipe1.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void normalShutdownDoesNotPropagate()
    {
        PipeActor<Integer,Integer> pipe1 = actorHub.pipe(i -> i);
        PipeActor<Integer,Integer> pipe2 = actorHub.pipe(i -> i);
        RecordingActor<Integer> sink = new RecordingActor<>(actorHub);

        pipe1.linkTo(pipe2).linkTo(sink);

        pipe1.shutdown(); // Normal shutdown, not cascading

        pipe1.waitForIdle().awaitTermination(25);
        assertTrue(pipe1.isTerminated());

        // pipe2 and sink should NOT be shut down
        assertFalse(pipe2.isShutdown());
        assertFalse(sink.isShutdown());
    }

    private static void assertFalse(boolean condition)
    {
        assertTrue(!condition);
    }
}
