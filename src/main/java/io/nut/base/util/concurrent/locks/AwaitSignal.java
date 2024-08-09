/*
 *  AwaitSignal.java
 *
 *  Copyright (C) 2007-2024 francitoshi@gmail.com
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
package io.nut.base.util.concurrent.locks;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author franci
 */
public class AwaitSignal
{
    private final AtomicInteger waiting = new AtomicInteger();
    private final Object lock = new Object();

    public final void await() throws InterruptedException
    {
        synchronized (lock)
        {
            waiting.incrementAndGet();
            lock.wait();
            waiting.decrementAndGet();
        }
    }
    
    public final void await(long timeout) throws InterruptedException
    {
        synchronized (lock)
        {
            waiting.incrementAndGet();
            lock.wait(timeout);
            waiting.decrementAndGet();
        }
    }

    public final void await(long timeout, int nanos) throws InterruptedException
    {
        synchronized (lock)
        {
            waiting.incrementAndGet();
            lock.wait(timeout, nanos);
            waiting.decrementAndGet();
        }
    }

    public final void signal()
    {
        if(waiting.get()>0)
        {
            synchronized (lock)
            {
                lock.notify();
            }
        }
    }

    public final void signalAll()
    {
        if(waiting.get()>0)
        {
            synchronized(lock)
            {
                lock.notifyAll();
            }
        }
    }
}
