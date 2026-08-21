/*
 * Copyright (C) 2017-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import io.nut.base.math.Nums;


/**
 *
 * @author franci
 */
public class BenfordLaw
{
    //computes the probability that the first digit of a value is value in Benford's Law
    public static double probability(int value, int radix)
    {
        if(value==0)
        {
            return 0;
        }
        if(value<0)
        {
            value = -value;
        }
        return Nums.log(radix, 1.0+1.0/value);
    }
    
    private final int radix;
    private volatile int count;
    private final int[] hits;

    public BenfordLaw()
    {
        this(10);
    }
    public BenfordLaw(int radix)
    {
        this.radix = radix;
        this.count = 0;
        this.hits = new int[radix];
    }
    
    public void update(int value)
    {
        value = value>=0 ? value : -value;
        while(value>=radix)
        {
            value /= radix;
        }
        this.count++;
        this.hits[value]++;
    }
    public void update(double value)
    {
        value = value>=0 ? value : -value;
        while(value>=radix)
        {
            value /= radix;
        }
        while(value<1 && value>0)
        {
            value *= radix;
        }
        int v = (int)value;
        this.count++;
        this.hits[v]++;
    }
    public void update(float value)
    {
        value = value>=0 ? value : -value;
        while(value>=radix)
        {
            value /= radix;
        }
        while(value<1 && value>0)
        {
            value *= radix;
        }
        this.count++;
        this.hits[(int)value]++;
    }
    
    public double frequency(int digit)
    {
        return count>0 ? (double)this.hits[digit]/(double)count : 0;
    }
    public double[] frequency()
    {
        double[] freq = new double[radix];
        for(int i=0;i<this.radix;i++)
        {
            freq[i] = this.frequency(i);
        }
        return freq;
    }
    public double delta(int digit)
    {
        double f = frequency(digit);
        double p = probability(digit, radix);
        return Math.abs(f-p);
    }
    public double delta()
    {
        double d = 0;
        for(int i=1;i<radix;i++)
        {
            double f = frequency(i);
            double p = probability(i, radix);
            
            double diff = Math.abs(f-p);
            d = Math.max(diff/p, d);
        }
        return d;
    }
    
}
