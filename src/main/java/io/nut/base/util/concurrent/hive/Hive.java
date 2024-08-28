/*
 *  Hive.java
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
package io.nut.base.util.concurrent.hive;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class Hive implements AutoCloseable
{
    public static final int CORES = Runtime.getRuntime().availableProcessors();
    
    public static final int KEEP_ALIVE_MILLIS = 30_000;

    private final ThreadPoolExecutor threadPoolExecutor;
    
    public Hive(int coreThreads, int maxThreads, int keepAliveMillis)
    {
        this.threadPoolExecutor = new ThreadPoolExecutor(coreThreads, maxThreads, keepAliveMillis, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }
    public Hive(int threads)
    {
        this( threads, threads, KEEP_ALIVE_MILLIS);
    }

    public Hive()
    {
        this( CORES + 1, CORES*2 + 1, KEEP_ALIVE_MILLIS);
    }
    
    void submit(Runnable task)
    {
        this.threadPoolExecutor.execute(task);
    }

    protected void terminated()
    {
    }

    public void shutdown()
    {
        this.threadPoolExecutor.shutdown();
    }

    public boolean isShutdown()
    {
        return threadPoolExecutor.isShutdown();
    }

    public boolean isTerminated()
    {
        return threadPoolExecutor.isTerminated();
    }

    public boolean awaitTermination(int millis) throws InterruptedException
    {
        return threadPoolExecutor.awaitTermination(millis, TimeUnit.MILLISECONDS);
    }
    
    public void shutdownAndAwaitTermination(Bee<?> ...bees)
    {
        try
        {
            Bee.shutdownAndAwaitTermination(bees);
            this.shutdown();
            this.awaitTermination(Integer.MAX_VALUE);
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Hive.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getCorePoolSize()
    {
        return threadPoolExecutor.getCorePoolSize();
    }

    public int getMaximumPoolSize()
    {
        return threadPoolExecutor.getMaximumPoolSize();
    }
    
    public void setCorePoolSize(int i)
    {
        threadPoolExecutor.setCorePoolSize(i);
    }

    public void setMaximumPoolSize(int i)
    {
        threadPoolExecutor.setMaximumPoolSize(i);
    }

    @Override
    public void close()
    {
        try
        {
            this.shutdown();
            this.awaitTermination(Integer.MAX_VALUE);
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Hive.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}


