/*
 * Copyright (C) 2013-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author franci
 */
public class Round
{
    private final int decimals;
    private final RoundingMode roundingMode;

    public Round(int decimals, RoundingMode roundingMode)
    {
        this.decimals = decimals;
        this.roundingMode = roundingMode;
    }

    public static Round getUpInstance(int decimals)
    {
        return new Round(decimals, RoundingMode.UP);
    }
    public static Round getDownInstance(int decimals)
    {
        return new Round(decimals, RoundingMode.DOWN);
    }
    public static Round getCeilingInstance(int decimals)
    {
        return new Round(decimals, RoundingMode.CEILING);
    }
    public static Round getFloorInstance(int decimals)
    {
        return new Round(decimals, RoundingMode.FLOOR);
    }
    public static Round getHalfUpInstance(int decimals)
    {
        return new Round(decimals, RoundingMode.HALF_UP);
    }
    public static Round getHalfDownInstance(int decimals)
    {
        return new Round(decimals, RoundingMode.HALF_DOWN);
    }
    public static Round getHalfEvenInstance(int decimals)
    {
        return new Round(decimals, RoundingMode.HALF_EVEN);
    }
    
    public double round(double d)
    {
        if( Double.isInfinite(d)|| Double.isNaN(d))
        {
            return d;
        }
        BigDecimal unscaled = BigDecimal.valueOf(d);
        BigDecimal scaled = unscaled.setScale(decimals, roundingMode);
        return scaled.doubleValue();
    }
    public BigDecimal round(BigDecimal d)
    {
        return d.setScale(decimals, roundingMode);
    }
    private double roundExtra(double d)
    {
        if( Double.isInfinite(d)|| Double.isNaN(d))
        {
            return d;
        }
        BigDecimal unscaled = BigDecimal.valueOf(d);
        BigDecimal scaled = unscaled.setScale(decimals+1, roundingMode);
        return scaled.doubleValue();
    }
    public double[] round(double[] d)
    {
        if(d==null) return null;
        double[] r = new double[d.length];
        for(int i=0;i<d.length;i++)
        {
            r[i] = this.round(d[i]);
        }
        return r;
    }
    public double round(double d, double step)
    {
        if(d==0.0)
        {
            return 0.0;
        }
        if(step==0.0)
        {
            return this.round(d);
        }

        long ticks = (long) (d / step);
        double round = this.round(ticks * step);
        
        if(round==d)
        {
            return round;
        }
        
        if(this.roundingMode==RoundingMode.CEILING || (this.roundingMode==RoundingMode.UP && d>0) || (this.roundingMode==RoundingMode.DOWN && d<0))
        {
            for(int i=0;round<d && i<Integer.MAX_VALUE;i++)
            {
                round = this.round(++ticks * step);
            }
        }
        else if(this.roundingMode==RoundingMode.FLOOR || (this.roundingMode==RoundingMode.UP && d<0) || (this.roundingMode==RoundingMode.DOWN && d>0))
        {
            for(int i=0;round>d && i<Integer.MAX_VALUE;i++)
            {
                round = this.round(--ticks * step);
            }
        }
        else
        {
            long ticksUp = ticks;
            long ticksDw = ticks;
            double roundUp = round;
            double roundDw = round;

            for(int i=0;roundUp<d && i<Integer.MAX_VALUE;i++)
            {
                roundUp = this.round(++ticksUp * step);
            }
            for(int i=0;roundDw>d && i<Integer.MAX_VALUE;i++)
            {
                roundDw = this.round(--ticksDw * step);
            }
            double mid = roundExtra((roundUp+roundDw+d) / 3.0);
            
            switch (this.roundingMode)
            {
                case HALF_DOWN:
                    round = d<=mid ? roundDw : roundUp;
                    break;
                case HALF_UP:
                    round = d>=mid ? roundUp : roundDw;
                    break;
                case HALF_EVEN:
                    boolean even = ticksDw%2==0;
                    if(d<mid)
                    {
                        round = roundDw;
                    }
                    else if(d>mid)
                    {
                        round = roundUp;
                    }
                    else
                    {
                        round = even ? roundDw : roundUp;
                    }   break;
                default:
                    round = roundDw;
                    break;
            }
        }
        return round;
    }
    
    public float round(float f)
    {
        if( Float.isInfinite(f)|| Float.isNaN(f))
        {
            return f;
        }
        BigDecimal unscaled = BigDecimal.valueOf(f);
        BigDecimal scaled = unscaled.setScale(decimals, roundingMode);
        return scaled.floatValue();
    }
    public float[] round(float[] d)
    {
        if(d==null) return null;
        float[] r = new float[d.length];
        for(int i=0;i<d.length;i++)
        {
            r[i] = this.round(d[i]);
        }
        return r;
    }
    public String toString(double d)
    {
        if( Double.isInfinite(d)|| Double.isNaN(d))
        {
            return Double.toString(d);
        }
        BigDecimal unscaled = BigDecimal.valueOf(d);
        BigDecimal scaled = unscaled.setScale(decimals, roundingMode);
        return scaled.toPlainString();
    }
    public String toString(double d, int briefDecimals)
    {
        double roundValue = this.round(d);
        for(int i=briefDecimals;i<this.decimals;i++)
        {
            Round brief = new Round(i, this.roundingMode);
            double briefValue = brief.round(roundValue);
            if(briefValue==roundValue)
            {
                return brief.toString(roundValue);
            }
        }
        return this.toString(roundValue);
    }
    public String toString(double d, int briefDecimals, double step)
    {
        return toString(round(d, step), briefDecimals);
    }

    public String toString(float f)
    {
        if( Float.isInfinite(f)|| Double.isNaN(f))
        {
            return Float.toString(f);
        }
        BigDecimal unscaled = BigDecimal.valueOf(f);
        BigDecimal scaled = unscaled.setScale(decimals, roundingMode);
        return scaled.toPlainString();
    }
    public String toString(float f, int briefDecimals)
    {
        float roundValue = this.round(f);
        for(int i=briefDecimals;i<this.decimals;i++)
        {
            Round brief = new Round(i, this.roundingMode);
            float briefValue = brief.round(roundValue);
            if(briefValue==roundValue)
            {
                return brief.toString(roundValue);
            }
        }
        return this.toString(roundValue);
    }
    
}
