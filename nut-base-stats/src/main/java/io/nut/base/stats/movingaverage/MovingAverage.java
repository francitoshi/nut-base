/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats.movingaverage;

import java.security.InvalidParameterException;

/**
 * Abstract base class for moving average calculations.
 * <p>
 * A moving average computes an average over a sliding window of data points
 * or an incremental average depending on the implementation. Subclasses
 * implement different moving average algorithms (SMA, EMA, WMA, etc.).
 * </p>
 *
 * @author franci
 */
public abstract class MovingAverage
{
    /**
     * The period (window size) for this moving average, or {@code 0} for
     * cumulative moving averages that have no fixed window.
     */
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


    /**
     * Updates the moving average with a new value and returns the new average.
     *
     * @param value the new data point to include
     * @return the current moving average after updating
     */
    public abstract double next(double value);
    
    /**
     * Returns the current moving average without updating with a new value.
     *
     * @return the current moving average
     */
    public abstract double average();
    
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
    public double[] next(double[] values)
    {
        if (values == null)
        {
            throw new IllegalArgumentException("values cannot be null");
        }
        double[] result = new double[values.length];
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
    public double[] next(double[] values, double[] output)
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
    
    public static MovingAverage create(MovingAverageType type, int period)
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
