/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.profile;

import io.nut.base.time.JavaTime;
import io.nut.base.util.Utils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ProfilerTest
{
    /**
     * Test of duration method, of class Profiler.
     */
    @Test
    public void testDuration()
    {
        assertEquals("0s", Profiler.duration(0));
        assertEquals("1m", Profiler.duration(60_000_000_000L));
        assertEquals("1m1s", Profiler.duration(61_000_000_000L));
        assertEquals("1m", Profiler.duration(60_999_999_999L));
        assertEquals("1m5s", Profiler.duration(65_100_200_300L));
        assertEquals("1m25s", Profiler.duration(85_000_000_000L));
        assertEquals("1m39s", Profiler.duration(99_900_900_000L));
    }

    @Test
    public void testExample1()
    {
        Profiler profiler = new Profiler(JavaTime.Resolution.MS);
        
        Profiler.Task a = profiler.getTask("a");
        
        a.start();
        Utils.sleep(10);
        a.stop();
        a.count();

        a.start();
        Utils.sleep(5);
        a.stop();
        a.count();
        
        profiler.print();
        
        Profiler.Task b = profiler.getTask("b");
        
        b.start();
        Utils.sleep(15);
        b.stop();
        b.count();
        
        profiler.print();
    }

}
