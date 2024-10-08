/*
 *  WeightedMovingAverage.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
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

import java.util.ArrayList;
import java.util.List;

public class WeightedMovingAverage extends MovingAverage
{

    private int count;
    private final int period;
    private final List<Double> values;
    private double sum;
    private double weightedSum;
    private double divisor;
    private double multiplier;

    public WeightedMovingAverage(int period)
    {
        if (period <= 0)
        {
            throw new IllegalArgumentException("Period must be positive");
        }
        this.period = period;
        this.values = new ArrayList<>(period);
        this.sum = 0.0;
        this.weightedSum = 0.0;
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

        return weightedSum/divisor;
    }
}
