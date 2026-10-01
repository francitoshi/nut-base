/*
 * Copyright (C) 2015-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats.polls;

import io.nut.base.lang.Maths;

/**
 *
 * @author franci
 */
public class DHont
{
    private int getMaxIndex(int[] votes)
    {
        // FIX: guard against empty array – caller (distribute) already checks
        // this, but defensive programming avoids a silent ArrayIndexOutOfBoundsException
        // if getMaxIndex is ever called from another path in the future.
        if (votes.length == 0)
        {
            throw new IllegalArgumentException("votes must not be empty");
        }
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
        // FIX: reject null or empty input with a clear message instead of
        // letting votes[0] in getMaxIndex throw ArrayIndexOutOfBoundsException.
        if (votes == null)
        {
            throw new IllegalArgumentException("votes must not be null");
        }
        if (votes.length == 0)
        {
            throw new IllegalArgumentException("votes must not be empty");
        }

        int[] v = votes.clone();
        int[] s = new int[votes.length];
        if(min<1.0)
        {
            int min = (int) (Maths.sum(votes)*this.min);
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
