/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.gauge;

import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class PrintStreamGaugeTest
{
    
    public PrintStreamGaugeTest()
    {
    }

    @Test
    public void testPaint() throws InterruptedException
    {
        PrintStreamGauge gauge = new PrintStreamGauge();
        int max = 200;
        
        gauge.setDebug(true);
        gauge.setShowPrev(true);
        gauge.setShowNext(true);
        gauge.setShowFull(true);
        gauge.start(max);
        
        for(int i=0;i<max;i++)
        {
            gauge.println(Integer.toString(i, 26)+" completed");
            gauge.setPrefix("["+i+"]");
            gauge.setVal(i);
            Thread.sleep(10);
        }
        gauge.close();
    }
    
}
