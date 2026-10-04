/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats.movingaverage;

import io.nut.base.stats.movingaverage.BigExponentialMovingAverage;
import io.nut.base.stats.movingaverage.BigDoubleExponentialMovingAverage;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;

class BigDoubleExponentialMovingAverageTest
{

    @Test
    void testInitialValue()
    {
        // En DEMA, el primer valor siempre debe ser igual al input,
        // ya que EMA(x) inicial = x, y 2x - x = x.
        int decimals = 4;
        BigDoubleExponentialMovingAverage dema = new BigDoubleExponentialMovingAverage(10, decimals, RoundingMode.HALF_UP);

        BigDecimal input = BigDecimal.valueOf(100.55);
        BigDecimal result = dema.next(input);

        // Verificamos valor y escala
        assertEquals(0, input.compareTo(result));
        assertEquals(decimals, result.scale());
    }

    @Test
    void testLogicManualCheck()
    {
        int period = 5;
        int decimals = 5;
        RoundingMode mode = RoundingMode.HALF_UP;

        // Usamos las clases reales para simular los pasos intermedios
        BigExponentialMovingAverage ema1 = new BigExponentialMovingAverage(period, decimals, mode);
        BigExponentialMovingAverage ema2 = new BigExponentialMovingAverage(period, decimals, mode);
        BigDoubleExponentialMovingAverage dema = new BigDoubleExponentialMovingAverage(period, decimals, mode);

        BigDecimal[] inputs =
        {
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(20),
            BigDecimal.valueOf(15)
        };

        for (BigDecimal val : inputs)
        {
            // 1. Calculamos manualmente usando dos instancias de EMA separadas
            BigDecimal e1 = ema1.next(val);
            BigDecimal e2 = ema2.next(e1);

            // Fórmula: 2*e1 - e2
            BigDecimal expected = e1.multiply(BigDecimal.valueOf(2))
                    .subtract(e2)
                    .setScale(decimals, mode);

            // 2. Calculamos con la clase DEMA
            BigDecimal actual = dema.next(val);

            assertEquals(expected, actual, "El cálculo de DEMA no coincide con la fórmula manual");
        }
    }

    @Test
    void testInvalidArgs()
    {
        assertThrows(IllegalArgumentException.class, () ->
        {
            new BigDoubleExponentialMovingAverage(0, 2, RoundingMode.HALF_UP);
        });
    }
}
