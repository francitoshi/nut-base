/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.profile;

import io.nut.base.util.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class UptimeTimingTest
{
    
    public UptimeTimingTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
    }
    
    @AfterAll
    public static void tearDownClass()
    {
    }
    
    @BeforeEach
    public void setUp()
    {
    }
    
    @AfterEach
    public void tearDown()
    {
    }


    /**
     * Test of getUptime method, of class UptimeTiming.
     */
    @Test
    public void testGetRootInsance()
    {
        UptimeTiming result = UptimeTiming.getRootInstance();
        
        for(int i=0;i<1000;i++)
        {
            Utils.sleep(1);
            result.trace("one");
            Utils.sleep(2);
            result.trace("two");
        }
        Utils.sleep(1000);
        result.uptime();
    }
}
