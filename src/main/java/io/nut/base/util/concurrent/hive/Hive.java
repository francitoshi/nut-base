/*
 *  Hive.java
 *
 *  Copyright (C) 2024-2025 francitoshi@gmail.com
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class Hive implements AutoCloseable, Executor
{
    public static final int CORES = Runtime.getRuntime().availableProcessors();
    
    public static final int KEEP_ALIVE_MILLIS = 30_000;
    
    private static final ThreadPoolExecutor.CallerRunsPolicy CALLER_RUNS_POLICY  = new ThreadPoolExecutor.CallerRunsPolicy();

    private final ThreadPoolExecutor threadPoolExecutor;
    
    
    protected Hive(ThreadPoolExecutor threadPoolExecutor)
    {
        this.threadPoolExecutor = threadPoolExecutor;
    }
    public Hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis)
    {
        BlockingQueue queue = queueCapacity==0 ? new SynchronousQueue() : new LinkedBlockingQueue(queueCapacity);
        this.threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, rushPoolSize, keepAliveMillis, TimeUnit.MILLISECONDS, queue, CALLER_RUNS_POLICY);
    }

    public Hive(int corePoolSize)
    {
        this( corePoolSize, corePoolSize, corePoolSize, KEEP_ALIVE_MILLIS);
    }
    public Hive()
    {
        this( CORES, CORES, CORES, KEEP_ALIVE_MILLIS);
    }
    
    public Hive add(Bee<?>... bees)
    {
        for(Bee<?> item : bees)
        {
            item.setHive(this);
        }
        return this;
    }
    @Override
    public void execute(Runnable task)
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


