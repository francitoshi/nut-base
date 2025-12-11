/*
 *  DoubleExponentialMovingAverageTest.java
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
        assertThrows(IllegalArgumentException.class, () ->
        {
            new DoubleExponentialMovingAverage(0);
        });

        assertThrows(IllegalArgumentException.class, () ->
        {
            new DoubleExponentialMovingAverage(-5);
        });
    }
}
