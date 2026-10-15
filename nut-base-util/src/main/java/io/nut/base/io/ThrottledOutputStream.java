/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.io;

import io.nut.base.lang.Pauses;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe OutputStream wrapper that throttles the data flow by introducing
 * delays per byte and per line.
 */
public final class ThrottledOutputStream extends FilterOutputStream
{

    private final Object lock = new Object();
    private final long nanosPerByte;
    private final long nanosPerLine;
    private final boolean average;
    private volatile long byteNanoTime;
    private volatile long lineNanoTime;
    private volatile long untilNanoTime;

    /**
     * Creates a throttled OutputStream.
     *
     * @param out underlying OutputStream (not null)
     * @param tpb time per byte
     * @param tpl time per line break ('\n')
     * @param timeUnit unit for tpb/tpl
     * @param average if true, regulates to keep average rate; if false,
     * enforces delay per write
     */
    public ThrottledOutputStream(OutputStream out, int tpb, int tpl, TimeUnit timeUnit, boolean average)
    {
        super(Objects.requireNonNull(out, "out must not be null"));
        if (tpb < 0 || tpl < 0)
        {
            throw new IllegalArgumentException("Delays must not be negative");
        }
        this.nanosPerByte = timeUnit.toNanos(tpb);
        this.nanosPerLine = timeUnit.toNanos(tpl);
        this.untilNanoTime = this.lineNanoTime = this.byteNanoTime = System.nanoTime();
        this.average = average;
    }

    @Override
    public void write(int b) throws IOException
    {
        synchronized (lock)
        {
            super.write(b);
            applyDelays(b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        synchronized (lock)
        {
            super.write(b, off, len);
            for (int i = 0; i < len; i++)
            {
                applyDelays(b[off + i]);
            }
        }
    }

    private void applyDelays(int b)
    {
        if(average)
        {
            this.byteNanoTime += this.nanosPerByte;
            if (b == '\n')
            {
                this.lineNanoTime += this.nanosPerLine;
            }
        }
        else
        {
            long now = System.nanoTime();
            this.byteNanoTime = Math.max(now, this.byteNanoTime) + this.nanosPerByte;
            if (b == '\n')
            {
                this.lineNanoTime = Math.max(now, this.lineNanoTime) + this.nanosPerLine;
            }
        }
        this.untilNanoTime = Math.max(this.byteNanoTime, this.lineNanoTime);
        Pauses.parkUntilNanoTime(this.untilNanoTime);
    }
}
