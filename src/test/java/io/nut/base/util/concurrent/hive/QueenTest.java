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
        Queen queen = new Queen(0, 0, 0, 0);
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

        try (Queen sync = new Queen(0, 0, 0, 0))
        {
            assertTrue(sync.isSynchronous());
            sync.submit(() -> {});
        }
    }
    
}
