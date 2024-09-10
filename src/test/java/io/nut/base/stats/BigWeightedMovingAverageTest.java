/*
 *  BigWeightedMovingAverageTest.java
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class BigWeightedMovingAverageTest
{ 
    /**
     * Test of next method, of class ExponentialMovingAverage.
     * data from https://learn.bybit.com/es/indicators/what-is-weighted-moving-average-wma/
     * there is an error in the 2nd ponderation value in the web, this test it is fixed
     */
    @Test
    public void testNext1()
    {
        int[] data = {23912, 22698, 22750, 24854, 25649};

        BigMovingAverage instance = BigMovingAverage.createWMA(5, 2, RoundingMode.HALF_UP);
        BigDecimal sma=BigDecimal.ZERO;
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(24347.93, sma.doubleValue(), 0.0000005);
    }
    
    /**
     * Test of next method, of class ExponentialMovingAverage.
     * data from https://www.earn2trade.com/blog/es/media-movil-ponderada/
     * 
     */
    @Test
    public void testNext2()
    {
        double[] data = {50.25, 56.39, 58.91, 61.52, 59.32, 55.43, 54.65};

        BigMovingAverage instance = BigMovingAverage.createWMA(7, 2, RoundingMode.HALF_UP);
        BigDecimal sma=BigDecimal.ZERO;
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(57.06, sma.doubleValue(), 0.0000005);
    }
   
}
