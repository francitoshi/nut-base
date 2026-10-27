/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io;

import io.github.francitoshi.nut.base.commons.lang.Pauses;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe InputStream wrapper that throttles the data flow by introducing 
 * delays per byte and per line.
 */
public final class ThrottledInputStream extends FilterInputStream
{
    private final Object lock = new Object();
    private final long nanosPerByte;
    private final long nanosPerLine;
    private final long nanosOnClose;
    private final boolean average;
    private volatile long byteNanoTime;
    private volatile long lineNanoTime;
    private volatile long untilNanoTime;
    private volatile boolean endReached;
    private volatile long parkedNanos;
    private volatile boolean singleLine;

    /**
     * Creates a throttled InputStream.
     *
     * @param in underlying InputStream (not null)
     * @param tpb time per byte
     * @param tpl time per line break ('\n')
     * @param toc time on close
     * @param timeUnit unit for tpb/tpl/toc
     * @param average if true, regulates to keep average rate; if false,
     * enforces delay per write
     */
    public ThrottledInputStream(InputStream in, int tpb, int tpl, int toc, TimeUnit timeUnit, boolean average)
    {
        super(Objects.requireNonNull(in, "in must not be null"));
        if (tpb < 0 || tpl < 0)
        {
            throw new IllegalArgumentException("Delays must not be negative");
        }
        this.nanosPerByte = timeUnit.toNanos(tpb);
        this.nanosPerLine = timeUnit.toNanos(tpl);
        this.nanosOnClose = timeUnit.toNanos(toc);
        this.untilNanoTime = this.lineNanoTime = this.byteNanoTime = System.nanoTime();
        this.average = average;
    }

    /**
     * Sets single-line mode. In this mode it always return when a LF characer
     * is reached.
     * @param singleLine
     * @return
     */
    public ThrottledInputStream setSingleLine(boolean singleLine)
    {
        this.singleLine = singleLine;
        return this;
    }

    @Override
    public int read() throws IOException
    {
        synchronized (lock)
        {
            int b = super.read();
            parkedNanos = applyDelays(b);
            return b;
        }
    }

    @Override
    public int read(byte[] b) throws IOException
    {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        synchronized (lock)
        {
            int bytesRead=0;
            for(int i=0;i<len;i++)
            {
                int bb = this.read();
                if(bb==-1)
                {
                    return i==0 ? bb : bytesRead;
                }
                b[off+bytesRead++] = (byte) bb;
                if(bb=='\n' && (singleLine || parkedNanos>0))
                {
                    return bytesRead;
                }
            }
            return bytesRead;
        }
    }

    private long applyDelays(int b)
    {
        if(b==-1)
        {
            if(!this.endReached)
            {
                this.untilNanoTime += this.nanosOnClose;
                this.endReached=true;
            }
        }
        else if(average)
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
        this.untilNanoTime = Math.max(this.untilNanoTime, Math.max(this.byteNanoTime, this.lineNanoTime));
        return Pauses.parkUntilNanoTime(this.untilNanoTime);
    }
}
