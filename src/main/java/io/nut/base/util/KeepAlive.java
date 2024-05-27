/*
 * KeepAlive.java
 *
 * Copyright (c) 2017-2023 francitoshi@gmail.com
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class keep the app alive by keeping a non-daemon thread waiting in an Object.wait() call. 
 * The start() method must be called in a non-daemon thread in order to work. The start() method
 * can be called multiple times, but close() just need to be called once to remove all start() calls.
 * 
 * @author franci
 */
public class KeepAlive<E>
{
    private final Object lock = new Object();
    
    private volatile int exitStatus = 0;
    private volatile boolean exitActive = false;
    private final AtomicInteger activeCount = new AtomicInteger();
    
    /**
     * Activates the call to System.exit() when the last thread created by start() 
     * finishes, and sets the status valued used in the call.
     * 
     * <p>This method will ensure the app ends after the last thread launched by start() finishes.
     * 
     * @param status
     * @return a reference to this object.
     */
    public KeepAlive exit(int status)
    {
        this.exitStatus = status;
        this.exitActive = true;
        return this;
    }

    /**
     *  Keeps the app alive forever or until close() is called.
     * 
     * <p>It is the same a call start(0);
     * @param data
     */
    public void start(final E... data)
    {
        this.start(0, data);
    }

    /**
     *  Keeps the app alive for a number of milliseconds or until close() is called.
     * 
     * @param aliveMillis the number of milliseconds to keep the app alive
     * @param data
     */
    public void start(final long aliveMillis, final E... data)
    {
        new Thread(new Runnable()
        {
            final E[] keepData = data;
            @Override
            public void run()
            {
                synchronized(lock)
                {
                    activeCount.incrementAndGet();
                    try
                    {
                        if(aliveMillis>0)
                        {
                            lock.wait(aliveMillis);
                        }
                        else
                        {
                            lock.wait();
                        }
                    }
                    catch (InterruptedException ex)
                    {
                        Logger.getLogger(KeepAlive.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    finally
                    {
                        activeCount.decrementAndGet();
                    }
                    if(exitActive && activeCount.get()==0)
                    {
                        System.exit(exitStatus);
                    }
                }
            }
        }).start();
    }

    /**
     * Ends all threads that keep the app alive.
     * 
     * <p>It will call System.exit() if KeepAlive.exit() was called.
     */
    public void close()
    {
        synchronized(lock)
        {
            lock.notifyAll();
            if(exitActive)
            {
                System.exit(exitStatus);
            }
        }
    }
}
