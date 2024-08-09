/*
 *  WorkerBee.java
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
class WorkerBee<M> implements Runnable
{   
    private final AtomicBoolean eagerLock = new AtomicBoolean(false); //controla si hay un hilo gloton
    private volatile boolean pendingData = false;
    private volatile boolean runningTask = false;

    private final Bee<M> bee;
    private final BlockingQueue<M> queue;
    private final AtomicInteger runningCount;

    private final ReentrantLock runningLock;
    private final Condition runningCondition;
    
    WorkerBee(Bee<M> bee)
    {
        this.bee = bee;
        this.queue = bee.queue;
        this.runningCount = bee.runningTasks;
        this.runningLock = bee.runningLock;
        this.runningCondition = bee.runningCondition;
    }

    @Override
    public void run()
    {
        if(!eagerLock.compareAndSet(false, true))
            return;
        try
        {
            while(pendingData)
            {
                pendingData = false;
                runningTask = true;
                runningCount.incrementAndGet();
                runningLock.lock();
                try
                {
                    M m;
                    while ( (m = queue.poll()) != null)
                    {
                        bee.receive(m);
                    }
                }
                catch (Exception ex)
                {
                    pendingData = true;
                    Logger.getLogger(WorkerBee.class.getName()).log(Level.SEVERE, null, ex);
                }
                finally
                {
                    runningTask = false;
                    runningCount.decrementAndGet();
                    runningCondition.signalAll();
                    runningLock.unlock();
                }
            }
        }
        finally
        {
            eagerLock.set(false);
        }
    }

    boolean isRunning()
    {
        return runningTask;
    }
    
    void touchPendingData()
    {
        pendingData = true;
    }

}
