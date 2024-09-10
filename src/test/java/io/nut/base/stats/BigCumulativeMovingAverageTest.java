/*
 *  BigCumulativeMovingAverageTest.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class BigCumulativeMovingAverageTest
{
    /**
     * Test of next method, of class CumulativeMovingAverage.
     */
    @Test
    public void testNext()
    {
        BigCumulativeMovingAverage cma = BigMovingAverage.createCMA(8, RoundingMode.HALF_UP);
        for(int i=1;i<100;i++)
        {
            BigSimpleMovingAverage sma = BigMovingAverage.createSMA(i, 8, RoundingMode.HALF_UP);

            BigDecimal expected = BigDecimal.ZERO;
            for(int j=1;j<=i;j++)
            {
                expected = sma.next(j);
            }
            BigDecimal result = cma.next(i);
            assertEquals(expected, result);
        }
    }
    
}
