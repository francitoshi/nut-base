/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.debug;

import io.github.francitoshi.nut.base.commons.lang.Pauses;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class TimingTest
{
    /**
     * Test of getInstance method, of class Timing.
     */
    @Test
    public void testGetSecondsInstance_int()
    {
        Timing timing = Timing.getSecondsInstance(2);
        long t1 = System.nanoTime();
        Pauses.sleepMillis(1);
        long t2 = System.nanoTime();
        Pauses.sleepMillis(1000);
        long t3 = System.nanoTime();
        
        timing.println("getSecondsInstance", t1, t2);
        timing.println("getSecondsInstance", t2, t3);
        
    }
    
    /**
     * Test of getInstance method, of class Timing.
     */
    @Test
    public void testGetMillisInstance_int()
    {
        Timing timing = Timing.getMillisInstance(2);
        long t1 = System.nanoTime();
        Pauses.sleepMillis(9);
        long t2 = System.nanoTime();
        Pauses.sleepMillis(99);
        long t3 = System.nanoTime();
        
        timing.println("getMillisInstance", t1, t2);
        timing.println("getMillisInstance", t2, t3);
        
    }
    
    /**
     * Test of getNanosInstance method, of class Timing.
     */
    @Test
    public void testGetNanosInstance_int()
    {
        Timing timing = Timing.getNanosInstance(2);
        long t1 = System.nanoTime();
        Pauses.sleepMillis(1);
        long t2 = System.nanoTime();
        Pauses.sleepMillis(1);
        long t3 = System.nanoTime();
        
        timing.println("getNanosInstance", t1, t2);
        timing.println("getNanosInstance", t2, t3);
    }

}
