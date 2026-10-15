/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats.movingaverage;

import io.nut.base.stats.movingaverage.BigMovingAverage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class BigExponentialMovingAverageTest
{
    /**
     * Test of next method, of class ExponentialMovingAverage.
     */
    @Test
    public void testNext1()
    {
        int[] data = {12, 14, 16, 15, 18, 20, 22, 21, 23, 25};
        double[] exp = {12, 13, 14.5, 14.75, 16.375, 18.1875, 20.09375, 20.546875, 21.773437, 23.386719};

        BigMovingAverage instance = BigMovingAverage.createEMA(3, 7, RoundingMode.HALF_UP);
        
        for(int i=0;i<data.length;i++)
        {
            BigDecimal sma = instance.next(data[i]);
            assertEquals(exp[i], sma.doubleValue(), 0.0000005);
        }
    }
    /**
     * Test of next method, of class ExponentialMovingAverage.
     */
    @Test
    public void testNext2()
    {
        int[] data = { 10, 12, 15, 14, 13, 11, 12, 13, 14, 15};
        double[] exp = {10.0000, 11.0000, 13.0000, 13.5000, 13.2500, 12.1250, 12.0625, 12.5313, 13.2656, 14.1328};

        BigMovingAverage instance = BigMovingAverage.createEMA(3, 4, RoundingMode.HALF_UP);
        
        for(int i=0;i<data.length;i++)
        {
            BigDecimal sma = instance.next(data[i]);
            assertEquals(exp[i], sma.doubleValue(), 0.000005);
        }
    }
    
}
