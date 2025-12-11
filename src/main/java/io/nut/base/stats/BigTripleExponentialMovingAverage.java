/*
 *  BigTripleExponentialMovingAverage.java
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

public class BigTripleExponentialMovingAverage extends BigMovingAverage
{

    private static final BigDecimal THREE = BigDecimal.valueOf(3);

    // Composición: TEMA usa tres EMAs internos
    private final BigExponentialMovingAverage ema1;
    private final BigExponentialMovingAverage ema2;
    private final BigExponentialMovingAverage ema3;

    private BigDecimal tema; // Valor actual
    private final int decimals;
    private final RoundingMode roundingMode;

    public BigTripleExponentialMovingAverage(int period, int decimals, RoundingMode roundingMode)
    {
        this.ema1 = new BigExponentialMovingAverage(period, decimals, roundingMode);
        this.ema2 = new BigExponentialMovingAverage(period, decimals, roundingMode);
        this.ema3 = new BigExponentialMovingAverage(period, decimals, roundingMode);

        this.decimals = decimals;
        this.roundingMode = roundingMode;
    }

    @Override
    public BigDecimal next(BigDecimal value)
    {
        // 1. Calcular cadena de EMAs
        BigDecimal e1 = ema1.next(value);
        BigDecimal e2 = ema2.next(e1);
        BigDecimal e3 = ema3.next(e2);

        // 2. Fórmula TEMA: (3 * EMA1) - (3 * EMA2) + EMA3
        BigDecimal term1 = e1.multiply(THREE);
        BigDecimal term2 = e2.multiply(THREE);

        tema = term1.subtract(term2)
                .add(e3)
                .setScale(decimals, roundingMode);

        return tema;
    }

    @Override
    public BigDecimal average()
    {
        return tema;
    }
}
