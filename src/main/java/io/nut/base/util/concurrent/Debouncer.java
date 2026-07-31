/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread-safe utility class that delays task execution until a specified
 * amount of time has elapsed since the last submission.
 *
 * <p>Supports both leading (execution on first call) and trailing (execution
 * of last call after delay) debouncing modes, customizable schedulers/executors,
 * and a lifecycle listener.</p>
 *
 * @author franci
 * @since 1.8
 */
public final class Debouncer implements AutoCloseable
{
    private static final Logger LOGGER = Logger.getLogger(Debouncer.class.getName());

    private final Object lock = new Object();
    private final long delayNanos;
    private final ScheduledExecutorService scheduler;
    private final Executor executor;
    private final boolean leading;
    private final boolean trailing;
    private final Listener listener;
    private final boolean isDefaultScheduler;

    private ScheduledFuture<?> scheduledFuture;
    private DebouncedTask pendingTask;
    private boolean cooldownActive;
    private boolean shutdown;
    // Monotonically increasing token. Every time a new schedule "supersedes"
    // a previous one, generation is bumped. Scheduled callbacks capture the
    // generation that was current when *they* were scheduled and check it
    // again once they acquire the lock: if it no longer matches, another
    // call already superseded them (even if Future.cancel(false) failed
    // because the callback had already started running), so they become a
    // no-op instead of firing a stale/premature execution.
    private long generation;

    private Debouncer(Builder builder)
    {
        this.delayNanos = builder.unit.toNanos(builder.delay);
        this.leading = builder.leading;
        this.trailing = builder.trailing;
        this.listener = builder.listener;

        if (builder.scheduler != null)
        {
            this.scheduler = builder.scheduler;
            this.isDefaultScheduler = false;
        }
        else
        {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("debouncer-scheduler-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            });
            this.isDefaultScheduler = true;
        }

        this.executor = builder.executor != null ? builder.executor : this.scheduler;
    }

    //--------------------------------------------------------------------------
    // Factory methods
    //--------------------------------------------------------------------------

    /**
     * Creates a new Debouncer with standard configuration: trailing execution,
     * no leading execution, and default daemon scheduler/executor.
     *
     * @param delay the debounce delay
     * @param unit the time unit of the delay
     * @return a configured Debouncer; never {@code null}
     * @throws IllegalArgumentException if delay is less than or equal to 0
     * @throws NullPointerException if unit is null
     */
    public static Debouncer of(long delay, TimeUnit unit)
    {
        return builder().delay(delay, unit).build();
    }

    /**
     * Creates a new Builder to configure a Debouncer.
     *
     * @return a new Builder; never {@code null}
     */
    public static Builder builder()
    {
        return new Builder();
    }

    //--------------------------------------------------------------------------
    // Execution
    //--------------------------------------------------------------------------

    /**
     * Submits a Runnable task to be debounced.
     *
     * @param task the task to run; must not be {@code null}
     * @throws IllegalStateException if the debouncer has been shut down
     * @throws NullPointerException if task is null
     */
    public void submit(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");
        submitInternal(new DebouncedTask(task));
    }

    /**
     * Submits a Callable task to be debounced. The return value is discarded.
     *
     * @param task the task to run; must not be {@code null}
     * @param <T> the result type
     * @throws IllegalStateException if the debouncer has been shut down
     * @throws NullPointerException if task is null
     */
    public <T> void submit(Callable<T> task)
    {
        Objects.requireNonNull(task, "task must not be null");
        submitInternal(new DebouncedTask(task));
    }

    private void submitInternal(DebouncedTask task)
    {
        boolean fireScheduled = false;
        boolean fireCancelled = false;

        synchronized (lock)
        {
            if (shutdown)
            {
                throw new IllegalStateException("Debouncer has been shutdown");
            }

            if (leading && !cooldownActive)
            {
                cooldownActive = true;
                long gen = ++generation;
                scheduledFuture = scheduler.schedule(() -> onCooldownExpired(gen), delayNanos, TimeUnit.NANOSECONDS);
                safeExecute(task);
            }
            else
            {
                if (scheduledFuture != null)
                {
                    if (pendingTask != null)
                    {
                        fireCancelled = true;
                    }
                    // generation is bumped below regardless of what cancel()
                    // returns, so even if this future already fired and is
                    // waiting on the lock, its callback will see a stale
                    // generation and do nothing.
                    scheduledFuture.cancel(false);
                }

                if (trailing || !leading)
                {
                    pendingTask = task;
                    fireScheduled = true;
                    long gen = ++generation;
                    if (leading)
                    {
                        scheduledFuture = scheduler.schedule(() -> onCooldownExpired(gen), delayNanos, TimeUnit.NANOSECONDS);
                    }
                    else
                    {
                        scheduledFuture = scheduler.schedule(() -> executePending(gen), delayNanos, TimeUnit.NANOSECONDS);
                    }
                }
                else
                {
                    long gen = ++generation;
                    scheduledFuture = scheduler.schedule(() -> onCooldownExpired(gen), delayNanos, TimeUnit.NANOSECONDS);
                }
            }
        }

        // Listener callbacks are invoked outside the lock so a slow/blocking
        // listener can't stall submit()/cancel()/flush() on other threads
        // (this also matches how onExecuted() is already invoked outside
        // the lock in DebouncedTask.run()).
        if (fireCancelled)
        {
            notifyCancelled();
        }
        if (fireScheduled)
        {
            notifyScheduled();
        }
    }

    /**
     * Cancels the pending execution, if any.
     *
     * @return true if a pending task was cancelled.
     */
    public boolean cancel()
    {
        boolean cancelled;
        boolean fireCancelled = false;

        synchronized (lock)
        {
            cancelled = false;
            // Invalidate any in-flight scheduled callback, even one that has
            // already started running and is waiting on this lock.
            generation++;
            if (scheduledFuture != null)
            {
                scheduledFuture.cancel(false);
                scheduledFuture = null;
                cancelled = true;
            }
            if (pendingTask != null)
            {
                pendingTask = null;
                cancelled = true;
                fireCancelled = true;
            }
            cooldownActive = false;
        }

        if (fireCancelled)
        {
            notifyCancelled();
        }
        return cancelled;
    }

    /**
     * Executes the pending task immediately.
     *
     * @return true if a task was executed.
     */
    public boolean flush()
    {
        synchronized (lock)
        {
            if (pendingTask != null)
            {
                DebouncedTask task = pendingTask;
                pendingTask = null;
                if (scheduledFuture != null)
                {
                    scheduledFuture.cancel(false);
                }
                cooldownActive = true;
                long gen = ++generation;
                scheduledFuture = scheduler.schedule(() -> onCooldownExpired(gen), delayNanos, TimeUnit.NANOSECONDS);
                safeExecute(task);
                return true;
            }
            return false;
        }
    }

    /**
     * Returns whether a task is currently pending.
     *
     * @return {@code true} if a task is pending execution, otherwise {@code false}
     */
    public boolean isPending()
    {
        synchronized (lock)
        {
            return pendingTask != null;
        }
    }

    /**
     * Returns the configured debounce delay.
     *
     * @param unit the time unit of the returned delay
     * @return the configured delay in the specified unit
     */
    public long getDelay(TimeUnit unit)
    {
        Objects.requireNonNull(unit, "unit must not be null");
        return unit.convert(delayNanos, TimeUnit.NANOSECONDS);
    }

    private void executePending(long gen)
    {
        DebouncedTask task;
        synchronized (lock)
        {
            if (gen != generation)
            {
                // Superseded by a later submit()/cancel()/flush() while this
                // callback was already in flight: do nothing, the current
                // state belongs to whoever bumped the generation.
                return;
            }
            task = pendingTask;
            pendingTask = null;
            scheduledFuture = null;
        }
        if (task != null)
        {
            safeExecute(task);
        }
    }

    private void onCooldownExpired(long gen)
    {
        DebouncedTask task = null;
        synchronized (lock)
        {
            if (gen != generation)
            {
                // Stale invocation, see executePending(long) above.
                return;
            }
            if (pendingTask != null)
            {
                task = pendingTask;
                pendingTask = null;
                long newGen = ++generation;
                scheduledFuture = scheduler.schedule(() -> onCooldownExpired(newGen), delayNanos, TimeUnit.NANOSECONDS);
            }
            else
            {
                cooldownActive = false;
                scheduledFuture = null;
            }
        }
        if (task != null)
        {
            safeExecute(task);
        }
    }

    /**
     * Submits a task to the configured executor, catching and logging any
     * exception the executor itself throws (e.g. a {@link
     * java.util.concurrent.RejectedExecutionException} if a user-supplied
     * executor was shut down externally) instead of letting it escape from
     * internal scheduler callbacks.
     */
    private void safeExecute(DebouncedTask task)
    {
        try
        {
            executor.execute(task);
        }
        catch (RuntimeException e)
        {
            LOGGER.log(Level.WARNING, "Error submitting debounced task to executor", e);
            if (listener != null)
            {
                try
                {
                    listener.onExecuted(e);
                }
                catch (RuntimeException ex)
                {
                    LOGGER.log(Level.WARNING, "Error in listener onExecuted", ex);
                }
            }
        }
    }

    private void notifyScheduled()
    {
        if (listener != null)
        {
            try
            {
                listener.onScheduled();
            }
            catch (RuntimeException e)
            {
                LOGGER.log(Level.WARNING, "Error in listener onScheduled", e);
            }
        }
    }

    private void notifyCancelled()
    {
        if (listener != null)
        {
            try
            {
                listener.onCancelled();
            }
            catch (RuntimeException e)
            {
                LOGGER.log(Level.WARNING, "Error in listener onCancelled", e);
            }
        }
    }

    //--------------------------------------------------------------------------
    // Lifecycle
    //--------------------------------------------------------------------------

    /**
     * Shuts down the debouncer, cancelling any pending execution, and releases
     * resources. If the scheduler was created internally, it is also shut down.
     */
    public void shutdown()
    {
        synchronized (lock)
        {
            if (shutdown)
            {
                return;
            }
            shutdown = true;
            cancel();
            if (isDefaultScheduler)
            {
                scheduler.shutdown();
            }
        }
    }

    @Override
    public void close()
    {
        shutdown();
    }

    //--------------------------------------------------------------------------
    // Builder
    //--------------------------------------------------------------------------

    /**
     * Builder class for configuring and constructing {@link Debouncer} instances.
     */
    public static final class Builder
    {
        private long delay = -1;
        private TimeUnit unit;
        private ScheduledExecutorService scheduler;
        private Executor executor;
        private boolean leading = false;
        private boolean trailing = true;
        private Listener listener;

        private Builder()
        {
        }

        /**
         * Sets the debounce delay.
         *
         * @param delay the delay duration; must be > 0
         * @param unit the time unit; must not be null
         * @return this Builder instance
         * @throws IllegalArgumentException if delay is less than or equal to 0
         * @throws NullPointerException if unit is null
         */
        public Builder delay(long delay, TimeUnit unit)
        {
            if (delay <= 0)
            {
                throw new IllegalArgumentException("delay must be > 0: " + delay);
            }
            this.delay = delay;
            this.unit = Objects.requireNonNull(unit, "unit must not be null");
            return this;
        }

        /**
         * Sets the ScheduledExecutorService to schedule delays.
         *
         * @param scheduler the scheduler; must not be null
         * @return this Builder instance
         * @throws NullPointerException if scheduler is null
         */
        public Builder scheduler(ScheduledExecutorService scheduler)
        {
            this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
            return this;
        }

        /**
         * Sets the Executor to run the debounced tasks.
         *
         * @param executor the executor; must not be null
         * @return this Builder instance
         * @throws NullPointerException if executor is null
         */
        public Builder executor(Executor executor)
        {
            this.executor = Objects.requireNonNull(executor, "executor must not be null");
            return this;
        }

        /**
         * Configures leading execution.
         *
         * @param leading {@code true} to execute immediately on the first trigger
         * @return this Builder instance
         */
        public Builder leading(boolean leading)
        {
            this.leading = leading;
            return this;
        }

        /**
         * Configures trailing execution.
         *
         * @param trailing {@code true} to execute the last call after the delay
         * @return this Builder instance
         */
        public Builder trailing(boolean trailing)
        {
            this.trailing = trailing;
            return this;
        }

        /**
         * Registers a Listener to observe scheduling and execution lifecycle.
         *
         * @param listener the listener; must not be null
         * @return this Builder instance
         * @throws NullPointerException if listener is null
         */
        public Builder listener(Listener listener)
        {
            this.listener = Objects.requireNonNull(listener, "listener must not be null");
            return this;
        }

        /**
         * Constructs a new {@link Debouncer} using the configured settings.
         *
         * @return a configured Debouncer; never {@code null}
         * @throws IllegalStateException if delay/unit is not configured
         * @throws IllegalArgumentException if both leading and trailing are false
         */
        public Debouncer build()
        {
            if (delay <= 0 || unit == null)
            {
                throw new IllegalStateException("Delay and unit must be configured");
            }
            if (!leading && !trailing)
            {
                throw new IllegalArgumentException("At least one of leading or trailing must be true");
            }
            return new Debouncer(this);
        }
    }

    //--------------------------------------------------------------------------
    // Listener
    //--------------------------------------------------------------------------

    /**
     * Interface to monitor scheduling, cancellation, and execution of debounced tasks.
     */
    public interface Listener
    {
        /**
         * Called when a task is scheduled to be run.
         */
        void onScheduled();

        /**
         * Called when a scheduled task is cancelled before execution.
         */
        void onCancelled();

        /**
         * Called when a task finishes execution.
         *
         * @param failure the exception thrown by the task, or {@code null} if it completed successfully
         */
        void onExecuted(Throwable failure);
    }

    private final class DebouncedTask implements Runnable
    {
        private final Runnable runnableTask;
        private final Callable<?> callableTask;

        DebouncedTask(Runnable runnableTask)
        {
            this.runnableTask = runnableTask;
            this.callableTask = null;
        }

        DebouncedTask(Callable<?> callableTask)
        {
            this.runnableTask = null;
            this.callableTask = callableTask;
        }

        @Override
        public void run()
        {
            Throwable failure = null;
            try
            {
                if (runnableTask != null)
                {
                    runnableTask.run();
                }
                else if (callableTask != null)
                {
                    callableTask.call();
                }
            }
            catch (Throwable t)
            {
                failure = t;
            }
            finally
            {
                if (listener != null)
                {
                    try
                    {
                        listener.onExecuted(failure);
                    }
                    catch (RuntimeException e)
                    {
                        LOGGER.log(Level.WARNING, "Error in listener onExecuted", e);
                    }
                }
            }
        }
    }
}
