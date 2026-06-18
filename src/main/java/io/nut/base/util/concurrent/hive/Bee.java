/*
 * Copyright (c) 2024-2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent.hive;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Bee<M> implements Sendable<M>
{
    private static final int RUNNING    = 0; // Accept new tasks and process queued tasks
    private static final int SHUTDOWN   = 1; // Don't accept new tasks, but process queued tasks
    private static final int TERMINATED = 2; // terminated() has completed
    
    private static final int QUEUE_SIZE = Short.MAX_VALUE;
    
    private final Object lock = new Object();
    private volatile int status = RUNNING;
    
    private volatile boolean allowLogger = true;
    private volatile boolean shutdownWhenEmpty = false;
    private volatile Executor hive;
    private final int threads;
    private final Semaphore semaphore;
    private final BlockingQueue<M> queue;

    /** The last exception that occurred during message processing or lifecycle operations */
    private volatile Exception ex;

    
    public Bee(int threads, Hive hive, int queueSize) 
    {
        if(threads < 0) 
        {
            throw new IllegalArgumentException("threads < 0");
        }
        if(queueSize < 0) 
        {
            throw new IllegalArgumentException("queueSize < 0");
        }
        this.threads = threads != 0 ? threads : Runtime.getRuntime().availableProcessors();
        this.hive = hive;
        this.queue = new LinkedBlockingQueue<>(queueSize != 0 ? queueSize : QUEUE_SIZE);
        this.semaphore = new Semaphore(this.threads);
    }
    
    public Bee(int threads, Hive hive) 
    {
        this(threads, hive, QUEUE_SIZE);
    }
    
    /**
     * Convenience constructor for a Bee attached to a Hive from the start,
     * using the default thread count and queue size. This is the constructor
     * used by Hive.bee(Consumer) and Pipe(Hive, Function) to create stages
     * that are bound to a Hive at creation time.
     */
    public Bee(Hive hive)
    {
        this(0, hive, QUEUE_SIZE);
    }
    
    public Bee(int threads)
    {
        this(threads, null, QUEUE_SIZE);
    }
    
    public Bee()
    {
        this(0, null, QUEUE_SIZE);
    }

    public Bee<M> dryLogger() 
    {
        this.allowLogger = false;
        return this;
    }

    
    public Exception getException()
    {
        return ex;
    }
    
    protected abstract void receive(M m);
    
    protected void terminate()
    {
    }
    
    protected void exception(Exception ex)
    {
        
    }
    
    @Override
    public boolean send(M message)
    {
        try 
        {
            if(this.status!=RUNNING)
            {
                return false;
            }
           
            if(this.hive!=null)
            {
                this.queue.put(message);
                // Submit task if permits available
                if (this.semaphore.availablePermits() > 0)
                {
                    this.hive.execute(receiveTask);
                }
            }
            else
            {
                this.receive(message);
            }
            return true;
        }
        catch (Exception ex) 
        {
            this.ex = ex;
            if(allowLogger)
            {
                Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, "Bee.send()", ex);
            }
            exception(ex);
            return false;
        }
    }

    private final Runnable receiveTask = new Runnable()
    {
        @Override
        public void run()
        {
            if(!semaphore.tryAcquire())
            {
                return;
            }
            try
            {
                M m;
                while ((m = queue.poll()) != null)
                {
                    receive(m);
                }
            }
            catch (Exception ex)
            {
                Bee.this.ex = ex;
                if(allowLogger)
                {
                    Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, "Bee.receiveTask.run()", ex);
                }
                exception(ex);
            }
            finally
            {
                semaphore.release();
                synchronized(lock)
                {
                    if(shutdownWhenEmpty && semaphore.availablePermits() == threads && queue.isEmpty())
                    {
                        shutdown(false);
                    }
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
            //last chance for messages in the queue to be received
            receiveTask.run();

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
                Bee.this.ex = ex;
                if(allowLogger) Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, "Bee.shutdownTask.run()", ex);
                exception(ex);
            }            
            finally
            {
                semaphore.release(threads);
            }
        }
    };

    public Bee<M> waitForIdle()
    {
        synchronized(lock)
        {
            try
            {
                while(semaphore.availablePermits() < threads || !queue.isEmpty())
                {
                    lock.wait();
                }
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return this;
    }
            
    public Bee<M> shutdown()
    {
        return shutdown(false);
    }

    public Bee<M> shutdown(boolean onlyWhenEmpty)
    {
        synchronized(lock)
        {
            if (onlyWhenEmpty)
            {
                this.shutdownWhenEmpty = true;
                if (semaphore.availablePermits() == threads && queue.isEmpty())
                {
                    shutdown(false);
                }
                return this;
            }
            if(this.status==RUNNING)
            {
                this.status = SHUTDOWN;
                if(this.hive!=null)
                {
                    this.hive.execute(shutdownTask);
                }
                else if(queue.isEmpty()) 
                {
                    status = TERMINATED;
                    terminate();
                }
            }
        }
        return this;
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
            long untilNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
            return awaitTerminationUntilNanos(untilNanos);
        }
        catch (InterruptedException ex)
        {
            Bee.this.ex = ex;
            if(allowLogger) Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, "Bee.awaitTermination()", ex);
            exception(ex);
            return false;
        }        
    }

    protected boolean awaitTerminationUntilNanos(long untilNanos) throws InterruptedException
    {
        boolean rc = false;
        synchronized(lock)
        {
            long now;
            while(!(rc=isTerminated()) && (now=System.nanoTime())<untilNanos)
            {
                long remaining = untilNanos - now;
                lock.wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
            }
            return rc;
        }
    }
    
    public void setHive(Hive hive)
    {
        this.hive = hive;
    }

}

