/*
 *  BigZeroLagExponentialMovingAverageTest.java
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
import java.math.BigDecimal;
import java.math.RoundingMode;

class BigZeroLagExponentialMovingAverageTest
{

    @Test
    void testLogicWithManualCalculation()
    {
        int period = 5; // Lag = 2
        int decimals = 4;
        RoundingMode mode = RoundingMode.HALF_UP;

        BigZeroLagExponentialMovingAverage zlema = new BigZeroLagExponentialMovingAverage(period, decimals, mode);
        BigExponentialMovingAverage shadowEma = new BigExponentialMovingAverage(period, decimals, mode);

        BigDecimal[] inputs =
        {
            BigDecimal.valueOf(10), // t=0. older=10 (default). Adj=10
            BigDecimal.valueOf(20), // t=1. older=20 (default, queue size=2 not > 2 yet if poll happens after). 
            // Wait: Logic is: add, if size > lag then poll.
            // Lag=2. 
            // Add 10. Size=1. Not > 2. older=10. Adj=10.
            // Add 20. Size=2. Not > 2. older=20. Adj=20.
            BigDecimal.valueOf(30), // Add 30. Size=3. > 2. Poll -> 10. older=10. Adj=2*30 - 10 = 50.
            BigDecimal.valueOf(40)  // Add 40. Size=3. > 2. Poll -> 20. older=20. Adj=2*40 - 20 = 60.
        };

        // Paso 0
        shadowEma.next(BigDecimal.valueOf(10));
        assertEquals(shadowEma.average(), zlema.next(inputs[0]));

        // Paso 1
        shadowEma.next(BigDecimal.valueOf(20));
        assertEquals(shadowEma.average(), zlema.next(inputs[1]));

        // Paso 2 (Aquí empieza el efecto lag real)
        // Input 30. Lag data was 10. Adjusted = 50.
        BigDecimal expectedVal2 = shadowEma.next(BigDecimal.valueOf(50));
        assertEquals(expectedVal2, zlema.next(inputs[2]));

        // Paso 3
        // Input 40. Lag data was 20. Adjusted = 60.
        BigDecimal expectedVal3 = shadowEma.next(BigDecimal.valueOf(60));
        assertEquals(expectedVal3, zlema.next(inputs[3]));
    }

    @Test
    void testPeriodOne()
    {
        // Periodo 1 -> Lag = 0.
        // Data adjusted = 2*Current - Current = Current.
        // ZLEMA se comporta idéntico a un EMA de periodo 1 (que es igual al raw value).
        BigZeroLagExponentialMovingAverage zlema = new BigZeroLagExponentialMovingAverage(1, 4, RoundingMode.HALF_UP);

        BigDecimal val = BigDecimal.valueOf(123.4567);
        assertEquals(0, val.compareTo(zlema.next(val)));

        BigDecimal val2 = BigDecimal.valueOf(999);
        assertEquals(0, val2.compareTo(zlema.next(val2)));
    }
}
