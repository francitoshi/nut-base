/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats.movingaverage;

import io.nut.base.stats.movingaverage.BigMovingAverage;
import io.nut.base.stats.movingaverage.MovingAverage;
import io.nut.base.stats.movingaverage.MovingAverageType;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class BigMovingAverageTest
{

    /**
     * Test of next method, of class MovingAverage.
     */
    @Test
    public void testNext0()
    {
        

        for(MovingAverageType t : MovingAverageType.values())
        {
            for(int p=1;p<10;p++)
            {
                BigMovingAverage instance = BigMovingAverage.create(t, p, 8, RoundingMode.HALF_UP);
                for(int i=0;i<25;i++)
                {
                    assertEquals(100.0, instance.next(100).doubleValue(), "t="+t+" p="+p+" i="+i);
                }
                //CMA can't pass this proof
                if(t!=MovingAverageType.CMA)
                {
                    for(int i=0;i<100;i++)
                    {
                        instance.next(101);
                    }
                    assertEquals(101.0, instance.next(101).doubleValue(), 0.000001, "t="+t+" p="+p);
                }
            }
        }
    }
    
}
