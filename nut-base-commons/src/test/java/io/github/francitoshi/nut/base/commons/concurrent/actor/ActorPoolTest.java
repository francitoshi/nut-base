/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ActorPoolTest
{
    
    /**
     * Test of actorPool method, of class ActorPool.
     */
    @Test
    public void testTryAutoClose() throws InterruptedException
    {
        AtomicInteger count = new AtomicInteger();
        
        try(ActorPool actorPool = ActorPool.actorPool(4))
        {
            actorPool.spawn(()-> count.incrementAndGet())
                 .spawn(()-> count.incrementAndGet())
                 .spawn(()-> count.incrementAndGet())
                 .spawn(()-> count.incrementAndGet())
                 .spawn(()-> count.incrementAndGet())
                 .spawn(()-> count.incrementAndGet())
                 .spawn(()-> count.incrementAndGet())
                 .spawn(()-> count.incrementAndGet())
                 .spawn(()-> count.incrementAndGet())
                 .spawn(()-> count.incrementAndGet());
            Thread.sleep(10);
        }
        assertEquals(10, count.get());
    }

    @Test
    public void zeroSizedActorPoolRunsSynchronously()
    {
        ActorPool actorPool = new ActorPool(0, 0, 0);
        assertTrue(actorPool.isSynchronous());
        assertEquals(0, actorPool.getCorePoolSize());
        assertEquals(0, actorPool.getMaximumPoolSize());

        String[] holder = {"not-run"};
        actorPool.execute(() -> holder[0] = "executed");
        assertEquals("executed", holder[0]);

        java.util.function.Supplier<String> supplier = () -> "submitted";
        try
        {
            assertEquals("submitted", actorPool.submit(supplier).get());
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

        AtomicInteger spawned = new AtomicInteger();
        actorPool.spawn(spawned::incrementAndGet);
        assertEquals(1, spawned.get());

        List<Integer> src = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Integer> out = new ArrayList<>();
        actorPool.forEach(src, out::add);
        assertEquals(Arrays.asList(1, 2, 3), out);

        try (ActorPool sync = new ActorPool(0, 0, 0))
        {
            assertTrue(sync.isSynchronous());
            sync.submit(() -> {});
        }
    }

    @Test
    public void forEachTasksAreTrackedUntilTheyComplete() throws Exception
    {
        try (ActorPool actorPool = new ActorPool(2, 2, 1000, false))
        {
            CountDownLatch started = new CountDownLatch(2);
            CountDownLatch release = new CountDownLatch(1);
            List<Integer> items = Arrays.asList(1, 2);

            Thread forEach = new Thread(() -> actorPool.forEach(items, item ->
            {
                started.countDown();
                try
                {
                    release.await();
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
            }), "forEach");
            forEach.setDaemon(true);
            forEach.start();

            Thread waiter = new Thread(actorPool::waitForIdle, "wait-for-idle");
            waiter.setDaemon(true);
            try
            {
                assertTrue(started.await(2, TimeUnit.SECONDS), "forEach tasks did not start");

                waiter.start();
                waiter.join(250);

                assertTrue(waiter.isAlive(), "waitForIdle() returned while forEach tasks were still running");
                assertFalse(actorPool.isIdle(), "isIdle() must be false while forEach tasks run");
            }
            finally
            {
                release.countDown();
            }

            forEach.join(2000);
            assertFalse(forEach.isAlive(), "forEach did not finish after releasing its tasks");
            waiter.join(2000);
            assertFalse(waiter.isAlive(), "waitForIdle() did not return after forEach tasks completed");
            assertTrue(actorPool.isIdle());
        }
    }

    @Test
    public void spawnWaitingForAThreadIsVisibleToWaitForIdle() throws Exception
    {
        // callerWaitsPolicy parks the spawning thread instead of running the
        // task inline, which is exactly the window waitForIdle() must cover.
        try (ActorPool actorPool = new ActorPool(1, 1, 1000, true))
        {
            CountDownLatch blockerStarted = new CountDownLatch(1);
            CountDownLatch releaseBlocker = new CountDownLatch(1);
            actorPool.execute(() ->
            {
                blockerStarted.countDown();
                try
                {
                    releaseBlocker.await();
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(blockerStarted.await(2, TimeUnit.SECONDS), "the blocking task did not start");

            CountDownLatch spawnedRan = new CountDownLatch(1);
            Thread spawner = new Thread(() -> actorPool.spawn(spawnedRan::countDown), "spawner");
            spawner.setDaemon(true);
            spawner.start();

            Thread waiter = new Thread(actorPool::waitForIdle, "wait-for-idle");
            waiter.setDaemon(true);
            try
            {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                while (actorPool.getActiveCount() < 2 && System.nanoTime() < deadline)
                {
                    Thread.sleep(5);
                }
                assertTrue(actorPool.getActiveCount() >= 2, "spawn() was not registered while waiting for a thread");

                waiter.start();
                waiter.join(250);
                assertTrue(waiter.isAlive(), "waitForIdle() ignored a spawn() still waiting for a free thread");
            }
            finally
            {
                releaseBlocker.countDown();
            }

            spawner.join(2000);
            assertFalse(spawner.isAlive(), "spawn() did not return after a thread became available");
            assertTrue(spawnedRan.await(2, TimeUnit.SECONDS), "the spawned task did not run");
            waiter.join(2000);
            assertFalse(waiter.isAlive(), "waitForIdle() did not return after the spawned task completed");
            assertTrue(actorPool.isIdle());
        }
    }

    @Test
    public void constructorValidatesThreadPoolConstraints()
    {
        assertThrows(IllegalArgumentException.class, () -> new ActorPool(-1, 4, 1000, false));
        assertThrows(IllegalArgumentException.class, () -> new ActorPool(4, 4, -1, false));
        assertThrows(IllegalArgumentException.class, () -> new ActorPool(-5, 4, 1000, false));
        assertThrows(IllegalArgumentException.class, () -> new ActorPool(4, 2, 1000, false));

        ActorPool actorPool = new ActorPool(4, 4, 1000, false);
        assertEquals(4, actorPool.getCoreThreads());
        assertEquals(4, actorPool.getMaxThreads());
        assertEquals(4, actorPool.getCorePoolSize());
        assertEquals(4, actorPool.getMaximumPoolSize());
    }

    @Test
    public void setThreadsResizesThePoolOrderIndependently()
    {
        try (ActorPool actorPool = new ActorPool(2, 4, 1000, false))
        {
            assertEquals(2, actorPool.getCoreThreads());
            assertEquals(4, actorPool.getMaxThreads());
            assertEquals(2, actorPool.getCorePoolSize());
            assertEquals(4, actorPool.getMaximumPoolSize());

            // grow: a single call, no need to raise the maximum first
            actorPool.setThreads(8, 16);
            assertEquals(8, actorPool.getCoreThreads());
            assertEquals(16, actorPool.getMaxThreads());
            assertEquals(8, actorPool.getCorePoolSize());
            assertEquals(16, actorPool.getMaximumPoolSize());

            // shrink: a single call, no need to lower the core first
            actorPool.setThreads(1, 2);
            assertEquals(1, actorPool.getCoreThreads());
            assertEquals(2, actorPool.getMaxThreads());
            assertEquals(1, actorPool.getCorePoolSize());
            assertEquals(2, actorPool.getMaximumPoolSize());

            // a floor above the ceiling raises the ceiling to match
            actorPool.setThreads(6, 3);
            assertEquals(6, actorPool.getCoreThreads());
            assertEquals(6, actorPool.getMaxThreads());
            assertEquals(6, actorPool.getCorePoolSize());
            assertEquals(6, actorPool.getMaximumPoolSize());

            assertThrows(IllegalArgumentException.class, () -> actorPool.setThreads(-1, 4));
            assertThrows(IllegalArgumentException.class, () -> actorPool.setThreads(2, -1));
        }
    }

    @Test
    public void setThreadsIsANoOpOnSynchronousPools()
    {
        try (ActorPool sync = new ActorPool(0, 0, 0))
        {
            sync.setThreads(4, 8);
            assertTrue(sync.isSynchronous());
            assertEquals(0, sync.getCoreThreads());
            assertEquals(0, sync.getMaxThreads());
            assertEquals(0, sync.getCorePoolSize());
            assertEquals(0, sync.getMaximumPoolSize());
        }
    }

    @Test
    public void synchronousActorPoolIsExemptFromValidation()
    {
        ActorPool actorPool = new ActorPool(0, 0, 0, false);
        assertTrue(actorPool.isSynchronous());
        assertEquals(0, actorPool.getCoreThreads());
        assertEquals(0, actorPool.getMaxThreads());
        assertEquals(0, actorPool.getCorePoolSize());
        assertEquals(0, actorPool.getMaximumPoolSize());
    }

    @Test
    public void coreThreadsStayAlivePastIdleTimeout() throws InterruptedException
    {
        try (ActorPool actorPool = new ActorPool(2, 8, 50, false))
        {
            // the core floor stays permanently alive: idle threads above the
            // floor are reclaimed, but the pool never drops below coreThreads
            assertEquals(2, actorPool.getCoreThreads());
            assertEquals(8, actorPool.getMaxThreads());
            assertEquals(2, actorPool.getCorePoolSize());
            assertEquals(8, actorPool.getMaximumPoolSize());

            AtomicInteger count = new AtomicInteger();
            for (int i = 0; i < 4; i++)
            {
                actorPool.spawn(count::incrementAndGet);
            }
            // spawn returns once each worker is on the starting line, so the
            // last increment may not have executed yet; give it a moment
            long countDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (count.get() < 4 && System.nanoTime() < countDeadline)
            {
                Thread.sleep(5);
            }
            assertEquals(4, count.get());

            // wait for the pool to drain and for the keep-alive window to pass
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (actorPool.getActiveCount() != 0 && System.nanoTime() < deadline)
            {
                Thread.sleep(10);
            }
            Thread.sleep(100);

            assertTrue(actorPool.getPoolSize() >= 2, "live threads dropped below the core floor: " + actorPool.getPoolSize());
        }
    }

    @Test
    public void excessThreadsGrowOnDemandAndShrinkBackToCore() throws InterruptedException
    {
        try (ActorPool actorPool = new ActorPool(1, 4, 50, false))
        {
            // with no task queue the pool grows past the core floor on demand,
            // up to maxThreads, and the overflow threads die back to the floor
            CountDownLatch go = new CountDownLatch(4);
            CountDownLatch done = new CountDownLatch(4);
            for (int i = 0; i < 4; i++)
            {
                actorPool.execute(() ->
                {
                    try
                    {
                        go.countDown();
                        if (!go.await(5, TimeUnit.SECONDS))
                        {
                            return;
                        }
                    }
                    catch (InterruptedException ex)
                    {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    finally
                    {
                        done.countDown();
                    }
                });
            }
            assertTrue(done.await(5, TimeUnit.SECONDS));

            // once drained and idle past the keep-alive, threads shrink back
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (actorPool.getPoolSize() >= 4 && System.nanoTime() < deadline)
            {
                Thread.sleep(20);
            }
            assertTrue(actorPool.getPoolSize() < 4,
                    "excess threads were not reclaimed: " + actorPool.getPoolSize());
            assertTrue(actorPool.getPoolSize() >= 1,
                    "live threads dropped below the core floor: " + actorPool.getPoolSize());
        }
    }

    @Test
    public void singleIntConstructorIsElastic()
    {
        try (ActorPool actorPool = new ActorPool(2))
        {
            // hub(n): coreThreads = n, maxThreads = 2 * n
            assertEquals(2, actorPool.getCoreThreads());
            assertEquals(4, actorPool.getMaxThreads());
        }
    }

    @Test
    public void daemonFlagMakesWorkerThreadsDaemon()
    {
        assertEquals(false, ActorPool.DEFAULT_DAEMON);
        assertPoolThreadDaemon(false);
        assertPoolThreadDaemon(true);
    }

    private void assertPoolThreadDaemon(boolean daemon)
    {
        CountDownLatch running = new CountDownLatch(1);
        AtomicInteger captured = new AtomicInteger();
        try (ActorPool actorPool = new ActorPool(1, 1, 1000, false, false, daemon))
        {
            actorPool.execute(() ->
            {
                captured.set(Thread.currentThread().isDaemon() ? 1 : 0);
                running.countDown();
            });
            try
            {
                assertTrue(running.await(5, TimeUnit.SECONDS));
                assertEquals(daemon, captured.get() == 1);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                fail("interrupted while waiting for the pool thread");
            }
        }
    }
    
}
