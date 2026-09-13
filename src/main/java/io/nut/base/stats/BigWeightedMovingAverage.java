/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class BigWeightedMovingAverage extends BigMovingAverage
{

    private long count;
    private final List<BigDecimal> values;
    private BigDecimal sum;
    private BigDecimal wma;
    private BigDecimal weightedSum;
    private BigDecimal divisor;
    private BigDecimal multiplier;

    public BigWeightedMovingAverage(int period, int scale, RoundingMode roundingMode)
    {
        super(period, scale, roundingMode);
        this.values = new ArrayList<>(period);
        this.sum = BigDecimal.ZERO;
        this.weightedSum = BigDecimal.ZERO;
        this.wma = BigDecimal.ZERO;
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

        return wma = weightedSum.divide(divisor, scale, roundingMode);
    }
    
    @Override
    public BigDecimal average()
    {
        return wma;
    }
        
}
