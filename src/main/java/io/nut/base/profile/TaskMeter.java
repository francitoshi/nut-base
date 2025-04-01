/*
 *  TaskMeter.java
 *
 *  Copyright (C) 2023-2025 francitoshi@gmail.com
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
package io.nut.base.profile;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author franci
 */
public class TaskMeter
{
    private volatile long startNanos = System.nanoTime();
    private volatile long stopNanos;
    private volatile long counter;

    public TaskMeter start()
    {
        this.startNanos = System.nanoTime();
        return this;
    }

    public TaskMeter count()
    {
        this.counter++;
        return this;
    }
    public TaskMeter stop()
    {
        this.stopNanos = System.nanoTime();
        return this;
    }
    public long millis()
    {
        long currNanos = System.nanoTime();
        long nanos = currNanos-this.startNanos;
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }
    public long averageMillis()
    {
        long currNanos = System.nanoTime();
        long nanos = currNanos-this.startNanos;
        long averageNanos = nanos/counter;
        return TimeUnit.NANOSECONDS.toMillis(averageNanos);
    }
    
    public TaskMeter log(String tag)
    {
        long nanos = stopNanos>startNanos ? stopNanos-startNanos : System.nanoTime()-startNanos;
        String s = duration(nanos);
        String pu = counter!=0 ? perUnit(nanos, counter) : "";
        String ps = counter!=0 ? perSecond(nanos, counter) : "";
        System.out.println(tag+" "+s+" "+pu.trim()+" "+ps.trim());
        return this;
    }
    public TaskMeter log()
    {
        log("");
        return this;
    }
    public static String duration(long nanos)
    {
        long[] values = split(nanos);
                
        for(int i=0;i<values.length;i++)
        {
            long val = values[i];
            if(val>99)
            {
                return values[i]+UNITS[i];
            }
            if(val>0)
            {
                String s = values[i]+UNITS[i];
                if(i+1<values.length && values[i+1]>0)
                {
                    s += values[i+1]+UNITS[i+1];
                }
                return s;
            }
        }
        return "0s";
    }
    public static String perUnit(long nanos, long count)
    {
        long n = nanos / count;
        return "["+duration(n)+" * "+count+"]";
    }
    
    private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);
    public static String perSecond(long nanos, long count)
    {
        double n = (count / (double)nanos) * NANOS_PER_SECOND;
        return "("+n+" / s )";
    }
    
    private static final String[] UNITS = {"d","h","m","s","ms","ns"};
    private static final int[] MAX = {1,24,60,60,1000,1000_000};
    
    public static long[] split(long nanos)
    {
        long[] values = new long[UNITS.length];
        
        for(int i=UNITS.length-1;i>=0;i--)
        {
            values[i] = nanos % MAX[i];
            nanos /= MAX[i];
        }
        
        return values;
    }
}
