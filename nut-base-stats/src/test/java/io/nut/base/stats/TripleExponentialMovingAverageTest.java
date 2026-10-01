/*
 *  TripleExponentialMovingAverageTest.java
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

class TripleExponentialMovingAverageTest
{

    private final double DELTA = 0.0000001;

    @Test
    void testInitialValue()
    {
        TripleExponentialMovingAverage tema = new TripleExponentialMovingAverage(10);
        double input = 50.0;

        // TEMA = 3*EMA1 - 3*EMA2 + EMA3
        // Inicialmente EMA1=EMA2=EMA3=50
        // TEMA = 150 - 150 + 50 = 50
        assertEquals(input, tema.next(input), DELTA, "El valor inicial debe ser igual al input");
    }

    @Test
    void testLogicAgainstManualCalculation()
    {
        int period = 4;
        TripleExponentialMovingAverage tema = new TripleExponentialMovingAverage(period);

        double alpha = 2.0 / (period + 1.0);
        double oneMinusAlpha = 1.0 - alpha;

        double ema1 = 0, ema2 = 0, ema3 = 0;

        double[] inputs =
        {
            100.0, 105.0, 102.0, 110.0, 108.0
        };

        for (int i = 0; i < inputs.length; i++)
        {
            double value = inputs[i];
            double actual = tema.next(value);

            // Simulación manual de los 3 niveles de EMA
            if (i == 0)
            {
                ema1 = value;
                ema2 = value;
                ema3 = value;
            }
            else
            {
                ema1 = (value * alpha) + (ema1 * oneMinusAlpha);
                ema2 = (ema1 * alpha) + (ema2 * oneMinusAlpha);
                ema3 = (ema2 * alpha) + (ema3 * oneMinusAlpha);
            }

            // Fórmula TEMA: 3*EMA1 - 3*EMA2 + EMA3
            double expected = (3.0 * ema1) - (3.0 * ema2) + ema3;

            assertEquals(expected, actual, DELTA, "Fallo de cálculo TEMA en índice " + i);
        }
    }

    @Test
    void testResponsivenessComparison()
    {
        // Test conceptual: TEMA debe reaccionar más rápido que DEMA y EMA
        // ante un cambio brusco de tendencia.
        int period = 10;
        ExponentialMovingAverage ema = new ExponentialMovingAverage(period);
        DoubleExponentialMovingAverage dema = new DoubleExponentialMovingAverage(period);
        TripleExponentialMovingAverage tema = new TripleExponentialMovingAverage(period);

        // Inicializar (zona estable)
        for (int i = 0; i < 10; i++)
        {
            ema.next(10);
            dema.next(10);
            tema.next(10);
        }

        // Salto brusco en el precio
        double jumpValue = 20.0;
        double e = ema.next(jumpValue);
        double d = dema.next(jumpValue);
        double t = tema.next(jumpValue);

        // TEMA debe estar más cerca de 20 (jumpValue) que DEMA, 
        // y DEMA más cerca que EMA.
        assertTrue(t > d, "TEMA debería reaccionar más rápido (ser mayor) que DEMA en subida");
        assertTrue(d > e, "DEMA debería reaccionar más rápido (ser mayor) que EMA en subida");
    }
}
