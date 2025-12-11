/*
 *  SimpleMovingAverage.java
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author franci
 */
public class BigSimpleMovingAverage extends BigMovingAverage 
{
    private int count;
    private final int period;
    private final BigDecimal p;
    private final int scale;
    
    private final RoundingMode roundingMode;
    private BigDecimal sum = BigDecimal.ZERO;
    private BigDecimal sma;
    private final Queue<BigDecimal> queue;
    
    public BigSimpleMovingAverage(int period, int scale, RoundingMode roundingMode)
    {
        if (period <= 0)
        {
            throw new IllegalArgumentException("period must be positive, but was: " + period);
        }
        this.period = period;
        this.p = BigDecimal.valueOf(period);
        this.scale = scale;
        this.roundingMode = roundingMode;
        this.queue = new ArrayBlockingQueue<>(period);
    }

    @Override
    public BigDecimal next(BigDecimal value)
    {
        if(count<period)
        {
            sum = sum.add(value);
            sma = sum.divide(BigDecimal.valueOf(count+1), scale, roundingMode);
        }
        else
        {
            sum = sum.add(value).subtract(queue.remove());
            sma = sum.divide(p, scale, roundingMode);
        }
        this.queue.add(value);
        count++;
        return sma;
    }

    @Override
    public BigDecimal average()
    {
        return sma;
    }    
}
