/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import java.security.InvalidParameterException;

/**
 *
 * @author franci
 */
public abstract class MovingAverage
{
    protected final int period;

    protected MovingAverage()
    {
        this.period = 0;
    }

    protected MovingAverage(int period)
    {
        if (period <= 0)
        {
            throw new IllegalArgumentException("period must be positive, but was: " + period);
        }
        this.period = period;
    }

    public enum Type { SMA, WMA, CMA, EMA, DEMA, TEMA, ZLEMA}; 

    public abstract double next(double value);
    public abstract double average();
    
    public static MovingAverage create(Type type, int period)
    {
        switch(type)
        {
            case SMA: 
                return createSMA(period);
            case WMA: 
                return createWMA(period);
            case CMA: 
                return createCMA();
            case EMA: 
                return createEMA(period);
            case DEMA: 
                return createDEMA(period);
            case TEMA: 
                return createTEMA(period);
            case ZLEMA: 
                return createZLEMA(period);
            default: 
                throw new InvalidParameterException("Unknown type "+type);
        }
    }
    public static SimpleMovingAverage createSMA(int period)
    {
        return new SimpleMovingAverage(period);
    }
    public static ExponentialMovingAverage createEMA(int period)
    {
        return new ExponentialMovingAverage(period);
    }
    public static DoubleExponentialMovingAverage createDEMA(int period)
    {
        return new DoubleExponentialMovingAverage(period);
    }
    public static TripleExponentialMovingAverage createTEMA(int period)
    {
        return new TripleExponentialMovingAverage(period);
    }
    public static ZeroLagExponentialMovingAverage createZLEMA(int period)
    {
        return new ZeroLagExponentialMovingAverage(period);
    }
    public static WeightedMovingAverage createWMA(int period)
    {
        return new WeightedMovingAverage(period);
    }
    public static CumulativeMovingAverage createCMA()
    {
        return new CumulativeMovingAverage();
    }
}
