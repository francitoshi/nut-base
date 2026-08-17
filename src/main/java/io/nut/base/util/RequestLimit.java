/*
 * Copyright (C) 2010-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import io.nut.base.time.JavaTime;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class RequestLimit
{
    private final Object lock = new Object();
    private final boolean debug = false;
    
    private final long nanosPerRequest;
    private final long nanoLimit;
    private volatile long nanoTime; 
    private volatile boolean terminated; 
    private final String tag;

    public RequestLimit(int countLimit, int millisLimit, String... tags)
    {
        this.nanosPerRequest = TimeUnit.MILLISECONDS.toNanos(millisLimit/countLimit);
        this.nanoLimit = countLimit*this.nanosPerRequest;
        this.nanoTime = System.nanoTime() - this.nanoLimit;
        this.tag = tags!=null && tags.length>0 ? Strings.join(".", tags) : null;
    }

    public boolean isTerminated()
    {
        return terminated;
    }
    
    public void terminate()
    {
        synchronized(this.lock)
        {
            this.terminated = true;
            this.lock.notifyAll();
        }
    }
    
    public void update()
    {
        this.update(1);
    }
    public void update(double count)
    {
        if(count>0)
        {
            try
            {
                synchronized(lock)
                {
                    this.nanoTime = limit(this.nanoTime + (long)(count*this.nanosPerRequest));
                    long overflowNanos = 0L;
                    while((overflowNanos=this.nanoTime-System.nanoTime())>0L)
                    {
                        long waitMillis = (long) (overflowNanos / JavaTime.NANOS_PER_MILLIS);
                        long waitNanos = (long) (overflowNanos % JavaTime.NANOS_PER_MILLIS);
                        lock.wait(waitMillis, (int) waitNanos);
                    }
                }
            }
            catch(InterruptedException ex)
            {
                Logger.getLogger(RequestLimit.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    public void waitAvailable()
    {
        this.waitAvailable(1);
    }
    public void waitAvailable(double count)
    {
        if(count>0)
        {
            try
            {
                synchronized(lock)
                {
                    long waitNanoTime = limit(this.nanoTime + (long)(count*this.nanosPerRequest));
                    long overflowNanos = 0L;
                    while((overflowNanos=waitNanoTime-System.nanoTime())>0L)
                    {
                        long waitMillis = (long) (overflowNanos / JavaTime.NANOS_PER_MILLIS);
                        long waitNanos = (long) (overflowNanos % JavaTime.NANOS_PER_MILLIS);
                        lock.wait(waitMillis, (int) waitNanos);
                    }
                }
            }
            catch(InterruptedException ex)
            {
                Logger.getLogger(RequestLimit.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private long limit(long value)
    {
        return Math.max(value, System.nanoTime() - this.nanoLimit);
    }
}
