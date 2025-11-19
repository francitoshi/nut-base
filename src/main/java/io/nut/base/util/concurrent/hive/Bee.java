/*
 *  Bee.java
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
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An abstract message-processing actor that manages concurrent message handling
 * within a thread pool. A Bee receives messages of type {@code M}, queues them,
 * and processes them asynchronously using a configurable number of worker threads.
 *
 * <p>The Bee class implements an actor-like pattern where messages are sent to
 * the Bee and processed asynchronously. It supports both asynchronous (when
 * associated with a Hive) and synchronous (when no Hive is provided) message
 * processing modes.
 * 
 * <p><strong>Lifecycle states:</strong>
 * <ul>
 *   <li>RUNNING: Accepts new messages and processes queued messages</li>
 *   <li>SHUTDOWN: Rejects new messages but continues processing queued messages</li>
 *   <li>TERMINATED: All processing completed and terminate() has been called</li>
 * </ul>
 * 
 * <p><strong>Usage example:</strong>
 * <pre>{@code
 * Hive hive = new Hive(4);
 * Bee<String> messageBee = new Bee<String>(2, hive) {
 *     @Override
 *     protected void receive(String message) {
 *         System.out.println("Processing: " + message);
 *     }
 * };
 * 
 * messageBee.send("Hello");
 * messageBee.send("World");
 * messageBee.shutdown();
 * messageBee.awaitTermination(5000);
 * }</pre>
 *
 * @author franci
 * @param <M> the type of messages this Bee processes
 */
public abstract class Bee<M>
{
    private static final int RUNNING    = 0; // Accept new tasks and process queued tasks
    private static final int SHUTDOWN   = 1; // Don't accept new tasks, but process queued tasks
    private static final int TERMINATED = 2; // terminated() has completed
    
    private static final int QUEUE_SIZE = Short.MAX_VALUE;
    
    private final Object lock = new Object();
    private volatile int status = RUNNING;
    
    private volatile boolean allowLogger = true;
    private volatile Executor hive;
    private final int threads;
    private final Semaphore semaphore;
    private final BlockingQueue<M> queue;

    /** The last exception that occurred during message processing or lifecycle operations */
    private volatile Exception ex;

    
    /**
     * Initializes a Bee system with the specified hive, thread pool size, and
     * queue size.
     *
     * @param threads the maximum number of threads that a Bee can run
     * concurrently. If set to zero, it defaults to the number of available
     * processors as determined by
     * {@link Runtime#getRuntime()#availableProcessors()}.
     * @param hive the hive that manages and coordinates the Bee instances. Use
     * null to make send() synchronous.
     * @param queueSize the maximum number of messages waiting to be processed.
     * If set to zero, defaults to {@link #QUEUE_SIZE}.
     */
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
    
    /**
     * Initializes a Bee system with the specified hive and thread pool size,
     * using the default queue size.
     *
     * @param threads the maximum number of threads that a Bee can run
     * concurrently. If set to zero, it defaults to the number of available
     * processors as determined by
     * {@link Runtime#getRuntime()#availableProcessors()}.
     * @param hive the hive that manages and coordinates the Bee instances. Use
     * null to make send() synchronous.
     */
    public Bee(int threads, Hive hive) 
    {
        this(threads, hive, QUEUE_SIZE);
    }
    
    /**
     * Initializes a Bee system with the specified thread pool size and no Hive.
     * Messages will be processed synchronously. The queue size will be the default size.
     *
     * @param threads the maximum number of threads that a Bee can run concurrently. 
     * If set to zero, it defaults to the number of available processors as 
     * determined by {@link Runtime#getRuntime()#availableProcessors()}.
     */
    public Bee(int threads)
    {
        this(threads, null, QUEUE_SIZE);
    }
    
    /**
     * Initializes a Bee system with default settings. The number of threads
     * defaults to the number of available processors, and the queue size is set
     * to the default size. Messages will be processed synchronously.
     */
    public Bee()
    {
        this(0, null, QUEUE_SIZE);
    }

    /**
     * Cancel all logger outputs from this instance
     */
    public Bee<M> dryLogger() 
    {
        this.allowLogger = false;
        return this;
    }

    
    /**
     * Returns the last exception that occurred during message processing or
     * lifecycle operations.
     *
     * @return the last exception that occurred, or null if no exception has occurred
     */
    public Exception getException()
    {
        return ex;
    }
    
    /**
     * Sends a message to this Bee for processing. If a Hive is configured,
     * the message is queued and processed asynchronously. If no Hive is
     * configured, the message is processed synchronously on the calling thread.
     *
     * @param message the message to be processed
     * @return true if the message was successfully sent or processed, false if
     * the Bee is not in RUNNING state or an interruption occurred
     */
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
                this.hive.execute(receiveTask);
            }
            else
            {
                this.receive(message);
            }
            return true;
        }
        catch (Exception ex) 
        {
            if(allowLogger) Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, "Bee.send()", ex);
            this.ex = ex;
            return false;
        }
    }

    /**
     * Processes a single message. This method must be implemented by subclasses
     * to define the message processing logic.
     *
     * @param m the message to process
     */
    protected abstract void receive(M m);
    
    /**
     * Called when the Bee has been terminated and all messages have been processed.
     * Subclasses can override this method to perform cleanup or finalization logic.
     */
    protected void terminate()
    {
    }
    
    /**
     * Called when an exception occurs during message processing or lifecycle operations.
     * Subclasses can override this method to implement custom exception handling.
     *
     * @param ex the exception that occurred
     */
    protected void exception(Exception ex)
    {
        
    }
    
    /**
     * Runnable task that processes queued messages. This task attempts to acquire
     * a permit from the semaphore and then processes all available messages in the queue.
     */
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
                Bee.this.ex = ex;
                if(allowLogger) Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, "Bee.receiveTask.run()", ex);
                exception(ex);
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
    
    /**
     * Runnable task that manages the shutdown process. This task waits for all
     * worker threads to complete, then waits for the queue to be empty before
     * transitioning to the TERMINATED state.
     */
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
        
    /**
     * Initiates an orderly shutdown in which previously submitted messages are
     * processed, but no new messages will be accepted. This method does not wait
     * for previously submitted messages to complete execution. Use
     * {@link #awaitTermination(int)} to wait for processing to complete.
     * 
     * <p>This method is idempotent - calling it multiple times has no additional effect.
     */
    public void shutdown()
    {
        synchronized(lock)
        {
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
    }
    
    /**
     * Returns true if this Bee has been shut down.
     *
     * @return true if shutdown has been initiated
     */
    public boolean isShutdown()
    {
        return this.status!=RUNNING;
    }
    
    /**
     * Returns true if all messages have been processed following shutdown.
     *
     * @return true if this Bee has completed termination
     */
    public boolean isTerminated()
    {
        return this.status==TERMINATED;
    }
    
    /**
     * Blocks until all messages have been processed after a shutdown request,
     * or the timeout occurs, or the current thread is interrupted, whichever
     * happens first.
     *
     * @param millis the maximum time to wait in milliseconds
     * @return true if this Bee terminated, false if the timeout elapsed before termination
     */
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
            Bee.this.ex = ex;
            if(allowLogger) Logger.getLogger(Bee.class.getName()).log(Level.SEVERE, "Bee.awaitTermination()", ex);
            exception(ex);
            return false;
        }        
    }
    
    /**
     * Shuts down the specified Bees and waits for their termination.
     * This is a convenience method that calls {@link #shutdown()} and
     * {@link #awaitTermination(int)} with a maximum timeout on each Bee.
     *
     * @param bees the Bees to shut down and wait for
     */
    public static void shutdownAndAwaitTermination(Bee<?> ...bees)
    {
        for(Bee<?> item : bees)
        {
            item.shutdown();
            item.awaitTermination(Integer.MAX_VALUE);
        }
    }

    /**
     * Sets the Hive executor for this Bee. This allows the Bee to be associated
     * with a different Hive after construction.
     *
     * @param hive the new Hive to use for executing tasks, or null for synchronous processing
     */
    public void setHive(Hive hive)
    {
        this.hive = hive;
    }
}
