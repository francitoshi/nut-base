/*
 *  PrintStreamGaugeTest.java
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
package io.nut.base.gauge;

import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class PrintStreamGaugeTest
{
    
    public PrintStreamGaugeTest()
    {
    }

    @Test
    public void testPaint() throws InterruptedException
    {
        PrintStreamGauge gauge = new PrintStreamGauge();
        int max = 200;
        
        gauge.setDebug(true);
        gauge.setShowPrev(true);
        gauge.setShowNext(true);
        gauge.setShowFull(true);
        gauge.start(max);
        
        for(int i=0;i<max;i++)
        {
            gauge.println(Integer.toString(i, 26)+" completed");
            gauge.setPrefix("["+i+"]");
            gauge.setVal(i);
            Thread.sleep(10);
        }
        gauge.close();
    }
    
}
