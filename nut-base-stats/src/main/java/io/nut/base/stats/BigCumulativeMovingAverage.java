/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigCumulativeMovingAverage extends BigMovingAverage
{
    private long count;
    private BigDecimal sum;
    private BigDecimal cma;
    
    public BigCumulativeMovingAverage(int scale, RoundingMode roundingMode)
    {
        super(scale, roundingMode);
        this.sum = BigDecimal.ZERO;
        this.cma = BigDecimal.ZERO;
    }

    @Override
    public BigDecimal next(BigDecimal value)
    {
        sum = sum.add(value);
        return cma = sum.divide(BigDecimal.valueOf(++count), scale, roundingMode);
    }

    @Override
    public BigDecimal average()
    {
        return cma;
    }
    
}
