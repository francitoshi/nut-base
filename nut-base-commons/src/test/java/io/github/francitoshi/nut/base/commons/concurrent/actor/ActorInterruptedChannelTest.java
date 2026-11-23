/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.francitoshi.nut.base.commons.lang.As;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the "zombie actor" state: an interrupted blocking
 * {@code get()} makes the closeable channels close themselves, without going
 * through the actor's own close path. Without the external-close hook in
 * {@code AsyncActor#workerDone}, the workers simply exited and the actor
 * stayed open forever: never terminated (so {@code awaitTermination()}/
 * {@code close()} hung) with no workers, and every later {@code accept()}
 * failing against the closed channel with the exception silently swallowed.
 * <p>
 * The <em>producer</em> side is different since the mailbox contract: a
 * producer's own interruption does not abort its put nor close the channel
 * (see {@code doc/actor-framework.md} R1) — the producer keeps waiting and
 * the message is delivered when there is room, so the reader-side zombie
 * scenario is the only one these tests exercise through a real close.
 */
class ActorInterruptedChannelTest
{
    /**
     * Interrupts a producer blocked in {@code accept} (rendezvous channel,
     * permanent worker busy processing). The mailbox contract keeps the
     * producer waiting and the actor alive: once the worker finishes the
     * current message the interrupted put delivers, and both messages are
     * processed in order.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void interruptedProducerKeepsWaitingAndDeliversInOrder() throws Exception
    {
        ActorHub hub = new ActorHub(1, 2, 1_000);
        CountDownLatch processing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<Integer> received = new CopyOnWriteArrayList<>();
        try
        {
            Actor<Integer> actor = Actor.create(hub, 1, 0, m ->
            {
                processing.countDown();
                received.add(m);
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

            // the interrupted producer keeps waiting: the actor stays alive
            Thread.sleep(200);
            assertFalse(actor.isShutdown(), "a producer interrupt must not close the actor");
            assertFalse(actor.isTerminated(), "a producer interrupt must not terminate the actor");

            release.countDown(); // the worker finishes message 1 and takes message 2
            producer.join(5_000);
            assertFalse(producer.isAlive(), "the interrupted producer must deliver once there is room");

            assertEquals(As.list(1, 2), received, "both messages must be processed in order");
            assertFalse(actor.isTerminated(), "the actor must survive the interrupted producer");

            actor.shutdown();
            assertTrue(actor.awaitTermination(5_000), "actor did not terminate on its own shutdown");
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
     * blocked downstream: the interrupted producer keeps waiting for room,
     * the queued batches flush in order, and all of them — including the
     * interrupted put's message — are forwarded; the stage stays alive.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void interruptedBatchActorProducerKeepsWaitingAndAllBatchesFlush() throws Exception
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
            // give the producer time to actually park inside the blocked put
            Thread.sleep(200);
            producer.interrupt();

            // the interrupted producer keeps waiting: the stage stays alive
            Thread.sleep(200);
            assertFalse(batch.isShutdown(), "a producer interrupt must not close the stage");
            assertFalse(batch.isTerminated(), "a producer interrupt must not terminate the stage");
            assertTrue(producer.isAlive(), "the interrupted producer must still be waiting for room");

            release.countDown(); // the drainer finishes flushing batch [1], then [2]
            producer.join(5_000);
            assertFalse(producer.isAlive(), "the interrupted producer must deliver once there is room");
            producer.join(5_000);

            // all three batches forwarded in order, including the interrupted
            // put's message
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (batches.size() < 3 && System.nanoTime() < deadline)
            {
                Thread.sleep(10);
            }
            assertEquals(3, batches.size(), "all batches must flush, in order");
            assertEquals(As.list(1), batches.get(0));
            assertEquals(As.list(2), batches.get(1));
            assertEquals(As.list(3), batches.get(2));
            assertFalse(batch.isTerminated(), "the stage must survive the interrupted producer");

            batch.close();
            assertTrue(batch.awaitTermination(5_000), "stage did not terminate on its own close");
        }
        finally
        {
            release.countDown();
            hub.shutdown();
        }
    }
}
