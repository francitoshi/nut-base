/*
 *  MovingAverageTest.java
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

import io.nut.base.stats.MovingAverage.Type;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class MovingAverageTest
{

    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext0()
    {
        MovingAverage.Type[] types = {Type.SMA, Type.EMA, Type.WMA, Type.CMA};

        for(MovingAverage.Type t : types)
        {
            for(int p=1;p<10;p++)
            {
                MovingAverage instance = MovingAverage.create(t, p, 8, RoundingMode.HALF_UP);
                for(int i=0;i<25;i++)
                {
                    assertEquals(100.0, instance.next(100).doubleValue(), "t="+t+" p="+p+" i="+i);
                }
                //CMA can't pass this proof
                if(t!=Type.CMA)
                {
                    for(int i=0;i<100;i++)
                    {
                        instance.next(101);
                    }
                    assertEquals(101.0, instance.next(101).doubleValue(), "t="+t+" p="+p);
                }
            }
        }
    }
    
}
