/*
 *  CumulativeMovingAverageTest.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
 *-
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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class CumulativeMovingAverageTest
{
    /**
     * Test of next method, of class CumulativeMovingAverage.
     */
    @Test
    public void testNext()
    {
        CumulativeMovingAverage cma = MovingAverage.createCMA();
        for(int i=1;i<100;i++)
        {
            SimpleMovingAverage sma = MovingAverage.createSMA(i);

            double expected = 0;
            for(int j=1;j<=i;j++)
            {
                expected = sma.next(j);
            }
            double result = cma.next(i);
            assertEquals(expected, result);
        }
    }
    
}
