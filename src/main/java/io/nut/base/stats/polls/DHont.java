/*
 *  DHont.java
 *
 *  Copyright (C) 2015-2026 francitoshi@gmail.com
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
