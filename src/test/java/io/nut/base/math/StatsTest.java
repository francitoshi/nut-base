/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

import io.nut.base.util.Utils;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class StatsTest
{

    static final int[] X1 = { 2,   3,  4,  4,  5,  6,  6,  7,  7,  8,  10, 10};
    static final int[] Y1 = { 1,   3,  2,  4,  4,  4,  6,  4,  6,  7,  9,  10};

    static final int[] X2 = { 0,   0,   0,  0,  0,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  4,  4,  4,  4,  4};
    static final int[] Y2 = { 1,   1,   2,  3,  3,  1,  2,  2,  2,  2,  3,  3,  3,  3,  3,  1,  1,  1,  2,  2};

    /**
     * Test of covariance method, of class Statistics.
     */
    @Test
    public void testCovariance_intArr_intArr()
    {
        int[] x = X1;
        int[] y = Y1;
        double result = Stats.covariance(x, y);
        assertEquals(5.916666667, result, 0.000001);
        
        x = X2;
        y = Y2;
        result = Stats.covariance(x, y);
        assertEquals(-0.3, result, 0.000001);
    }

    /**
     * Test of covariance method, of class Statistics.
     */
    @Test
    public void testCovariance_longArr_longArr()
    {
        long[] x = Utils.asLongs(X1);
        long[] y = Utils.asLongs(Y1);
        double result = Stats.covariance(x, y);
        assertEquals(5.916666667, result, 0.000001);
        
        x = Utils.asLongs(X2);
        y = Utils.asLongs(Y2);
        result = Stats.covariance(x, y);
        assertEquals(-0.3, result, 0.000001);
    }

    @Test
    public void testCovariance_floatArr_floatArr()
    {
        float[] x = Utils.asFloats(X1);
        float[] y = Utils.asFloats(Y1);
        double result = Stats.covariance(x, y);
        assertEquals(5.916666667, result, 0.000001);
        
        x = Utils.asFloats(X2);
        y = Utils.asFloats(Y2);
        result = Stats.covariance(x, y);
        assertEquals(-0.3, result, 0.000001);
    }

    @Test
    public void testCovariance_doubleArr_doubleArr()
    {
        double[] x = Utils.asDoubles(X1);
        double[] y = Utils.asDoubles(Y1);
        double result = Stats.covariance(x, y);
        assertEquals(5.916666667, result, 0.000001);
        
        x = Utils.asDoubles(X2);
        y = Utils.asDoubles(Y2);
        result = Stats.covariance(x, y);
        assertEquals(-0.3, result, 0.000001);
    }

    static final int[] X3 = { 9, 3, 8, 8, 9, 8, 9, 18};

    @Test
    public void testStandardDeviation_intArr()
    {
        int[] x = X3;
        double result = Stats.standardDeviation(x);
        assertEquals(3.872983346, result, 0.000001);
    }

    /**
     * Test of standardDeviation method, of class Statistics.
     */
    @Test
    public void testStandardDeviation_longArr()
    {
        long[] x = Utils.asLongs(X3);
        double result = Stats.standardDeviation(x);
        assertEquals(3.872983346, result, 0.000001);
    }

    /**
     * Test of standardDeviation method, of class Statistics.
     */
    @Test
    public void testStandardDeviation_floatArr()
    {
        float[] x = Utils.asFloats(X3);
        double result = Stats.standardDeviation(x);
        assertEquals(3.872983346, result, 0.000001);
    }
    
    /**
     * Test of standardDeviation method, of class Statistics.
     */
    @Test
    public void testStandardDeviation_doubleArr()
    {
        double[] x = Utils.asDoubles(X3);
        double result = Stats.standardDeviation(x);
        assertEquals(3.872983346, result, 0.000001);
    }

    /**
     * Test of correlationCoefficient method, of class Statistics.
     */
    @Test
    public void testCorrelationCoefficient_intArr_intArr()
    {
        int[] x = X1;
        int[] y = Y1;
        double result = Stats.correlationCoefficient(x, y);
        assertEquals(0.935507141, result, 0.000001);
    }

    /**
     * Test of correlationCoefficient method, of class Statistics.
     */
    @Test
    public void testCorrelationCoefficient_longArr_longArr()
    {
        long[] x = Utils.asLongs(X1);
        long[] y = Utils.asLongs(Y1);
        double result = Stats.correlationCoefficient(x, y);
        assertEquals(0.935507141, result, 0.000001);
    }

    /**
     * Test of correlationCoefficient method, of class Statistics.
     */
    @Test
    public void testCorrelationCoefficient_floatArr_floatArr()
    {
        float[] x = Utils.asFloats(X1);
        float[] y = Utils.asFloats(Y1);
        double result = Stats.correlationCoefficient(x, y);
        assertEquals(0.935507141, result, 0.000001);
    }

    /**
     * Test of correlationCoefficient method, of class Statistics.
     */
    @Test
    public void testCorrelationCoefficient_doubleArr_doubleArr()
    {
        double[] x = Utils.asDoubles(X1);
        double[] y = Utils.asDoubles(Y1);
        double result = Stats.correlationCoefficient(x, y);
        assertEquals(0.935507141, result, 0.000001);
    }

    @Test
    public void testSome()
    {
        double[] x = { 1, 2, 1, 3, 1, 4, 1, 5};
        double[] y = { 1, 1, 1, 1, 1, 1, 1, 1.1};
        double result = Stats.correlationCoefficient(x, y);
        System.out.println("correlacion="+result);
    }

    /**
     * Test of median method, of class Statistics.
     */
    @Test
    public void testMedian_intArr()
    {
        double delta = 0.0000001;
        // samples from https://en.wikipedia.org/wiki/Median
        assertEquals(6, Stats.median(1, 3, 3, 6, 7, 8, 9), delta);
        assertEquals(4.5, Stats.median(1, 2, 3, 4, 5, 6, 8, 9), delta);
        assertEquals(2, Stats.median(1, 2, 2, 2, 3, 14), delta);
        assertEquals(5, Stats.median(1, 5, 2, 8, 7), delta);
        assertEquals(4, Stats.median(1, 6, 2, 8, 7, 2), delta);
        
        // samples from https://es.wikipedia.org/wiki/Mediana_(estad%C3%ADstica)
        assertEquals(5, Stats.median(2, 3, 4, 4, 5, 5, 5, 6, 6), delta);
        assertEquals(9.5, Stats.median(7, 8, 9, 10, 11, 12), delta);
    }

    /**
     * Test of median method, of class Statistics.
     */
    @Test
    public void testMedian_longArr()
    {
        double delta = 0.0000001;
        // samples from https://en.wikipedia.org/wiki/Median
        assertEquals(6, Stats.median(1L, 3L, 3L, 6L, 7L, 8L, 9), delta);
        assertEquals(4.5, Stats.median(1L, 2L, 3L, 4L, 5L, 6L, 8L, 9), delta);
        assertEquals(2.0, Stats.median(1L, 2L, 2L, 2L, 3L, 14), delta);
        assertEquals(5.0, Stats.median(1L, 5L, 2L, 8L, 7), delta);
        assertEquals(4.0, Stats.median(1L, 6L, 2L, 8L, 7L, 2), delta);
        
        // samples from https://es.wikipedia.org/wiki/Mediana_(estad%C3%ADstica)
        assertEquals(5.0, Stats.median(2L, 3L, 4L, 4L, 5L, 5L, 5L, 6L, 6), delta);
        assertEquals(9.5, Stats.median(7L, 8L, 9L, 10L, 11L, 12), delta);
    }

    /**
     * Test of median method, of class Statistics.
     */
    @Test
    public void testMedian_floatArr()
    {
        double delta = 0.0000001;
        // samples from https://en.wikipedia.org/wiki/Median
        assertEquals(6, Stats.median(1f, 3f, 3f, 6f, 7f, 8f, 9f), delta);
        assertEquals(4.5, Stats.median(1f, 2f, 3f, 4f, 5f, 6f, 8f, 9f), delta);
        assertEquals(2, Stats.median(1f, 2f, 2f, 2f, 3f, 14f), delta);
        assertEquals(5, Stats.median(1f, 5f, 2f, 8f, 7f), delta);
        assertEquals(4, Stats.median(1f, 6f, 2f, 8f, 7f, 2f), delta);
        
        // samples from https://es.wikipedia.org/wiki/Mediana_(estad%C3%ADstica)
        assertEquals(5, Stats.median(2f, 3f, 4f, 4f, 5f, 5f, 5f, 6f, 6f), delta);
        assertEquals(9.5, Stats.median(7f, 8f, 9f, 10f, 11f, 12f), delta);
    }

    /**
     * Test of median method, of class Statistics.
     */
    @Test
    public void testMedian_doubleArr()
    {
        double delta = 0.0000001;
        // samples from https://en.wikipedia.org/wiki/Median
        assertEquals(6, Stats.median(1d, 3d, 3d, 6d, 7d, 8d, 9d), delta);
        assertEquals(4.5, Stats.median(1d, 2d, 3d, 4d, 5d, 6d, 8d, 9d), delta);
        assertEquals(2, Stats.median(1d, 2d, 2d, 2d, 3d, 14d), delta);
        assertEquals(5, Stats.median(1d, 5d, 2d, 8d, 7d), delta);
        assertEquals(4, Stats.median(1d, 6d, 2d, 8d, 7d, 2d), delta);
        
        // samples from https://es.wikipedia.org/wiki/Mediana_(estad%C3%ADstica)
        assertEquals(5, Stats.median(2d, 3d, 4d, 4d, 5d, 5d, 5d, 6d, 6d), delta);
        assertEquals(9.5, Stats.median(7d, 8d, 9d, 10d, 11d, 12d), delta);
    }

    //samples from https://admiralmarkets.com/education/articles/forex-indicators/exponential-moving-average
    double[][] MOVING_AVERAGE8 = 
    {
//        {168},
//        {170},
//        {171},
//        {175},
//        {170},
//        {172},
//        {176},
//        {179},
        {172.625},
//        {178, 172.625},
        {172.625, 172.625},
        {186, 175.5972},
        {192, 179.2423},
        {183, 180.0773},
        {177, 179.3935},
        {172, 177.7505},
        {167, 175.3615},
        {177, 175.7256},
        {180, 176.6755},
    };
    //https://github.com/jonschlinkert/exponential-moving-average
    double[][] MOVING_AVERAGE10 =
    {
        {22.27},
        {22.19},
        {22.08},
        {22.17},
        {22.18},
        {22.13},
        {22.23},
        {22.43},
        {22.24},
//        {22.29, 22.22},
        {22.15, 22.22},
        {22.15, 22.21},
        {22.39, 22.24},
        {22.38, 22.27},
        {22.61, 22.33},
        {23.36, 22.52},
        {24.05, 22.8},
        {23.75, 22.97},
        {23.83, 23.13},
        {23.95, 23.28},
        {23.63, 23.34},
        {23.82, 23.43},
        {23.87, 23.51},
        {23.65, 23.53},
        {23.19, 23.47},
        {23.10, 23.40},
        {23.33, 23.39},
        {22.68, 23.26},
        {23.10, 23.23},
        {22.40, 23.08},
        {22.17, 22.92},        
    };
    
    /**
     * Test of exponentialMovingAverage method, of class Statistics.
     */
    @Test
    public void testExponentialMovingAverage_3args_1()
    {
        double sma8 = MOVING_AVERAGE8[0][0];
        for(int i=0;i<MOVING_AVERAGE8.length;i++)
        {
            sma8 = Stats.exponentialMovingAverage(sma8, MOVING_AVERAGE8[i][0], 8);
            if(MOVING_AVERAGE8[i].length>1)
            {
                assertEquals(MOVING_AVERAGE8[i][1], sma8, 0.0001, "i="+i);
            }
        }
        
        double sma10 = MOVING_AVERAGE10[0][0];
        for(int i=0;i<MOVING_AVERAGE10.length;i++)
        {
            sma10 = Stats.exponentialMovingAverage(sma10, MOVING_AVERAGE10[i][0], 10);
            if(MOVING_AVERAGE10[i].length>1)
            {
                assertEquals(MOVING_AVERAGE10[i][1], sma10, 0.0099, "i="+i);
            }
        }
    }

    /**
     * Test of exponentialMovingAverage method, of class Statistics.
     */
    @Test
    public void testExponentialMovingAverage_3args_2()
    {
        MathContext mc = new MathContext(16 ,RoundingMode.HALF_UP);
        BigDecimal sma8 = BigDecimal.valueOf(MOVING_AVERAGE8[0][0]);
        for(int i=0;i<MOVING_AVERAGE8.length;i++)
        {
            sma8 = Stats.exponentialMovingAverage(sma8, BigDecimal.valueOf(MOVING_AVERAGE8[i][0]), 8, mc);
            if(MOVING_AVERAGE8[i].length>1)
            {
                assertEquals(MOVING_AVERAGE8[i][1], sma8.doubleValue(), 0.0001, "i="+i);
            }
        }
        
        BigDecimal sma10 = BigDecimal.valueOf(MOVING_AVERAGE10[0][0]);
        for(int i=0;i<MOVING_AVERAGE10.length;i++)
        {
            sma10 = Stats.exponentialMovingAverage(sma10, BigDecimal.valueOf(MOVING_AVERAGE10[i][0]), 10, mc);
            if(MOVING_AVERAGE10[i].length>1)
            {
                assertEquals(MOVING_AVERAGE10[i][1], sma10.doubleValue(), 0.0099, "i="+i);
            }
        }
    }
    /**
     * Test that the overloaded versions produce the same results
     */
    @Test
    public void testExponentialMovingAverage_same_results()
    {
        MathContext mc = new MathContext(16 ,RoundingMode.HALF_UP);
        double doubleSma8 = 100;
        BigDecimal decimalSma8 = BigDecimal.valueOf(doubleSma8);
        for(int i=0;i<10000;i++)
        {
            double d = i % 97;
            doubleSma8 = Stats.exponentialMovingAverage(doubleSma8, d, 8);
            decimalSma8 = Stats.exponentialMovingAverage(decimalSma8, BigDecimal.valueOf(d), 8, mc);
            assertEquals(doubleSma8, decimalSma8.doubleValue(), 0.00000001, "i="+i);
        }
    }
}
