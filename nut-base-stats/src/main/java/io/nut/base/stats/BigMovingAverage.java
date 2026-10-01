/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import static io.nut.base.stats.MovingAverage.Type.DEMA;
import static io.nut.base.stats.MovingAverage.Type.EMA;
import static io.nut.base.stats.MovingAverage.Type.SMA;
import static io.nut.base.stats.MovingAverage.Type.TEMA;
import static io.nut.base.stats.MovingAverage.Type.WMA;
import static io.nut.base.stats.MovingAverage.Type.ZLEMA;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidParameterException;

/**
 *
 * @author franci
 */
public abstract class BigMovingAverage
{
    protected final int period;
    protected final int scale;
    protected final RoundingMode roundingMode;

    protected BigMovingAverage(int scale, RoundingMode roundingMode)
    {
        this.period = 0;
        this.scale = scale;
        this.roundingMode = roundingMode;
    }

    protected BigMovingAverage(int period, int scale, RoundingMode roundingMode)
    {
        if (period <= 0)
        {
            throw new IllegalArgumentException("period must be positive, but was: " + period);
        }
        this.period = period;
        this.scale = scale;
        this.roundingMode = roundingMode;
    }

    public abstract BigDecimal next(BigDecimal value);
    public abstract BigDecimal average();

    public final BigDecimal next(long value)
    {
        return next(BigDecimal.valueOf(value));
    }
    public final BigDecimal next(double value)
    {
        return next(BigDecimal.valueOf(value));
    }
    
    public static BigMovingAverage create(MovingAverage.Type type, int period, int decimals, RoundingMode roundingMode)
    {
        switch(type)
        {
            case SMA: 
                return createSMA(period, decimals, roundingMode);
            case WMA: 
                return createWMA(period, decimals, roundingMode);
            case CMA: 
                return createCMA(decimals, roundingMode);
            case EMA: 
                return createEMA(period, decimals, roundingMode);
            case DEMA: 
                return createDEMA(period, decimals, roundingMode);
            case TEMA: 
                return createTEMA(period, decimals, roundingMode);
            case ZLEMA: 
                return createZLEMA(period, decimals, roundingMode);
            default: 
                throw new InvalidParameterException("Unknown type "+type);
        }
    }
    public static BigSimpleMovingAverage createSMA(int period, int decimals, RoundingMode roundingMode)
    {
        return new BigSimpleMovingAverage(period, decimals, roundingMode);
    }
    public static BigExponentialMovingAverage createEMA(int period, int decimals, RoundingMode roundingMode)
    {
        return new BigExponentialMovingAverage(period, decimals, roundingMode);
    }
    public static BigDoubleExponentialMovingAverage createDEMA(int period, int decimals, RoundingMode roundingMode)
    {
        return new BigDoubleExponentialMovingAverage(period, decimals, roundingMode);
    }
    public static BigTripleExponentialMovingAverage createTEMA(int period, int decimals, RoundingMode roundingMode)
    {
        return new BigTripleExponentialMovingAverage(period, decimals, roundingMode);
    }
    public static BigZeroLagExponentialMovingAverage createZLEMA(int period, int decimals, RoundingMode roundingMode)
    {
        return new BigZeroLagExponentialMovingAverage(period, decimals, roundingMode);
    }
    public static BigWeightedMovingAverage createWMA(int period, int decimals, RoundingMode roundingMode)
    {
        return new BigWeightedMovingAverage(period, decimals, roundingMode);
    }
    public static BigCumulativeMovingAverage createCMA(int decimals, RoundingMode roundingMode)
    {
        return new BigCumulativeMovingAverage(decimals, roundingMode);
    }
}
