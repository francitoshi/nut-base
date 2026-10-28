/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.gauge;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class LongGaugeProgressTest
{
    
    public LongGaugeProgressTest()
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
     * Test of setPrefix method, of class LongGaugeProgress.
     */
    @Test
    public void testSetPrefix()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        instance.setPrefix("prefix");
        assertEquals("prefix", instance.getPrefix());
    }

    /**
     * Test of setMax method, of class LongGaugeProgress.
     */
    @Test
    public void testSetMax()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        instance.setMax(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, instance.getMax());
        instance.setMax(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, instance.getMax());
    }

    /**
     * Test of getVal method, of class LongGaugeProgress.
     */
    @Test
    public void testGetVal()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        
        instance.setMax(Long.MAX_VALUE);
        instance.setVal(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, instance.getVal());
        
        instance.setVal(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, instance.getVal());
    }

    /**
     * Test of step method, of class LongGaugeProgress.
     */
    @Test
    public void testStep_long()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        instance.start(Integer.MAX_VALUE+1L);
        instance.step(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, instance.getVal());
        instance.step(1);
        assertEquals(Integer.MAX_VALUE+1L, instance.getVal());
    }

    /**
     * Test of start method, of class LongGaugeProgress.
     */
    @Test
    public void testStart_long()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        instance.start(Integer.MAX_VALUE+1L);
        assertEquals(Integer.MAX_VALUE+1L, instance.getMax());
    }

    /**
     * Test of isStarted method, of class LongGaugeProgress.
     */
    @Test
    public void testIsStarted()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        assertEquals(false, instance.isStarted());
        instance.start();
        assertEquals(true, instance.isStarted());
    }

    /**
     * Test of step method, of class LongGaugeProgress.
     */
    @Test
    public void testStep_0args()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        instance.start(Integer.MAX_VALUE+1L);
        instance.setVal(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, instance.getVal());
        instance.step();
        assertEquals(Integer.MAX_VALUE+1L, instance.getVal());
    }

    /**
     * Test of setVal method, of class LongGaugeProgress.
     */
    @Test
    public void testSetVal()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        instance.start(Integer.MAX_VALUE+1L);
        instance.setVal(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, instance.getVal());
        instance.setVal(Integer.MAX_VALUE+1L);
        assertEquals(Integer.MAX_VALUE+1L, instance.getVal());
    }

    /**
     * Test of getMax method, of class LongGaugeProgress.
     */
    @Test
    public void testGetMax()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        instance.start(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, instance.getMax());
        instance.setMax(Integer.MAX_VALUE+1L);
        assertEquals(Integer.MAX_VALUE+1L, instance.getMax());
    }

    /**
     * Test of start method, of class LongGaugeProgress.
     */
    @Test
    public void testStart_0args()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        assertEquals(false, instance.isStarted());
        instance.start();
        assertEquals(true, instance.isStarted());
    }

    /**
     * Test of getPrefix method, of class LongGaugeProgress.
     */
    @Test
    public void testGetPrefix()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        assertEquals("", instance.getPrefix());
        instance.setPrefix("prefix");
        assertEquals("prefix", instance.getPrefix());
    }

    /**
     * Test of start method, of class LongGaugeProgress.
     */
    @Test
    public void testStart_long_String()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        instance.start(Long.MAX_VALUE, "prefix");
        assertEquals(true, instance.isStarted());
        assertEquals(Long.MAX_VALUE, instance.getMax());
        assertEquals("prefix", instance.getPrefix());
    }

    /**
     * Test of getDone method, of class LongGaugeProgress.
     */
    @Test
    public void testGetDone()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        instance.start(Integer.MAX_VALUE+1L);
        assertEquals(0.0, instance.getDone(),0.000001);
        instance.setVal(Integer.MAX_VALUE+1L);
        assertEquals(1.0, instance.getDone(),0.000001);
    }

    /**
     * Test of close method, of class LongGaugeProgress.
     */
    @Test
    public void testClose()
    {
        LongGaugeProgress instance = new LongGaugeProgress(new ProxyGaugeProgress());
        instance.start();
        assertEquals(true, instance.isStarted());
        instance.close();
        assertEquals(false, instance.isStarted());
    }
}
