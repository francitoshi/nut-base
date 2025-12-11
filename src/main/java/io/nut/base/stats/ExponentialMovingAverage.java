/*
 *  ExponentialMovingAverage.java
 *
 *  Copyright (c) 2024-2025 francitoshi@gmail.com
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
package io.nut.base.stats;

public class ExponentialMovingAverage extends MovingAverage 
{
    private int count;
    private final double alpha;  // The smoothing factor
    private final double oneMinusAlpha;
    private double ema;          // Stores the current EMA value
  
    public ExponentialMovingAverage(int period)
    {
        if (period <= 0)
        {
            throw new IllegalArgumentException("period must be positive, but was: " + period);
        }
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
