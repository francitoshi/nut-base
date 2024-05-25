/*
 * Stats.java
 *
 * Copyright (c) 2012-2024 francitoshi@gmail.com
 *
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
package io.nut.base.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;

/**
 *
 * @author franci
 */
public class Stats
{
    public static double exponentialMovingAverage(double ema, double value, int n)
    {
        double k = 2.0/(n+1);
        return value*k + ema*(1-k);
    }
    public static BigDecimal exponentialMovingAverage(BigDecimal ema, BigDecimal value, int n, MathContext mc)
    {
        BigDecimal k = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(n+1), mc);
        BigDecimal _1_k = BigDecimal.ONE.subtract(k, mc);
        return value.multiply(k).add(ema.multiply(_1_k),mc);
    }
    
    public static double covariance(int[] x, int[] y)
    {
        assert (x.length==y.length) : "x.length!=y.length";
        
        final int n = x.length;
        
        long sx = 0;
        long sy = 0;
        long sxy= 0;
        
        for(int i=0;i<n;i++)
        {
            sx += x[i];
            sy += y[i];
            sxy+= x[i] * y[i];
        }
        return (sxy/(double)n) - (sx*sy)/(double)(n*n);
    }
    
    public static double covariance(long[] x, long[] y)
    {
        assert (x.length==y.length) : "x.length!=y.length";
        
        final int n = x.length;
        
        long sx = 0;
        long sy = 0;
        long sxy= 0;
        
        for(int i=0;i<n;i++)
        {
            sx += x[i];
            sy += y[i];
            sxy+= x[i] * y[i];
        }
        return (sxy/(double)n) - (sx*sy)/(double)(n*n);
    }

    public static double covariance(float[] x, float[] y)
    {
        assert (x.length==y.length) : "x.length!=y.length";
        
        final int n = x.length;
        
        double sx = 0;
        double sy = 0;
        double sxy= 0;
        
        for(int i=0;i<n;i++)
        {
            sx += x[i];
            sy += y[i];
            sxy+= x[i] * y[i];
        }
        return (sxy/n) - (sx*sy)/(n*n);
    }

    public static double covariance(double[] x,double[] y)
    {
        assert (x.length==y.length) : "x.length!=y.length";
        
        final int n = x.length;
        
        double sx = 0;
        double sy = 0;
        double sxy= 0;
        
        for(int i=0;i<n;i++)
        {
            sx += x[i];
            sy += y[i];
            sxy+= x[i] * y[i];
        }
        return (sxy/n) - (sx*sy)/(n*n);
    }

    public static double standardDeviation(int[] x)
    {
        double avg = Nums.avg(x);
        double x2  = 0;
        for(int i=0;i<x.length;i++)
        {
            x2 += Math.pow(x[i]-avg, 2);
        }
        return Math.sqrt(x2/x.length);
    }
    public static double standardDeviation(long[] x)
    {
        double avg = Nums.avg(x);
        double x2  = 0;
        for(int i=0;i<x.length;i++)
        {
            x2 += Math.pow(x[i]-avg, 2);
        }
        return Math.sqrt(x2/x.length);
    }
    public static double standardDeviation(float[] x)
    {
        double avg = Nums.avg(x);
        double x2  = 0;
        for(int i=0;i<x.length;i++)
        {
            x2 += Math.pow(x[i]-avg, 2);
        }
        return Math.sqrt(x2/x.length);
    }
    public static double standardDeviation(double[] x)
    {
        double avg = Nums.avg(x);
        double x2  = 0;
        for(int i=0;i<x.length;i++)
        {
            x2 += Math.pow(x[i]-avg, 2);
        }
        return Math.sqrt(x2/x.length);
    }
    
    //Pearson correlation coefficient
    public static double correlationCoefficient(int[] x, int[] y)
    {
        return covariance(x,y) / (standardDeviation(x) * standardDeviation(y));
    }
    public static double correlationCoefficient(long[] x, long[] y)
    {
        return covariance(x,y) / (standardDeviation(x) * standardDeviation(y));
    }
    public static double correlationCoefficient(float[] x, float[] y)
    {
        return covariance(x,y) / (standardDeviation(x) * standardDeviation(y));
    }
    public static double correlationCoefficient(double[] x, double[] y)
    {
        return covariance(x,y) / (standardDeviation(x) * standardDeviation(y));
    }

    public static double median(int... data)
    {
        if(data==null || data.length==0)
        {
            return 0.0;
        }
        data = data.clone();
        Arrays.sort(data);
        int m = data.length/2;
        return data.length%2!=0 ? data[m] : (data[m]+data[m-1])/2.0;
    }
    public static double median(long... data)
    {
        if(data==null || data.length==0)
        {
            return 0.0;
        }
        data = data.clone();
        Arrays.sort(data);
        int m = data.length/2;
        return data.length%2!=0 ? data[m] : (data[m]+data[m-1])/2.0;
    }
    public static double median(float... data)
    {
        if(data==null || data.length==0)
        {
            return 0.0;
        }
        data = data.clone();
        Arrays.sort(data);
        int m = data.length/2;
        return data.length%2!=0 ? data[m] : (data[m]+data[m-1])/2.0;
    }
    public static double median(double... data)
    {
        if(data==null || data.length==0)
        {
            return 0.0;
        }
        data = data.clone();
        Arrays.sort(data);
        int m = data.length/2;
        return data.length%2!=0 ? data[m] : (data[m]+data[m-1])/2.0;
    }
    
}
