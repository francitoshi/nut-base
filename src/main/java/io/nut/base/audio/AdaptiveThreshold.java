/*
 * AdaptiveThreshold.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.audio;

import io.nut.base.stats.MovingAverage;

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
