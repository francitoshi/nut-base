/*
 *  ExponentialMovingAverage.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
 *-
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

/**
 *
 * @author franci
 */
public class ExponentialMovingAverage extends MovingAverage 
{
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private int count;
    private final BigDecimal alpha;  // The smoothing factor
    private final BigDecimal oneMinusAlpha;
    private BigDecimal ema;          // Stores the current EMA value
    private final int decimals;
    private final RoundingMode roundingMode;
  
    public ExponentialMovingAverage(int period, int decimals, RoundingMode roundingMode)
    {
        if (period <= 0)
        {
            throw new IllegalArgumentException("Period must be positive");
        }
        // Calculate alpha: 2 / (period + 1)
        this.alpha = TWO.divide(BigDecimal.valueOf(period + 1), decimals*2, RoundingMode.HALF_UP).stripTrailingZeros();
        this.oneMinusAlpha = BigDecimal.ONE.subtract(alpha).stripTrailingZeros();
        this.decimals = decimals;
        this.roundingMode = roundingMode;
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
                    .setScale(decimals*2, roundingMode);
        }
        count++;
        return ema.setScale(decimals, roundingMode);
    }
    
}
