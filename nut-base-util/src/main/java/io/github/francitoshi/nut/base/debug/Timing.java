/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.debug;

import io.github.francitoshi.nut.base.commons.time.JavaTime;
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
