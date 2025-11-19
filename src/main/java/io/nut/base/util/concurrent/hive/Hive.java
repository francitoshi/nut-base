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

import io.nut.base.util.concurrent.CallerWaitsPolicy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread pool executor wrapper that manages and coordinates {@link Bee} instances.
 * The Hive provides a shared thread pool for executing tasks submitted by multiple Bees,
 * enabling efficient resource utilization and centralized thread management.
 *
 * <p>The Hive implements both {@link Executor} for task execution and {@link AutoCloseable}
 * for resource management in try-with-resources blocks. It wraps a {@link ThreadPoolExecutor}
 * with configurable pool sizes, queue capacity, and rejection policies.
 * 
 * <p><strong>Thread Pool Configuration:</strong>
 * <ul>
 *   <li><strong>Core pool size:</strong> Minimum number of threads kept alive</li>
 *   <li><strong>Rush pool size:</strong> Maximum number of threads during high load</li>
 *   <li><strong>Queue capacity:</strong> Number of tasks that can be queued when all threads are busy</li>
 *   <li><strong>Keep-alive time:</strong> How long excess threads wait for work before terminating</li>
 * </ul>
 * 
 * <p><strong>Rejection Policies:</strong>
 * <ul>
 *   <li><strong>CallerRunsPolicy:</strong> The calling thread executes the task when the pool is saturated</li>
 *   <li><strong>CallerWaitsPolicy:</strong> The calling thread waits until space is available in the queue</li>
 * </ul>
 * 
 * <p><strong>Usage example:</strong>
 * <pre>{@code
 * try (Hive hive = new Hive(4, 8, 100, 30000)) {
 *     Bee<String> bee1 = new Bee<String>(2, hive) {
 *         @Override
 *         protected void receive(String message) {
 *             System.out.println("Bee1: " + message);
 *         }
 *     };
 *     
 *     Bee<Integer> bee2 = new Bee<Integer>(2, hive) {
 *         @Override
 *         protected void receive(Integer number) {
 *             System.out.println("Bee2: " + number);
 *         }
 *     };
 *     
 *     hive.add(bee1, bee2);
 *     
 *     bee1.send("Hello");
 *     bee2.send(42);
 *     
 *     hive.shutdownAndAwaitTermination(bee1, bee2);
 * }
 * }</pre>
 *
 * @author franci
 */
public class Hive implements AutoCloseable, Executor
{
    /** Number of available processor cores in the system */
    public static final int CORES = Runtime.getRuntime().availableProcessors();
    
    /** Default keep-alive time for idle threads (30 seconds) */
    public static final int KEEP_ALIVE_MILLIS = 30_000;
    
    /** Policy that executes tasks in the calling thread when the pool is saturated */
    private static final ThreadPoolExecutor.CallerRunsPolicy CALLER_RUNS_POLICY  = new ThreadPoolExecutor.CallerRunsPolicy();
    
    /** Policy that makes the calling thread wait when the pool is saturated */
    private static final CallerWaitsPolicy CALLER_WAITS_POLICY  = new CallerWaitsPolicy();

    /** The underlying thread pool executor that manages thread execution */
    private final ThreadPoolExecutor threadPoolExecutor;
    
    /**
     * Protected constructor that accepts a pre-configured ThreadPoolExecutor.
     * This allows subclasses to provide custom executor configurations.
     *
     * @param threadPoolExecutor the configured thread pool executor to use
     */
    protected Hive(ThreadPoolExecutor threadPoolExecutor)
    {
        this.threadPoolExecutor = threadPoolExecutor;
    }
    
    /**
     * Creates a Hive with the specified thread pool configuration and rejection policy.
     *
     * @param corePoolSize the minimum number of threads to keep in the pool, even if idle
     * @param rushPoolSize the maximum number of threads allowed in the pool during high load
     * @param queueCapacity the capacity of the task queue. Use 0 for a SynchronousQueue
     * (direct handoff), or a positive value for a bounded LinkedBlockingQueue
     * @param keepAliveMillis the time in milliseconds that excess idle threads will wait
     * for new tasks before terminating
     * @param callerWaitsPolicy if true, uses CallerWaitsPolicy (calling thread blocks until
     * space is available); if false, uses CallerRunsPolicy (calling thread executes the task)
     */
    public Hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy)
    {
        BlockingQueue queue = queueCapacity==0 ? new SynchronousQueue() : new LinkedBlockingQueue(queueCapacity);
        this.threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, rushPoolSize, keepAliveMillis, TimeUnit.MILLISECONDS, queue, callerWaitsPolicy ? CALLER_WAITS_POLICY : CALLER_RUNS_POLICY); 
    }
    
    /**
     * Creates a Hive with the specified thread pool configuration using the
     * default CallerRunsPolicy for task rejection.
     *
     * @param corePoolSize the minimum number of threads to keep in the pool, even if idle
     * @param rushPoolSize the maximum number of threads allowed in the pool during high load
     * @param queueCapacity the capacity of the task queue. Use 0 for a SynchronousQueue,
     * or a positive value for a bounded LinkedBlockingQueue
     * @param keepAliveMillis the time in milliseconds that excess idle threads will wait
     * for new tasks before terminating
     */
    public Hive(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis)
    {
        this(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis, false);
    }

    /**
     * Creates a Hive with a fixed pool size. The core pool size, maximum pool size,
     * and queue capacity are all set to the same value, with the default keep-alive time.
     *
     * @param corePoolSize the fixed number of threads in the pool and queue capacity
     */
    public Hive(int corePoolSize)
    {
        this( corePoolSize, corePoolSize, corePoolSize, KEEP_ALIVE_MILLIS, false);
    }
    
    /**
     * Creates a Hive with default settings. The pool size is set to the number
     * of available processor cores, with matching core, maximum, and queue sizes.
     */
    public Hive()
    {
        this( CORES, CORES, CORES, KEEP_ALIVE_MILLIS, false);
    }
    
    /**
     * Associates the specified Bees with this Hive. Each Bee will use this
     * Hive's thread pool for executing tasks.
     *
     * @param bees the Bees to associate with this Hive
     * @return this Hive instance for method chaining
     */
    public Hive add(Bee<?>... bees)
    {
        for(Bee<?> item : bees)
        {
            item.setHive(this);
        }
        return this;
    }
    
    /**
     * Executes the given task using the underlying thread pool. This method
     * is called by Bees to submit their tasks for execution.
     *
     * @param task the runnable task to execute
     */
    @Override
    public void execute(Runnable task)
    {
        this.threadPoolExecutor.execute(task);
    }

    /**
     * Called when the Hive has been terminated and all tasks have completed.
     * Subclasses can override this method to perform cleanup or finalization logic.
     */
    protected void terminated()
    {
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are
     * executed, but no new tasks will be accepted. This method does not wait
     * for previously submitted tasks to complete execution.
     */
    public void shutdown()
    {
        this.threadPoolExecutor.shutdown();
    }

    /**
     * Returns true if this Hive has been shut down.
     *
     * @return true if shutdown has been initiated
     */
    public boolean isShutdown()
    {
        return threadPoolExecutor.isShutdown();
    }

    /**
     * Returns true if all tasks have completed following shutdown.
     * This will only return true if shutdown() or shutdownNow() was called first.
     *
     * @return true if all tasks have completed following shutdown
     */
    public boolean isTerminated()
    {
        return threadPoolExecutor.isTerminated();
    }

    /**
     * Blocks until all tasks have completed execution after a shutdown request,
     * or the timeout occurs, or the current thread is interrupted, whichever
     * happens first.
     *
     * @param millis the maximum time to wait in milliseconds
     * @return true if this executor terminated, false if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(int millis) throws InterruptedException
    {
        return threadPoolExecutor.awaitTermination(millis, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Shuts down the specified Bees and the Hive, waiting for all tasks to complete.
     * This is a convenience method that:
     * <ol>
     *   <li>Shuts down each Bee and waits for its termination</li>
     *   <li>Shuts down this Hive</li>
     *   <li>Waits for the Hive to terminate</li>
     * </ol>
     *
     * @param bees the Bees to shut down before shutting down the Hive
     */
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

    /**
     * Returns the core number of threads in the thread pool.
     *
     * @return the core pool size
     */
    public int getCorePoolSize()
    {
        return threadPoolExecutor.getCorePoolSize();
    }

    /**
     * Returns the maximum number of threads allowed in the thread pool.
     *
     * @return the maximum pool size
     */
    public int getMaximumPoolSize()
    {
        return threadPoolExecutor.getMaximumPoolSize();
    }
    
    /**
     * Sets the core number of threads in the thread pool. If the new value is
     * smaller than the current value, excess existing threads will be terminated
     * when they next become idle.
     *
     * @param i the new core pool size
     * @throws IllegalArgumentException if i is less than zero or greater than the maximum pool size
     */
    public void setCorePoolSize(int i)
    {
        threadPoolExecutor.setCorePoolSize(i);
    }

    /**
     * Sets the maximum number of threads allowed in the thread pool.
     *
     * @param i the new maximum pool size
     * @throws IllegalArgumentException if i is less than zero or less than the core pool size
     */
    public void setMaximumPoolSize(int i)
    {
        threadPoolExecutor.setMaximumPoolSize(i);
    }

    /**
     * Closes this Hive by initiating an orderly shutdown and waiting for all
     * tasks to complete. This method is called automatically when used in a
     * try-with-resources statement.
     */
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
