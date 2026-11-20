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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the {@link Actor#accept(Object)} delivery-or-throw
 * contract: a returned call means the message was queued and will be
 * processed; a thrown exception means the message was not delivered. Under
 * the old catch-all, a refused worker start or an aborted put left the
 * caller believing the message was accepted.
 */
class ActorAcceptContractTest
{
    /**
     * An interrupted blocking put aborts and closes the channel; accept() must
     * surface the rejection to the caller instead of returning normally (the
     * message was not delivered).
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void acceptThrowsWhenItsPutIsAbortedByAnInterrupt() throws Exception
    {
        ActorHub hub = new ActorHub(1, 2, 1_000, true, true);
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

            AtomicReference<Throwable> thrown = new AtomicReference<>();
            Thread producer = new Thread(() ->
            {
                try
                {
                    actor.accept(2); // blocks inside the rendezvous put
                }
                catch (Throwable t)
                {
                    thrown.set(t);
                }
            });
            producer.start();
            // give the producer time to actually park inside the blocked put
            Thread.sleep(200);
            producer.interrupt();
            producer.join(5_000);

            assertTrue(thrown.get() instanceof IllegalStateException,
                    "the aborted put must surface as a rejected accept, got " + thrown.get());
        }
        finally
        {
            release.countDown();
            hub.shutdown();
        }
    }

    /**
     * When the executor refuses to run the permanent worker, the message was
     * never queued: accept() must throw instead of silently dropping it.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void acceptThrowsWhenTheExecutorRefusesTheWorker() throws Exception
    {
        ActorHub hub = new ActorHub(1, 2, 1_000, true, true);
        hub.shutdown(); // shut the pool down before any actor exists
        try
        {
            Actor<Integer> actor = hub.actor(1, 0, m ->
            {
            });

            // CallerRunsPolicy would silently discard the worker task and the
            // message with it; the guard must reject the accept instead.
            assertThrows(IllegalStateException.class, () -> actor.accept(1));
            assertTrue(actor.isIdle(), "no message may be left accounted");
        }
        finally
        {
            hub.shutdown();
        }
    }

    /**
     * A failing rush-worker start with the message already queued must NOT
     * surface as a rejected accept: the message is safe (the permanent worker
     * drains it) and a rethrow would invite a duplicate resend. The failure
     * must still be recorded.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void acceptSurvivesAFailingRushWorkerStart() throws Exception
    {
        ActorHub hub = new ActorHub(2, 4, 1_000);
        try
        {
            List<Integer> received = new CopyOnWriteArrayList<>();
            CountDownLatch delivered = new CountDownLatch(1);
            MultiActor<Integer> actor = new MultiActor<Integer>(hub, 2, 0)
            {
                @Override
                protected void receive(Integer m)
                {
                    received.add(m);
                    delivered.countDown();
                }

                @Override
                protected void startWorker()
                {
                    throw new RejectedExecutionException("boom");
                }
            };

            actor.accept(1); // must return normally: the message is queued

            assertTrue(delivered.await(5, TimeUnit.SECONDS),
                    "the permanent worker must drain the queued message");
            assertEquals(List.of(1), received);
            assertTrue(actor.getException() instanceof RejectedExecutionException,
                    "the rush-start failure must be recorded, got " + actor.getException());
        }
        finally
        {
            hub.shutdown();
        }
    }
}
