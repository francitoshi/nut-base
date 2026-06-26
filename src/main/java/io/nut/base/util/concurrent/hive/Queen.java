/*
 * Copyright (c) 2026 francitoshi@gmail.com
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

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;

/**
 * A managed thread-pool with a rich task-execution API.
 * <p>
 * {@code Queen} wraps a {@link ThreadPoolExecutor} and exposes five execution
 * modes that cover every combination of <em>who executes</em> and
 * <em>whether a result is returned</em>:
 *
 * <table border="1">
 *   <tr><th></th><th>void</th><th>Future&lt;U&gt;</th></tr>
 *   <tr><td>fire &amp; forget</td><td>{@link #run}</td><td>{@link #submit(Runnable)} / {@link #submit(Supplier)}</td></tr>
 *   <tr><td>guaranteed start</td><td>{@link #spawn}</td><td>—</td></tr>
 *   <tr><td>parallel + blocking</td><td>{@link #forEach}</td><td>—</td></tr>
 * </table>
 *
 * <p>{@code Queen} also implements {@link Executor} (via {@link #execute}) so
 * it can be passed anywhere a plain {@code Executor} is expected.
 * <p>
 * {@code Queen} implements {@link AutoCloseable}: {@link #close()} shuts the
 * pool down gracefully and blocks until all tasks have finished.
 * <p>
 * {@link Hive} extends {@code Queen} and adds the Bee-specific factory methods
 * and the pub/sub registry on top of this execution foundation.
 */
public class Queen implements AutoCloseable, Executor
{
    /**
     * Number of available processor cores, used as the default pool size.
     */
    public static final int CORES = Runtime.getRuntime().availableProcessors();

    /**
     * Default keep-alive time for idle excess threads, in milliseconds (30 s).
     */
    public static final int KEEP_ALIVE_MILLIS = 30_000;

    /**
     * Saturation policy: run the overflowing task in the calling thread.
     */
    private static final ThreadPoolExecutor.CallerRunsPolicy CALLER_RUNS_POLICY = new ThreadPoolExecutor.CallerRunsPolicy();

    /**
     * Saturation policy: block the calling thread until a slot is available.
     */
    private static final CallerWaitsPolicy CALLER_WAITS_POLICY = new CallerWaitsPolicy();

    /**
     * The underlying thread pool.
     */
    private final ThreadPoolExecutor threadPoolExecutor;

    private final Phaser phaser;
    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Protected constructor for subclasses that supply their own
     * {@link ThreadPoolExecutor} (e.g. {@link ProxyHive}).
     *
     * @param threadPoolExecutor the executor to delegate to; may be
     *                           {@code null} for lazy subclasses
     */
    protected Queen(ThreadPoolExecutor threadPoolExecutor)
    {
        this.threadPoolExecutor = threadPoolExecutor;
        this.phaser = new Phaser(1);
    }

    /**
     * Full constructor.
     *
     * @param corePoolSize      threads kept alive even when idle
     * @param rushPoolSize      maximum threads in the pool
     * @param queueCapacity     task-queue capacity; {@code 0} for a
     *                          {@link SynchronousQueue} (no buffering)
     * @param keepAliveMillis   lifetime of excess idle threads, in milliseconds
     * @param callerWaitsPolicy {@code true} to block the caller on saturation;
     *                          {@code false} to run the task in the caller
     * @param avoidTracker       {@code true} to disable active-task tracking
     *                          
     */
    public Queen(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy, boolean avoidTracker)
    {
        BlockingQueue<Runnable> queue = queueCapacity == 0
                ? new SynchronousQueue<>()
                : new LinkedBlockingQueue<>(queueCapacity);
        this.threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize, rushPoolSize,
                keepAliveMillis, TimeUnit.MILLISECONDS,
                queue,
                callerWaitsPolicy ? CALLER_WAITS_POLICY : CALLER_RUNS_POLICY);
        this.phaser = avoidTracker ? null : new Phaser(1);
    }

    /**
     * Full constructor with tracking disabled.
     *
     * @param corePoolSize      threads kept alive even when idle
     * @param rushPoolSize      maximum threads in the pool
     * @param queueCapacity     task-queue capacity; {@code 0} for a
     *                          {@link SynchronousQueue} (no buffering)
     * @param keepAliveMillis   lifetime of excess idle threads, in milliseconds
     * @param callerWaitsPolicy {@code true} to block the caller on saturation;
     *                          {@code false} to run the task in the caller
     */
    public Queen(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy)
    {
        this(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis, callerWaitsPolicy, false);
    }

    /**
     * Constructs a Queen with the {@link ThreadPoolExecutor.CallerRunsPolicy}.
     *
     * @param corePoolSize    threads kept alive when idle
     * @param rushPoolSize    maximum threads in the pool
     * @param queueCapacity   task-queue capacity (0 = no buffering)
     * @param keepAliveMillis lifetime of excess idle threads, in milliseconds
     */
    public Queen(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis)
    {
        this(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis, false);
    }

    /**
     * Constructs a Queen with a symmetric pool of {@code corePoolSize} threads,
     * a bounded queue of the same size, and the default keep-alive time.
     *
     * @param corePoolSize number of threads and queue slots
     */
    public Queen(int corePoolSize)
    {
        this(corePoolSize, corePoolSize, corePoolSize, KEEP_ALIVE_MILLIS, false);
    }

    /**
     * Constructs a Queen sized to the number of available CPU cores.
     */
    public Queen()
    {
        this(CORES, CORES, CORES, KEEP_ALIVE_MILLIS, false);
    }

    // -------------------------------------------------------------------------
    // Static factories
    // -------------------------------------------------------------------------

    /**
     * @return a new Queen with default (CPU-core-sized) settings.
     */
    public static Queen queen()
    {
        return new Queen();
    }

    /**
     * @param corePoolSize number of threads and queue slots
     * @return a new Queen with a symmetric pool of {@code corePoolSize} threads
     */
    public static Queen queen(int corePoolSize)
    {
        return new Queen(corePoolSize);
    }

    /**
     * @param corePoolSize    threads kept alive when idle
     * @param rushPoolSize    maximum threads in the pool
     * @param queueCapacity   task-queue capacity (0 = no buffering)
     * @param keepAliveMillis lifetime of excess idle threads, in milliseconds
     * @return a new Queen with the given pool configuration
     */
    public static Queen queen(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis)
    {
        return new Queen(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis);
    }

    /**
     * @param corePoolSize      threads kept alive when idle
     * @param rushPoolSize      maximum threads in the pool
     * @param queueCapacity     task-queue capacity (0 = no buffering)
     * @param keepAliveMillis   lifetime of excess idle threads, in milliseconds
     * @param callerWaitsPolicy {@code true} to block caller on saturation
     * @return a new Queen with full pool configuration
     */
    public static Queen queen(int corePoolSize, int rushPoolSize, int queueCapacity, int keepAliveMillis, boolean callerWaitsPolicy)
    {
        return new Queen(corePoolSize, rushPoolSize, queueCapacity, keepAliveMillis, callerWaitsPolicy);
    }

    // -------------------------------------------------------------------------
    // Execution API
    // -------------------------------------------------------------------------

    /**
     * Wraps {@code task} with active-count tracking if tracking is enabled,
     * or returns it unchanged if tracking is disabled. Also registers the
     * task lifecycle within the execution phaser.
     *
     * @param task the original task; must not be {@code null}
     * @return the task, wrapped or not
     */
    private Runnable wrap(Runnable task)
    {
        if(phaser==null)
        {
            return task;
        }
        
        return () ->
        {
            // We register the arrival of a new task in the Phaser immediately
            phaser.register();
            try
            {
                task.run();
            }
            finally
            {
                // Upon completion, the task arrives and is unloaded from the Phaser
                phaser.arriveAndDeregister();
            }
        };
    }

    /**
     * Returns the number of tasks currently registered with the internal
     * {@link Phaser} (i.e. executing or about to execute on the pool), or
     * {@code -1} if active-task tracking was not enabled at construction time.
     * <p>
     * <strong>Implementation note:</strong> the {@code Phaser} is initialised
     * with one permanent "owner" party (the party used by
     * {@link #waitForIdle()} to synchronise). That party is subtracted here
     * so the return value reflects only actual in-flight tasks. Do not remove
     * the {@code - 1} without also changing the {@code Phaser} initialisation.
     *
     * @return active task count (&ge; 0), or {@code -1} if tracking is disabled
     */
    public int getActiveCount()
    {
        // Subtract 1 to exclude the permanent owner party registered at construction.
        return phaser != null ? phaser.getRegisteredParties() - 1 : -1;
    }

    /**
     * Returns {@code true} if tracking is enabled and no task is currently
     * executing, or if tracking is disabled and the pool reports no active
     * threads. When tracking is enabled this is an exact, lock-free check;
     * when disabled it relies on {@link ThreadPoolExecutor#getActiveCount()},
     * which is an approximation.
     *
     * @return {@code true} if the pool is idle
     */
    public boolean isIdle()
    {
        // getActiveCount() already subtracts the permanent owner party,
        // so == 0 means no tasks are registered.
        return phaser != null ? getActiveCount() == 0 : threadPoolExecutor.getActiveCount() == 0;
    }

    /**
     * Blocks the calling thread until all running tasks have finished. If there
     * are no tasks running or queued, returns immediately.
     */
    public Queen waitForIdle()
    {
        if(phaser!=null)
        {
            // We loop because new tasks may register between iterations.
            // arriveAndAwaitAdvance() atomically marks our arrival and
            // blocks until every other currently-registered party also
            // arrives, eliminating the read-then-wait race condition of
            // the previous getPhase() / awaitAdvance(phase) pattern.
            //
            // The owner party (registered at construction) is the one
            // calling arriveAndAwaitAdvance here; tasks register/
            // deregister transiently around their execution via wrap().
            while (phaser.getRegisteredParties() > 1)
            {
                phaser.arriveAndAwaitAdvance();
            }
        }
        return this;
    }    
    /**
     * Submits {@code task} to the thread pool for fire-and-forget execution.
     * Implements {@link Executor}; prefer {@link #run} for direct use.
     *
     * @param task the task to execute; must not be {@code null}
     */
    @Override
    public void execute(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");
        this.threadPoolExecutor.execute(wrap(task));
    }

    /**
     * Submits {@code task} to the thread pool for fire-and-forget execution.
     * Equivalent to {@link #execute} but named consistently with the rest of
     * the execution API.
     *
     * @param task the task to run; must not be {@code null}
     */
    public void run(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");
        this.threadPoolExecutor.execute(wrap(task));
    }

    /**
     * Submits {@code task} to the thread pool and returns a
     * {@link Future}{@code <Void>} that completes when the task finishes.
     *
     * @param task the task to submit; must not be {@code null}
     * @return a {@code Future<Void>} representing pending completion
     */
    public Future<Void> submit(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");
        return CompletableFuture.runAsync(wrap(task), this.threadPoolExecutor);
    }

    /**
     * Submits {@code supplier} to the thread pool and returns a
     * {@link Future}{@code <U>} that holds the computed result.
     *
     * @param <U>      the result type
     * @param supplier the computation to submit; must not be {@code null}
     * @return a {@code Future<U>} representing the pending result
     */
    public <U> Future<U> submit(Supplier<U> supplier)
    {
        Objects.requireNonNull(supplier, "supplier must not be null");
        // wrap via Runnable adapter so tracking applies uniformly
        if (phaser == null)
        {
            return CompletableFuture.supplyAsync(supplier, this.threadPoolExecutor);
        }
        return CompletableFuture.supplyAsync(() ->
        {
            phaser.register();
            try
            {
                return supplier.get();
            }
            finally
            {
                phaser.arriveAndDeregister();
            }
        }, this.threadPoolExecutor);
    }

    /**
     * Submits {@code task} to the thread pool and blocks until the worker is
     * guaranteed to be executing {@code task.run()} as its very next
     * instruction, then returns. See class-level documentation for details.
     *
     * @param task the task to spawn; must not be {@code null}
     */
    public void spawn(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");

        final CountDownLatch ready = new CountDownLatch(1);

        this.threadPoolExecutor.execute(wrap(() ->
        {
            ready.countDown(); // "my next instruction is task.run()"
            task.run();
        }));

        try
        {
            ready.await();     // return only when worker is on the starting line
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            this.threadPoolExecutor.execute(wrap(task)); // fallback: do not lose the task
        }
    }

    /**
     * Applies {@code consumer} to every element of {@code iterable} in
     * parallel on the thread pool, blocking until all invocations have
     * finished.
     * <p>
     * If any invocation throws, this method waits for the rest to complete and
     * then re-throws the first failure wrapped in a {@link CompletionException},
     * with any further failures added as
     * {@linkplain Throwable#addSuppressed suppressed exceptions}.
     *
     * @param <T>      the element type
     * @param iterable the elements to process; must not be {@code null}
     * @param consumer the action to perform for each element; must not be
     *                 {@code null}
     * @throws CompletionException if one or more invocations threw an exception
     */
    public <T> void forEach(Iterable<T> iterable, Consumer<? super T> consumer)
    {
        Objects.requireNonNull(iterable, "iterable must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        iterable.forEach(item -> futures.add(CompletableFuture.runAsync( () -> consumer.accept(item), this.threadPoolExecutor)));

        AtomicReference<Throwable> first = new AtomicReference<>();
        futures.forEach(future ->
        {
            try
            {
                future.join();
            }
            catch (CompletionException | CancellationException ex)
            {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                if (!first.compareAndSet(null, cause))
                {
                    first.get().addSuppressed(cause);
                }
            }
        });
        if (first.get() != null)
        {
            throw (first.get() instanceof CompletionException)
                    ? (CompletionException) first.get()
                    : new CompletionException(first.get());
        }
    }

    // -------------------------------------------------------------------------
    // Pool lifecycle
    // -------------------------------------------------------------------------

    /**
     * Called by the underlying {@link ThreadPoolExecutor} after it has
     * terminated. Subclasses may override to perform post-termination cleanup.
     * The default implementation does nothing.
     */
    protected void terminated()
    {
    }

    /**
     * Initiates a graceful shutdown: previously submitted tasks continue
     * executing, but no new tasks are accepted.
     *
     * @return this Queen, for fluent chaining
     */
    public Queen shutdown()
    {
        this.threadPoolExecutor.shutdown();
        return this;
    }

    /**
     * @return {@code true} if {@link #shutdown()} has been called
     */
    public boolean isShutdown()
    {
        return threadPoolExecutor.isShutdown();
    }

    /**
     * @return {@code true} if all tasks have completed following a shutdown
     */
    public boolean isTerminated()
    {
        return threadPoolExecutor.isTerminated();
    }

    /**
     * Blocks the calling thread until the pool has terminated or the timeout
     * elapses.
     *
     * @param millis maximum time to wait, in milliseconds
     * @return {@code true} if the pool terminated within the timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(int millis) throws InterruptedException
    {
        return threadPoolExecutor.awaitTermination(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the core number of threads in the pool.
     *
     * @return core pool size
     */
    public int getCorePoolSize()
    {
        return threadPoolExecutor.getCorePoolSize();
    }

    /**
     * Returns the maximum allowed number of threads in the pool.
     *
     * @return maximum pool size
     */
    public int getMaximumPoolSize()
    {
        return threadPoolExecutor.getMaximumPoolSize();
    }

    /**
     * Sets the core number of threads in the pool.
     *
     * @param cps new core pool size
     */
    public void setCorePoolSize(int cps)
    {
        threadPoolExecutor.setCorePoolSize(cps);
    }

    /**
     * Sets the maximum allowed number of threads in the pool.
     *
     * @param mps new maximum pool size
     */
    public void setMaximumPoolSize(int mps)
    {
        threadPoolExecutor.setMaximumPoolSize(mps);
    }

    /**
     * Shuts down this Queen and blocks until the pool terminates.
     * Implements {@link AutoCloseable} for use in try-with-resources.
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
            Logger.getLogger(Queen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
