/*
 *  ExponentialMovingAverageTest.java
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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ExponentialMovingAverageTest
{
    /**
     * Test of next method, of class ExponentialMovingAverage.
     */
    @Test
    public void testNext1()
    {
        int[] data = {12, 14, 16, 15, 18, 20, 22, 21, 23, 25};
        double[] exp = {12, 13, 14.5, 14.75, 16.375, 18.1875, 20.09375, 20.546875, 21.773437, 23.386719};

        MovingAverage instance = MovingAverage.createEMA(3);
        
        for(int i=0;i<data.length;i++)
        {
            double sma = instance.next(data[i]);
            assertEquals(exp[i], sma, 0.0000005);
        }
    }
    /**
     * Test of next method, of class ExponentialMovingAverage.
     */
    @Test
    public void testNext2()
    {
        int[] data = { 10, 12, 15, 14, 13, 11, 12, 13, 14, 15};
        double[] exp = {10.0000, 11.0000, 13.0000, 13.5000, 13.2500, 12.1250, 12.0625, 12.5313, 13.2656, 14.1328};

        MovingAverage instance = MovingAverage.createEMA(3);
        
        for(int i=0;i<data.length;i++)
        {
            double sma = instance.next(data[i]);
            assertEquals(exp[i], sma, 0.00005);
        }
    }
    
}
