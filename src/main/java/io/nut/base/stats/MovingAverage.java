/*
 *  MovingAverage.java
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidParameterException;

/**
 *
 * @author franci
 */
public abstract class MovingAverage
{
    public enum Type { SMA, EMA, WMA, CMA}; 

    public abstract BigDecimal next(BigDecimal value);

    public final BigDecimal next(long value)
    {
        return next(BigDecimal.valueOf(value));
    }
    public final BigDecimal next(double value)
    {
        return next(BigDecimal.valueOf(value));
    }
    
    public static MovingAverage create(Type type, int period, int decimals, RoundingMode roundingMode)
    {
        switch(type)
        {
            case SMA: 
                return createSMA(period, decimals, roundingMode);
            case EMA: 
                return createEMA(period, decimals, roundingMode);
            case WMA: 
                return createWMA(period, decimals, roundingMode);
            case CMA: 
                return createCMA(decimals, roundingMode);
            default: 
                throw new InvalidParameterException("Unknown type "+type);
        }
    }
    public static SimpleMovingAverage createSMA(int period, int decimals, RoundingMode roundingMode)
    {
        return new SimpleMovingAverage(period, decimals, roundingMode);
    }
    public static ExponentialMovingAverage createEMA(int period, int decimals, RoundingMode roundingMode)
    {
        return new ExponentialMovingAverage(period, decimals, roundingMode);
    }
    public static WeightedMovingAverage createWMA(int period, int decimals, RoundingMode roundingMode)
    {
        return new WeightedMovingAverage(period, decimals, roundingMode);
    }
    public static CumulativeMovingAverage createCMA(int decimals, RoundingMode roundingMode)
    {
        return new CumulativeMovingAverage(decimals, roundingMode);
    }
}
