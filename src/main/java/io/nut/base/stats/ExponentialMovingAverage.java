/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

public class ExponentialMovingAverage extends MovingAverage 
{
    private long count;
    private final double alpha;  // The smoothing factor
    private final double oneMinusAlpha;
    private double ema;          // Stores the current EMA value
  
    public ExponentialMovingAverage(int period)
    {
        super(period);
        this.alpha = 2.0 / (period + 1.0);
        this.oneMinusAlpha = 1 - alpha;
    }

    @Override
    public double next(double value)
    {
        return ema = (count++ == 0) ? value : value*alpha + ema*oneMinusAlpha;
    }

    @Override
    public double average()
    {
        return ema;
    }
    
}
