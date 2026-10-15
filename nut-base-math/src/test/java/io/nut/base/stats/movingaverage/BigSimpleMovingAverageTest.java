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
public class BigSimpleMovingAverageTest
{
    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext1()
    {
        int[] data = {20, 22, 24, 25, 23, 26, 28, 26, 29, 27, 28, 30, 27, 29, 28};
        
        BigDecimal sma = null;
        BigMovingAverage instance = BigMovingAverage.createSMA(15, 2, RoundingMode.HALF_UP);
        
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(BigDecimal.valueOf(data[i]));
        }
        assertEquals(new BigDecimal("26.13"), sma);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(new BigDecimal("26.13"), sma);
    }
    
    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext2()
    {
        int[] data = {10, 12, 9, 10, 15, 13, 18, 18, 20, 24};
        
        BigDecimal sma = null;
        
        BigMovingAverage instance = BigMovingAverage.createSMA(5, 2, RoundingMode.HALF_UP);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(new BigDecimal("18.60"), sma);
        
        instance = BigMovingAverage.createSMA(10, 2, RoundingMode.HALF_UP);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(new BigDecimal("14.90"), sma);
    }

    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext3()
    {
        int[] data = {1, 2, 3, 7, 9};
        
        BigDecimal sma;
        
        BigMovingAverage instance = BigMovingAverage.createSMA(3, 2, RoundingMode.HALF_UP);
        int index=0;
        instance.next(data[index++]);
        instance.next(data[index++]);
        sma = instance.next(data[index++]);

        assertEquals(new BigDecimal("2").stripTrailingZeros(), sma.stripTrailingZeros());
        
        sma = instance.next(data[index++]);

        assertEquals(new BigDecimal("4").stripTrailingZeros(), sma.stripTrailingZeros());
        sma = instance.next(data[index++]);
        
        assertEquals(new BigDecimal("6.33").stripTrailingZeros(), sma.stripTrailingZeros());
    }   
}
