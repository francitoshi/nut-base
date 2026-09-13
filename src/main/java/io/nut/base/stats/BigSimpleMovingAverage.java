/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * @author franci
 */
public class BigSimpleMovingAverage extends BigMovingAverage 
{
    private long count;
    private final BigDecimal p;
    
    private BigDecimal sum = BigDecimal.ZERO;
    private BigDecimal sma;
    private final Queue<BigDecimal> queue;
    
    public BigSimpleMovingAverage(int period, int scale, RoundingMode roundingMode)
    {
        super(period, scale, roundingMode);
        this.p = BigDecimal.valueOf(period);
        this.queue = new ArrayBlockingQueue<>(period);
    }

    @Override
    public BigDecimal next(BigDecimal value)
    {
        if(count<period)
        {
            sum = sum.add(value);
            sma = sum.divide(BigDecimal.valueOf(count+1), scale, roundingMode);
        }
        else
        {
            sum = sum.add(value).subtract(queue.remove());
            sma = sum.divide(p, scale, roundingMode);
        }
        this.queue.add(value);
        count++;
        return sma;
    }

    @Override
    public BigDecimal average()
    {
        return sma;
    }    
}
