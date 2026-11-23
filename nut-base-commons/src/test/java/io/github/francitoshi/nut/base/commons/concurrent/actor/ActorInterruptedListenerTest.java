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
import java.util.concurrent.atomic.AtomicReference;

import static io.github.francitoshi.nut.base.commons.lang.Unchecked.sneakyThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Interruption handling of the actor framework: the framework stops the
 * interruptions it handles and routes the caught exceptions to the hub's
 * interruption listener when they are raised by the machinery that works
 * with an actor; an interruption escaping {@code receive(m)} takes the
 * regular exception path instead ({@code handleException} →
 * {@code setOnException}, recorded in {@code getException()}).
 */
class ActorInterruptedListenerTest
{
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void interruptionEscapingReceiveIsHandledThroughOnException() throws Exception
    {
        ActorHub hub = new ActorHub(1, 2, 1_000);
        try
        {
            List<InterruptedException> onExceptionInterrupts = new CopyOnWriteArrayList<>();
            List<InterruptedException> hubInterrupts = new CopyOnWriteArrayList<>();

            Actor<Integer> actor = Actor.create(hub, 1, 4, m ->
            {
                // an interruption landing while the message is being
                // processed: receive lets it escape (sneaky throw, as
                // unchecked) and handleException captures it
                throw sneakyThrow(new InterruptedException("interrupted while receiving"));
            }, null, null);
            actor.setOnException(ex -> onExceptionInterrupts.add((InterruptedException) ex));
            hub.setOnException(ex -> hubInterrupts.add((InterruptedException) ex));

            actor.accept(1);

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (onExceptionInterrupts.isEmpty() && System.nanoTime() < deadline)
            {
                Thread.sleep(10);
            }
            assertEquals(1, onExceptionInterrupts.size(), "the interruption takes the regular exception path");
            assertSame("interrupted while receiving", onExceptionInterrupts.get(0).getMessage());
            assertSame(onExceptionInterrupts.get(0), actor.getException(), "the exception is recorded");
            assertTrue(hubInterrupts.isEmpty(), "no hub routing for a receive-phase interruption");
        }
        finally
        {
            hub.shutdown();
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void interruptionInMailboxMachineryGoesToTheHubOnException() throws Exception
    {
        ActorHub hub = new ActorHub(1, 2, 1_000);
        try
        {
            List<InterruptedException> hubInterrupts = new CopyOnWriteArrayList<>();

            // rendezvous with the worker busy inside receive: a put has no
            // taker and waits; interrupting the producer is an interruption
            // raised by the actor's mailbox machinery, attributed to the actor
            CountDownLatch processing = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            Actor<Integer> actor = Actor.create(hub, 1, 0, m ->
            {
                processing.countDown();
                try
                {
                    release.await();
                }
                catch (InterruptedException ignored)
                {
                }
            }, null, null);
            hub.setOnException(ex -> hubInterrupts.add((InterruptedException) ex));

            actor.accept(1); // the permanent worker parks inside receive()
            assertTrue(processing.await(5, TimeUnit.SECONDS));

            CountDownLatch waiting = new CountDownLatch(1);
            Thread producer = new Thread(() ->
            {
                waiting.countDown();
                try
                {
                    actor.accept(2); // waits in the rendezvous hand-off
                }
                catch (Exception ignored)
                {
                }
            });
            producer.start();
            assertTrue(waiting.await(5, TimeUnit.SECONDS));
            producer.interrupt();

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (hubInterrupts.isEmpty() && System.nanoTime() < deadline)
            {
                Thread.sleep(10);
            }
            assertEquals(1, hubInterrupts.size(), "the hub's onException receives the caught interruption");
            assertFalse(actor.isTerminated(), "the interrupted producer does not terminate the actor");
        }
        finally
        {
            hub.shutdown();
        }
    }
}
