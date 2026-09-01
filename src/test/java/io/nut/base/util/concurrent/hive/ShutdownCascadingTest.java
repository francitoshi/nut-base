/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for cascading shutdown: when one stage in a chain is shut down
 * with shutdownCascading(), the shutdown propagates through all linked
 * downstream stages until reaching a plain Bee or a disconnected stage.
 */
class ShutdownCascadingTest
{
    private Hive hive;

    @BeforeEach
    void setUp()
    {
        hive = Hive.hive(2);
    }

    @AfterEach
    void tearDown()
    {
        hive.shutdown();
        try
        {
            hive.awaitTermination(2000);
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void shutdownCascadingPropagatesThroughLinearPipeBeeChain() throws InterruptedException
    {
        PipeBee<Integer,Integer> pipe1 = hive.pipe(i -> i * 2);
        PipeBee<Integer,String> pipe2 = hive.pipe(i -> "v=" + i);
        RecordingBee<String> sink = new RecordingBee<>(hive);

        pipe1.linkTo(pipe2).linkTo(sink);

        hive.shutdownAndAwaitTermination(true);

        assertTrue(pipe1.isTerminated());
        assertTrue(pipe2.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void shutdownCascadingPropagatesThroughFilterBee() throws InterruptedException
    {
        PipeBee<Integer,Integer> pipe = hive.pipe(i -> i);
        FilterBee<Integer> filter = hive.filter(i -> i > 0);
        RecordingBee<Integer> sink = new RecordingBee<>(hive);

        pipe.linkTo(filter).linkTo(sink);

        hive.shutdownAndAwaitTermination(true);

        assertTrue(pipe.isTerminated());
        assertTrue(filter.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void shutdownCascadingPropagatesThroughBatchBee() throws InterruptedException
    {
        PipeBee<Integer,Integer> pipe = hive.pipe(i -> i);
        BatchBee<Integer> batch = hive.batch(5, 0L);
        RecordingBee<java.util.List<Integer>> sink = new RecordingBee<>(hive);

        pipe.linkTo(batch).linkTo(sink);

        hive.shutdownAndAwaitTermination(true);

        assertTrue(pipe.isTerminated());
        assertTrue(batch.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void shutdownCascadingFromFanOutBeeShutdownsAllTargets() throws InterruptedException
    {
        PipeBee<Integer,Integer> pipe = hive.pipe(i -> i);
        RecordingBee<Integer> target1 = new RecordingBee<>(hive);
        RecordingBee<Integer> target2 = new RecordingBee<>(hive);
        RecordingBee<Integer> target3 = new RecordingBee<>(hive);

        FanOutBee<Integer> broadcast = hive.broadcast(target1, target2, target3);
        pipe.linkTo(broadcast);

        hive.shutdownAndAwaitTermination(true);

        assertTrue(pipe.isTerminated());
        assertTrue(broadcast.isTerminated());
        assertTrue(target1.isTerminated());
        assertTrue(target2.isTerminated());
        assertTrue(target3.isTerminated());
    }

    @Test
    void shutdownCascadingFromFanOutBeeDirectly() throws InterruptedException
    {
        RecordingBee<String> t1 = new RecordingBee<>(hive);
        RecordingBee<String> t2 = new RecordingBee<>(hive);
        FanOutBee<String> bc = hive.broadcast(t1, t2);

        hive.shutdownAndAwaitTermination(true);

        assertTrue(bc.isTerminated());
        assertTrue(t1.isTerminated());
        assertTrue(t2.isTerminated());
    }

    @Test
    void shutdownCascadingWithComplexTopology() throws InterruptedException
    {
        // pipe1 -> filter -> batch -> (pipe2, pipe3)
        PipeBee<Integer,Integer> pipe1 = hive.pipe(i -> i * 2);
        FilterBee<Integer> filter = hive.filter(i -> i > 0);
        BatchBee<Integer> batch = hive.batch(3, 0L);
        PipeBee<java.util.List<Integer>,String> pipe2 = hive.pipe(lst -> "batch=" + lst);
        RecordingBee<String> sink = new RecordingBee<>(hive);

        pipe1.linkTo(filter).linkTo(batch).linkTo(pipe2).linkTo(sink);

        hive.shutdownAndAwaitTermination(true);

        assertTrue(pipe1.isTerminated());
        assertTrue(filter.isTerminated());
        assertTrue(batch.isTerminated());
        assertTrue(pipe2.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void shutdownCascadingWithOnlyWhenEmptyFlag() throws InterruptedException
    {
        PipeBee<Integer,Integer> pipe1 = hive.pipe(i -> i);
        RecordingBee<Integer> sink = new RecordingBee<>(hive);
        pipe1.linkTo(sink);

        hive.shutdownAndAwaitTermination(true);

        assertTrue(pipe1.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void normalShutdownDoesNotPropagate()
    {
        PipeBee<Integer,Integer> pipe1 = hive.pipe(i -> i);
        PipeBee<Integer,Integer> pipe2 = hive.pipe(i -> i);
        RecordingBee<Integer> sink = new RecordingBee<>(hive);

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
