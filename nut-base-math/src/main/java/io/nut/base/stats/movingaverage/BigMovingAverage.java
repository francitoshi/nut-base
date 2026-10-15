/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats.movingaverage;

import static io.nut.base.stats.movingaverage.MovingAverageType.DEMA;
import static io.nut.base.stats.movingaverage.MovingAverageType.EMA;
import static io.nut.base.stats.movingaverage.MovingAverageType.SMA;
import static io.nut.base.stats.movingaverage.MovingAverageType.TEMA;
import static io.nut.base.stats.movingaverage.MovingAverageType.WMA;
import static io.nut.base.stats.movingaverage.MovingAverageType.ZLEMA;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidParameterException;

/**
 * Abstract base class for moving average calculations using {@link BigDecimal}.
 * <p>
 * A moving average computes an average over a sliding window of data points
 * or an incremental average depending on the implementation. Subclasses
 * implement different moving average algorithms (SMA, EMA, WMA, etc.) with
 * configurable scale and rounding mode.
 * </p>
 *
 * @author franci
 */
public abstract class BigMovingAverage
{
    /**
     * The period (window size) for this moving average, or {@code 0} for
     * cumulative moving averages that have no fixed window.
     */
    protected final int period;
    
    /**
     * The scale to use for {@link BigDecimal} calculations.
     */
    protected final int scale;
    
    /**
     * The rounding mode to use for {@link BigDecimal} calculations.
     */
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

    /**
     * Updates the moving average with a new value and returns the new average.
     *
     * @param value the new data point to include
     * @return the current moving average after updating
     */
    public abstract BigDecimal next(BigDecimal value);
    
    /**
     * Returns the current moving average without updating with a new value.
     *
     * @return the current moving average
     */
    public abstract BigDecimal average();

    /**
     * Updates the moving average with a new long value and returns the new average.
     *
     * @param value the new data point to include
     * @return the current moving average after updating
     */
    public final BigDecimal next(long value)
    {
        return next(BigDecimal.valueOf(value));
    }
    
    /**
     * Updates the moving average with a new double value and returns the new average.
     *
     * @param value the new data point to include
     * @return the current moving average after updating
     */
    public final BigDecimal next(double value)
    {
        return next(BigDecimal.valueOf(value));
    }
    
    /**
     * Updates the moving average with an array of values and returns the
     * resulting averages for each input value in order.
     * <p>
     * This processes each value sequentially, updating the internal state
     * after each call.
     * </p>
     *
     * @param values the array of data points to include
     * @return an array of current moving averages after each update
     * @throws IllegalArgumentException if {@code values} is null
     */
    public BigDecimal[] next(BigDecimal[] values)
    {
        if (values == null)
        {
            throw new IllegalArgumentException("values cannot be null");
        }
        BigDecimal[] result = new BigDecimal[values.length];
        for (int i = 0; i < values.length; i++)
        {
            result[i] = next(values[i]);
        }
        return result;
    }
    
    /**
     * Updates the moving average with an array of values and stores the
     * resulting averages in the provided output array.
     * <p>
     * This processes each value sequentially, updating the internal state
     * after each call.
     * </p>
     *
     * @param values the array of data points to include
     * @param output the array to store the resulting averages
     * @return the output array containing current moving averages after each update
     * @throws IllegalArgumentException if {@code values} is null, if {@code output} is null,
     *         or if {@code output.length < values.length}
     */
    public BigDecimal[] next(BigDecimal[] values, BigDecimal[] output)
    {
        if (values == null)
        {
            throw new IllegalArgumentException("values cannot be null");
        }
        if (output == null)
        {
            throw new IllegalArgumentException("output cannot be null");
        }
        if (output.length < values.length)
        {
            throw new IllegalArgumentException("output.length < values.length");
        }
        for (int i = 0; i < values.length; i++)
        {
            output[i] = next(values[i]);
        }
        return output;
    }
    
    public static BigMovingAverage create(MovingAverageType type, int period, int decimals, RoundingMode roundingMode)
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
