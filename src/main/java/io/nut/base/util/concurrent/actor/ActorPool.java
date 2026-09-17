/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import io.nut.base.math.Nums;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;

/**
 * A managed thread-pool with a rich task-execution API.
 * <p>
 * {@code ActorPool} wraps a {@link ThreadPoolExecutor} and exposes five execution
 * modes that cover every combination of <em>who executes</em> and
 * <em>whether a result is returned</em>:
 *
 * <table border="1">
 *   <tr><th></th><th>void</th><th>Future&lt;U&gt;</th></tr>
 *   <tr><td>fire &amp; forget</td><td>{@link #execute}</td><td>{@link #submit(Runnable)} / {@link #submit(Supplier)}</td></tr>
 *   <tr><td>guaranteed start</td><td>{@link #spawn}</td><td>—</td></tr>
 *   <tr><td>parallel + blocking</td><td>{@link #forEach}</td><td>—</td></tr>
 * </table>
 *
 * <p>The async pool is configured with two independent thread counts. The
 * {@code coreThreads} floor is the number of threads kept alive permanently:
 * they are never reclaimed, no matter the keep-alive time. The {@code maxThreads}
 * ceiling is the usual maximum number of live threads, never exceeded in
 * ordinary operation. Because the pool has no task queue (a
 * {@link SynchronousQueue} hands every submitted task directly to a thread),
 * worker threads grow on demand from the {@code coreThreads} floor up to
 * {@code maxThreads}, and the threads above the floor are reclaimed once they
 * stay idle for the keep-alive time, so the pool never drops below
 * {@code coreThreads} live threads. Constructing an ActorPool with
 * {@code coreThreads == 0 && maxThreads == 0} enables the synchronous mode, in
 * which every task runs directly in the calling thread and no backing pool
 * exists. The only case where the effective maximum may exceed the configured
 * {@code maxThreads} is the Actor-driven sizing of {@link ActorHub}: when more
 * non-synchronous Actors are registered than {@code maxThreads}, the pool must
 * still be able to host one thread per Actor, so the effective maximum is
 * lifted to exactly the number of registered Actors (never higher).
 * <p>
 * {@code ActorPool} also implements {@link Executor} (via {@link #execute}) so
 * it can be passed anywhere a plain {@code Executor} is expected.
 * <p>
 * {@code ActorPool} implements {@link AutoCloseable}: {@link #close()} shuts the
 * pool down gracefully and blocks until all tasks have finished.
 * <p>
 * {@link ActorHub} extends {@code ActorPool} and adds the Actor-specific factory methods
 * and the pub/sub registry on top of this execution foundation.
 */
public class ActorPool implements ActorLifecycle, Executor
{    

    /**
     * Number of available processor cores. Used as the default {@code coreThreads}
     * floor and, doubled, as the default {@code maxThreads} ceiling.
     */
    public static final int CORES = Runtime.getRuntime().availableProcessors();

    /**
     * Default keep-alive time for idle threads, in milliseconds (30 s).
     * Applies only to the threads above the effective core size: those threads
     * are reclaimed once idle for this long and the pool then stays at (or
     * shrinks down to) the core floor. Core threads are never timed out.
     */
    public static final int DEFAULT_KEEP_ALIVE_MILLIS = 30_000;
    public static final boolean DEFAULT_CALLER_WAITS_POLICY = false;
    public static final boolean DEFAULT_AVOID_TRACKER = false;

    /**
     * Saturation policy: run the overflowing task in the calling thread.
     */
    private static final ThreadPoolExecutor.CallerRunsPolicy CALLER_RUNS_POLICY = new ThreadPoolExecutor.CallerRunsPolicy();

    /**
     * Saturation policy: block the calling thread until a slot is available.
     */
    private static final CallerWaitsPolicy CALLER_WAITS_POLICY = new CallerWaitsPolicy();

    /**
     * The underlying thread pool. {@code null} in synchronous mode.
     */
    private final ThreadPoolExecutor threadPoolExecutor;

    private final Phaser phaser;

    /**
     * When {@code true}, all {@code execute}/{@code submit}/{@code spawn}/
     * {@code forEach} invocations run the task synchronously in the calling
     * thread, and there is no backing thread pool. Enabled when the pool is
     * constructed with {@code coreThreads == 0 && maxThreads == 0}.
     */
    private final boolean synchronous;

    /**
     * The configured minimum number of threads that stay permanently alive.
     * {@code 0} in synchronous mode. May be changed with
     * {@link #setThreads}; an {@link ActorHub} also recomputes the effective
     * core to cover one thread per registered async Actor.
     */
    protected volatile int coreThreads;

    /**
     * The configured ceiling on live threads. {@code 0} in synchronous mode.
     * May be changed with {@link #setThreads}. Bounds every thread count in
     * ordinary operation; the Actor-driven sizing of {@link ActorHub} may lift
     * the effective maximum past this value only when more non-synchronous
     * Actors are registered than {@code maxThreads} (then it is exactly the
     * number of Actors).
     */
    protected volatile int maxThreads;

    /**
     * Tracks the shutdown state in synchronous mode (there is no backing pool).
     */
    private volatile boolean shutdown;

    /**
     * When {@code true}, {@link #shutdown(boolean)} has been asked to close
     * admission of new work once the pool becomes idle. Cleared when the
     * request is honoured (or an immediate shutdown supersedes it).
     */
    private volatile boolean shutdownWhenIdle;
    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Protected constructor for subclasses that supply their own
     * {@link ThreadPoolExecutor} (e.g. {@link ProxyActorHub}).
     *
     * @param threadPoolExecutor the executor to delegate to; may be
     *                           {@code null} for lazy subclasses
     */
    protected ActorPool(ThreadPoolExecutor threadPoolExecutor)
    {
        this.threadPoolExecutor = threadPoolExecutor;
        this.synchronous = threadPoolExecutor==null;
        this.phaser = this.synchronous ? null : new Phaser(1);
        this.coreThreads = 0;
        this.maxThreads = 0;
    }

    /**
     * Full constructor.
     * <p>
     * The backing pool has no task queue: a {@link SynchronousQueue} hands every
     * submitted task directly to a worker thread, so the pool grows threads on
     * demand from the {@code coreThreads} floor up to {@code maxThreads} without
     * parked work. {@link ThreadPoolExecutor#allowCoreThreadTimeOut} is never
     * enabled, so the core threads stay alive permanently; only the threads above
     * the floor are reclaimed once idle for {@code keepAliveMillis}. Subclasses
     * such as {@link ActorHub} may resize the pool later via
     * {@link #resizePool(int, int)}.
     *
     * @param coreThreads      the minimum number of threads kept permanently
     *                         alive; must be &ge; 0 and &le; {@code maxThreads}
     * @param maxThreads       the absolute maximum number of live threads; must
     *                         be &ge; 0; {@code coreThreads == 0 && maxThreads == 0}
     *                         selects the synchronous mode (no pool)
     * @param keepAliveMillis  keep-alive time for the threads above the core
     *                         floor, in milliseconds; must be &ge; 0
     * @param callerWaitsPolicy {@code true} to block the caller on saturation;
     *                          {@code false} to run the task in the caller
     * @param avoidTracker       {@code true} to disable active-task tracking
     * @throws IllegalArgumentException if {@code coreThreads &lt; 0},
     *         {@code maxThreads &lt; 0}, {@code coreThreads &gt; maxThreads}, or
     *         {@code keepAliveMillis &lt; 0}; the synchronous case is exempt from
     *         these checks
     */
    public ActorPool(int coreThreads, int maxThreads, int keepAliveMillis, boolean callerWaitsPolicy, boolean avoidTracker)
    {
        this.synchronous = coreThreads == 0 && maxThreads == 0;
        if (!this.synchronous)
        {
            if (coreThreads < 0)
            {
                throw new IllegalArgumentException("coreThreads must be >= 0, got " + coreThreads);
            }
            if (coreThreads > maxThreads)
            {
                throw new IllegalArgumentException("coreThreads must be <= maxThreads, got " + coreThreads + " > " + maxThreads);
            }
            if (keepAliveMillis < 0)
            {
                throw new IllegalArgumentException("keepAliveMillis must be >= 0, got " + keepAliveMillis);
            }
        }
        this.coreThreads = coreThreads;
        this.maxThreads = maxThreads;
        if (this.synchronous)
        {
            this.threadPoolExecutor = null;
            this.phaser = null;
            return;
        }
        this.threadPoolExecutor = new ThreadPoolExecutor(
                coreThreads, maxThreads,
                keepAliveMillis, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                callerWaitsPolicy ? CALLER_WAITS_POLICY : CALLER_RUNS_POLICY);
        this.phaser = avoidTracker ? null : new Phaser(1);
    }

    /**
     * Full constructor with tracking disabled.
     *
     * @param coreThreads       the minimum number of threads kept permanently alive
     * @param maxThreads        the absolute maximum number of live threads; both
     *                          {@code 0} selects the synchronous mode
     * @param keepAliveMillis   keep-alive time for the threads above the core
     *                          floor, in milliseconds
     * @param callerWaitsPolicy {@code true} to block the caller on saturation;
     *                          {@code false} to run the task in the caller
     */
    public ActorPool(int coreThreads, int maxThreads, int keepAliveMillis, boolean callerWaitsPolicy)
    {
        this(coreThreads, maxThreads, keepAliveMillis, callerWaitsPolicy, DEFAULT_AVOID_TRACKER);
    }

    /**
     * Constructs an ActorPool with the {@link ThreadPoolExecutor.CallerRunsPolicy}.
     *
     * @param coreThreads      the minimum number of threads kept permanently alive
     * @param maxThreads       the absolute maximum number of live threads; both
     *                         {@code 0} selects the synchronous mode
     * @param keepAliveMillis  keep-alive time for the threads above the core
     *                         floor, in milliseconds
     */
    public ActorPool(int coreThreads, int maxThreads, int keepAliveMillis)
    {
        this(coreThreads, maxThreads, keepAliveMillis, DEFAULT_CALLER_WAITS_POLICY, DEFAULT_AVOID_TRACKER);
    }

    /**
     * Constructs an elastic ActorPool with {@code coreThreads} threads kept
     * permanently alive and a {@code maxThreads} ceiling of {@code 2 * coreThreads}.
     * With {@code coreThreads == 0} this yields the synchronous mode.
     *
     * @param coreThreads number of permanently alive threads
     */
    public ActorPool(int coreThreads)
    {
        this(coreThreads, coreThreads + coreThreads, DEFAULT_KEEP_ALIVE_MILLIS, DEFAULT_CALLER_WAITS_POLICY, DEFAULT_AVOID_TRACKER);
    }

    /**
     * Constructs an elastic ActorPool sized to the number of available CPU cores:
     * {@code coreThreads = CORES} and {@code maxThreads = 2 * CORES}.
     */
    public ActorPool()
    {
        this(CORES, CORES + CORES, DEFAULT_KEEP_ALIVE_MILLIS, DEFAULT_CALLER_WAITS_POLICY, DEFAULT_AVOID_TRACKER);
    }

    // -------------------------------------------------------------------------
    // Static factories
    // -------------------------------------------------------------------------

    /**
     * @return a new ActorPool with default (elastic, CPU-core-sized) settings.
     */
    public static ActorPool actorPool()
    {
        return new ActorPool();
    }

    /**
     * @param coreThreads the floor of permanently alive threads
     * @return a new elastic ActorPool with {@code maxThreads = 2 * coreThreads}
     *         ({@code 0} selects the synchronous mode)
     */
    public static ActorPool actorPool(int coreThreads)
    {
        return new ActorPool(coreThreads);
    }

    /**
     * @param coreThreads      the floor of permanently alive threads
     * @param maxThreads       the absolute maximum number of live threads
     * @param keepAliveMillis  keep-alive time for the threads above the core
     *                         floor, in milliseconds
     * @return a new ActorPool with the given pool configuration
     */
    public static ActorPool actorPool(int coreThreads, int maxThreads, int keepAliveMillis)
    {
        return new ActorPool(coreThreads, maxThreads, keepAliveMillis);
    }

    /**
     * @param coreThreads       the floor of permanently alive threads
     * @param maxThreads        the absolute maximum number of live threads
     * @param keepAliveMillis   keep-alive time for the threads above the core
     *                          floor, in milliseconds
     * @param callerWaitsPolicy {@code true} to block caller on saturation
     * @return a new ActorPool with full pool configuration
     */
    public static ActorPool actorPool(int coreThreads, int maxThreads, int keepAliveMillis, boolean callerWaitsPolicy)
    {
        return new ActorPool(coreThreads, maxThreads, keepAliveMillis, callerWaitsPolicy);
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this ActorPool was constructed with
     * {@code coreThreads == 0 && maxThreads == 0}, in which case every
     * execution method runs tasks synchronously in the calling thread and no
     * backing pool exists.
     *
     * @return {@code true} if tasks run synchronously
     */
    public boolean isSynchronous()
    {
        return synchronous;
    }

    /**
     * Returns the configured minimum number of threads kept permanently alive.
     * In an {@link ActorHub} the effective core size may additionally grow to
     * cover one thread per registered async Actor (see
     * {@link #getCorePoolSize()}).
     *
     * @return the configured {@code coreThreads} value
     */
    public int getCoreThreads()
    {
        return coreThreads;
    }

    /**
     * Returns the configured absolute ceiling on live threads.
     *
     * @return the configured {@code maxThreads} value
     */
    public int getMaxThreads()
    {
        return maxThreads;
    }

    // -------------------------------------------------------------------------
    // Execution API
    // -------------------------------------------------------------------------

    /**
     * Wraps {@code task} with the execution phaser, registering a party when
     * the task starts running and arriving/deregistering on completion. Used
     * by {@link #spawn(Runnable)}, which already blocks until the task is
     * running. All other execution paths pre-register in the submitting thread
     * and use {@link #tracked(Runnable)} instead, so that queued tasks are
     * visible to {@link #waitForIdle()} too.
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
            // Registers the task in the Phaser when it starts running, leaving
            // it to the callers that pre-register on submission to manage the
            // matching deregistration.
            phaser.register();
            try
            {
                task.run();
            }
            finally
            {
                phaser.arriveAndDeregister();
                maybeShutdownWhenIdle();
            }
        };
    }
    
    /**
     * Wraps {@code task} so that exactly one {@code arriveAndDeregister} is
     * issued on the execution phaser when the task completes, without
     * registering anything. The matching {@code register()} is performed by
     * the submitter <em>before</em> handing the task to the pool, so that
     * queued-but-not-yet-started tasks are still visible to
     * {@link #waitForIdle()}.
     *
     * @param task the already-registered task; must not be {@code null}
     * @return the wrapped task, or {@code task} unchanged if tracking is disabled
     */
    private Runnable tracked(Runnable task)
    {
        if(phaser==null)
        {
            return task;
        }
        return () ->
        {
            try
            {
                task.run();
            }
            finally
            {
                phaser.arriveAndDeregister();
                maybeShutdownWhenIdle();
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
        if (synchronous)
        {
            return true;
        }
        return phaser != null ? getActiveCount() == 0 : threadPoolExecutor.getActiveCount() == 0;
    }

    /**
     * Blocks the calling thread until all running tasks have finished. If there
     * are no tasks running or queued, returns immediately.
     */
    @Override
    public ActorPool waitForIdle()
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
     * Implements {@link Executor}.
     *
     * @param task the task to execute; must not be {@code null}
     */
    @Override
    public void execute(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");
        if (synchronous)
        {
            task.run();
            return;
        }
        if(phaser==null)
        {
            this.threadPoolExecutor.execute(task);
            return;
        }
        // Register before handing the task to the pool: a task that is merely
        // queued (waiting for a free pool thread) is already "active" from the
        // point of view of waitForIdle()/isIdle(). Registering inside the task
        // (as this class once did) let waitForIdle() return while worker tasks
        // sat in the pool queue, so a subsequent shutdown raced in-flight
        // forwards and silently dropped messages.
        phaser.register();
        try
        {
            this.threadPoolExecutor.execute(tracked(task));
        }
        catch (RuntimeException ex)
        {
            phaser.arriveAndDeregister();
            throw ex;
        }
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
        if (synchronous)
        {
            task.run();
            return CompletableFuture.completedFuture(null);
        }
        if(phaser==null)
        {
            return CompletableFuture.runAsync(task, this.threadPoolExecutor);
        }
        // Pre-register like execute(Runnable) so queued tasks are visible to
        // waitForIdle(). If the pool never runs the task (rejection), the
        // whenComplete callback deregisters the pre-registered party exactly
        // once; otherwise the task's own finally does.
        phaser.register();
        AtomicBoolean arrived = new AtomicBoolean();
        Runnable registeredTask = () ->
        {
            try
            {
                task.run();
            }
            finally
            {
                arrived.set(true);
                phaser.arriveAndDeregister();
                maybeShutdownWhenIdle();
            }
        };
        return CompletableFuture.runAsync(registeredTask, this.threadPoolExecutor).whenComplete((v, ex) ->
        {
            if (arrived.compareAndSet(false, true))
            {
                phaser.arriveAndDeregister();
                maybeShutdownWhenIdle();
            }
        });
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
        if (synchronous)
        {
            return CompletableFuture.completedFuture(supplier.get());
        }
        if (phaser == null)
        {
            return CompletableFuture.supplyAsync(supplier, this.threadPoolExecutor);
        }
        // Pre-register on submission; see submit(Runnable) for the
        // deregistration guarantee.
        phaser.register();
        AtomicBoolean arrived = new AtomicBoolean();
        Supplier<U> registeredSupplier = () ->
        {
            try
            {
                return supplier.get();
            }
            finally
            {
                arrived.set(true);
                phaser.arriveAndDeregister();
                maybeShutdownWhenIdle();
            }
        };
        return CompletableFuture.supplyAsync(registeredSupplier, this.threadPoolExecutor).whenComplete((v, ex) ->
        {
            if (arrived.compareAndSet(false, true))
            {
                phaser.arriveAndDeregister();
                maybeShutdownWhenIdle();
            }
        });
    }

    /**
     * Submits {@code task} to the thread pool and blocks until the worker is
     * guaranteed to be executing {@code task.run()} as its very next
     * instruction, then returns. See class-level documentation for details.
     *
     * @param task the task to spawn; must not be {@code null}
     */
    public ActorPool spawn(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");

        if (synchronous)
        {
            task.run();
            return this;
        }

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
            // The task was already accepted by the pool in the execute() above
            // and is guaranteed to run exactly once; only the wait is given up.
            Thread.currentThread().interrupt();
        }
        return this;
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

        if (synchronous)
        {
            for (T item : iterable)
            {
                consumer.accept(item);
            }
            return;
        }

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
     * <p>
     * Never blocks: the underlying {@link ThreadPoolExecutor} shuts down in
     * the background, completing tasks that were already submitted before the
     * shutdown.
     *
     * @return this ActorPool, for fluent chaining
     */
    @Override
    public ActorPool shutdown()
    {
        if (synchronous)
        {
            this.shutdown = true;
        }
        else
        {
            this.threadPoolExecutor.shutdown();
        }
        return this;
    }

    /**
     * Stops accepting new work, optionally deferring admission closure until
     * the pool is idle. Never blocks: with {@code true} the pool keeps
     * accepting and processing work until it runs empty and only then shuts
     * down its backing pool; {@link ThreadPoolExecutor#shutdown()} is already
     * graceful, so previously submitted work is always completed in both
     * forms.
     * <p>
     * If idle-tracking was disabled at construction time ({@code avoidTracker}
     * or synchronous mode), the deferral cannot be reliably observed and this
     * degrades to the immediate {@link #shutdown()}.
     *
     * @param whenIdle if {@code true}, shut down once the pool is idle
     *                 (deferred); if {@code false}, shut down immediately
     * @return this ActorPool, for fluent chaining
     */
    @Override
    public ActorPool shutdown(boolean whenIdle)
    {
        if (whenIdle)
        {
            if (phaser == null)
            {
                shutdown();
                return this;
            }
            if (shutdownWhenIdle)
            {
                return this;
            }
            shutdownWhenIdle = true;
            maybeShutdownWhenIdle();
        }
        else
        {
            shutdown();
        }
        return this;
    }

    /**
     * Shuts the backing pool down if a deferred
     * ({@code shutdown(true)}) request is pending and the pool is now idle.
     * Called from the completion hooks of {@link #wrap(Runnable)},
     * {@link #tracked(Runnable)} and {@link #submit}, so it runs on the
     * worker thread that just finished the last tracked task. Idempotent:
     * clears {@link #shutdownWhenIdle} before invoking {@link #shutdown()}.
     */
    private void maybeShutdownWhenIdle()
    {
        if (!shutdownWhenIdle || synchronous)
        {
            return;
        }
        boolean idle = phaser == null
                ? threadPoolExecutor.getActiveCount() == 0
                : phaser.getRegisteredParties() == 1;
        if (idle)
        {
            shutdownWhenIdle = false;
            shutdown();
        }
    }

    /**
     * @return {@code true} if this instance has actually closed admission of
     *         new work: immediately for {@link #shutdown()}, or once idle for
     *         the deferred form {@link #shutdown(boolean) shutdown(true)}
     */
    @Override
    public boolean isShutdown()
    {
        return synchronous ? shutdown : threadPoolExecutor.isShutdown();
    }

    /**
     * @return {@code true} if all tasks have completed following a shutdown
     */
    @Override
    public boolean isTerminated()
    {
        return synchronous ? shutdown : threadPoolExecutor.isTerminated();
    }

    /**
     * Blocks the calling thread until the pool has terminated or the timeout
     * elapses.
     * <p>
     * Not responsive to interruption: an interrupt leaves this method waiting
     * (until the deadline or termination) and does not restore the interrupt
     * flag.
     *
     * @param millis maximum time to wait, in milliseconds
     * @return {@code true} if the pool terminated within the timeout
     */
    public boolean awaitTermination(int millis)
    {
        if (synchronous)
        {
            return true;
        }
        long untilNanos = Nums.saturatedAdd(System.nanoTime(), TimeUnit.MILLISECONDS.toNanos(millis));
        while (true)
        {
            long remainingNanos = untilNanos - System.nanoTime();
            if (remainingNanos <= 0)
            {
                return threadPoolExecutor.isTerminated();
            }
            try
            {
                if (threadPoolExecutor.awaitTermination(remainingNanos, TimeUnit.NANOSECONDS))
                {
                    return true;
                }
            }
            catch (InterruptedException ex)
            {
                // Ignored by contract: keep waiting until the deadline or
                // termination. The interrupt flag is deliberately not restored.
            }
        }
    }

    /**
     * Blocks until the pool has terminated.
     * <p>
     * Not responsive to interruption: an interrupt leaves this method waiting
     * until termination and does not restore the interrupt flag.
     *
     * @return this ActorPool, for fluent chaining
     */
    @Override
    public ActorPool awaitTermination()
    {
        awaitTermination(Integer.MAX_VALUE);
        return this;
    }

    /**
     * Returns the number of threads that are kept alive permanently (never
     * reclaimed, regardless of the keep-alive time). In an {@link ActorHub} this
     * is the {@link #getCoreThreads()} floor grown to also cover one thread per
     * registered async Actor.
     *
     * @return effective core pool size
     */
    public int getCorePoolSize()
    {
        return synchronous ? 0 : threadPoolExecutor.getCorePoolSize();
    }

    /**
     * Returns the maximum allowed number of threads in the pool. Never exceeds
     * the configured {@link #getMaxThreads()} ceiling.
     *
     * @return maximum pool size
     */
    public int getMaximumPoolSize()
    {
        return synchronous ? 0 : threadPoolExecutor.getMaximumPoolSize();
    }

    /**
     * Returns the current number of live threads in the pool (running or
     * parked), or {@code 0} in synchronous mode.
     *
     * @return current live thread count
     */
    public int getPoolSize()
    {
        return synchronous ? 0 : threadPoolExecutor.getPoolSize();
    }

    /**
     * Sets both the configured {@code coreThreads} floor and the
     * {@code maxThreads} ceiling and resizes the pool to them in a single call,
     * so no particular call order is required whether the pool is grown or
     * shrunk. The pair is kept coherent: if {@code coreThreads > maxThreads},
     * the ceiling is raised to the floor. The backing pool has no task queue,
     * so the threads above the floor come and go on demand; only the core
     * floor is kept permanently alive.
     * <p>
     * In an {@link ActorHub} the effective pool sizes are recomputed from the
     * registered Actors as well: the effective core never drops below one
     * thread per registered non-synchronous Actor and the effective maximum
     * never drops below the summed thread demand, but the configured
     * {@code maxThreads} ceiling materializes in the live pool immediately
     * ({@link #getMaximumPoolSize()}), not only when the Actors demand the
     * threads.
     * <p>
     * A no-op on a synchronous pool (constructed with
     * {@code coreThreads == 0 && maxThreads == 0}), which stays synchronous by
     * design.
     *
     * @param coreThreads the new permanently-alive thread floor; must be &ge; 0
     * @param maxThreads  the new ceiling on live threads; must be &ge;
     *                    {@code coreThreads} (excess core is not an error:
     *                    the ceiling is raised to match)
     * @throws IllegalArgumentException if either value is negative
     */
    public void setThreads(int coreThreads, int maxThreads)
    {
        if (synchronous)
        {
            return;
        }
        if (coreThreads < 0)
        {
            throw new IllegalArgumentException("coreThreads must be >= 0, got " + coreThreads);
        }
        if (maxThreads < 0)
        {
            throw new IllegalArgumentException("maxThreads must be >= 0, got " + maxThreads);
        }
        if (maxThreads < coreThreads)
        {
            maxThreads = coreThreads;
        }
        this.coreThreads = coreThreads;
        this.maxThreads = maxThreads;
        resizePool(coreThreads, maxThreads);
    }

    /**
     * Resizes the backing pool to the given effective core and maximum sizes,
     * keeping the two coherent: the maximum is never left below the core (nor
     * below 1, which {@link ThreadPoolExecutor} requires), so when growing the
     * maximum is set first and when shrinking the core is set first. The
     * configured {@code maxThreads} ceiling has already been applied (or
     * deliberately lifted to the Actor count) by the caller.
     *
     * @param core the new effective core size
     * @param max the new effective maximum size
     */
    protected void resizePool(int core, int max)
    {
        if (!synchronous)
        {
            if (core > max)
            {
                max = core;
            }
            // ThreadPoolExecutor requires a positive maximumPoolSize
            if (max < 1)
            {
                max = 1;
            }
            if (core >= threadPoolExecutor.getCorePoolSize())
            {
                threadPoolExecutor.setMaximumPoolSize(max);
                threadPoolExecutor.setCorePoolSize(core);
            }
            else
            {
                threadPoolExecutor.setCorePoolSize(core);
                threadPoolExecutor.setMaximumPoolSize(max);
            }
        }
    }

    /**
     * Applies the Actor-driven sizing of an {@link ActorHub} to the backing pool:
     * the effective core covers, at least, one permanently alive thread per
     * registered non-synchronous Actor and the effective maximum covers both the
     * configured {@code maxThreads} ceiling and all of the Actors' declared
     * thread demand (the maximum never dropping below the effective core). The
     * sole exception to the {@code maxThreads} ceiling: when more non-synchronous
     * Actors are registered than {@code maxThreads}, the pool must be able to
     * host one thread per Actor, so the effective maximum is lifted to exactly
     * {@code asyncActorCount} (never higher). This helper is {@code final} and
     * uses only base-class state, so it is safe to call from constructors of
     * subclasses that override the config getters with a delegation layer.
     *
     * @param asyncActorCount number of registered Actors with a positive thread
     *                        demand
     * @param threadDemand    summed thread demand of the registered Actors
     */
    protected final void applyActorSizing(int asyncActorCount, int threadDemand)
    {
        if (threadPoolExecutor == null)
        {
            return;
        }
        int ceiling = asyncActorCount > this.maxThreads ? asyncActorCount : this.maxThreads;
        int core = Math.min(ceiling, Math.max(this.coreThreads, asyncActorCount));
        int max = Math.min(ceiling, Nums.maxOf(this.maxThreads, threadDemand, core));
        resizePool(core, max);
    }

    /**
     * Shuts down this ActorPool and blocks until the pool terminates.
     * Implements {@link AutoCloseable} for use in try-with-resources.
     */
    @Override
    public void close()
    {
        this.shutdown();
        this.awaitTermination(Integer.MAX_VALUE);
    }
}
