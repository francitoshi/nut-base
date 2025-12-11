/*
 *  SimpleMovingAverageTest.java
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
public class SimpleMovingAverageTest
{
    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext1()
    {
        int[] data = {20, 22, 24, 25, 23, 26, 28, 26, 29, 27, 28, 30, 27, 29, 28};
        
        double sma = 0;
        MovingAverage instance = MovingAverage.createSMA(15);
        
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(26.13, sma, 0.005);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(26.13, sma, 0.005);
    }
    
    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext2()
    {
        int[] data = {10, 12, 9, 10, 15, 13, 18, 18, 20, 24};
        
        double sma = 0;
        
        MovingAverage instance = MovingAverage.createSMA(5);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(18.60, sma, 0.005);
        
        instance = MovingAverage.createSMA(10);
        for(int i=0;i<data.length;i++)
        {
            sma = instance.next(data[i]);
        }
        assertEquals(14.90, sma, 0.005);
    }

    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext3()
    {
        int[] data = {1, 2, 3, 7, 9};
        
        double sma;
        
        MovingAverage instance = MovingAverage.createSMA(3);
        int index=0;
        instance.next(data[index++]);
        instance.next(data[index++]);
        sma = instance.next(data[index++]);

        assertEquals(2, sma, 0.005);
        
        sma = instance.next(data[index++]);

        assertEquals(4, sma, 0.005);
        sma = instance.next(data[index++]);
        
        assertEquals(6.33, sma, 0.005);
    }   
}
