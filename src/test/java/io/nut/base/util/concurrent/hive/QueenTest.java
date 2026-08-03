/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import io.nut.base.util.Utils;
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
    
}
