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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 * @param <M>
 */
public abstract class Bee<M>
{
    private static final int RUNNING    = 0; // Accept new tasks and process queued tasks
    private static final int SHUTDOWN   = 1; // Don't accept new tasks, but process queued tasks
    private static final int TERMINATED = 2; // terminated() has completed
    
    private final Object lock = new Object();
    private volatile int status = RUNNING;
    
    private final Hive hive;
    private final int threads;
    private final Semaphore semaphore;
    private final BlockingQueue<M> queue = new LinkedBlockingQueue<>();
    
    public Bee(Hive hive, int threads)
    {
        this.hive = hive;
        this.threads = threads!=0 ? threads : Runtime.getRuntime().availableProcessors();
        this.semaphore = new Semaphore(this.threads);
    }
    public Bee(Hive hive)
    {
        this(hive,0);
    }
    
    public void send(M message) throws InterruptedException
    {
        if(this.status==RUNNING)
        {
            this.queue.put(message);
            this.hive.submit(receiveTask);
        }
    }

    protected abstract void receive(M m);
    protected void terminate()
    {
    }
    
    private final Runnable receiveTask = new Runnable()
    {
        @Override
        public void run()
        {
            if(!semaphore.tryAcquire())
                return;
            try
            {
                M m;
                while ( (m = queue.poll()) != null)
                {
                    receive(m);
                }
            }
            catch (Exception ex)
            {
                Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, null, ex);
            }
            finally
            {
                semaphore.release();
                synchronized(lock)
                {
                    lock.notifyAll();
                }
            }
        }
    };
    private final Runnable shutdownTask = new Runnable()
    {
        @Override
        public void run()
        {
            semaphore.acquireUninterruptibly(threads);
            try
            {
                synchronized(lock)
                {
                    while(status==SHUTDOWN)
                    {
                        if(queue.isEmpty())
                        {
                            status=TERMINATED;
                            terminate();
                            break;
                        }
                        lock.wait();
                    }
                    lock.notifyAll();
                }
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, null, ex);
            }            
            finally
            {
                semaphore.release(threads);
            }
        }
    };
        
    public void shutdown()
    {
        synchronized(lock)
        {
            if(this.status==RUNNING)
            {
                this.status = SHUTDOWN; 
                hive.submit(shutdownTask);
            }
        }
    }
    public boolean isShutdown()
    {
        return this.status!=RUNNING;
    }
    public boolean isTerminated()
    {
        return this.status==TERMINATED;
    }
    
    public boolean awaitTermination(int millis)
    {
        try
        {
            synchronized(lock)
            {
                while(!isTerminated())
                {
                    lock.wait(millis);
                }
                return true;
            }
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }        
    }
}
