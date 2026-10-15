/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats.movingaverage;

import io.nut.base.stats.movingaverage.BigCumulativeMovingAverage;
import io.nut.base.stats.movingaverage.BigMovingAverage;
import io.nut.base.stats.movingaverage.BigSimpleMovingAverage;
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
