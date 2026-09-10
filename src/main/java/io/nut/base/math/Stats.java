/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("x.length != y.length");
        }
        final int n = x.length;
        if (n == 0)
        {
            return 0.0;
        }

        double mx = Nums.avg(x);
        double my = Nums.avg(y);

        double sum = 0;
        for(int i=0;i<n;i++)
        {
            sum += (x[i]-mx) * (y[i]-my);
        }
        return sum/n;
    }
    
    public static double covariance(long[] x, long[] y)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("x.length != y.length");
        }
        final int n = x.length;
        if (n == 0)
        {
            return 0.0;
        }

        double mx = Nums.avg(x);
        double my = Nums.avg(y);

        double sum = 0;
        for(int i=0;i<n;i++)
        {
            sum += (x[i]-mx) * (y[i]-my);
        }
        return sum/n;
    }

    public static double covariance(float[] x, float[] y)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("x.length != y.length");
        }
        final int n = x.length;
        if (n == 0)
        {
            return 0.0;
        }

        double mx = Nums.avg(x);
        double my = Nums.avg(y);

        double sum = 0;
        for(int i=0;i<n;i++)
        {
            sum += (x[i]-mx) * (y[i]-my);
        }
        return sum/n;
    }

    public static double covariance(double[] x,double[] y)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("x.length != y.length");
        }
        final int n = x.length;
        if (n == 0)
        {
            return 0.0;
        }

        double mx = Nums.avg(x);
        double my = Nums.avg(y);

        double sum = 0;
        for(int i=0;i<n;i++)
        {
            sum += (x[i]-mx) * (y[i]-my);
        }
        return sum/n;
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
