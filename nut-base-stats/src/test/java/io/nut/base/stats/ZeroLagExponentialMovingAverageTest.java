/*
 *  ZeroLagExponentialMovingAverageTest.java
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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ZeroLagExponentialMovingAverageTest
{

    private final double DELTA = 0.0000001;

    @Test
    void testInitialBehavior()
    {
        // Periodo 5 -> Lag = (5-1)/2 = 2
        ZeroLagExponentialMovingAverage zlema = new ZeroLagExponentialMovingAverage(5);

        // Primer valor: History=[10]. Lag no lleno. olderValue=10.
        // Adjusted = 2*10 - 10 = 10.
        // EMA(10) -> 10.
        assertEquals(10.0, zlema.next(10.0), DELTA);
    }

    @Test
    void testLagLogic()
    {
        // Periodo 3 -> Lag = (3-1)/2 = 1.
        // Necesitamos el valor de hace 1 turno.
        int period = 3;
        ZeroLagExponentialMovingAverage zlema = new ZeroLagExponentialMovingAverage(period);
        ExponentialMovingAverage refEma = new ExponentialMovingAverage(period);

        // Paso 1: Input 10
        // History: [10]. Lag (1) no lleno. older=10. Adj=10.
        zlema.next(10);
        refEma.next(10); // EMA interno se inicializa en 10

        // Paso 2: Input 20
        // History antes de poll: [10, 20]. Size > 1. poll() -> 10. older=10.
        // Adj = 2*20 - 10 = 30.
        // El ZLEMA debería ser el EMA calculado sobre el valor 30.
        double expectedStep2 = refEma.next(30);
        double actualStep2 = zlema.next(20);

        assertEquals(expectedStep2, actualStep2, DELTA, "El ZLEMA no aplicó el EMA al valor ajustado correctamente en el paso 2");
    }

    @Test
    void testStability()
    {
        ZeroLagExponentialMovingAverage zlema = new ZeroLagExponentialMovingAverage(10);
        // Si alimentamos el mismo valor, el promedio debe ser ese valor
        for (int i = 0; i < 20; i++)
        {
            zlema.next(100.0);
        }
        assertEquals(100.0, zlema.average(), DELTA);
    }
}
