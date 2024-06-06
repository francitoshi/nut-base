/*
 *  DelayOutputStream.java
 *
 *  Copyright (C) 2024 francitoshi@gmail.com
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
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DelayOutputStream extends FilterOutputStream
{

    private final Object lock = new Object();
    private final long nanosPerByte;
    private final long nanosPerLine;
    private volatile int bytes;
    private volatile int lines;
    private final int period;
    private final Queue<Long> byteQueue;
    private final Queue<Long> lineQueue;

    public DelayOutputStream(OutputStream out, int mpb, int mpl, int period)
    {
        super(out);
        this.nanosPerByte = TimeUnit.MILLISECONDS.toNanos(mpb);
        this.nanosPerLine = TimeUnit.MILLISECONDS.toNanos(mpl);
        this.period = period;
        this.byteQueue = new ArrayBlockingQueue<>(period);
        this.lineQueue = new ArrayBlockingQueue<>(period);
    }

    @Override
    public void write(int b) throws IOException
    {
        synchronized (lock)
        {
            final long nanos = System.nanoTime();
            if(nanosPerByte>0)
            {
                waitForNanoTime(byteQueue, nanos, period, ++bytes, nanosPerByte);
            }
            if(nanosPerLine>0 && b == '\n')
            {
                waitForNanoTime(lineQueue, nanos, period, ++lines, nanosPerLine);
            }
        }
        out.write(b);
    }

    private static void waitForNanoTime(Queue<Long> queue, long nanoTime, int period, int count, long nanosPerItem)
    {
        queue.offer(nanoTime);
        final long first = count >= period ? queue.poll() : queue.peek();
        long until = first + Math.min(count, period) * nanosPerItem;
        long waitNanos = Math.max(until - nanoTime, 0);
        if (waitNanos > 0)
        {
            long ms = waitNanos / Utils.NANOS_PER_MILLIS;
            int ns = (int) (waitNanos % Utils.NANOS_PER_MILLIS);
            Utils.sleep(ms, ns);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        synchronized (lock)
        {
            for (int i = off; i < off + len; i++)
            {
                write(b[i]);
            }
        }
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        write(b, 0, b.length);
    }

    public static void main(String... args) throws IOException
    {

        PrintStream out = new PrintStream(new DelayOutputStream(System.out, 0, 100, 10));
        out.println("Hello, World!");
        out.println("This is a delayed output test.");
        for (int i = 0; i < 100; i++)
        {
            out.println(i + " " + LocalDateTime.now());
        }
    }
}
