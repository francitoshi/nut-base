/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class ThreadsTest
{
    @Test
    void isVirtualAvailableIsConsistentWithTheRunningJdk() throws Exception
    {
        boolean apiPresent = true;
        try
        {
            Class.forName("java.lang.Thread$Builder");
        }
        catch (ClassNotFoundException ex)
        {
            apiPresent = false;
        }
        assertEquals(apiPresent, Threads.isVirtualAvailable());
        // the holder computes it once: repeated calls return the same answer
        assertEquals(Threads.isVirtualAvailable(), Threads.isVirtualAvailable());
    }

    @Test
    void startThreadRejectsANullTask()
    {
        assertThrows(NullPointerException.class, () -> Threads.startThread(null, false, false));
        assertThrows(NullPointerException.class, () -> Threads.startThread(null, true, true));
    }

    @Test
    void startThreadRunsTheTaskOnAPlatformThread() throws Exception
    {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Thread> self = new AtomicReference<>();
        Thread thread = Threads.startThread(() ->
        {
            self.set(Thread.currentThread());
            done.countDown();
        }, false, false);

        assertTrue(thread.isAlive() || thread.getState() == Thread.State.TERMINATED);
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(thread, self.get());
    }

    @Test
    void platformThreadHonoursTheDaemonFlag() throws Exception
    {
        CountDownLatch release = new CountDownLatch(1);
        Thread daemon = Threads.startThread(() -> awaitSilently(release), true, false);
        assertTrue(daemon.isDaemon());

        release.countDown();
        daemon.join(5000);

        CountDownLatch release2 = new CountDownLatch(1);
        Thread nonDaemon = Threads.startThread(() -> awaitSilently(release2), false, false);
        assertFalse(nonDaemon.isDaemon());
        release2.countDown();
        nonDaemon.join(5000);
    }

    @Test
    void virtualThreadsRunTheTaskAndAreAlwaysDaemon() throws Exception
    {
        assumeTrue(Threads.isVirtualAvailable());
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Thread> self = new AtomicReference<>();

        // daemon=false is ignored for virtual threads: they are always daemon
        Thread thread = Threads.startThread(() ->
        {
            self.set(Thread.currentThread());
            done.countDown();
        }, false, true);

        assertTrue(thread.isDaemon(), "virtual threads are always daemon");
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(thread, self.get());
        thread.join(5000);
    }

    @Test
    void virtualFactoryProducesDaemonThreadsThatRun() throws Exception
    {
        assumeTrue(Threads.isVirtualAvailable());
        ThreadFactory factory = Threads.getThreadFactory(true);
        CountDownLatch done = new CountDownLatch(1);

        Thread thread = factory.newThread(done::countDown);
        assertNotNull(thread);
        assertTrue(thread.isAlive() || thread.getState() == Thread.State.NEW);
        thread.start();
        assertTrue(thread.isDaemon(), "virtual threads are always daemon");
        assertTrue(done.await(5, TimeUnit.SECONDS));
        thread.join(5000);
    }

    @Test
    void platformFactoryProducesStandardThreads()
    {
        ThreadFactory factory = Threads.getThreadFactory(false);
        Thread thread = factory.newThread(() ->
        {
        });
        assertNotNull(thread);
        assertFalse(thread.isDaemon());
    }

    @Test
    void factoryRejectsANullTask()
    {
        assertThrows(NullPointerException.class, () -> Threads.getThreadFactory(false).newThread(null));
    }

    @Test
    void allowVirtualDegradesToPlatformThreadsWhenUnavailable()
    {
        assumeTrue(!Threads.isVirtualAvailable());
        // when Loom is absent the daemon flag must reach the created platform thread
        Thread thread = Threads.startThread(() ->
        {
        }, true, true);
        assertTrue(thread.isDaemon());
    }

    private static void awaitSilently(CountDownLatch latch)
    {
        try
        {
            latch.await();
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }
}
