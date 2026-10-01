/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.logging;

import io.nut.base.logging.Log.FormatType;
import io.nut.base.profile.Profiler;
import io.nut.base.time.JavaTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class LogTest
{
    
    static final Profiler profiler = new Profiler(JavaTime.Resolution.MS);
    
    public LogTest()
    {
    }

    static final int LOOPS = 5;

    @AfterAll
    public static void tearDownClass() throws Exception
    {
        profiler.print();
    }

    /**
     * Test of of method, of class Log.
     */
    @Test
    @Order(1)
    public void testNop()
    {
        Log.setNopBuilder();
        
        Log log = Log.of(LogTest.class);

        Profiler.Task t0 = profiler.getTask("t0").start();
        for(int i=0;i<LOOPS;i++)
        {
            log.debug("i=%d",i);
        }
        t0.stop().count();
    }

    /**
     * Test of of method, of class Log.
     */
    @Test
    @Order(2)
    public void testJul()
    {
        Log.setJulBuilder(true, Log.ALL, JulLog.getConsoleHandler(Log.ALL, FormatType.DT_LEV_MSG));
        
        Log log = Log.of(LogTest.class);

        Profiler.Task t1 = profiler.getTask("t1").start();
        for(int i=0;i<LOOPS;i++)
        {
            log.error("i=%d",i);
        }
        t1.stop().count();

        log.error("i=%d",1);
        log.error("{0}");
        log.error("' %d", 1,2);

    }

    /**
     * Test of of method, of class Log.
     */
    @Test
    @Order(3)
    public void testJul2()
    {
        Logger log = Logger.getLogger(LogTest.class.getName());
        //log.setLevel(Level.SEVERE);
        Profiler.Task t2 = profiler.getTask("t2").start();
        for(int i=0;i<LOOPS;i++)
        {
            log.log(Level.FINE, "i=%d",i);
        }
        t2.stop().count();
    }
}
