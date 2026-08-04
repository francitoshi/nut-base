/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.profile;


import java.io.PrintStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public abstract class UptimeTiming
{
    public enum Unit
    {
        S(TimeUnit.SECONDS,"s"), MS(TimeUnit.MILLISECONDS,"ms"), NS(TimeUnit.NANOSECONDS,"ns");

        Unit(TimeUnit timeUnit, String unitName)
        {
            this.unitNanos = timeUnit.toNanos(1);
            this.unitName = unitName;
        }
        final long unitNanos;
        final String unitName;
    }

    private static class Holder
    {
        private static final UptimeTiming ROOT = UptimeTiming.getInstance(false, "[root]", 16, Unit.MS, System.out);
    }

    public static UptimeTiming getInstance(boolean fake, String name, int min, Unit unit, PrintStream out)
    {
        return fake ? new FakeTracer(unit) : new RealTracer(name, min, unit, out);
    }
    public static UptimeTiming getInstance(boolean fake, String name, int min, Unit unit)
    {
        return fake ? new FakeTracer(unit) : new RealTracer(name, min, unit, System.out);
    }
    public static UptimeTiming getInstance(boolean fake, String name, int min)
    {
        return fake ? new FakeTracer(Unit.MS) : new RealTracer(name, min, Unit.MS, System.out);
    }
    public static UptimeTiming getRootInstance()
    {
        return Holder.ROOT;
    }

    public abstract void trace(String pointName);
    public abstract void trace(String pointName, int min);
    public abstract long uptime();

    static class RealTracer extends UptimeTiming
    {
        private final Object lock = new Object();
        final String name;
        final int min;
        final long start;
        final PrintStream out;
        volatile long last;

        final long unitNanos;
        final String unitName;

        private RealTracer(String name, int min, Unit unit, PrintStream out)
        {
            final long now = System.nanoTime();
            this.name = name;
            this.min = min;
            this.unitNanos = unit.unitNanos;
            this.unitName = unit.unitName;
            this.start = now;
            this.last  = now;
            this.out = out;
            synchronized(lock)
            {
                this.out.println(format("<init>", 0, 0));
                this.last = System.nanoTime();
            }
        }

        @Override
        public void trace(String pointName)
        {
            trace(pointName, this.min);
        }
        @Override
        public void trace(String pointName, int min)
        {
            final long now = System.nanoTime();
            synchronized(lock)
            {
                long unitCount = (now - last) / unitNanos;
                if(min==0 || min <= unitCount)
                {
                    long unitPoint = (now - start) / unitNanos;
                    this.out.println(format(pointName, unitPoint, unitCount));
                }
                this.last = System.nanoTime();
            }
        }

        @Override
        public long uptime()
        {
            return (System.nanoTime() - start) / unitNanos;
        }

        private String format(String pointName, long now, long delta)
        {
            return String.format(Locale.ROOT, "[timing].%-32s = %8d%s + %6d%s", name + "." + pointName, now, unitName, delta, unitName);
        }
    }
    static class FakeTracer extends UptimeTiming
    {
        final long start;
        final long unitNanos;

        public FakeTracer(Unit unit)
        {
            this.start = System.nanoTime();
            this.unitNanos = unit.unitNanos;
        }

        @Override
        public void trace(String pointName)
        {
        }

        @Override
        public void trace(String pointName, int min)
        {
        }

        @Override
        public long uptime()
        {
            return (System.nanoTime() - start) / unitNanos;
        }
    }
}
