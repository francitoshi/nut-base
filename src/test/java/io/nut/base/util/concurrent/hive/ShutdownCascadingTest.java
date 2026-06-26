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

        Hive.shutdownAndAwaitTermination(true, pipe1);

        assertTrue(pipe1.isTerminated());
        assertTrue(pipe2.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void shutdownCascadingStopsAtDisconnectedStage() throws InterruptedException
    {
        PipeBee<Integer,Integer> pipe1 = hive.pipe(i -> i);
        PipeBee<Integer,Integer> pipe2 = hive.pipe(i -> i);
        // Not linked

        Hive.shutdownAndAwaitTermination(true, pipe1);

        assertTrue(pipe1.isTerminated());
        // pipe2 was never linked, so it's not affected
        assertFalse(pipe2.isShutdown());
    }

    @Test
    void shutdownCascadingPropagatesThroughFilterBee() throws InterruptedException
    {
        PipeBee<Integer,Integer> pipe = hive.pipe(i -> i);
        FilterBee<Integer> filter = hive.filter(i -> i > 0);
        RecordingBee<Integer> sink = new RecordingBee<>(hive);

        pipe.linkTo(filter).linkTo(sink);

        Hive.shutdownAndAwaitTermination(true, pipe);

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

        Hive.shutdownAndAwaitTermination(true, pipe);

        assertTrue(pipe.isTerminated());
        assertTrue(batch.isTerminated());
        assertTrue(sink.isTerminated());
    }

    @Test
    void shutdownCascadingFromBroadcastBeeShutdownsAllTargets() throws InterruptedException
    {
        PipeBee<Integer,Integer> pipe = hive.pipe(i -> i);
        RecordingBee<Integer> target1 = new RecordingBee<>(hive);
        RecordingBee<Integer> target2 = new RecordingBee<>(hive);
        RecordingBee<Integer> target3 = new RecordingBee<>(hive);

        BroadcastBee<Integer> broadcast = hive.broadcast(target1, target2, target3);
        pipe.linkTo(broadcast);

        Hive.shutdownAndAwaitTermination(true, pipe);

        assertTrue(pipe.isTerminated());
        assertTrue(broadcast.isTerminated());
        assertTrue(target1.isTerminated());
        assertTrue(target2.isTerminated());
        assertTrue(target3.isTerminated());
    }

    @Test
    void shutdownCascadingFromBroadcastBeeDirectly() throws InterruptedException
    {
        RecordingBee<String> t1 = new RecordingBee<>(hive);
        RecordingBee<String> t2 = new RecordingBee<>(hive);
        BroadcastBee<String> bc = hive.broadcast(t1, t2);

        Hive.shutdownAndAwaitTermination(true, bc);

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

        Hive.shutdownAndAwaitTermination(true, pipe1);

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

        Hive.shutdownAndAwaitTermination(true, pipe1);

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
