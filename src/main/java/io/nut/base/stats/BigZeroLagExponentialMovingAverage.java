/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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

    public BigZeroLagExponentialMovingAverage(int period, int decimals, RoundingMode roundingMode)
    {
        super(period, decimals, roundingMode);
        this.ema = new BigExponentialMovingAverage(period, decimals, roundingMode);
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
                .setScale(scale, roundingMode);

        // 3. Pasar al EMA interno
        return zlema = ema.next(adjustedData);
    }

    @Override
    public BigDecimal average()
    {
        return zlema;
    }
}
