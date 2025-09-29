/*
 *  UptimeTiming.java
 *
 *  Copyright (c) 2023-2025 francitoshi@gmail.com
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


import io.nut.base.util.Utils;
import java.io.PrintStream;
import java.util.Locale;

public abstract class UptimeTiming
{
    public enum Unit
    {
        Seconds(Utils.SECOND_NANOS,"s"), Millis(Utils.NANOS_PER_MILLIS,"ms"), Nanos(1,"ns");

        Unit(long unitNanos, String unitName)
        {
            this.unitNanos = unitNanos;
            this.unitName = unitName;
        }
        final long unitNanos;
        final String unitName;
    }

    private static class Holder
    {
        private static final UptimeTiming UPTIME = UptimeTiming.getInstance(false, "[uptime]", 16, Unit.Millis, System.out);
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
        return fake ? new FakeTracer(Unit.Millis) : new RealTracer(name, min, Unit.Millis, System.out);
    }
    public static UptimeTiming getUptime()
    {
        return Holder.UPTIME;
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
            synchronized(lock)
            {
                this.start = now;
                this.last  = now;
                this.out = out;
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
