/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import io.github.francitoshi.nut.base.commons.lang.As;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the graceful hub shutdown
 * ({@code ActorHub#shutdown(boolean) shutdown(true)}): work submitted before
 * the shutdown and while the drain is running must all be processed, work
 * submitted after a stage has terminated must be rejected loudly, and no
 * accepted message may ever be silently dropped by the close.
 */
class ActorHubDrainingShutdownTest
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
     * A message submitted while the graceful shutdown is already draining
     * (its upstream stage is still busy with an earlier message) must be
     * processed and rippled to the downstream stage, not dropped by the
     * close.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shutdownWhenEmptyProcessesMessagesSubmittedWhileDraining() throws Exception
    {
        List<Integer> received = new CopyOnWriteArrayList<>();
        CountDownLatch firstInFlight = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        PipeActor<Integer, Integer> pipe = hub.pipe(1, 0, i -> i);
        Actor<Integer> sink = hub.actor(1, 0, m ->
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
            received.add(m);
        });
        pipe.linkTo(sink);

        pipe.accept(1); // the ripple parks inside the sink's receive
        assertTrue(firstInFlight.await(5, TimeUnit.SECONDS));

        Thread shutdowner = new Thread(() -> hub.shutdown(true));
        shutdowner.start();

        // submitted while the shutdown is draining: must not be dropped
        pipe.accept(2);
        release.countDown();
        shutdowner.join(20_000);
        assertTrue(hub.awaitTermination(5_000), "hub did not terminate");

        assertEquals(As.list(1, 2), received);
    }

    /** After the graceful shutdown completed, accept() must reject loudly. */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void acceptAfterShutdownWhenEmptyThrows() throws Exception
    {
        Actor<Integer> sink = hub.actor(1, 0, m ->
        {
        });
        sink.accept(1);
        hub.shutdown(true);
        assertTrue(hub.awaitTermination(5_000), "hub did not terminate");

        assertThrows(IllegalStateException.class, () -> sink.accept(2));
    }

    /**
     * Deterministic tail-first chain: the sink is registered before the pipe
     * (the documented ActorHub example pattern), so registration order is
     * downstream-first. The cascading close must derive its order from the
     * link graph and keep the sink open until the pipe's in-flight message
     * has been forwarded.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void tailFirstChainClosesUpstreamFirst() throws Exception
    {
        List<Integer> received = new CopyOnWriteArrayList<>();
        CountDownLatch firstInFlight = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Actor<Integer> sink = hub.actor(1, 0, m ->
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
            received.add(m);
        });
        PipeActor<Integer, Integer> pipe = hub.pipe(1, 4, (Integer i) ->
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
        pipe.linkTo(sink);

        pipe.accept(1); // the pipe parks inside its function
        assertTrue(firstInFlight.await(5, TimeUnit.SECONDS));

        Thread shutdowner = new Thread(() -> hub.shutdown(true));
        shutdowner.start();

        pipe.accept(2); // queued while the shutdown is already cascading
        release.countDown();
        shutdowner.join(20_000);
        assertFalse(shutdowner.isAlive(), "the cascading shutdown did not finish");

        assertEquals(As.list(1, 2), received);
    }

    /**
     * Stress invariant: every message a producer successfully accepted
     * (accept() did not throw) must be delivered, even when the producer
     * races the graceful shutdown. Under the old immediate close, messages
     * slipping into the window between the quiescence check and the close
     * were silently dropped, so delivered < accepted.
     */
    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void shutdownWhenEmptyDoesNotLoseAcceptedMessagesUnderLoad() throws Exception
    {
        for (int round = 0; round < 10; round++)
        {
            ActorHub roundHub = new ActorHub(2, 4, 1_000);
            try
            {
                AtomicInteger received = new AtomicInteger();
                AtomicInteger forwarded = new AtomicInteger();
                Actor<Integer> sink = roundHub.actor(1, 8, m -> received.incrementAndGet());
                PipeActor<Integer, Integer> pipe = roundHub.pipe(1, 8, i ->
                {
                    forwarded.incrementAndGet();
                    return i;
                });
                pipe.linkTo(sink);

                AtomicInteger accepted = new AtomicInteger();
                Thread producer = new Thread(() ->
                {
                    for (int i = 0; i < 5_000; i++)
                    {
                        try
                        {
                            pipe.accept(i);
                            accepted.incrementAndGet();
                        }
                        catch (IllegalStateException closedOrAborted)
                        {
                            return; // the stage closed under us: stop submitting
                        }
                    }
                });
                producer.start();
                Thread.sleep(20); // let some messages flow before the shutdown
                roundHub.shutdown(true);
                producer.join(20_000);
                assertTrue(roundHub.awaitTermination(10_000), "round " + round + ": hub did not terminate");

                // The chain is built tail-first (the documented ActorHub
                // example pattern): the cascading close must derive its order
                // from the link graph, not from registration order.
                assertEquals(accepted.get(), received.get(),
                        "round " + round + ": every accepted message must be delivered; forwarded=" + forwarded.get());
            }
            finally
            {
                roundHub.shutdown();
            }
        }
    }
}
