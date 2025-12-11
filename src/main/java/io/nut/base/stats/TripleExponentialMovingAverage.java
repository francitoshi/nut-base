/*
 *  TripleExponentialMovingAverage.java
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

public class TripleExponentialMovingAverage extends MovingAverage 
{
    // Composition: TEMA relies on three internal EMA calculations.
    private final ExponentialMovingAverage ema1;
    private final ExponentialMovingAverage ema2;
    private final ExponentialMovingAverage ema3;
    
    // Store the current calculation
    private double tema;

    public TripleExponentialMovingAverage(int period)
    {
        // Initialize all three internal EMAs with the same period
        this.ema1 = new ExponentialMovingAverage(period);
        this.ema2 = new ExponentialMovingAverage(period);
        this.ema3 = new ExponentialMovingAverage(period);
    }

    @Override
    public double next(double value)
    {
        // 1. Calculate EMA of the input value
        double e1 = ema1.next(value);
        
        // 2. Calculate EMA of the EMA (Smoothing the smoothed value)
        double e2 = ema2.next(e1);
        
        // 3. Calculate EMA of the EMA of the EMA (Triple smoothing)
        double e3 = ema3.next(e2);
        
        // 4. TEMA Formula: (3 * EMA1) - (3 * EMA2) + EMA3
        return tema = (3.0 * e1) - (3.0 * e2) + e3;
    }

    @Override
    public double average()
    {
        return tema;
    }
}