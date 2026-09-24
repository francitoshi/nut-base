/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import io.nut.base.util.Utils;
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
    public void testTryAutoClose()
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
            Utils.parkMillis(10);
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
    public void coreThreadsStayAlivePastIdleTimeout()
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
                Utils.parkMillis(5);
            }
            assertEquals(4, count.get());

            // wait for the pool to drain and for the keep-alive window to pass
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (actorPool.getActiveCount() != 0 && System.nanoTime() < deadline)
            {
                Utils.parkMillis(10);
            }
            Utils.parkMillis(100);

            assertTrue(actorPool.getPoolSize() >= 2,
                    "live threads dropped below the core floor: " + actorPool.getPoolSize());
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
                Utils.parkMillis(20);
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
