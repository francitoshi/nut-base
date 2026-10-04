/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats.movingaverage;

import io.nut.base.stats.movingaverage.MovingAverage;
import io.nut.base.stats.movingaverage.MovingAverageType;
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
        MovingAverageType[] types = {MovingAverageType.SMA, MovingAverageType.EMA, MovingAverageType.DEMA, MovingAverageType.TEMA, MovingAverageType.WMA, MovingAverageType.CMA};

        for(MovingAverageType t : types)
        {
            for(int p=1;p<10;p++)
            {
                MovingAverage instance = MovingAverage.create(t, p);
                for(int i=0;i<25;i++)
                {
                    assertEquals(100.0, instance.next(100), 0.005, "t="+t+" p="+p+" i="+i);
                }
                //CMA can't pass this proof
                if(t!=MovingAverageType.CMA)
                {
                    for(int i=0;i<100;i++)
                    {
                        instance.next(101);
                    }
                    assertEquals(101.0, instance.next(101), 0.005, "t="+t+" p="+p);
                }
            }
        }
    }
    
}
