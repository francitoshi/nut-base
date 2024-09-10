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

import java.security.InvalidParameterException;

/**
 *
 * @author franci
 */
public abstract class MovingAverage
{
    public enum Type { SMA, EMA, WMA, CMA}; 

    public abstract double next(double value);
    
    public static MovingAverage create(Type type, int period)
    {
        switch(type)
        {
            case SMA: 
                return createSMA(period);
            case EMA: 
                return createEMA(period);
            case WMA: 
                return createWMA(period);
            case CMA: 
                return createCMA();
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
    public static WeightedMovingAverage createWMA(int period)
    {
        return new WeightedMovingAverage(period);
    }
    public static CumulativeMovingAverage createCMA()
    {
        return new CumulativeMovingAverage();
    }
}
