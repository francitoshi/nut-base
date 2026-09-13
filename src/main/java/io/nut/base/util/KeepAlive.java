/*
 * Copyright (C) 2017-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.concurrent.atomic.AtomicInteger;
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
    private static final Logger LOG = Logger.getLogger(KeepAlive.class.getName());

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
                        Exceptions.severe(LOG, ex);
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
