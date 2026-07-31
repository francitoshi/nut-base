/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * A thread-safe, immutable utility class for executing tasks with retry logic.
 * Supports configurable maximum attempts, backoff strategies, custom filters
 * for exceptions and results, listeners, and custom sleepers for testing.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Retry retry = Retry.builder()
 *     .maxAttempts(3)
 *     .backoff(Retry.Backoffs.exponential(100, 2.0, 5000))
 *     .retryOn(IOException.class)
 *     .listener((attempt, error, delay) -> System.out.println("Failed attempt " + attempt))
 *     .build();
 *
 * String result = retry.call(() -> fetchDataFromRemoteServer());
 * }</pre>
 *
 * @author franci
 * @since 1.8
 */
public final class Retry
{
    private static final Sleeper DEFAULT_SLEEPER = Thread::sleep;
    private static final Retry DEFAULTS = new Builder().build();

    private final int maxAttempts;
    private final Backoff backoff;
    private final List<Class<? extends Throwable>> retryOnClasses;
    private final List<Predicate<Throwable>> retryIfPredicates;
    private final List<Predicate<Object>> retryIfResultPredicates;
    private final List<RetryListener> listeners;
    private final Sleeper sleeper;

    private Retry(Builder builder)
    {
        this.maxAttempts = builder.maxAttempts;
        this.backoff = builder.backoff;
        this.retryOnClasses = Collections.unmodifiableList(new ArrayList<>(builder.retryOnClasses));
        this.retryIfPredicates = Collections.unmodifiableList(new ArrayList<>(builder.retryIfPredicates));
        this.retryIfResultPredicates = Collections.unmodifiableList(new ArrayList<>(builder.retryIfResultPredicates));
        this.listeners = Collections.unmodifiableList(new ArrayList<>(builder.listeners));
        this.sleeper = builder.sleeper;
    }

    /**
     * Creates a new builder for configuring a custom {@code Retry} instance.
     *
     * @return a new builder; never {@code null}
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Returns a {@code Retry} instance configured with default settings.
     * By default, it allows up to 3 attempts, has no backoff (no delay between retries),
     * retries on any throwables (except interruptions), and uses the system sleeper.
     *
     * @return a default {@code Retry} instance; never {@code null}
     */
    public static Retry defaults()
    {
        return DEFAULTS;
    }

    /**
     * Executes the given callable, retrying if it fails according to the configured rules.
     *
     * @param callable the task to execute; must not be {@code null}
     * @param <T> the type of the result
     * @return the result of a successful task execution
     * @throws Exception if the maximum number of attempts is reached, or an unhandled exception occurs
     */
    public <T> T call(Callable<T> callable) throws Exception
    {
        Objects.requireNonNull(callable, "callable must not be null");
        int attempt = 0;
        while (true)
        {
            attempt++;
            try
            {
                T result = callable.call();
                if (attempt < maxAttempts && shouldRetryResult(result))
                {
                    long delay = backoff.delayMillis(attempt);
                    notifyListeners(attempt, null, delay);
                    if (delay > 0)
                    {
                        sleeper.sleep(delay);
                    }
                    continue;
                }
                return result;
            }
            catch (Throwable t)
            {
                if (attempt < maxAttempts && shouldRetryException(t))
                {
                    long delay = backoff.delayMillis(attempt);
                    notifyListeners(attempt, t, delay);
                    if (delay > 0)
                    {
                        sleeper.sleep(delay);
                    }
                    continue;
                }
                if (t instanceof Exception)
                {
                    throw (Exception) t;
                }
                if (t instanceof Error)
                {
                    throw (Error) t;
                }
                throw new RuntimeException(t);
            }
        }
    }

    /**
     * Executes the given runnable, retrying if it fails according to the configured rules.
     *
     * @param runnable the task to execute; must not be {@code null}
     * @throws Exception if the maximum number of attempts is reached, or an unhandled exception occurs
     */
    public void run(CheckedRunnable runnable) throws Exception
    {
        Objects.requireNonNull(runnable, "runnable must not be null");
        int attempt = 0;
        while (true)
        {
            attempt++;
            try
            {
                runnable.run();
                return;
            }
            catch (Throwable t)
            {
                if (attempt < maxAttempts && shouldRetryException(t))
                {
                    long delay = backoff.delayMillis(attempt);
                    notifyListeners(attempt, t, delay);
                    if (delay > 0)
                    {
                        sleeper.sleep(delay);
                    }
                    continue;
                }
                if (t instanceof Exception)
                {
                    throw (Exception) t;
                }
                if (t instanceof Error)
                {
                    throw (Error) t;
                }
                throw new RuntimeException(t);
            }
        }
    }

    private boolean shouldRetryException(Throwable error)
    {
        if (isInterrupt(error))
        {
            return false;
        }
        if (retryOnClasses.isEmpty() && retryIfPredicates.isEmpty())
        {
            return true;
        }
        for (Class<? extends Throwable> clazz : retryOnClasses)
        {
            if (clazz.isInstance(error))
            {
                return true;
            }
        }
        for (Predicate<Throwable> predicate : retryIfPredicates)
        {
            if (predicate.test(error))
            {
                return true;
            }
        }
        return false;
    }

    private boolean shouldRetryResult(Object result)
    {
        for (Predicate<Object> predicate : retryIfResultPredicates)
        {
            if (predicate.test(result))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isInterrupt(Throwable t)
    {
        return t instanceof InterruptedException || t instanceof InterruptedIOException;
    }

    private void notifyListeners(int attempt, Throwable error, long delayMillis)
    {
        for (RetryListener listener : listeners)
        {
            listener.onFailure(attempt, error, delayMillis);
        }
    }

    /**
     * A builder for configuring and creating instances of {@link Retry}.
     */
    public static final class Builder
    {
        private int maxAttempts = 3;
        private Backoff backoff = Backoffs.none();
        private final List<Class<? extends Throwable>> retryOnClasses = new ArrayList<>();
        private final List<Predicate<Throwable>> retryIfPredicates = new ArrayList<>();
        private final List<Predicate<Object>> retryIfResultPredicates = new ArrayList<>();
        private final List<RetryListener> listeners = new ArrayList<>();
        private Sleeper sleeper = DEFAULT_SLEEPER;

        private Builder()
        {
        }

        /**
         * Sets the maximum number of attempts (including the initial execution).
         *
         * @param maxAttempts the maximum number of attempts; must be at least 1
         * @return this builder instance; never {@code null}
         * @throws IllegalArgumentException if {@code maxAttempts} is less than 1
         */
        public Builder maxAttempts(int maxAttempts)
        {
            if (maxAttempts < 1)
            {
                throw new IllegalArgumentException("maxAttempts must be >= 1: " + maxAttempts);
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Sets the backoff strategy to determine the delay between attempts.
         *
         * @param backoff the backoff strategy; must not be {@code null}
         * @return this builder instance; never {@code null}
         * @throws NullPointerException if {@code backoff} is {@code null}
         */
        public Builder backoff(Backoff backoff)
        {
            this.backoff = Objects.requireNonNull(backoff, "backoff must not be null");
            return this;
        }

        /**
         * Registers a class of exception that should trigger a retry.
         * By default, if no exception classes or predicates are registered,
         * all exceptions except interruption exceptions will trigger a retry.
         *
         * @param exceptionClass the exception class; must not be {@code null}
         * @return this builder instance; never {@code null}
         * @throws NullPointerException if {@code exceptionClass} is {@code null}
         */
        public Builder retryOn(Class<? extends Throwable> exceptionClass)
        {
            Objects.requireNonNull(exceptionClass, "exceptionClass must not be null");
            this.retryOnClasses.add(exceptionClass);
            return this;
        }

        /**
         * Registers a predicate to evaluate if a thrown exception should trigger a retry.
         *
         * @param predicate the predicate to evaluate the exception; must not be {@code null}
         * @return this builder instance; never {@code null}
         * @throws NullPointerException if {@code predicate} is {@code null}
         */
        public Builder retryIf(Predicate<Throwable> predicate)
        {
            Objects.requireNonNull(predicate, "predicate must not be null");
            this.retryIfPredicates.add(predicate);
            return this;
        }

        /**
         * Registers a predicate to evaluate if the returned result should trigger a retry.
         * This applies only to {@link #call(Callable)} executions.
         *
         * @param predicate the predicate to evaluate the result; must not be {@code null}
         * @return this builder instance; never {@code null}
         * @throws NullPointerException if {@code predicate} is {@code null}
         */
        public Builder retryIfResult(Predicate<Object> predicate)
        {
            Objects.requireNonNull(predicate, "predicate must not be null");
            this.retryIfResultPredicates.add(predicate);
            return this;
        }

        /**
         * Registers a listener to be notified when an attempt fails before a retry delay.
         *
         * @param listener the listener; must not be {@code null}
         * @return this builder instance; never {@code null}
         * @throws NullPointerException if {@code listener} is {@code null}
         */
        public Builder listener(RetryListener listener)
        {
            Objects.requireNonNull(listener, "listener must not be null");
            this.listeners.add(listener);
            return this;
        }

        /**
         * Sets a custom sleeper implementation to handle delays.
         * Useful for mock testing to avoid actual sleeping.
         *
         * @param sleeper the sleeper implementation; must not be {@code null}
         * @return this builder instance; never {@code null}
         * @throws NullPointerException if {@code sleeper} is {@code null}
         */
        public Builder sleeper(Sleeper sleeper)
        {
            this.sleeper = Objects.requireNonNull(sleeper, "sleeper must not be null");
            return this;
        }

        /**
         * Builds a new immutable, thread-safe {@link Retry} instance.
         *
         * @return a configured {@link Retry} instance; never {@code null}
         */
        public Retry build()
        {
            return new Retry(this);
        }
    }

    /**
     * A runnable task that can throw checked exceptions.
     */
    @FunctionalInterface
    public interface CheckedRunnable
    {
        /**
         * Runs the task.
         *
         * @throws Exception if an error occurs during execution
         */
        void run() throws Exception;
    }

    /**
     * Strategy to determine the delay in milliseconds between retry attempts.
     */
    @FunctionalInterface
    public interface Backoff
    {
        /**
         * Calculates the delay in milliseconds for the given attempt.
         *
         * @param attempt the 1-based attempt number that failed (1 represents the first failure)
         * @return the delay in milliseconds; must be non-negative
         */
        long delayMillis(int attempt);
    }

    /**
     * Listener interface to observe attempt failures.
     */
    @FunctionalInterface
    public interface RetryListener
    {
        /**
         * Called when an attempt fails, before sleeping and retrying.
         *
         * @param attempt the attempt number that failed (1-based)
         * @param error the exception that caused the failure, or {@code null} if retrying due to result predicate
         * @param delayMillis the delay in milliseconds before the next attempt
         */
        void onFailure(int attempt, Throwable error, long delayMillis);
    }

    /**
     * An abstraction for pausing execution. Typically wraps {@link Thread#sleep(long)}.
     */
    @FunctionalInterface
    public interface Sleeper
    {
        /**
         * Pauses execution for the specified milliseconds.
         *
         * @param millis the milliseconds to sleep
         * @throws InterruptedException if the sleep is interrupted
         */
        void sleep(long millis) throws InterruptedException;
    }

    /**
     * Factory class for standard, built-in backoff strategies.
     */
    public static final class Backoffs
    {
        private Backoffs()
        {
        }

        /**
         * Returns a backoff strategy that never waits between attempts.
         *
         * @return a zero delay backoff strategy; never {@code null}
         */
        public static Backoff none()
        {
            return attempt -> 0L;
        }

        /**
         * Returns a backoff strategy with a fixed delay between attempts.
         *
         * @param delay the delay in milliseconds; must be non-negative
         * @return a fixed delay backoff strategy; never {@code null}
         * @throws IllegalArgumentException if {@code delay} is negative
         */
        public static Backoff fixed(long delay)
        {
            if (delay < 0)
            {
                throw new IllegalArgumentException("delay must be >= 0: " + delay);
            }
            return attempt -> delay;
        }

        /**
         * Returns an exponential backoff strategy with default values:
         * 100 ms initial delay, multiplier factor of 2.0, and 30,000 ms maximum delay.
         *
         * @return an exponential backoff strategy; never {@code null}
         */
        public static Backoff exponential()
        {
            return exponential(100L, 2.0, 30000L);
        }

        /**
         * Returns an exponential backoff strategy with a default multiplier factor of 2.0
         * and 30,000 ms maximum delay.
         *
         * @param initial the initial delay in milliseconds; must be greater than 0
         * @return an exponential backoff strategy; never {@code null}
         * @throws IllegalArgumentException if {@code initial} is <= 0
         */
        public static Backoff exponential(long initial)
        {
            return exponential(initial, 2.0, 30000L);
        }

        /**
         * Returns a customized exponential backoff strategy.
         *
         * @param initial the initial delay in milliseconds; must be greater than 0
         * @param factor the multiplication factor; must be at least 1.0
         * @param maximum the maximum delay in milliseconds; must be at least {@code initial}
         * @return an exponential backoff strategy; never {@code null}
         * @throws IllegalArgumentException if arguments do not satisfy constraints
         */
        public static Backoff exponential(long initial, double factor, long maximum)
        {
            if (initial <= 0)
            {
                throw new IllegalArgumentException("initial delay must be > 0: " + initial);
            }
            if (factor < 1.0)
            {
                throw new IllegalArgumentException("factor must be >= 1.0: " + factor);
            }
            if (maximum < initial)
            {
                throw new IllegalArgumentException("maximum delay must be >= initial delay: " + maximum);
            }
            return attempt -> 
            {
                if (attempt <= 1)
                {
                    return initial;
                }
                double delay = initial * Math.pow(factor, attempt - 1);
                if (delay >= maximum || delay < 0 || Double.isInfinite(delay) || Double.isNaN(delay))
                {
                    return maximum;
                }
                return Math.min(maximum, (long) delay);
            };
        }

        /**
         * Wraps the given backoff strategy to add randomized jitter with a default percentage of 0.5 (50%).
         *
         * @param backoff the delegate backoff strategy; must not be {@code null}
         * @return a jittered backoff strategy; never {@code null}
         * @throws NullPointerException if {@code backoff} is {@code null}
         */
        public static Backoff jitter(Backoff backoff)
        {
            return jitter(backoff, 0.5);
        }

        /**
         * Wraps the given backoff strategy to add randomized jitter.
         * For a base delay D and percentage P, the delay will be selected randomly
         * from the range [D * (1 - P), D * (1 + P)].
         *
         * @param backoff the delegate backoff strategy; must not be {@code null}
         * @param percentage the jitter range percentage; must be between 0.0 and 1.0 inclusive
         * @return a jittered backoff strategy; never {@code null}
         * @throws NullPointerException if {@code backoff} is {@code null}
         * @throws IllegalArgumentException if {@code percentage} is not within [0.0, 1.0]
         */
        public static Backoff jitter(Backoff backoff, double percentage)
        {
            Objects.requireNonNull(backoff, "backoff must not be null");
            if (percentage < 0.0 || percentage > 1.0)
            {
                throw new IllegalArgumentException("percentage must be between 0.0 and 1.0: " + percentage);
            }
            return attempt -> {
                long delay = backoff.delayMillis(attempt);
                if (delay <= 0)
                {
                    return 0L;
                }
                double jitterFactor = 1.0 - percentage + (ThreadLocalRandom.current().nextDouble() * 2.0 * percentage);
                return Math.max(0L, Math.round(delay * jitterFactor));
            };
        }
    }
}
