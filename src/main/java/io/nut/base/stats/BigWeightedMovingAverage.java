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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class BigWeightedMovingAverage extends BigMovingAverage
{

    private int count;
    private final int period;
    private final int scale;
    private final RoundingMode roundingMode;
    private final List<BigDecimal> values;
    private BigDecimal sum;
    private BigDecimal weightedSum;
    private BigDecimal divisor;
    private BigDecimal multiplier;

    public BigWeightedMovingAverage(int period, int scale, RoundingMode roundingMode)
    {
        if (period <= 0)
        {
            throw new IllegalArgumentException("Period must be positive");
        }
        this.period = period;
        this.scale = scale;
        this.roundingMode = roundingMode;
        this.values = new ArrayList<>(period);
        this.sum = BigDecimal.ZERO;
        this.weightedSum = BigDecimal.ZERO;
    }

    @Override
    public BigDecimal next(BigDecimal value)
    {
        if (count++ >= period)
        {
            BigDecimal oldest = values.remove(0);
            weightedSum = weightedSum.subtract(sum);
            sum = sum.subtract(oldest);
        }
        else
        {
            divisor = BigDecimal.valueOf((count * (count+1)) / 2);
            multiplier = BigDecimal.valueOf(count);
        }
        
        values.add(value);
        sum = sum.add(value);
        weightedSum = weightedSum.add(value.multiply(multiplier));

        return weightedSum.divide(divisor, scale, roundingMode);
    }
}
