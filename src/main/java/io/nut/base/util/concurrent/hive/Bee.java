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
import java.util.concurrent.Executor;
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
    
    private static final int QUEUE_SIZE = Short.MAX_VALUE;
    
    private static final Executor EXECUTOR = new Executor()
    {
        @Override
        public void execute(Runnable task)
        {
            task.run();
        }
    };
    
    private final Object lock = new Object();
    private volatile int status = RUNNING;
    
    private volatile Executor hive;
    private final int threads;
    private final Semaphore semaphore;
    private final BlockingQueue<M> queue;
    
    /**
     * Initializes a Bee system with the specified hive, thread pool size, and queue size.
     *
     * @param threads   the maximum number of threads that a Bee can run concurrently. If set to zero, 
     *                  it defaults to the number of available processors as determined by {@link Runtime#getRuntime()#availableProcessors()}.
     * @param hive      the hive that manages and coordinates the Bee instances.
     * @param queueSize the maximum number of messages waiting to be processed. If set to zero, threads will be used.
     */    
    public Bee(int threads, Hive hive, int queueSize)
    {
        this.threads = threads!=0 ? threads : Runtime.getRuntime().availableProcessors();
        this.hive = hive!=null ? hive : EXECUTOR;
        this.queue = queueSize!=0 ? new LinkedBlockingQueue<>(queueSize) : new LinkedBlockingQueue<>(QUEUE_SIZE);
        this.semaphore = new Semaphore(this.threads);
    }
    /**
     * Initializes a Bee system with the specified hive, thread pool size, and queue size.
     *
     * @param threads   the maximum number of threads that a Bee can run concurrently. If set to zero, 
     *                  it defaults to the number of available processors as determined by {@link Runtime#getRuntime()#availableProcessors()}.
     * @param hive      the hive that manages and coordinates the Bee instances.
     */    
    public Bee(int threads, Hive hive)
    {
        this(threads, hive, QUEUE_SIZE);
    }
    /**
     * Initializes a Bee system with the specified hive and thread pool size. The queue size will be the number of threads.
     *
     * @param threads   the maximum number of threads that a Bee can run concurrently. If set to zero, 
     *                  it defaults to the number of available processors as determined by {@link Runtime#getRuntime()#availableProcessors()}.
     */
    public Bee(int threads)
    {
        this(threads, null, QUEUE_SIZE);
    }
    /**
     * Initializes a Bee system with the specified hive. The number of threads and queue size will be the number of processors.
     *
     */
    public Bee()
    {
        this(0, null, QUEUE_SIZE);
    }
    
    private volatile InterruptedException interruptedException;

    public InterruptedException getInterruptedException()
    {
        return interruptedException;
    }
    
    public boolean send(M message)
    {
        try 
        {
            if(this.status==RUNNING)
            {
                this.queue.put(message);
                this.hive.execute(receiveTask);
                return true;
            }
        }
        catch (InterruptedException ex) 
        {
            Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, null, ex);
            interruptedException = ex;
        }
        return false;
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
                this.hive.execute(shutdownTask);
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
    public static void shutdownAndAwaitTermination(Bee<?> ...bees)
    {
        for(Bee<?> item : bees)
        {
            item.shutdown();
            item.awaitTermination(Integer.MAX_VALUE);
        }
    }

    public void setHive(Hive hive)
    {
        this.hive = hive;
    }
}
