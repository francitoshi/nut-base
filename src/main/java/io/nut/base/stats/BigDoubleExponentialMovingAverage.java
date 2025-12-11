/*
 *  BigDoubleExponentialMovingAverage.java
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

public class BigDoubleExponentialMovingAverage extends BigMovingAverage
{

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    // Composición: DEMA usa dos EMAs internos
    private final BigExponentialMovingAverage ema1;
    private final BigExponentialMovingAverage ema2;

    private BigDecimal dema; // Valor actual
    private final int decimals;
    private final RoundingMode roundingMode;

    public BigDoubleExponentialMovingAverage(int period, int decimals, RoundingMode roundingMode)
    {
        // No necesitamos validar el periodo aquí porque BigExponentialMovingAverage lo hará.
        this.ema1 = new BigExponentialMovingAverage(period, decimals, roundingMode);
        this.ema2 = new BigExponentialMovingAverage(period, decimals, roundingMode);

        this.decimals = decimals;
        this.roundingMode = roundingMode;
    }

    @Override
    public BigDecimal next(BigDecimal value)
    {
        // 1. Calcular EMA del input
        BigDecimal e1 = ema1.next(value);

        // 2. Calcular EMA del EMA anterior
        BigDecimal e2 = ema2.next(e1);

        // 3. Fórmula DEMA: (2 * EMA1) - EMA2
        dema = e1.multiply(TWO)
                .subtract(e2)
                .setScale(decimals, roundingMode);

        return dema;
    }

    @Override
    public BigDecimal average()
    {
        return dema;
    }
}
