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

        ActorPool actorPool = new ActorPool(4, 4, 1000, false);
        assertEquals(4, actorPool.getCorePoolSize());
        assertEquals(4, actorPool.getMaximumPoolSize());
    }

    @Test
    public void synchronousActorPoolIsExemptFromValidation()
    {
        ActorPool actorPool = new ActorPool(0, 0, 0, false);
        assertTrue(actorPool.isSynchronous());
        assertEquals(0, actorPool.getCorePoolSize());
        assertEquals(0, actorPool.getMaximumPoolSize());
    }

    @Test
    public void coreThreadsTimeOutBackToZero()
    {
        try (ActorPool actorPool = new ActorPool(4, 4, 50, false))
        {
            // the pool is sized to a single corePoolSize value reused as maximum
            assertEquals(4, actorPool.getCorePoolSize());
            assertEquals(4, actorPool.getMaximumPoolSize());

            AtomicInteger count = new AtomicInteger();
            for (int i = 0; i < 4; i++)
            {
                actorPool.spawn(count::incrementAndGet);
            }
            assertEquals(4, count.get());

            // idle core threads (with allowCoreThreadTimeOut) should time out
            // and die, so the pool eventually reports zero active work.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (actorPool.getActiveCount() != 0 && System.nanoTime() < deadline)
            {
                Utils.parkMillis(10);
            }
            assertEquals(0, actorPool.getActiveCount());
        }
    }
    
}
