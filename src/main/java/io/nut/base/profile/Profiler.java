/*
 *  Profiler.java
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

import io.nut.base.text.Table;
import io.nut.base.time.JavaTime;
import java.io.PrintStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author franci
 */
public class Profiler
{
    public static class Task
    {
        public final String label;
        private final Object lock = new Object();

        private volatile boolean active;

        private volatile long accumNanos;
        private volatile long startNanos;
        private volatile long stopNanos;
        private volatile long stepNanos;
        private volatile long counter;
        private volatile long minNanos = Long.MAX_VALUE;
        private volatile long maxNanos = 0L;

        public Task(String label)
        {
            this.label = label;
            this.startNanos = this.stopNanos = System.nanoTime();
        }

        public Task start()
        {
            synchronized (lock)
            {
                this.startNanos = System.nanoTime();
                this.active = true;
            }
            return this;
        }

        public Task stop()
        {
            synchronized (lock)
            {
                this.active = false;
                this.stopNanos = System.nanoTime();
                this.stepNanos += this.stopNanos - this.startNanos;
                this.startNanos = this.stopNanos;
            }
            return this;
        }

        public void count()
        {
            synchronized (lock)
            {
                if (this.active)
                {
                    this.stopNanos = System.nanoTime();
                    this.stepNanos += this.stopNanos - this.startNanos;
                    this.startNanos = this.stopNanos;
                }
                else
                {
                    this.startNanos = this.stopNanos = System.nanoTime();
                }
                this.minNanos = Math.min(this.stepNanos, this.minNanos);
                this.maxNanos = Math.max(this.stepNanos, this.maxNanos);
                this.accumNanos += this.stepNanos;
                this.stepNanos = 0L;
                this.counter++;
            }
        }
        
        public long nanos()
        {
            synchronized (lock)
            {
                return accumNanos + stepNanos + (active ? System.nanoTime() - startNanos : stopNanos - startNanos);
            }
        }

        public long averageNanos()
        {
            synchronized (lock)
            {
                return nanos() / counter;
            }
        }
        
        public long millis()
        {
            return TimeUnit.NANOSECONDS.toMillis(nanos());
        }
        
        public long averageMillis()
        {
            return TimeUnit.NANOSECONDS.toMillis(averageNanos());
        }
        
        String[] data(JavaTime.Resolution resolution)
        {
            long ns = nanos();
            return new String[]
            {
                label,
                Long.toString(counter),
                JavaTime.toString(Duration.ofNanos(ns), 2, resolution), 
                counter > 0 ? perUnit(ns, counter) : "", 
                counter > 0 ? perSecond(ns, counter) : "",
                minNanos+"ns",
                maxNanos+"ns"
            };
        }

        public String toString(JavaTime.Resolution resolution)
        {
            StringJoiner sj = new StringJoiner(" ");
            for(String item : data(resolution))
            {
                sj.add(item);
            }
            return sj.toString();
        }

        @Override
        public String toString()
        {
            return toString(JavaTime.Resolution.MS);
        }
        
    }

    private final HashMap<String, Task> map = new HashMap<>();
    private final JavaTime.Resolution resolution;

    public Profiler(JavaTime.Resolution resolution)
    {
        this.resolution = resolution;
    }

    public Profiler()
    {
        this(JavaTime.Resolution.MS);
    }

    public Task getTask(String label)
    {
        Task task = map.getOrDefault(label, null);
        if(task==null)
        {
            map.put(label, task=new Task(label));
        }
        return task;
    }
    private static final String[] COL_NAMES = {"id","count","time","avg","speed","min","max"};
    public void print(PrintStream out)
    {
        Task[] tasks = map.values().toArray(new Task[0]);
        String[][] cells = new String[tasks.length][];
        for(int i=0;i<cells.length;i++)
        {
            cells[i] = tasks[i].data(resolution);
        }
        
        Table table = new Table(null,COL_NAMES,cells, true).setBorder(Table.Paint.BoxLight);
        out.println(table.toString());
    }
    
    public void print()
    {
        print(System.out);
    }

    public static String duration(long nanos)
    {
        long[] values = split(nanos);

        for (int i = 0; i < values.length; i++)
        {
            long val = values[i];
            if (val > 99)
            {
                return values[i] + UNITS[i];
            }
            if (val > 0)
            {
                String s = values[i] + UNITS[i];
                if (i + 1 < values.length && values[i + 1] > 0)
                {
                    s += values[i + 1] + UNITS[i + 1];
                }
                return s;
            }
        }
        return "0s";
    }

    public static String perUnit(long nanos, long count)
    {
        long n = nanos / count;
        return duration(n);
    }

    private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

    public static String perSecond(long nanos, long count)
    {
        double n = (count / (double) nanos) * NANOS_PER_SECOND;
        return String.format("%.3f/s", n);
    }

    private static final String[] UNITS = { "d", "h", "m", "s", "ms", "ns" };
    private static final int[] MAX = { 1, 24, 60, 60, 1000, 1000_000 };

    public static long[] split(long nanos)
    {
        long[] values = new long[UNITS.length];

        for (int i = UNITS.length - 1; i >= 0; i--)
        {
            values[i] = nanos % MAX[i];
            nanos /= MAX[i];
        }

        return values;
    }
}
