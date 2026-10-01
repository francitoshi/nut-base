/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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

    public BigTripleExponentialMovingAverage(int period, int decimals, RoundingMode roundingMode)
    {
        super(period, decimals, roundingMode);
        this.ema1 = new BigExponentialMovingAverage(period, decimals, roundingMode);
        this.ema2 = new BigExponentialMovingAverage(period, decimals, roundingMode);
        this.ema3 = new BigExponentialMovingAverage(period, decimals, roundingMode);
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
                .setScale(scale, roundingMode);

        return tema;
    }

    @Override
    public BigDecimal average()
    {
        return tema;
    }
}
