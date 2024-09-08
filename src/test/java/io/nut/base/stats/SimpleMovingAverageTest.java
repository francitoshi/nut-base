/*
 *  SimpleMovingAverageTest.java
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
public class SimpleMovingAverageTest
{
    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext1()
    {
        int[] data = {20, 22, 24, 25, 23, 26, 28, 26, 29, 27, 28, 30, 27, 29, 28};
        
        BigDecimal sma = null;
        MovingAverage instance = MovingAverage.createSMA(15, 2, RoundingMode.HALF_UP);
        
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(BigDecimal.valueOf(data[i]));
        }
        assertEquals(new BigDecimal("26.13"), sma);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(new BigDecimal("26.13"), sma);
    }
    
    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext2()
    {
        int[] data = {10, 12, 9, 10, 15, 13, 18, 18, 20, 24};
        
        BigDecimal sma = null;
        
        MovingAverage instance = MovingAverage.createSMA(5, 2, RoundingMode.HALF_UP);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(new BigDecimal("18.60"), sma);
        
        instance = MovingAverage.createSMA(10, 2, RoundingMode.HALF_UP);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(new BigDecimal("14.90"), sma);
    }

    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext3()
    {
        int[] data = {1, 2, 3, 7, 9};
        
        BigDecimal sma;
        
        MovingAverage instance = MovingAverage.createSMA(3, 2, RoundingMode.HALF_UP);
        int index=0;
        instance.next(data[index++]);
        instance.next(data[index++]);
        sma = instance.next(data[index++]);

        assertEquals(new BigDecimal("2").stripTrailingZeros(), sma.stripTrailingZeros());
        
        sma = instance.next(data[index++]);

        assertEquals(new BigDecimal("4").stripTrailingZeros(), sma.stripTrailingZeros());
        sma = instance.next(data[index++]);
        
        assertEquals(new BigDecimal("6.33").stripTrailingZeros(), sma.stripTrailingZeros());
    }   
}
