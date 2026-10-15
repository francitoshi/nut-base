/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats.movingaverage;

import io.nut.base.stats.movingaverage.MovingAverage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class SimpleMovingAverageTest
{
    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext1()
    {
        int[] data = {20, 22, 24, 25, 23, 26, 28, 26, 29, 27, 28, 30, 27, 29, 28};
        
        double sma = 0;
        MovingAverage instance = MovingAverage.createSMA(15);
        
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(26.13, sma, 0.005);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(26.13, sma, 0.005);
    }
    
    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext2()
    {
        int[] data = {10, 12, 9, 10, 15, 13, 18, 18, 20, 24};
        
        double sma = 0;
        
        MovingAverage instance = MovingAverage.createSMA(5);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(18.60, sma, 0.005);
        
        instance = MovingAverage.createSMA(10);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(14.90, sma, 0.005);
    }

    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext3()
    {
        int[] data = {1, 2, 3, 7, 9};
        
        double sma;
        
        MovingAverage instance = MovingAverage.createSMA(3);
        int index=0;
        instance.next(data[index++]);
        instance.next(data[index++]);
        sma = instance.next(data[index++]);

        assertEquals(2, sma, 0.005);
        
        sma = instance.next(data[index++]);

        assertEquals(4, sma, 0.005);
        sma = instance.next(data[index++]);
        
        assertEquals(6.33, sma, 0.005);
    }   
}
