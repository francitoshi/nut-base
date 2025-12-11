/*
 *  DoubleExponentialMovingAverage.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
        // We do not need to check for (period <= 0) here because the 
        // ExponentialMovingAverage constructor will perform that check for us.
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