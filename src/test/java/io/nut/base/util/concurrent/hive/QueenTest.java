/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

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
public class QueenTest
{
    
    /**
     * Test of queen method, of class Queen.
     */
    @Test
    public void testTryAutoClose()
    {
        AtomicInteger count = new AtomicInteger();
        
        try(Queen queen = Queen.queen(4))
        {
            queen.spawn(()-> count.incrementAndGet())
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
    public void zeroSizedQueenRunsSynchronously()
    {
        Queen queen = new Queen(0, 0, 0);
        assertTrue(queen.isSynchronous());
        assertEquals(0, queen.getCorePoolSize());
        assertEquals(0, queen.getMaximumPoolSize());

        String[] holder = {"not-run"};
        queen.execute(() -> holder[0] = "executed");
        assertEquals("executed", holder[0]);

        java.util.function.Supplier<String> supplier = () -> "submitted";
        try
        {
            assertEquals("submitted", queen.submit(supplier).get());
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

        AtomicInteger spawned = new AtomicInteger();
        queen.spawn(spawned::incrementAndGet);
        assertEquals(1, spawned.get());

        List<Integer> src = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Integer> out = new ArrayList<>();
        queen.forEach(src, out::add);
        assertEquals(Arrays.asList(1, 2, 3), out);

        try (Queen sync = new Queen(0, 0, 0))
        {
            assertTrue(sync.isSynchronous());
            sync.submit(() -> {});
        }
    }

    @Test
    public void constructorValidatesThreadPoolConstraints()
    {
        assertThrows(IllegalArgumentException.class, () -> new Queen(-1, 4, 1000, false));
        assertThrows(IllegalArgumentException.class, () -> new Queen(4, 4, -1, false));
        assertThrows(IllegalArgumentException.class, () -> new Queen(-5, 4, 1000, false));

        Queen queen = new Queen(4, 4, 1000, false);
        assertEquals(4, queen.getCorePoolSize());
        assertEquals(4, queen.getMaximumPoolSize());
    }

    @Test
    public void synchronousQueenIsExemptFromValidation()
    {
        Queen queen = new Queen(0, 0, 0, false);
        assertTrue(queen.isSynchronous());
        assertEquals(0, queen.getCorePoolSize());
        assertEquals(0, queen.getMaximumPoolSize());
    }

    @Test
    public void coreThreadsTimeOutBackToZero()
    {
        try (Queen queen = new Queen(4, 4, 50, false))
        {
            // the pool is sized to a single corePoolSize value reused as maximum
            assertEquals(4, queen.getCorePoolSize());
            assertEquals(4, queen.getMaximumPoolSize());

            AtomicInteger count = new AtomicInteger();
            for (int i = 0; i < 4; i++)
            {
                queen.spawn(count::incrementAndGet);
            }
            assertEquals(4, count.get());

            // idle core threads (with allowCoreThreadTimeOut) should time out
            // and die, so the pool eventually reports zero active work.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (queen.getActiveCount() != 0 && System.nanoTime() < deadline)
            {
                Utils.parkMillis(10);
            }
            assertEquals(0, queen.getActiveCount());
        }
    }
    
}
