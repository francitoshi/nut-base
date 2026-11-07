/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.profile;

import io.github.francitoshi.nut.base.commons.time.JavaTime;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

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
        private final LatencyHistogram histogram = new LatencyHistogram();

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
                this.histogram.record(this.stepNanos, 1);
                this.stepNanos = 0L;
                this.counter++;
            }
        }
        
        public void count(int steps)
        {
            if(steps<=0)
            {
                throw new IllegalArgumentException("steps must be >0 but is "+steps);
            }
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
                this.minNanos = Math.min(this.stepNanos/steps, this.minNanos);
                this.maxNanos = Math.max(this.stepNanos/steps, this.maxNanos);
                this.accumNanos += this.stepNanos;
                this.histogram.record(this.stepNanos / steps, steps);
                this.stepNanos = 0L;
                this.counter += steps;
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
            return data(resolution, DEFAULT_PERCENTILES);
        }

        String[] data(JavaTime.Resolution resolution, double[] percentiles)
        {
            long ns = nanos();
            String[] cells = new String[7 + percentiles.length];
            cells[0] = label;
            cells[1] = Long.toString(counter);
            cells[2] = JavaTime.toString(Duration.ofNanos(ns), 2, resolution);
            cells[3] = counter > 0 ? JavaTime.toString(Duration.ofNanos(ns/counter), 2, resolution): "";
            cells[4] = counter > 0 ? perSecond(ns, counter) : "";
            cells[5] = JavaTime.toString(Duration.ofNanos(minNanos), 2, resolution);
            cells[6] = JavaTime.toString(Duration.ofNanos(maxNanos), 2, resolution);

            for (int i = 0; i < percentiles.length; i++)
            {
                cells[7 + i] = counter > 0
                    ? JavaTime.toString(Duration.ofNanos(nanosAtPercentile(percentiles[i])), 2, resolution)
                    : "";
            }

            return cells;
        }

        /**
         * Returns an estimate of the duration at the given percentile, 0 to
         * 100, from the histogram the task keeps: 0 is the exact minimum,
         * 100 the exact maximum and values in between interpolate inside the
         * log-buckets.
         *
         * @param percentile the percentile, 0 exclusive to 100 inclusive
         * @return the estimated duration in nanoseconds
         */
        public long nanosAtPercentile(double percentile)
        {
            if (percentile <= 0 || percentile > 100)
            {
                throw new IllegalArgumentException("percentile must be in (0, 100], got " + percentile);
            }

            synchronized (lock)
            {
                return histogram.quantileNanos(percentile / 100);
            }
        }

        /**
         * Returns an estimate of the duration at the given percentile, 0 to
         * 100, in milliseconds.
         *
         * @param percentile the percentile, 0 exclusive to 100 inclusive
         * @return the estimated duration in milliseconds
         */
        public long millisAtPercentile(double percentile)
        {
            return TimeUnit.NANOSECONDS.toMillis(nanosAtPercentile(percentile));
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

    /** The default percentile columns: the upper tail of the distribution. */
    public static final double[] DEFAULT_PERCENTILES = {90, 99, 99.9, 99.99};

    private final HashMap<String, Task> map = new HashMap<>();
    private final JavaTime.Resolution resolution;
    private final boolean sortByTime;
    private final double[] percentiles;

    public Profiler(JavaTime.Resolution resolution, boolean sortByTime)
    {
        this(resolution, sortByTime, DEFAULT_PERCENTILES);
    }

    /**
     * Creates a profiler that shows the given percentile columns on print.
     *
     * @param resolution the time resolution of the printed values
     * @param sortByTime true to sort the tasks by accumulated time
     * @param percentiles the percentiles to show, each in (0, 100], ascending;
     *                    50 is the median, 99.9 the upper tail; 0 (the
     *                    minimum) and 100 (the maximum) are always shown by
     *                    their own min/max columns
     */
    public Profiler(JavaTime.Resolution resolution, boolean sortByTime, double[] percentiles)
    {
        this.resolution = resolution;
        this.sortByTime = sortByTime;
        this.percentiles = validatePercentiles(percentiles);
    }

    private static double[] validatePercentiles(double[] percentiles)
    {
        if (percentiles == null || percentiles.length == 0)
        {
            throw new IllegalArgumentException("at least one percentile is required");
        }

        double[] copy = percentiles.clone();

        for (int i = 0; i < copy.length; i++)
        {
            if (Double.isNaN(copy[i]) || copy[i] <= 0 || copy[i] > 100)
            {
                throw new IllegalArgumentException("percentile must be in (0, 100], got " + copy[i]);
            }

            if (i > 0 && copy[i] <= copy[i - 1])
            {
                throw new IllegalArgumentException("percentiles must be ascending: " + Arrays.toString(copy));
            }
        }

        return copy;
    }

    /**
     * Returns the column label of a percentile: p50, p99.9, ...
     *
     * @param percentile the percentile value
     * @return the label
     */
    static String percentileLabel(double percentile)
    {
        return percentile == Math.rint(percentile)
            ? "p" + (long) percentile
            : "p" + percentile;
    }
    public Profiler(JavaTime.Resolution resolution)
    {
        this(resolution, false);
    }

    public Profiler()
    {
        this(JavaTime.Resolution.MS, false);
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

    private static final Comparator<Task> CMP_LABEL = (Task a, Task b) ->
    {
        return a.label.compareToIgnoreCase(b.label);
    };

    private static final Comparator<Task> CMP_TIME = (Task a, Task b) ->
    {
        int cmp = Long.compare(a.accumNanos, b.accumNanos);
        if(cmp==0)
        {
            cmp = a.label.compareToIgnoreCase(b.label);
        }
        return cmp;
    };

    public void print(PrintStream out)
    {
        Task[] tasks = map.values().toArray(new Task[0]);
        if(sortByTime)
        {
            Arrays.sort(tasks, CMP_TIME);
        }
        else
        {
            Arrays.sort(tasks, CMP_LABEL);
        }
        
        String[][] cells = new String[tasks.length][];
        for(int i=0;i<cells.length;i++)
        {
            cells[i] = tasks[i].data(resolution, percentiles);
        }

        String[] colNames = new String[COL_NAMES.length + percentiles.length];
        System.arraycopy(COL_NAMES, 0, colNames, 0, COL_NAMES.length);
        for(int i=0;i<percentiles.length;i++)
        {
            colNames[COL_NAMES.length + i] = percentileLabel(percentiles[i]);
        }

        boolean[] right = new boolean[colNames.length];
        for(int col=1;col<right.length;col++)
        {
            right[col] = true;
        }

        int[] widths = new int[colNames.length];
        for(int col=0;col<widths.length;col++)
        {
            widths[col] = colNames[col].length();
            for(int row=0;row<cells.length;row++)
            {
                int len = cells[row][col].length();
                if(len>widths[col])
                {
                    widths[col] = len;
                }
            }
        }
        
        BiFunction<String, String, String> border = (left, cross) ->
        {
            StringBuilder sb = new StringBuilder(left);
            for(int col=0;col<widths.length;col++)
            {
                if(col>0)
                {
                    sb.append(cross);
                }
                for(int i=0;i<widths[col]+2;i++)
                {
                    sb.append('─');
                }
            }
            return sb.toString();
        };
        
        BiFunction<String[], boolean[], String> format = (values, aligns) ->
        {
            StringBuilder sb = new StringBuilder();
            for(int col=0;col<values.length;col++)
            {
                String value = values[col];
                sb.append("│ ");
                if(aligns[col])
                {
                    for(int i=value.length();i<widths[col];i++)
                    {
                        sb.append(' ');
                    }
                    sb.append(value);
                }
                else
                {
                    sb.append(value);
                    for(int i=value.length();i<widths[col];i++)
                    {
                        sb.append(' ');
                    }
                }
                sb.append(' ');
            }
            return sb.append('│').toString();
        };
        
        out.println(border.apply("┌", "┬")+"┐");
        out.println(format.apply(colNames, right));
        out.println(border.apply("├", "┼")+"┤");
        for(int row=0;row<cells.length;row++)
        {
            out.println(format.apply(cells[row], right));
        }
        out.println(border.apply("└", "┴")+"┘");
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

    private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

    public static String perSecond(long nanos, long count)
    {
        double n = (count / (double) nanos) * NANOS_PER_SECOND;
        return String.format("%.3f/s", n);
    }

    private static final String[] UNITS = { "d", "h", "m", "s", "ms", "ns" };
    private static final int[] FRACTIONS = { 1, 24, 60, 60, 1000, 1000_000 };

    public static long[] split(long nanos)
    {
        long[] values = new long[UNITS.length];
        long r = nanos;
        for (int i = UNITS.length - 1; i >= 0; i--)
        {
            values[i] = r % FRACTIONS[i];
            r /= FRACTIONS[i];
        }

        return values;
    }
}
