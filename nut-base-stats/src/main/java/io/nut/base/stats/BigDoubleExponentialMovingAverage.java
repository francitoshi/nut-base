/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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

    public BigDoubleExponentialMovingAverage(int period, int decimals, RoundingMode roundingMode)
    {
        super(period, decimals, roundingMode);
        this.ema1 = new BigExponentialMovingAverage(period, decimals, roundingMode);
        this.ema2 = new BigExponentialMovingAverage(period, decimals, roundingMode);
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
                .setScale(scale, roundingMode);

        return dema;
    }

    @Override
    public BigDecimal average()
    {
        return dema;
    }
}
