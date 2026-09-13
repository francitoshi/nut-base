/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

public class DoubleExponentialMovingAverage extends MovingAverage 
{
    // We use Composition here: DEMA relies on two internal EMA calculations.
    private final ExponentialMovingAverage ema1;
    private final ExponentialMovingAverage ema2;
    
    // We must store the current calculation to return it in average()
    private double dema;

    public DoubleExponentialMovingAverage(int period)
    {
        // The DEMA uses the same period for both internal smoothing steps.
        super(period);
        this.ema1 = new ExponentialMovingAverage(period);
        this.ema2 = new ExponentialMovingAverage(period);
    }

    @Override
    public double next(double value)
    {
        // 1. Calculate EMA of the input value
        double e1 = ema1.next(value);
        
        // 2. Calculate EMA of the EMA (Smoothing the smoothed value)
        double e2 = ema2.next(e1);
        
        // 3. DEMA Formula: (2 * EMA1) - EMA2
        return dema = (2.0 * e1) - e2;
    }

    @Override
    public double average()
    {
        return dema;
    }
}