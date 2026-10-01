/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

public class CumulativeMovingAverage extends MovingAverage
{
    private long count;
    private double cma;
    
    public CumulativeMovingAverage()
    {
        this.cma = 0.0;
    }

    @Override
    public double next(double value)
    {
        
        return cma = cma + (value - cma) / ++count;
    }

    @Override
    public double average()
    {
        return cma;
    }
    
}
