/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import io.github.francitoshi.nut.base.commons.lang.Saturated;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stress test for the interplay between {@link Actor#shutdown(boolean)
 * shutdown(true)} and the completion of the last in-flight message.
 * <p>
 * The concern is a "lost flag": the caller observes a non-idle actor, sets
 * {@code shutdownWhenEmpty}, and the worker that was completing the last
 * message exits without ever re-reading the flag — which would leave the
 * actor open forever and hang {@code awaitTermination()}/{@code close()}.
 * <p>
 * This cannot happen under the current lock discipline: the {@code isIdle()}
 * check and the flag write share one critical section, and every completion
 * path that brings the counters to zero ends in a locked re-read of the flag
 * ({@code workerDone}, the all-zero block of {@code drainWhileRegistered},
 * and {@code flushPending}). These tests exercise that interleaving at high
 * iteration counts on all three paths as a regression guard.
 */
class ActorShutdownWhenEmptyRaceTest
{
    @Test
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void shutdownWhenEmptyTerminatesWhenLastMessageCompletesConcurrently()
    {
        for (int i = 0; i < 200; i++)
        {
            // SingleActor + buffered channel: the message may still be queued
            // or in processing when shutdown(true) runs.
            raceShutdownWithLastMessage("single-buffered", hub -> Actor.create(hub, 1, 4,
                    m -> { }, null, null));

            // MultiActor + buffered channel: rush-worker path (workerDone).
            raceShutdownWithLastMessage("multi-buffered", hub -> Actor.create(hub, 2, 4,
                    m -> { }, null, null));

            // BatchActor: partial batch in pending, drainer parked in the
            // channel read with the time-based flush disabled (flushPending
            // path).
            raceShutdownWithLastMessage("batch-partial", hub -> hub.batch(2, 0));
        }
    }

    private interface ActorFactory
    {
        Linkable create(ActorHub hub);
    }

    @SuppressWarnings("unchecked")
    private static void raceShutdownWithLastMessage(String label, ActorFactory factory)
    {
        ActorHub hub = new ActorHub(2, 4, 1_000);
        try
        {
            Linkable stage = factory.create(hub);
            ((Consumer<Object>) stage).accept(1);
            // Races the worker's completion of the (possibly still in-flight)
            // message. Under a lost-flag bug the actor would stay open and
            // awaitTermination would hang until the deadline.
            stage.shutdown(true);
            long untilNanos = Saturated.saturatedAdd(System.nanoTime(),
                    TimeUnit.MILLISECONDS.toNanos(5_000));
            assertTrue(stage.awaitTerminationUntilNanos(untilNanos), label + ": stage did not terminate");
            assertTrue(stage.isIdle(), label + ": stage not idle after termination");
        }
        finally
        {
            hub.shutdown();
        }
    }
}
