/*
 *  BigZeroLagExponentialMovingAverage.java
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.Queue;

public class BigZeroLagExponentialMovingAverage extends BigMovingAverage
{

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private final BigExponentialMovingAverage ema;
    private final int lag;
    private final Queue<BigDecimal> history;
    private BigDecimal zlema;

    private final int decimals;
    private final RoundingMode roundingMode;

    public BigZeroLagExponentialMovingAverage(int period, int decimals, RoundingMode roundingMode)
    {
        this.ema = new BigExponentialMovingAverage(period, decimals, roundingMode);
        this.decimals = decimals;
        this.roundingMode = roundingMode;

        this.lag = (period - 1) / 2;
        this.history = new LinkedList<>();
    }

    @Override
    public BigDecimal next(BigDecimal value)
    {
        // 1. Gestión del historial
        history.add(value);

        BigDecimal olderValue = value;
        if (history.size() > lag)
        {
            olderValue = history.poll();
        }

        // 2. Calcular dato ajustado: (2 * value) - olderValue
        BigDecimal adjustedData = value.multiply(TWO)
                .subtract(olderValue)
                .setScale(decimals, roundingMode);

        // 3. Pasar al EMA interno
        return zlema = ema.next(adjustedData);
    }

    @Override
    public BigDecimal average()
    {
        return zlema;
    }
}
