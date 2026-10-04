/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats.movingaverage;

import io.nut.base.stats.movingaverage.DoubleExponentialMovingAverage;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DoubleExponentialMovingAverageTest
{

    private final double DELTA = 0.0000001;

    @Test
    void testInitialValue()
    {
        // El primer valor de un DEMA debe ser igual al input,
        // ya que los EMA internos se inicializan con ese valor.
        DoubleExponentialMovingAverage dema = new DoubleExponentialMovingAverage(10);
        double input = 100.0;
        assertEquals(input, dema.next(input), DELTA, "El valor inicial debe ser igual al input");
        assertEquals(input, dema.average(), DELTA);
    }

    @Test
    void testLogicAgainstManualCalculation()
    {
        int period = 5;
        DoubleExponentialMovingAverage dema = new DoubleExponentialMovingAverage(period);

        // Configuración manual para verificar la fórmula
        double alpha = 2.0 / (period + 1.0);
        double oneMinusAlpha = 1.0 - alpha;

        // Variables para simular el estado interno
        double ema1 = 0;
        double ema2 = 0;

        double[] inputs =
        {
            10.0, 12.0, 11.5, 14.0, 13.0
        };

        for (int i = 0; i < inputs.length; i++)
        {
            double value = inputs[i];
            double actual = dema.next(value);

            // Simulación manual
            if (i == 0)
            {
                ema1 = value;
                ema2 = ema1; // El segundo EMA recibe el output del primero
            }
            else
            {
                ema1 = (value * alpha) + (ema1 * oneMinusAlpha);
                ema2 = (ema1 * alpha) + (ema2 * oneMinusAlpha);
            }

            // Fórmula DEMA: 2*EMA1 - EMA2
            double expected = (2.0 * ema1) - ema2;

            assertEquals(expected, actual, DELTA, "Fallo de cálculo en el índice " + i);
        }
    }

    @Test
    void testInvalidConstruction()
    {
        // Verifica que la validación del periodo se propaga correctamente
        assertThrows(IllegalArgumentException.class, () -> { new DoubleExponentialMovingAverage(0); });

        assertThrows(IllegalArgumentException.class, () -> { new DoubleExponentialMovingAverage(-5); });
    }
}
