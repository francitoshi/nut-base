/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import java.util.ArrayList;
import java.util.List;

public class WeightedMovingAverage extends MovingAverage
{

    private long count;
    private final List<Double> values;
    private double sum;
    private double wma;
    private double weightedSum;
    private double divisor;
    private double multiplier;

    public WeightedMovingAverage(int period)
    {
        super(period);
        this.values = new ArrayList<>(period);
        this.sum = 0.0;
        this.weightedSum = 0.0;
        this.wma = 0.0;
    }

    @Override
    public double next(double value)
    {
        if (count++ >= period)
        {
            double oldest = values.remove(0);
            weightedSum -= sum;
            sum -= oldest;
        }
        else
        {
            divisor = (count * (count+1)) / 2;
            multiplier = count;
        }
        
        values.add(value);
        sum += value;
        weightedSum += value * multiplier;

        return wma = weightedSum/divisor;
    }
    
    @Override
    public double average()
    {
        return wma;
    }
    
}
