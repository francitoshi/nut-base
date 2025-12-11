/*
 *  CumulativeMovingAverage.java
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

public class BigCumulativeMovingAverage extends BigMovingAverage
{
    private int count;
    private final int scale;
    private final RoundingMode roundingMode;
    private BigDecimal sum;
    private BigDecimal cma;
    
    public BigCumulativeMovingAverage(int scale, RoundingMode roundingMode)
    {
        this.scale = scale;
        this.roundingMode = roundingMode;
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
