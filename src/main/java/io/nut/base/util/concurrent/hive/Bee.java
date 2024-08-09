/*
 *  Bee.java
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

import io.nut.base.util.RoundRobin;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 * @param <M>
 */
public abstract class Bee<M>
{
    private final Hive hive;
    private final int threads;
    private final RoundRobin<WorkerBee> rrWorkers;
    
    final BlockingQueue<M> queue = new LinkedBlockingQueue<>();
    final AtomicInteger runningTasks = new AtomicInteger();
    final ReentrantLock runningLock = new ReentrantLock();
    final Condition runningCondition = runningLock.newCondition();
    
    public Bee(Hive hive, int threads)
    {
        this.hive = hive;
        this.threads = threads;
        WorkerBee[] workers = new WorkerBee[threads];
        for(int i=0;i<threads;i++)
        {
            workers[i] = new WorkerBee<>(this);
        }
        this.rrWorkers = RoundRobin.create(workers);
    }
    
    public void send(M message) throws InterruptedException
    {
        this.queue.put(message);
        if(this.runningTasks.get()<this.threads)
        {
            for(int i=0;i<this.threads;i++)
            {
                WorkerBee wb = rrWorkers.next();
                if(!wb.isRunning())
                {
                    this.hive.submit(wb);
                    wb.touchPendingData();
                    break;
                }
            }
        }
    }

    public abstract void receive(M m);
 
    public boolean isRunning()
    {
        return runningTasks.get()>0;
    }
    
    public boolean join()
    {
        runningLock.lock();
        try
        {
            while(runningTasks.get()>0 || !queue.isEmpty())
            {
                runningCondition.await();
            }
            return true;
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }        
        finally
        {
            runningLock.unlock();
        }
    }
    
}
