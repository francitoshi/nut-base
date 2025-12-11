/*
 *  BigTripleExponentialMovingAverageTest.java
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

import static io.nut.base.util.Assert.assertEquals;
import static io.nut.base.util.Assert.assertNotNull;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;

class BigTripleExponentialMovingAverageTest
{

    @Test
    void testInitialValue()
    {
        // TEMA inicial también debe ser igual al input
        int decimals = 4;
        BigTripleExponentialMovingAverage tema = new BigTripleExponentialMovingAverage(14, decimals, RoundingMode.HALF_UP);

        BigDecimal input = BigDecimal.valueOf(50.1234);
        BigDecimal result = tema.next(input);

        assertEquals(0, input.compareTo(result));
        assertEquals(decimals, result.scale());
    }

    @Test
    void testLogicManualCheck()
    {
        int period = 5;
        int decimals = 6;
        RoundingMode mode = RoundingMode.HALF_UP;

        // Simuladores independientes
        BigExponentialMovingAverage ema1 = new BigExponentialMovingAverage(period, decimals, mode);
        BigExponentialMovingAverage ema2 = new BigExponentialMovingAverage(period, decimals, mode);
        BigExponentialMovingAverage ema3 = new BigExponentialMovingAverage(period, decimals, mode);

        BigTripleExponentialMovingAverage tema = new BigTripleExponentialMovingAverage(period, decimals, mode);

        BigDecimal[] inputs =
        {
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(105),
            BigDecimal.valueOf(102)
        };

        BigDecimal THREE = BigDecimal.valueOf(3);

        for (BigDecimal val : inputs)
        {
            // 1. Simulación manual paso a paso
            BigDecimal e1 = ema1.next(val);
            BigDecimal e2 = ema2.next(e1);
            BigDecimal e3 = ema3.next(e2);

            // Fórmula: 3*e1 - 3*e2 + e3
            BigDecimal expected = e1.multiply(THREE)
                    .subtract(e2.multiply(THREE))
                    .add(e3)
                    .setScale(decimals, mode);

            // 2. Cálculo real
            BigDecimal actual = tema.next(val);

            assertEquals(expected, actual, "El cálculo de TEMA no coincide con la fórmula manual");
        }
    }

    @Test
    void testHighPrecisionRounding()
    {
        // Verificar que no explota con muchos decimales
        BigTripleExponentialMovingAverage tema = new BigTripleExponentialMovingAverage(10, 20, RoundingMode.DOWN);
        BigDecimal val = tema.next(new BigDecimal("1.12345678901234567890"));
        assertNotNull(val);
        assertEquals(20, val.scale());
    }
}
