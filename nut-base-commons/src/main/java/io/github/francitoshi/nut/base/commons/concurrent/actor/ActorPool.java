/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import io.github.francitoshi.nut.base.commons.concurrent.Threads;
import io.github.francitoshi.nut.base.commons.lang.Saturated;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * A managed thread-pool with a rich task-execution API.
 * <p>
 * {@code ActorPool} wraps a {@link ThreadPoolExecutor} and exposes five execution
 * methods, grouped by <em>when control returns to the caller</em> and by
 * <em>whether a result is produced</em>:
 *
 * <table border="1">
 *   <caption>Execution methods grouped by completion semantics and result type</caption>
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
 * lifted to the number of registered Actors plus a rush budget of
 * {@code maxThreads} extra threads (see the ActorHub documentation).
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
    private static final Logger LOG = Logger.getLogger(ActorPool.class.getName());

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

    /**
     * Default saturation policy: {@code false} selects
     * {@link ThreadPoolExecutor.CallerRunsPolicy}, which runs the overflowing
     * task in the calling thread when no worker thread is available.
     */
    public static final boolean DEFAULT_CALLER_WAITS_POLICY = false;

    /**
     * Default active-task tracking: {@code false} keeps the active-task
     * tracker enabled, which backs {@link #isIdle()},
     * {@link #getActiveCount()} and {@link #waitForIdle()}.
     */
    public static final boolean DEFAULT_AVOID_TRACKER = false;

    /**
     * Default worker threading: {@code false} creates ordinary non-daemon
     * threads, which keep the JVM alive while tasks are running.
     */
    public static final boolean DEFAULT_DAEMON = false;

    /**
     * Default virtual-thread preference: {@code false} keeps the classic
     * platform-thread pool. Pass {@code allowVirtual == true} to opt in.
     */
    public static final boolean DEFAULT_ALLOW_VIRTUAL = false;

    /**
     * Saturation policy: run the overflowing task in the calling thread.
     */
    private static final ThreadPoolExecutor.CallerRunsPolicy CALLER_RUNS_POLICY = new ThreadPoolExecutor.CallerRunsPolicy();

    /**
     * Saturation policy: block the calling thread until a slot is available.
     */
    private static final CallerWaitsPolicy CALLER_WAITS_POLICY = new CallerWaitsPolicy();

    /**
     * Wraps {@code delegate}, the thread factory, so that every thread it
     * creates is a daemon thread. Used when an ActorPool is constructed with
     * {@code daemon == true}: pool threads then never keep the JVM alive on
     * their own, so background tasks cannot prolong application exit.
     *
     * @param delegate the factory to delegate to; must not be {@code null}
     * @return a factory producing daemon threads
     */
    private static ThreadFactory daemonFactory(ThreadFactory delegate)
    {
        return r ->
        {
            Thread t = delegate.newThread(r);
            t.setDaemon(true);
            return t;
        };
    }

    /**
     * The underlying thread pool. {@code null} in synchronous mode.
     */
    private final ThreadPoolExecutor threadPoolExecutor;

    /**
     * The thread factory of the backing pool, kept so
     * {@link #executeUntracked(Runnable)} can give a starved permanent worker
     * a dedicated thread with the same characteristics (daemon flag, virtual
     * support) as pool threads. {@code null} in synchronous mode.
     */
    private final ThreadFactory threadFactory;

    /**
     * Active-task tracker used by {@link #getActiveCount()},
     * {@link #isIdle()} and {@link #waitForIdle()}. Tasks pre-register on
     * submission and deregister around their execution; the tracker parks
     * {@link #waitForIdle()} callers until the count reaches zero.
     * {@code null} when active-task tracking was disabled at construction
     * ({@code avoidTracker == true}) or in synchronous mode.
     */
    private final TaskTracker tracker;

    /**
     * The active-task tracker itself. It replaced a {@code Phaser} (one
     * permanent owner party, tasks registering on submission and
     * deregistering on completion): the atomic counter is a single RMW per
     * task on both sides instead of the Phaser's multi-field phase
     * bookkeeping, and the {@code waitForIdle()} machinery only takes the
     * lock on the transition to zero.
     */
    private static final class TaskTracker
    {
        private final AtomicInteger active = new AtomicInteger();
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition allDone = lock.newCondition();

        /**
         * Registers a task <em>before</em> it is handed to the pool, so a
         * task that is merely queued is already visible to
         * {@code waitForIdle()}/{@code isIdle()}.
         */
        void beforeSubmit()
        {
            active.incrementAndGet();
        }

        /**
         * Deregisters a completed task (or a submission whose pool rejected
         * it). Only the transition to zero takes the lock and signals.
         */
        void afterComplete()
        {
            if (active.decrementAndGet() == 0)
            {
                lock.lock();
                try
                {
                    allDone.signalAll();
                }
                finally
                {
                    lock.unlock();
                }
            }
        }

        int getActive()
        {
            return active.get();
        }

        boolean isIdle()
        {
            return active.get() == 0;
        }

        /**
         * Blocks until every registered task has completed. New tasks may
         * register while waiting; the waiter re-checks the count under the
         * lock and every transition to zero signals under that same lock,
         * so a wake-up cannot be lost. Not responsive to interruption,
         * matching the Phaser-based implementation it replaced.
         */
        void awaitIdle()
        {
            lock.lock();
            try
            {
                while (active.get() != 0)
                {
                    allDone.awaitUninterruptibly();
                }
            }
            finally
            {
                lock.unlock();
            }
        }
    }

    /**
     * When {@code true}, all {@code execute}/{@code submit}/{@code spawn}/
     * {@code forEach} invocations run the task synchronously in the calling
     * thread, and there is no backing thread pool. Enabled when the pool is
     * constructed with {@code coreThreads == 0 && maxThreads == 0}.
     */
    private final boolean synchronous;

    /**
     * {@code true} when the backing pool was built with an abort-style
     * saturation policy (the {@link ActorHub} does this): rejections throw,
     * and every submission path resolves them explicitly — worker starts
     * degrade (skip / dedicated thread) and user submissions re-admit by
     * blocking until a worker accepts them. The plain {@code ActorPool}
     * policies (caller-runs / caller-waits) resolve rejections inside the
     * policy itself and never reach the hooks.
     */
    private final boolean abortSaturation;

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
        this.tracker = this.synchronous ? null : new TaskTracker();
        this.threadFactory = this.synchronous
                ? null
                : workerThreadFactory(DEFAULT_DAEMON, DEFAULT_ALLOW_VIRTUAL);
        this.abortSaturation = false;
        this.coreThreads = 0;
        this.maxThreads = 0;
    }

    /**
     * Full constructor with platform threads only: equivalent to
     * {@code ActorPool(coreThreads, maxThreads, keepAliveMillis,
     * callerWaitsPolicy, avoidTracker, daemon, false)}. See the seven-parameter
     * constructor for the full documentation.
     * <p>
     * The backing pool has no task queue: a {@link SynchronousQueue} hands every
     * submitted task directly to a worker thread, so the pool grows threads on
     * demand from the {@code coreThreads} floor up to {@code maxThreads} without
     * a task ever waiting in a queue. {@link ThreadPoolExecutor#allowCoreThreadTimeOut}
     * is never enabled, so the core threads stay alive permanently; only the
     * threads above the floor are reclaimed once idle for
     * {@code keepAliveMillis}. Subclasses such as {@link ActorHub} may resize
     * the pool later via {@link #resizePool(int, int)}.
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
     * @param daemon             {@code true} to create the pool's worker threads
     *                           as daemon threads, so that the pool never keeps
     *                           the JVM alive on its own when only background
     *                           tasks are running; {@code false} (the default)
     *                           to use the framework's ordinary non-daemon
     *                           worker threads. Only the thread creation is
     *                           intercepted: each thread still comes from the
     *                           {@link ThreadPoolExecutor}'s default
     *                           {@link ThreadFactory}, merely with
     *                           {@link Thread#setDaemon(boolean) setDaemon(true)}
     *                           applied
     * @throws IllegalArgumentException if {@code coreThreads &lt; 0},
     *         {@code maxThreads &lt; 0}, {@code coreThreads &gt; maxThreads}, or
     *         {@code keepAliveMillis &lt; 0}; the synchronous case is exempt from
     *         these checks
     */
    public ActorPool(int coreThreads, int maxThreads, int keepAliveMillis, boolean callerWaitsPolicy, boolean avoidTracker, boolean daemon)
    {
        this(coreThreads, maxThreads, keepAliveMillis, callerWaitsPolicy, avoidTracker, daemon, DEFAULT_ALLOW_VIRTUAL);
    }

    /**
     * Full constructor with virtual-thread support.
     * <p>
     * The backing pool has no task queue: a {@link SynchronousQueue} hands every
     * submitted task directly to a worker thread, so the pool grows threads on
     * demand from the {@code coreThreads} floor up to {@code maxThreads} without
     * a task ever waiting in a queue. {@link ThreadPoolExecutor#allowCoreThreadTimeOut}
     * is never enabled, so the core threads stay alive permanently; only the
     * threads above the floor are reclaimed once idle for
     * {@code keepAliveMillis}. Subclasses such as {@link ActorHub} may resize
     * the pool later via {@link #resizePool(int, int)}.
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
     * @param daemon             {@code true} to create the pool's worker threads
     *                           as daemon threads, so that the pool never keeps
     *                           the JVM alive on its own when only background
     *                           tasks are running; {@code false} (the default)
     *                           to use the framework's ordinary non-daemon
     *                           worker threads. Only the thread creation is
     *                           intercepted: each thread still comes from the
     *                           {@link ThreadPoolExecutor}'s default
     *                           {@link ThreadFactory}, merely with
     *                           {@link Thread#setDaemon(boolean) setDaemon(true)}
     *                           applied
     * @param allowVirtual       {@code true} to create the pool's worker threads
     *                           as virtual threads when the running JVM supports
     *                           them (see
     *                           {@link Threads#isVirtualAvailable()}), which
     *                           makes the per-thread cost negligible and the
     *                           {@code daemon} flag inapplicable (virtual
     *                           threads are always daemon threads); if virtual
     *                           threads are unavailable, it degrades to the
     *                           platform behaviour described by {@code daemon}.
     *                           {@code false} (the default) always uses the
     *                           platform threads of {@code daemon}. The pool
     *                           sizing (core floor, ceiling and keep-alive)
     *                           keeps its meaning: a parked virtual worker
     *                           costs almost nothing
     * @throws IllegalArgumentException if {@code coreThreads &lt; 0},
     *         {@code maxThreads &lt; 0}, {@code coreThreads &gt; maxThreads}, or
     *         {@code keepAliveMillis &lt; 0}; the synchronous case is exempt from
     *         these checks
     */
    public ActorPool(int coreThreads, int maxThreads, int keepAliveMillis, boolean callerWaitsPolicy, boolean avoidTracker, boolean daemon, boolean allowVirtual)
    {
        this(coreThreads, maxThreads, keepAliveMillis,
                callerWaitsPolicy ? CALLER_WAITS_POLICY : CALLER_RUNS_POLICY,
                avoidTracker, daemon, allowVirtual);
    }

    /**
     * The saturation policy of {@link ActorHub} pools: rejections throw, and
     * every submission path resolves them explicitly at the call site —
     * worker starts degrade (skip / dedicated thread, see
     * {@code doc/actor-rules.txt} R1–R3) and user submissions block until
     * a worker accepts them. No rejection may ever be resolved by running
     * the task in the caller, and no framework submission may ever block
     * inside a policy where it could not tell rejection from admission.
     */
    static final RejectedExecutionHandler ABORT_SATURATION_POLICY = new ThreadPoolExecutor.AbortPolicy();

    /**
     * Package-private constructor used by {@link ActorHub}: same pool shape
     * as the public constructor, but with the abort-style saturation policy
     * ({@link #ABORT_SATURATION_POLICY}).
     */
    /**
     * Package-private constructor used by {@link ActorHub}: same pool shape
     * as the public constructor, but with the abort-style saturation policy
     * ({@link #ABORT_SATURATION_POLICY}).
     */
    ActorPool(int coreThreads, int maxThreads, int keepAliveMillis, RejectedExecutionHandler saturationPolicy, boolean avoidTracker, boolean daemon, boolean allowVirtual)
    {
        this.abortSaturation = saturationPolicy == ABORT_SATURATION_POLICY;
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
            this.tracker = null;
            this.threadFactory = null;
        }
        else
        {
            this.threadFactory = workerThreadFactory(daemon, allowVirtual);
            this.threadPoolExecutor = new ThreadPoolExecutor(
                    coreThreads, maxThreads,
                    keepAliveMillis, TimeUnit.MILLISECONDS,
                    new SynchronousQueue<>(),
                    this.threadFactory,
                    saturationPolicy);
            this.tracker = avoidTracker ? null : new TaskTracker();
        }
    }

    /**
     * Builds the thread factory of the backing pool. When {@code allowVirtual}
     * is {@code true} and the JVM has virtual threads, the factory comes from
     * {@link Threads#getThreadFactory(boolean)} and creates virtual threads
     * (always daemon, so {@code daemon} is not applied). Otherwise it creates
     * platform threads: the default factory, wrapped so every thread is a
     * daemon thread when {@code daemon} is {@code true}.
     *
     * @param daemon the requested daemon flag for platform threads
     * @param allowVirtual whether virtual threads are preferred when available
     * @return the thread factory; never {@code null}
     */
    private static ThreadFactory workerThreadFactory(boolean daemon, boolean allowVirtual)
    {
        if (allowVirtual && Threads.isVirtualAvailable())
        {
            return Threads.getThreadFactory(true);
        }
        ThreadFactory platform = Threads.getThreadFactory(false);
        return daemon ? daemonFactory(platform) : platform;
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
     * @param avoidTracker      {@code true} to disable active-task tracking
     */
    public ActorPool(int coreThreads, int maxThreads, int keepAliveMillis, boolean callerWaitsPolicy, boolean avoidTracker)
    {
        this(coreThreads, maxThreads, keepAliveMillis, callerWaitsPolicy, avoidTracker, DEFAULT_DAEMON);
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
     * Static factory for an ActorPool with default (elastic, CPU-core-sized)
     * settings, equivalent to {@link #ActorPool()}.
     *
     * @return a new ActorPool with default (elastic, CPU-core-sized) settings.
     */
    public static ActorPool actorPool()
    {
        return new ActorPool();
    }

    /**
     * Static factory for an elastic ActorPool with {@code maxThreads = 2 * coreThreads}.
     *
     * @param coreThreads the floor of permanently alive threads
     * @return a new elastic ActorPool with {@code maxThreads = 2 * coreThreads}
     *         ({@code 0} selects the synchronous mode)
     */
    public static ActorPool actorPool(int coreThreads)
    {
        return new ActorPool(coreThreads);
    }

    /**
     * Static factory for an ActorPool with explicit bounds and keep-alive,
     * equivalent to {@link #ActorPool(int, int, int)}.
     *
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
     * Static factory for an ActorPool with an explicit saturation policy,
     * equivalent to {@link #ActorPool(int, int, int, boolean)}.
     *
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
     * Runs {@code task} on the backing pool without registering it with the
     * active-task tracker. This is reserved for long-lived background workers,
     * such as the permanent worker of an async {@link Actor}: they live for as
     * long as the actor exists and would otherwise keep {@link #waitForIdle()}
     * and {@link #isIdle()} forever busy. Their activity is tracked by the
     * owning actor instead.
     * <p>
     * <strong>Saturation guard:</strong> the task must never end up running in
     * the caller and never block the caller waiting for a pool slot. A
     * {@link ThreadPoolExecutor.CallerRunsPolicy} would execute the task
     * inline when no thread can take it — for a permanent worker that parks
     * the producer inside the actor's drain loop until close, and a blocking
     * policy would wait for a slot that may never appear, since permanent
     * workers never poll the pool's queue. Admission is therefore three-tier:
     * <ol>
     * <li>when the pool is at its maximum and every thread is busy inside a
     *     task, the worker goes straight to a dedicated thread;</li>
     * <li>otherwise a non-blocking {@code offer} on the pool's hand-off queue
     *     delivers the task to a polling worker without ever reaching the
     *     rejection handler;</li>
     * <li>as a last resort the task is submitted through the pool wrapped in
     *     a guard: if the rejection policy runs it in the submitting thread
     *     (what {@link ThreadPoolExecutor.CallerRunsPolicy} does), the guard
     *     detects it and hands the real task to a dedicated thread, so the
     *     caller can never end up parked inside the worker's drain loop.
     *     With a blocking policy ({@link CallerWaitsPolicy}) the guarded
     *     wrapper cannot help — the policy blocks inside its own
     *     {@code rejectedExecution} before any task code runs; the window
     *     where that can happen is the narrow race between the checks above
     *     and the executor's admission, and it ends as soon as any rush
     *     worker finishes its drain and polls the queue.</li>
     * </ol>
     *
     * @param task the background task; must not be {@code null}
     * @throws IllegalStateException if the pool is shut down
     */
    void executeUntracked(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");
        if (synchronous)
        {
            task.run();
            return;
        }
        if (threadPoolExecutor.isShutdown())
        {
            // The saturation policies disagree here: CallerRunsPolicy would
            // silently discard the task on a shut-down executor, leaving the
            // caller (the permanent worker of an Actor) believing it started
            // — queued messages would then strand forever with no worker to
            // drain them. Fail loudly instead, with the same IllegalStateException
            // a closed actor raises, so the caller can surface the rejection.
            throw new IllegalStateException("executor is shut down");
        }
        int poolSize = threadPoolExecutor.getPoolSize();
        if (poolSize >= threadPoolExecutor.getMaximumPoolSize()
                && threadPoolExecutor.getActiveCount() >= poolSize)
        {
            // Every pool thread is busy inside a task and the pool cannot
            // grow: no thread is polling the pool's hand-off queue, so the
            // submission would end up running in this thread (CallerRuns) or
            // blocking it (a waiting policy). A permanent worker must do
            // neither — and must not fail either: it is the actor's safety
            // net. Run it on a dedicated thread instead; the pool's threads
            // may all be transient rush workers that exit on their own, but
            // the producer cannot wait for them.
            startDedicatedWorker(task);
            return;
        }
        // Non-blocking hand-off first: if a worker is polling the queue the
        // task is taken here, and the rejection handler — hence the
        // saturation policy — is never reached.
        if (threadPoolExecutor.getQueue().offer(task))
        {
            return;
        }
        // The offer found nobody polling and the pool could not take the task
        // yet; execute() still can (it spawns when below its maximum), but
        // the state may shift again between the checks above and the
        // executor's own admission logic. If the rejection policy then runs
        // the wrapper in THIS thread — exactly what CallerRunsPolicy does —
        // a permanent worker would park the caller inside its drain loop
        // until close. The wrapper detects that case (it can only run in the
        // submitting thread through the rejection path: a pool worker taking
        // the task is never the submitting thread, which is busy inside this
        // very call) and hands the task to a dedicated thread instead.
        Thread caller = Thread.currentThread();
        this.threadPoolExecutor.execute(() ->
        {
            if (Thread.currentThread() == caller)
            {
                startDedicatedWorker(task);
                return;
            }
            task.run();
        });
    }

    /**
     * Runs a permanent worker on a dedicated thread outside the pool. Used
     * only when the pool cannot admit the worker (see
     * {@link #executeUntracked(Runnable)}): the task is untracked anyway, and
     * the thread lives exactly as long as the task — a permanent worker exits
     * when its actor closes, so no thread is leaked by an actor that never
     * starts.
     */
    private void startDedicatedWorker(Runnable task)
    {
        Thread worker = threadFactory.newThread(task);
        worker.start();
    }

    /**
     * Submits {@code task} to the backing pool guaranteeing that it never
     * runs in the calling thread and never blocks it. Admission never
     * consults the saturation policy: a non-blocking {@code offer} on the
     * pool's hand-off queue first (a polling worker takes it), then a
     * guarded submission — if the rejection policy ends up running the
     * wrapper in the submitting thread (what
     * {@link ThreadPoolExecutor.CallerRunsPolicy} does), that is a refusal in
     * disguise: {@code onReject} runs in the caller, the task is
     * <em>not</em> executed, and the caller owns any bookkeeping rollback.
     * <p>
     * The task is tracked exactly like {@link #execute(Runnable)} tracks it:
     * registered before submission, released exactly once whether it runs or
     * is refused.
     *
     * @param task     the task to submit; must not be {@code null}
     * @param onReject runs in the calling thread when the pool refused the
     *                 task; must not throw; must not be {@code null}
     * @return {@code true} when the task was admitted to the pool,
     *         {@code false} when it was refused and {@code onReject} ran
     */
    boolean tryExecuteOffCaller(Runnable task, Runnable onReject)
    {
        Objects.requireNonNull(task, "task must not be null");
        Objects.requireNonNull(onReject, "onReject must not be null");
        if (synchronous || threadPoolExecutor.isShutdown())
        {
            onReject.run();
            return false;
        }
        if (tracker != null)
        {
            tracker.beforeSubmit();
        }
        Runnable trackedTask = tracked(task);
        Thread caller = Thread.currentThread();
        AtomicBoolean settled = new AtomicBoolean();
        Runnable guarded = () ->
        {
            if (Thread.currentThread() == caller)
            {
                // The rejection policy ran the wrapper in the submitting
                // thread: a refusal in disguise (a pool worker taking the
                // task is never the submitting thread, which is busy inside
                // this very call). Undo the tracking and refuse properly.
                if (settled.compareAndSet(false, true))
                {
                    if (tracker != null)
                    {
                        tracker.afterComplete();
                        maybeShutdownWhenIdle();
                    }
                    onReject.run();
                }
                return;
            }
            trackedTask.run();
        };
        if (threadPoolExecutor.getQueue().offer(guarded))
        {
            return true;
        }
        try
        {
            threadPoolExecutor.execute(guarded);
        }
        catch (RuntimeException rex)
        {
            // An abort-style policy, or a shutdown race: refuse the same way.
            if (settled.compareAndSet(false, true))
            {
                if (tracker != null)
                {
                    tracker.afterComplete();
                    maybeShutdownWhenIdle();
                }
                onReject.run();
            }
        }
        return !settled.get();
    }

    /**
     * Rejects execution when the backing pool is shut down. The saturation
     * policies disagree on this case: {@code CallerRunsPolicy} silently
     * discards the task, which would leak a {@link TaskTracker} registration
     * ({@code waitForIdle()} would then hang forever), hang a
     * never-completing future, or strand an accepted message without any
     * worker to drain it.
     *
     * @throws RejectedExecutionException if the pool is shut down
     */
    private void checkPoolAlive()
    {
        if (threadPoolExecutor != null && threadPoolExecutor.isShutdown())
        {
            throw new RejectedExecutionException("executor is shut down");
        }
    }

    /**
     * Runs {@code supplier} on the pool as a tracked task, pre-registering it
     * with the execution phaser <em>before</em> submission so that a task that
     * is still queued is visible to {@link #waitForIdle()} and
     * {@link #isIdle()}. The registration is released exactly once, whether
     * the task runs or the submission is rejected.
     *
     * @param <U>      the result type
     * @param supplier the computation to run; must not be {@code null}
     * @return a future holding the computation result
     */
    private <U> CompletableFuture<U> trackedAsync(Supplier<U> supplier)
    {
        if (!synchronous)
        {
            checkPoolAlive();
        }
        if (tracker == null)
        {
            return CompletableFuture.supplyAsync(supplier, this.threadPoolExecutor);
        }
        tracker.beforeSubmit();
        AtomicBoolean arrived = new AtomicBoolean();
        CompletableFuture<U> future;
        try
        {
            future = CompletableFuture.supplyAsync(() ->
            {
                try
                {
                    return supplier.get();
                }
                finally
                {
                    arrived.set(true);
                    tracker.afterComplete();
                    maybeShutdownWhenIdle();
                }
            }, this.threadPoolExecutor);
        }
        catch (RuntimeException ex)
        {
            if (arrived.compareAndSet(false, true))
            {
                tracker.afterComplete();
                maybeShutdownWhenIdle();
            }
            if (abortSaturation)
            {
                // user-facing blocking admission: wait until a worker accepts
                // the task instead of surfacing the rejection
                return supplyBlocking(supplier);
            }
            throw ex;
        }
        return future.whenComplete((value, ex) ->
        {
            if (arrived.compareAndSet(false, true))
            {
                tracker.afterComplete();
                maybeShutdownWhenIdle();
            }
        });
    }

    /**
     * Blocking-admission variant of {@link CompletableFuture#supplyAsync}:
     * waits until a worker accepts the supplier, like
     * {@link #blockUntilAccepted(Runnable)} — never dropped, never run in the
     * caller, not abortable by interruption; the only rejection is a pool
     * that is shut down, which completes the future exceptionally. Used by
     * {@link #submit(Supplier)} and {@link #forEach} on abort-saturated
     * pools.
     */
    private <U> CompletableFuture<U> supplyBlocking(Supplier<U> supplier)
    {
        if (tracker != null)
        {
            tracker.beforeSubmit();
        }
        CompletableFuture<U> future = new CompletableFuture<>();
        Runnable task = () ->
        {
            try
            {
                future.complete(supplier.get());
            }
            catch (Throwable t)
            {
                future.completeExceptionally(t);
            }
            finally
            {
                if (tracker != null)
                {
                    tracker.afterComplete();
                }
                maybeShutdownWhenIdle();
            }
        };
        while (true)
        {
            if (threadPoolExecutor.isShutdown())
            {
                if (tracker != null)
                {
                    tracker.afterComplete();
                }
                future.completeExceptionally(new RejectedExecutionException("executor is shut down"));
                return future;
            }
            try
            {
                if (threadPoolExecutor.getQueue().offer(task, 50, TimeUnit.MILLISECONDS))
                {
                    return future;
                }
                if (tracker != null)
                {
                    tracker.afterComplete();
                }
            }
            catch (InterruptedException ex)
            {
                // the wait is not abortable: the flag is already cleared by
                // catching the exception, the bookkeeping is undone and the
                // interruption is routed to the hub's onException (R8)
                if (tracker != null)
                {
                    tracker.afterComplete();
                }
                handleException(ex);
            }
        }
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
        if(tracker==null)
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
                tracker.afterComplete();
                maybeShutdownWhenIdle();
            }
        };
    }

    /**
     * Called when the backing pool rejected a submission. Only an abort-style
     * saturation policy rejects by throwing — the caller-runs and
     * caller-waits policies resolve rejections inside the policy itself and
     * never reach this hook. The default re-throws the rejection;
     * {@link ActorHub} re-admits the task by blocking until a worker accepts
     * it (see {@link #blockUntilAccepted(Runnable)}).
     *
     * @param task the rejected task, not yet tracked (the submission's own
     *             bookkeeping has been undone before this hook runs)
     * @return {@code true} when the task was re-admitted and the rejection
     *         must not surface to the caller
     */
    protected boolean onSubmissionRejected(Runnable task)
    {
        return false;
    }

    /**
     * Re-admits {@code task} by waiting until a pool worker accepts it. This
     * is the user-facing saturation semantics of the hub (R6 of
     * {@code doc/actor-rules.txt}): a submitted task is never dropped,
     * never runs in the caller, and the caller waits for admission.
     * <p>
     * The wait alternates a spawn attempt (below the maximum the pool can
     * still create a worker for this task) with a bounded taker-wait on the
     * pool's hand-off queue, so both admission routes are exercised while
     * the shutdown state is re-checked regularly. It is <strong>not
     * abortable by interruption</strong> — an interrupt is consumed and the
     * wait continues (the framework convention of
     * {@code waitForIdle()}/{@code awaitTermination()}) — and the only
     * rejection is a pool that is shut down.
     *
     * @return {@code true} when the task was accepted by a worker,
     *         {@code false} only when the pool is shut down
     */
    protected boolean blockUntilAccepted(Runnable task)
    {
        while (true)
        {
            if (threadPoolExecutor.isShutdown())
            {
                return false;
            }
            if (tracker != null)
            {
                tracker.beforeSubmit();
            }
            try
            {
                threadPoolExecutor.execute(tracked(task));
                return true;
            }
            catch (RejectedExecutionException rex)
            {
                // at the maximum with no taker available: fall through to
                // the bounded taker wait, re-examining the shutdown state
                // and the spawn opportunity on every turn
                if (tracker != null)
                {
                    tracker.afterComplete();
                }
            }
            if (tracker != null)
            {
                tracker.beforeSubmit();
            }
            try
            {
                if (threadPoolExecutor.getQueue().offer(tracked(task), 50, TimeUnit.MILLISECONDS))
                {
                    return true;
                }
                if (tracker != null)
                {
                    tracker.afterComplete();
                }
            }
            catch (InterruptedException ex)
            {
                // the wait is not abortable: the flag is already cleared by
                // catching the exception, the bookkeeping is undone and the
                // interruption is routed to the hub's onException (R8)
                if (tracker != null)
                {
                    tracker.afterComplete();
                }
                handleException(ex);
            }
        }
    }

    /**
     * Returns the number of tasks currently registered with the internal
     * active-task tracker (i.e. executing or about to execute on the pool),
     * or {@code -1} if active-task tracking was not enabled at construction
     * time. Long-lived background workers started with
     * {@link #executeUntracked(Runnable)} (such as the permanent workers of
     * async Actors) are not counted.
     *
     * @return active task count (&ge; 0), or {@code -1} if tracking is disabled
     */
    public int getActiveCount()
    {
        return tracker != null ? tracker.getActive() : -1;
    }

    /**
     * Returns {@code true} if no task is currently active. When tracking is
     * enabled this is an exact, lock-free check based on the {@link TaskTracker}
     * counter; when disabled it relies on
     * {@link ThreadPoolExecutor#getActiveCount()}, which is an approximation.
     * Permanent background workers (see {@link #executeUntracked(Runnable)})
     * do not count as active tasks. Synchronous mode is always idle.
     *
     * @return {@code true} if the pool is idle
     */
    public boolean isIdle()
    {
        if (synchronous)
        {
            return true;
        }
        return tracker != null ? tracker.isIdle() : threadPoolExecutor.getActiveCount() == 0;
    }

    /**
     * Blocks the calling thread until all running tasks have finished. If there
     * are no tasks running or queued, returns immediately.
     *
     * @return this ActorPool, for fluent chaining
     */
    @Override
    public ActorPool waitForIdle()
    {
        if(tracker!=null)
        {
            // The tracker parks until the active count is observed zero under
            // its lock; every transition to zero signals under that same
            // lock, so a wake-up cannot be lost. New tasks may register while
            // waiting; the loop keeps waiting until the count is zero.
            tracker.awaitIdle();
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
        // before any registration: a rejected task must not leak a tracker count
        checkPoolAlive();
        if(tracker==null)
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
        tracker.beforeSubmit();
        try
        {
            this.threadPoolExecutor.execute(tracked(task));
        }
        catch (RuntimeException ex)
        {
            tracker.afterComplete();
            if (abortSaturation && onSubmissionRejected(task))
            {
                return;
            }
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
        return trackedAsync(() ->
        {
            task.run();
            return (Void) null;
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
        return trackedAsync(supplier);
    }

    /**
     * Submits {@code task} to the thread pool and blocks until the worker is
     * guaranteed to be executing {@code task.run()} as its very next
     * instruction, then returns. Unlike {@link #execute}, this guarantees the
     * task is actually running (not merely queued) when the call returns.
     * <p>
     * Not responsive to interruption: if the <em>calling</em> thread is
     * interrupted while waiting on the readiness handshake, the wait is
     * abandoned and the interrupt flag is re-marked, but the task was already
     * accepted by the pool and still runs exactly once in the background.
     *
     * @param task the task to spawn; must not be {@code null}
     * @return this ActorPool, for fluent chaining
     */
    public ActorPool spawn(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");

        if (synchronous)
        {
            task.run();
            return this;
        }

        // before any registration: a rejected spawn must not leak a Phaser
        // party, and the readiness wait below must not hang forever
        checkPoolAlive();

        final CountDownLatch ready = new CountDownLatch(1);

        Runnable startingTask = () ->
        {
            ready.countDown(); // "my next instruction is task.run()"
            task.run();
        };

        if (tracker == null)
        {
            this.threadPoolExecutor.execute(startingTask);
        }
        else
        {
            // Pre-register before handing the task to the pool: a spawned task
            // still waiting for a free thread must be visible to waitForIdle().
            tracker.beforeSubmit();
            try
            {
                this.threadPoolExecutor.execute(tracked(startingTask));
            }
            catch (RuntimeException ex)
            {
                tracker.afterComplete();
                if (abortSaturation && onSubmissionRejected(startingTask))
                {
                    // re-admitted by blocking: fall through to the readiness
                    // wait, the worker counts the starting task down itself
                }
                else
                {
                    throw ex;
                }
            }
        }

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
        iterable.forEach(item -> futures.add(trackedAsync(() ->
        {
            consumer.accept(item);
            return (Void) null;
        })));

        AtomicReference<Throwable> first = new AtomicReference<>();
        futures.forEach(future ->
        {
            try
            {
                future.join();
            }
            catch (CompletionException | CancellationException ex)
            {
                Throwable cause = ex.getCause();
                if(cause == null)
                {
                    cause = ex;
                }
                if (!first.compareAndSet(null, cause))
                {
                    first.get().addSuppressed(cause);
                }
            }
        });
        Throwable firstGet = first.get();
        if (firstGet != null)
        {
            throw (firstGet instanceof CompletionException)
                    ? (CompletionException) first.get()
                    : new CompletionException(first.get());
        }
    }

    // -------------------------------------------------------------------------
    // Pool lifecycle
    // -------------------------------------------------------------------------

    /**
     * Overridable hook for post-termination cleanup.
     * <p>
     * <strong>This hook is never invoked automatically.</strong> The
     * {@code terminated()} callback of {@link ThreadPoolExecutor} belongs to
     * the executor's own class, and {@code ActorPool} delegates to a plain
     * executor instance instead of subclassing it, so nothing wires the
     * automatic notification. Subclasses that need the callback must invoke
     * this method from their own code path ( {@link ProxyActorHub} overrides
     * it to forward to its delegate).
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
            if (tracker == null)
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
     * Called from the completion hooks of {@link #tracked(Runnable)},
     * {@link #trackedAsync(Supplier)}, {@link #spawn(Runnable)} and
     * {@link #forEach}, so it runs on the worker thread that just finished the
     * last tracked task. Idempotent: clears {@link #shutdownWhenIdle} before
     * invoking {@link #shutdown()}.
     */
    private void maybeShutdownWhenIdle()
    {
        if (!shutdownWhenIdle || synchronous)
        {
            return;
        }
        boolean idle = tracker == null
                ? threadPoolExecutor.getActiveCount() == 0
                : tracker.isIdle();
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
        long untilNanos = Saturated.saturatedAdd(System.nanoTime(), TimeUnit.MILLISECONDS.toNanos(millis));
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
     * Returns the maximum allowed number of threads in the pool. This is the
     * configured {@link #getMaxThreads()} ceiling in ordinary operation; the
     * Actor-driven sizing of an {@link ActorHub} may lift the effective
     * maximum above the ceiling when more non-synchronous Actors are
     * registered than the ceiling allows, to host one thread per Actor plus a
     * rush budget of {@code maxThreads} extra threads (see the class-level
     * documentation).
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
     * The hub's interruption consumer, or the default (log the event).
     * Settable at any time; it receives the caught interruptions the
     * framework stops — the ones raised by the mailbox machinery of an
     * actor and the ones from plain pool machinery alike.
     */
    private volatile Consumer<Exception> onException = EXCEPTION;

    static private Consumer<Exception> EXCEPTION = (ex) ->
    {
        LOG.log(Level.WARNING, "interruption consumed", ex);
    };

    /**
     * Sets the hub's interruption consumer, replacing the default (which
     * logs the event). It can be replaced at any time.
     *
     * @param onException the consumer, or {@code null} to restore the
     *                    default logging behaviour
     */
    public ActorPool setOnException(Consumer<Exception> onException)
    {
        this.onException = onException != null ? onException : EXCEPTION;
        return this;
    }

    /**
     * Handles an interruption the framework stopped: hands the caught
     * exception to the hub's interruption consumer. Package-private: the
     * call sites are the mailbox wiring of the actors and the pool's own
     * consuming waits.
     */
    void handleException(Exception ex)
    {
        onException.accept(ex);
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
     * registered non-synchronous Actor and the effective maximum covers both
     * the configured {@code maxThreads} ceiling and all of the Actors'
     * declared thread demand (the maximum never dropping below the effective
     * core). The sole exception to the {@code maxThreads} ceiling: when more
     * non-synchronous Actors are registered than {@code maxThreads}, the pool
     * must still be able to host one thread per Actor, so the effective
     * maximum is lifted to {@code asyncActorCount} plus a rush budget of
     * {@code maxThreads} extra threads: the permanent worker of every Actor
     * is guaranteed a thread (the safety net that always processes
     * messages), while the remaining declared demand is a target honoured on
     * a best-effort basis — when the budget is exhausted, rush starts degrade
     * gracefully to inline drains in the producer thread (the saturation
     * policy), exactly as the N-threads-as-a-ceiling-not-a-guarantee rule
     * allows. The rush threads created above the effective core are
     * non-core, so they are reclaimed after the keep-alive time instead of
     * lingering forever. This helper is {@code final} and uses only
     * base-class state, so it is safe to call from constructors of
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
        int ceiling = asyncActorCount > this.maxThreads
                ? asyncActorCount + this.maxThreads
                : this.maxThreads;
        int core = Math.min(ceiling, Math.max(this.coreThreads, asyncActorCount));
        int max = Math.min(ceiling, Math.max(this.maxThreads, Math.max(threadDemand, core)));
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
