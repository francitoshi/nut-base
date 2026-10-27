/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.lang;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class PausesTest
{

    @Test
    public void testSleepMillis()
    {
        long t0 = System.nanoTime();
        Pauses.sleepMillis(16);
        long t1 = System.nanoTime();
        long diff = TimeUnit.NANOSECONDS.toMillis(t1-t0);
        
        assertTrue(diff>=16);
    }

    @Test
    public void testSleepNanos()
    {
        long t0 = System.nanoTime();
        Pauses.sleepNanos(123);
        long t1 = System.nanoTime();
        long diff = (t1-t0);
        
        assertTrue(diff>=123);
    }
    
    @Test
    public void testParkUntilNanoTime()
    {
        long now = System.nanoTime();
        long t[] = new long[6];
        t[0] = now + 1000L;
        t[1] = now + 1000_000L;
        t[2] = now + 100_000_000L;
        t[3] = now + 200_000_000L;
        t[4] = now + 300_000_000L;
        t[5] = now + 400_000_000L;
        
        for(int i=0;i<t.length;i++)
        {
            Pauses.parkUntilNanoTime(t[i]);
            assertTrue( System.nanoTime() > t[i]);
        }
        
    }

    @Test
    public void testParkNanos()
    {
        long t0 = System.nanoTime();
        Pauses.parkNanos(123);
        long t1 = System.nanoTime();
        long diff = (t1-t0);
        
        assertTrue(diff>=123);
    }

    @Test
    public void testParkMillis()
    {
        long t0 = System.nanoTime();
        Pauses.parkMillis(16);
        long t1 = System.nanoTime();
        long diff = TimeUnit.NANOSECONDS.toMillis(t1-t0);
        
        assertTrue(diff>=16);
    }

}
