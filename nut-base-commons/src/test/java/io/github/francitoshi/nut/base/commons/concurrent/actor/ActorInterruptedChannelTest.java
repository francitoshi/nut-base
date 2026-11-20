/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the "zombie actor" state: an interrupted blocking
 * {@code get()}/{@code put()} makes the closeable channels close themselves,
 * without going through the actor's own close path. Without the
 * external-close hook in {@code AsyncActor#workerDone}, the workers simply
 * exited and the actor stayed open forever: never terminated (so
 * {@code awaitTermination()}/{@code close()} hung) with no workers, and every
 * later {@code accept()} failing against the closed channel with the
 * exception silently swallowed.
 */
class ActorInterruptedChannelTest
{
    /**
     * Interrupts a producer blocked in {@code accept} (rendezvous channel,
     * permanent worker busy processing). The interrupted put closes the
     * channel; the actor must then run its normal close path and terminate
     * instead of becoming a zombie.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void interruptedProducerTerminatesActor() throws Exception
    {
        ActorHub hub = new ActorHub(1, 2, 1_000);
        CountDownLatch processing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try
        {
            Actor<Integer> actor = Actor.create(hub, 1, 0, m ->
            {
                processing.countDown();
                try
                {
                    release.await();
                }
                catch (InterruptedException ignored)
                {
                    // the release below always arrives before the assertions
                }
            }, null, null);

            actor.accept(1); // the permanent worker parks inside receive()
            assertTrue(processing.await(5, TimeUnit.SECONDS));

            Thread producer = new Thread(() -> actor.accept(2)); // blocks in channel.put
            producer.start();
            // give the producer time to actually park inside the blocked put
            Thread.sleep(200);
            producer.interrupt();
            producer.join(5_000);
            assertFalse(producer.isAlive(), "producer still blocked in put");

            // let the worker finish message 1; the closed channel then ends
            // its read loop and the external-close hook must terminate the actor
            release.countDown();

            assertTrue(actor.awaitTermination(5_000), "actor did not terminate after the interrupted put");
            assertTrue(actor.isShutdown(), "actor not marked shut down");
            assertTrue(actor.isTerminated(), "actor not marked terminated");
        }
        finally
        {
            release.countDown();
            hub.shutdown();
        }
    }

    /**
     * Same trigger for a {@link BatchActor} with buffered channel and a
     * blocked downstream: the buffered messages queued before the interrupted
     * put must still be flushed, and the stage must terminate.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void interruptedProducerTerminatesBatchActorAndFlushesQueuedMessages() throws Exception
    {
        ActorHub hub = new ActorHub(1, 2, 1_000);
        CountDownLatch forwarding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<List<Integer>> batches = new CopyOnWriteArrayList<>();
        try
        {
            // threads=1, queueSize=1 (buffered), maxSize=1, no time-based flush
            BatchActor<Integer> batch = new BatchActor<>(hub, 1, 1, 1, 0);
            batch.linkTo(b ->
            {
                forwarding.countDown();
                try
                {
                    release.await();
                }
                catch (InterruptedException ignored)
                {
                    // the release below always arrives before the assertions
                }
                batches.add(b);
            });

            batch.accept(1); // buffered: enqueued, the drainer flushes it and blocks in forward
            assertTrue(forwarding.await(5, TimeUnit.SECONDS));

            batch.accept(2); // buffered: enqueued behind the busy drainer, buffer now full

            Thread producer = new Thread(() -> batch.accept(3)); // blocks: buffer full
            producer.start();
            producer.interrupt(); // the aborted put closes the channel
            producer.join(5_000);
            assertFalse(producer.isAlive(), "producer still blocked in put");

            release.countDown(); // the drainer finishes flushing batch [1]

            assertTrue(batch.awaitTermination(5_000), "batch stage did not terminate after the interrupted put");
            assertTrue(batch.isTerminated(), "batch stage not marked terminated");

            // message 1 was in forward; message 2 was queued in the buffer
            // before the channel closed and must still have been flushed
            assertEquals(Arrays.asList(Arrays.asList(1), Arrays.asList(2)), batches);
        }
        finally
        {
            release.countDown();
            hub.shutdown();
        }
    }
}
