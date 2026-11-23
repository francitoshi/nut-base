/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import io.github.francitoshi.nut.base.commons.concurrent.Threads;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class ActorPoolVirtualTest
{
    @Test
    void allowVirtualCreatesVirtualWorkerThreads() throws Exception
    {
        assumeTrue(Threads.isVirtualAvailable());
        try (ActorPool pool = new ActorPool(1, 2, 1000, false, false, false, true))
        {
            AtomicReference<Thread> worker = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            pool.execute(() ->
            {
                worker.set(Thread.currentThread());
                done.countDown();
            });
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertTrue(worker.get().getName().startsWith("virtual-"), "the worker must be a virtual thread, was " + worker.get().getName());
            assertTrue(worker.get().isDaemon(), "virtual threads are always daemon");
        }
    }

    @Test
    void defaultModeKeepsPlatformWorkers() throws Exception
    {
        try (ActorPool pool = new ActorPool(1, 2, 1000, false, false, false))
        {
            AtomicReference<Thread> worker = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            pool.execute(() ->
            {
                worker.set(Thread.currentThread());
                done.countDown();
            });
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertFalse(worker.get().getName().startsWith("virtual-"),
                    "platform mode must not create virtual threads");
        }
    }

    @Test
    void virtualModeRespectsDaemonIgnoredAndPoolSizing() throws Exception
    {
        assumeTrue(Threads.isVirtualAvailable());
        // daemon=false is ignored when the workers are virtual: they are
        // always daemon
        try (ActorPool pool = new ActorPool(1, 2, 1000, false, false, false, true))
        {
            AtomicReference<Boolean> daemonSeen = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            pool.execute(() ->
            {
                daemonSeen.set(Thread.currentThread().isDaemon());
                done.countDown();
            });
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertEquals(Boolean.TRUE, daemonSeen.get(), "virtual workers are always daemon");
            assertEquals(1, pool.getCorePoolSize());
            assertEquals(2, pool.getMaximumPoolSize());
        }
    }

    @Test
    void virtualActorsProcessMessagesEndToEnd() throws Exception
    {
        assumeTrue(Threads.isVirtualAvailable());
        try (ActorHub hub = ActorHub.hub(1, 2, 1000, false, false, true))
        {
            int messages = 1000;
            CountDownLatch done = new CountDownLatch(messages);
            Actor<Integer> sink = hub.actor(2, 0, m -> done.countDown());
            for (int i = 0; i < messages; i++)
            {
                sink.accept(i);
            }
            assertTrue(done.await(5, TimeUnit.SECONDS), "all messages must be processed");
        }
    }

    @Test
    void synchronousModeIgnoresAllowVirtual() throws Exception
    {
        try (ActorPool pool = new ActorPool(0, 0, 0, false, false, false, true))
        {
            assertTrue(pool.isSynchronous());
            AtomicReference<Thread> runner = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);
            pool.execute(() ->
            {
                runner.set(Thread.currentThread());
                done.countDown();
            });
            assertTrue(done.await(5, TimeUnit.SECONDS));
            // synchronous mode runs the task in the calling thread: never virtual
            assertFalse(runner.get().getName().startsWith("virtual-"));
        }
    }
}
