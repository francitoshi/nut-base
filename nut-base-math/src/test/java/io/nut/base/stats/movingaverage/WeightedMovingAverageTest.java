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
public class WeightedMovingAverageTest
{ 
    /**
     * Test of next method, of class ExponentialMovingAverage.
     * data from https://learn.bybit.com/es/indicators/what-is-weighted-moving-average-wma/
     * there is an error in the 2nd ponderation value in the web, this test it is fixed
     */
    @Test
    public void testNext1()
    {
        int[] data = {23912, 22698, 22750, 24854, 25649};

        MovingAverage instance = MovingAverage.createWMA(5);
        double sma=0;
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(24347.93, sma, 0.005);
    }
    
    /**
     * Test of next method, of class ExponentialMovingAverage.
     * data from https://www.earn2trade.com/blog/es/media-movil-ponderada/
     * 
     */
    @Test
    public void testNext2()
    {
        double[] data = {50.25, 56.39, 58.91, 61.52, 59.32, 55.43, 54.65};

        MovingAverage instance = MovingAverage.createWMA(7);
        double sma=0;
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(57.06, sma, 0.005);
    }
   
}
