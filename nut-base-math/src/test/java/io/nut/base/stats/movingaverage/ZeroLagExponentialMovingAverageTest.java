/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats.movingaverage;

import io.nut.base.stats.movingaverage.ExponentialMovingAverage;
import io.nut.base.stats.movingaverage.ZeroLagExponentialMovingAverage;
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
