/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.audio;

import io.nut.base.stats.movingaverage.MovingAverage;

public class AdaptiveThreshold
{
    private final MovingAverage emaLow = MovingAverage.createEMA(10);
    private final MovingAverage emaHigh = MovingAverage.createEMA(10);
    private final double beta;
    private volatile double threshold;

    public AdaptiveThreshold(double beta, double threshold)
    {
        this.beta = beta;
        this.threshold = threshold;
    }

    public boolean update(double e)
    {
        double p;
        double q;
        
        if(e>this.threshold)
        {
            p = emaHigh.next(e);
            q = emaLow.average();
        }
        else
        {
            p = emaHigh.average();
            q = emaLow.next(e);
        }

        threshold = p>q ? q + beta * (p - q) : Math.max(threshold, q*10);
        
        return e>threshold;
    }

    public double getThreshold()
    {
        return threshold;
    }

}
