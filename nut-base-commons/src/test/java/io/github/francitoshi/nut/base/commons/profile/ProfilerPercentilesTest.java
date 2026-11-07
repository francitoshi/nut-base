/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.profile;

import io.github.francitoshi.nut.base.commons.time.JavaTime;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the percentile columns the Profiler prints from the
 * LatencyHistogram of every task.
 */
public class ProfilerPercentilesTest
{
    private static String render(Profiler profiler) throws UnsupportedEncodingException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        profiler.print(new PrintStream(out, true, StandardCharsets.UTF_8.name()));
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    public void defaultColumnsAreShown() throws UnsupportedEncodingException
    {
        Profiler profiler = new Profiler(JavaTime.Resolution.MS, false);

        for (double p : Profiler.DEFAULT_PERCENTILES)
        {
            String label = Profiler.percentileLabel(p);
            assertTrue(render(profiler).contains(label), "the table must contain " + label);
        }
    }

    @Test
    public void customPercentileColumns() throws UnsupportedEncodingException
    {
        Profiler profiler = new Profiler(JavaTime.Resolution.MS, false, new double[]{10, 25, 33.3, 75});

        String table = render(profiler);
        assertTrue(table.contains("p10"));
        assertTrue(table.contains("p25"));
        assertTrue(table.contains("p33.3"));
        assertTrue(table.contains("p75"));
        assertFalse(table.contains("p50"), "not printed unless asked");
        assertFalse(table.contains("p95"), "not printed unless asked");
    }

    @Test
    public void emptyTasksRenderBlankColumns() throws UnsupportedEncodingException
    {
        Profiler profiler = new Profiler(JavaTime.Resolution.MS, false);
        profiler.getTask("nothing");

        assertFalse(render(profiler).isEmpty());
        assertTrue(render(profiler).contains("nothing"));
    }

    @Test
    public void countedTaskFillsTheColumnsAscending() throws UnsupportedEncodingException
    {
        Profiler profiler = new Profiler(JavaTime.Resolution.MS, false, new double[]{50, 90, 99});

        Profiler.Task task = profiler.getTask("hot");
        for (int i = 0; i < 100; i++)
        {
            task.start();
            long busy = 0;
            for (int j = 0; j < 200; j++)
            {
                busy += (j * j) % 7;
            }
            task.stop().count(1);
        }

        assertTrue(task.nanos() > 0);

        long p50 = task.nanosAtPercentile(50);
        long p90 = task.nanosAtPercentile(90);
        long p99 = task.nanosAtPercentile(99);

        assertTrue(p50 > 0);
        assertTrue(p50 <= p90 && p90 <= p99, "quantiles cannot decrease: " + p50 + "," + p90 + "," + p99);
        assertTrue(task.nanosAtPercentile(100) >= p99);

        assertTrue(render(profiler).contains("hot"));
    }

    @Test
    public void percentileValidation()
    {
        assertThrows(IllegalArgumentException.class, () -> new Profiler(JavaTime.Resolution.MS, false, new double[]{}));
        assertThrows(IllegalArgumentException.class, () -> new Profiler(JavaTime.Resolution.MS, false, new double[]{50, 50}));
        assertThrows(IllegalArgumentException.class, () -> new Profiler(JavaTime.Resolution.MS, false, new double[]{90, 50}));
        assertThrows(IllegalArgumentException.class, () -> new Profiler(JavaTime.Resolution.MS, false, new double[]{0}));
        assertThrows(IllegalArgumentException.class, () -> new Profiler(JavaTime.Resolution.MS, false, new double[]{101}));
        assertThrows(IllegalArgumentException.class, () -> new Profiler(JavaTime.Resolution.MS, false, null));
    }

    @Test
    public void queryValidation()
    {
        Profiler.Task task = new Profiler().getTask("query-validation");
        task.start();
        task.stop().count(3);

        assertThrows(IllegalArgumentException.class, () -> task.nanosAtPercentile(0));
        assertThrows(IllegalArgumentException.class, () -> task.nanosAtPercentile(-1));
        assertThrows(IllegalArgumentException.class, () -> task.nanosAtPercentile(100.1));
        assertEquals(0, task.millisAtPercentile(100));
    }
}
