/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import java.util.ArrayDeque;
import java.util.Queue;

public class SimpleMovingAverage extends MovingAverage 
{
    private long count;
    
    private double sum = 0.0;
    private double sma;
    private final Queue<Double> queue;
    
    public SimpleMovingAverage(int period)
    {
        super(period);
        this.queue = new ArrayDeque<>(period);
    }

    @Override
    public double next(double value)
    {
        if(count++ < period)
        {
            sum += value;
            sma = sum/count;
        }
        else
        {
            sum = sum + value - queue.remove();
            sma = sum / period;
        }
        this.queue.add(value);
        return sma;
    }
    
    @Override
    public double average()
    {
        return sma;
    }
}
