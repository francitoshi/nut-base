/*
 * RequestLimit.java
 *
 * Copyright (c) 2018-2023 francitoshi@gmail.com
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
package io.nut.base.util;

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
                        long waitMillis = (long) (overflowNanos / Utils.NANOS_PER_MILLIS);
                        long waitNanos = (long) (overflowNanos % Utils.NANOS_PER_MILLIS);
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
                        long waitMillis = (long) (overflowNanos / Utils.NANOS_PER_MILLIS);
                        long waitNanos = (long) (overflowNanos % Utils.NANOS_PER_MILLIS);
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
