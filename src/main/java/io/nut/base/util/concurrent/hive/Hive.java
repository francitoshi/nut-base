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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author franci
 */
public class Hive
{
    private final ExecutorService executorService;
    
    public Hive(ExecutorService executorService)
    {
        this.executorService = executorService;
    }
    public Hive(int threads, boolean fixed)
    {
        this(fixed ? Executors.newFixedThreadPool(threads) : Executors.newWorkStealingPool(threads) );
    }
    public Hive(int threads)
    {
        this(Executors.newWorkStealingPool(threads));
    }
    public Hive()
    {
        this(Executors.newWorkStealingPool());
    }
    
    void submit(Runnable task)
    {
        this.executorService.execute(task);
    }

    protected void terminated()
    {
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


