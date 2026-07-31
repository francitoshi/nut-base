/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * A thread-safe utility class that enforces a rate limit (cooldown period) between
 * executions of tasks.
 *
 * <p>Unlike debouncing, throttling ensures that a task is executed immediately (leading)
 * and subsequent calls within the interval window are ignored (throttled).</p>
 *
 * <p>Note: the throttle window is reserved <em>before</em> the task actually runs. If the
 * task (or, when using an {@link Executor}, the call to {@link Executor#execute(Runnable)})
 * throws an exception, the throttler's internal state is not rolled back; the interval is
 * still considered "consumed" as if the task had completed successfully.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Throttler throttler = Throttler.of(1, TimeUnit.SECONDS);
 *
 * if (throttler.submit(() -> System.out.println("Executed"))) {
 *     // Run successfully
 * }
 * }</pre>
 *
 * @author franci
 * @since 1.8
 */
public final class Throttler
{
    private final Object lock = new Object();
    private final long intervalNanos;
    private final Executor executor;

    private long lastExecutionNanos;
    private boolean hasExecuted;

    private Throttler(Builder builder)
    {
        this.intervalNanos = builder.unit.toNanos(builder.interval);
        this.executor = builder.executor;
    }

    /**
     * Creates a new Throttler with the specified interval delay.
     * By default, it executes tasks synchronously on the caller's thread.
     *
     * @param interval the rate limit interval duration
     * @param unit the time unit of the interval
     * @return a configured Throttler; never {@code null}
     * @throws IllegalArgumentException if interval is less than or equal to 0
     * @throws NullPointerException if unit is null
     */
    public static Throttler of(long interval, TimeUnit unit)
    {
        return builder().interval(interval, unit).build();
    }

    /**
     * Creates a new Builder to configure a Throttler.
     *
     * @return a new Builder; never {@code null}
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Attempts to execute the given task. If the throttler is ready (the interval
     * since the last execution has elapsed), the task is run and this method returns
     * {@code true}. Otherwise, the task is dropped and this method returns {@code false}.
     *
     * <p>The internal state (last-execution timestamp) is updated under lock, but the
     * task itself is invoked <em>outside</em> the lock, so it cannot block concurrent
     * calls to {@link #isReady()}, {@link #getRemainingDelay(TimeUnit)}, {@link #reset()},
     * or other invocations of this method.</p>
     *
     * @param task the task to execute; must not be {@code null}
     * @return {@code true} if the task was executed, {@code false} if it was throttled
     * @throws NullPointerException if task is null
     */
    public boolean submit(Runnable task)
    {
        Objects.requireNonNull(task, "task must not be null");

        synchronized (lock)
        {
            if (!isReady())
            {
                return false;
            }
            lastExecutionNanos = System.nanoTime();
            hasExecuted = true;
        }

        // Run the task outside the lock so a slow task (or a blocking Executor.execute,
        // e.g. a bounded queue with CallerRunsPolicy) does not stall other threads that
        // are only checking state (isReady/getRemainingDelay) or calling reset().
        if (executor != null)
        {
            executor.execute(task);
        }
        else
        {
            task.run();
        }
        return true;
    }

    /**
     * Returns whether the throttler is ready to execute a task.
     *
     * @return {@code true} if the interval delay has elapsed since the last execution,
     *         or if no task has been executed yet; otherwise {@code false}
     */
    public boolean isReady()
    {
        synchronized (lock)
        {
            if (!hasExecuted)
            {
                return true;
            }
            return System.nanoTime() - lastExecutionNanos >= intervalNanos;
        }
    }

    /**
     * Returns the remaining delay until the throttler is ready to execute another task.
     *
     * @param unit the time unit of the returned delay
     * @return the remaining delay in the specified unit, or 0 if it is ready immediately
     * @throws NullPointerException if unit is null
     */
    public long getRemainingDelay(TimeUnit unit)
    {
        synchronized (lock)
        {
            Objects.requireNonNull(unit, "unit must not be null");
            if (!hasExecuted)
            {
                return 0L;
            }
            long elapsed = System.nanoTime() - lastExecutionNanos;
            long remaining = intervalNanos - elapsed;
            return remaining <= 0 ? 0L : unit.convert(remaining, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Resets the throttler state, making it ready immediately.
     */
    public void reset()
    {
        synchronized (lock)
        {
            hasExecuted = false;
            lastExecutionNanos = 0L;
        }
    }

    /**
     * Returns the configured throttle interval delay.
     *
     * @param unit the time unit of the returned interval
     * @return the configured interval in the specified unit
     * @throws NullPointerException if unit is null
     */
    public long getInterval(TimeUnit unit)
    {
        Objects.requireNonNull(unit, "unit must not be null");
        return unit.convert(intervalNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Builder class for configuring and constructing {@link Throttler} instances.
     */
    public static final class Builder
    {
        private long interval = -1;
        private TimeUnit unit;
        private Executor executor;

        private Builder()
        {
        }

        /**
         * Sets the throttle interval delay.
         *
         * @param interval the interval duration; must be > 0
         * @param unit the time unit; must not be null
         * @return this Builder instance
         * @throws IllegalArgumentException if interval is less than or equal to 0
         * @throws NullPointerException if unit is null
         */
        public Builder interval(long interval, TimeUnit unit)
        {
            if (interval <= 0)
            {
                throw new IllegalArgumentException("interval must be > 0: " + interval);
            }
            this.interval = interval;
            this.unit = Objects.requireNonNull(unit, "unit must not be null");
            return this;
        }

        /**
         * Sets the Executor to run tasks. If not specified, tasks will be run
         * synchronously on the caller's thread during {@link #submit(Runnable)}.
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
         * Constructs a new {@link Throttler} using the configured settings.
         *
         * @return a configured Throttler; never {@code null}
         * @throws IllegalStateException if interval or unit is not configured
         */
        public Throttler build()
        {
            if (interval <= 0 || unit == null)
            {
                throw new IllegalStateException("Interval and unit must be configured");
            }
            return new Throttler(this);
        }
    }
}
