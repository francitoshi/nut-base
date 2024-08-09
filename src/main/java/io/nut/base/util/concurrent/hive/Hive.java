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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author franci
 */
public class Hive
{
    public final int threads;
    public final int queueSize;
    
    private final ExecutorService executorService;
    
    public Hive(int threads, int queueSize)
    {
        this.threads = threads;
        this.queueSize = queueSize;
        this.executorService = new ThreadPoolExecutor(0, threads, 1234, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueSize));
    }
    
    public Hive(int threads)
    {
        this(threads, Integer.MAX_VALUE);
    }
    
    public Hive()
    {
        this(Runtime.getRuntime().availableProcessors(), Integer.MAX_VALUE);
    }

    void submit(Runnable task)
    {
        this.executorService.submit(task);
    }

    public void shutdown()
    {
        this.executorService.shutdown();
    }

    public boolean isShutdown()
    {
        return executorService.isShutdown();
    }

    public boolean isTerminated()
    {
        return executorService.isTerminated();
    }

    public boolean awaitTermination(int millis) throws InterruptedException
    {
        return executorService.awaitTermination(millis, TimeUnit.MILLISECONDS);
    }
    
}


