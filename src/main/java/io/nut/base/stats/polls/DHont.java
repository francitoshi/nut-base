/*
 *  DHont.java
 *
 *  Copyright (C) 2015-2026 Francisco Gómez Carrasco
 *
 *  Report bugs or new features to: flikxxi@gmail.com
 *
 */
package io.nut.base.stats.polls;

import io.nut.base.math.Nums;

/**
 *
 * @author franci
 */
public class DHont
{
    private int getMaxIndex(int[] votes)
    {
        int max=votes[0];
        int index=0;
        for(int i=1;i<votes.length;i++)
        {
            if(votes[i]>max)
            {
                max=votes[i];
                index=i;
            }
        }
        return index;
    }
    
    private final int seats;
    private final double min;
    public DHont(int seats)
    {
        this.seats = seats;
        this.min = 1.0;
    }
    public DHont(int seats, double min)
    {
        this.seats = seats;
        this.min   = min;
    }
    public int[] distribute(int[] votes)
    {
        int[] v = votes.clone();
        int[] s = new int[votes.length];
        if(min<1.0)
        {
            int min = (int) (Nums.sum(votes)*this.min);
            for(int i=0;i<v.length;i++)
            {
                if(v[i]<min)
                {
                    v[i]=0;
                }
            }
        }
        
        for(int i=0;i<this.seats;i++)
        {
            int max = getMaxIndex(v);
            s[max]++;
            v[max] = votes[max]/(s[max]+1);
        }
        return s;
    }
}
