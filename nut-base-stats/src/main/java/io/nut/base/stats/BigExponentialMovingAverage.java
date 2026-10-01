/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigExponentialMovingAverage extends BigMovingAverage 
{
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private long count;
    private final BigDecimal alpha;  // The smoothing factor
    private final BigDecimal oneMinusAlpha;
    private BigDecimal ema;          // Stores the current EMA value
    private BigDecimal avg;
  
    public BigExponentialMovingAverage(int period, int decimals, RoundingMode roundingMode)
    {
        super(period, decimals, roundingMode);
        // Calculate alpha: 2 / (period + 1)
        this.alpha = TWO.divide(BigDecimal.valueOf(period + 1), scale*2, RoundingMode.HALF_UP).stripTrailingZeros();
        this.oneMinusAlpha = BigDecimal.ONE.subtract(alpha).stripTrailingZeros();
    }

    @Override
    public BigDecimal next(BigDecimal value)
    {
        if(count==0)
        {
            // First value initializes the EMA directly
            ema = value;
        }
        else
        {
            // Calculate EMA: EMA = (Value * Alpha) + (EMA_prev * (1 - Alpha))
            ema = value.multiply(alpha)
                    .add(ema.multiply(oneMinusAlpha))
                    .setScale(scale*2, roundingMode);
        }
        count++;
        return avg = ema.setScale(scale, roundingMode);
    }
    
    @Override
    public BigDecimal average()
    {
        return avg;
    }
    
}
