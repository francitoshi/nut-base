/*
 *  Timing.java
 *
 *  Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.debug;

import io.nut.base.time.JavaTime;
import java.time.Duration;

/**
 *
 * @author franci
 */
public abstract class Timing
{
    public final int elements;
    public final JavaTime.Resolution resolution;

    public Timing(int elements, JavaTime.Resolution resolution)
    {
        this.elements = elements;
        this.resolution = resolution;
    }
    
    public static Timing getSecondsInstance(int elements, boolean muted)
    {
        return muted ? new NullTiming(elements, JavaTime.Resolution.S) : new NormalTiming(elements, JavaTime.Resolution.S);
    }

    public static Timing getSecondsInstance(int elements)
    {
        return getSecondsInstance(elements, false);
    }

    public static Timing getMillisInstance(int elements, boolean muted)
    {
        return muted ? new NullTiming(elements, JavaTime.Resolution.MS) : new NormalTiming(elements, JavaTime.Resolution.MS);
    }

    public static Timing getMillisInstance(int elements)
    {
        return getMillisInstance(elements, false);
    }
    
    public static Timing getNanosInstance(int elements, boolean muted)
    {
        return muted ? new NullTiming(elements, JavaTime.Resolution.NS) : new NormalTiming(elements, JavaTime.Resolution.NS);
    }
    public static Timing getNanosInstance(int elements)
    {
        return getNanosInstance(elements, false);
    }
    
    public String format(String tag, long startNanos, long stopNanos)
    {
        StringBuilder sb = new StringBuilder();
        if(tag!=null && !tag.isEmpty())
        {
            sb.append(tag).append("=");
        }
        sb.append(JavaTime.toString(Duration.ofNanos(stopNanos-startNanos), elements, resolution));
        return sb.toString();
    }
    public abstract void println(String tag, long startNanos, long stopNanos);
    //public abstract void log(Level level, String tag, long startNanos, long stopNanos);
 
    private static class NullTiming extends Timing
    {
        public NullTiming(int elements, JavaTime.Resolution resolution)
        {
            super(elements, resolution);
        }

        @Override
        public void println(String tag, long startNanos, long stopNanos)
        {
        }
    }
    
    private static class NormalTiming extends Timing
    {
        public NormalTiming(int elements, JavaTime.Resolution resolution)
        {
            super(elements, resolution);
        }

        @Override
        public void println(String tag, long startNanos, long stopNanos)
        {
            System.out.println(format(tag, startNanos, stopNanos));
        }
    }
    
}
