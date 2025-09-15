/*
 *  ThrottledInputStream.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
package io.nut.base.io;

import io.nut.base.util.Utils;
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
    private final boolean average;
    private volatile long byteNanoTime;
    private volatile long lineNanoTime;
    private volatile long untilNanoTime;

    /**
     * Creates a throttled InputStream.
     *
     * @param in underlying InputStream (not null)
     * @param tpb time per byte
     * @param tpl time per line break ('\n')
     * @param timeUnit unit for tpb/tpl
     * @param average if true, regulates to keep average rate; if false,
     * enforces delay per write
     */
    public ThrottledInputStream(InputStream in, int tpb, int tpl, TimeUnit timeUnit, boolean average)
    {
        super(Objects.requireNonNull(in, "InputStream must not be null"));
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
    public int read() throws IOException
    {
        synchronized (lock)
        {
            int b = super.read();
            if (b != -1)
            {
                applyDelays(b);
            }
            return b;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        synchronized (lock)
        {
            int bytesRead = super.read(b, off, len);
            if (bytesRead > 0)
            {
                for (int i = 0; i < bytesRead; i++)
                {
                    applyDelays(b[off + i]);
                }
            }
            return bytesRead;
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
        Utils.parkUntilNanoTime(this.untilNanoTime);
    }
}
