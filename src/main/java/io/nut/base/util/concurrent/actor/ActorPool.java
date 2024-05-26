/*
 *  ActorPool.java
 *
 *  Copyright (C) 2009-2023 francitoshi@gmail.com
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
package io.nut.base.util.concurrent.actor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author franci
 */
public class ActorPool implements Executor
{
    public final static int CORES = Runtime.getRuntime().availableProcessors();
    private static int keepAliveTime = 333;
    private static volatile int defaultPoolSize = CORES*2+CORES+CORES/2;

    private final BlockingQueue<Runnable> inbox;
    private final Executor executor;

    public ActorPool(int poolSize)
    {
        if(poolSize>0)
        {
            inbox = new LinkedBlockingQueue<>();
            ThreadPoolExecutor pool = new ThreadPoolExecutor(poolSize, poolSize, keepAliveTime, TimeUnit.MILLISECONDS, inbox);
            pool.allowCoreThreadTimeOut(true);
            this.executor = pool;
        }
        else
        {
            inbox   = null;
            executor = null;
        }
    }
    
    public ActorPool()
    {
        this(defaultPoolSize);
    }
    
    @Override
    public void execute(Runnable task)//throws InterruptedException
    {
        if(executor==null)
        {
            task.run();
        }
        else
        {
            executor.execute(task);
        }
    }

    public static int getKeepAliveTime()
    {
        return keepAliveTime;
    }

    public static void setKeepAliveTime(int keepAliveTime)
    {
        ActorPool.keepAliveTime = keepAliveTime;
    }

    public static int getDefaultPoolSize()
    {
        return defaultPoolSize;
    }

    public static void setDefaultPoolSize(int defaultPoolSize)
    {
        ActorPool.defaultPoolSize = defaultPoolSize;
    }
}
