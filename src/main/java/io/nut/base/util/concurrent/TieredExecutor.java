/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Task executor that distributes work across N ordered tiers of slots.
 *
 * <p>Each tier {@code i} owns a pool of {@code limits[i]} slots. A task submitted to tier
 * {@code i} may occupy either one of tier {@code i}'s own slots or an idle slot from any lower
 * tier {@code j < i}. Consequently, at any instant the total number of running tasks of tiers
 * {@code 0..k} never exceeds the sum of the first {@code k+1} limits, for every prefix {@code k}:
 * <pre>
 *   running(0)                        &lt;= limits[0]
 *   running(0) + running(1)           &lt;= limits[0] + limits[1]
 *   ...
 *   running(0) + ... + running(k)     &lt;= limits[0] + ... + limits[k]
 * </pre>
 *
 * <p>In practice a tier-0 task is guaranteed at least {@code limits[0]} slots; a tier-1 task can
 * use a slot that tier-0 tasks are not currently using (as long as the combined usage stays under
 * {@code limits[0]+limits[1]}); and so on up to the highest tier, which may use any slot left over
 * after all lower tiers, up to the grand total.
 *
 * <p>Submitting a task never blocks the caller. If there's no free slot for the task's tier right
 * now, the task is appended to a single internal FIFO queue shared by all tiers. Whenever a slot
 * frees up, the whole queue is scanned front-to-back and every waiting task that now fits is
 * started, regardless of tier. This means older tasks get priority over newer ones, but a tier is
 * never starved just because another tier keeps producing work: a higher-tier task queued before a
 * batch of lower-tier tasks gets its chance to run as soon as there's total capacity for it, even
 * while lower-tier tasks behind it in the queue are still waiting on their own limit.
 *
 * <p>Thread-safe. Not reusable after {@link #shutdown()}.
 */
public class TieredExecutor
{

    private final int[] limits;
    private final int[] prefixLimits;
    private final int[] running;
    private final ExecutorService pool;

    private final ReentrantLock lock = new ReentrantLock();

    private final Deque<QueuedTask<?>> queue = new ArrayDeque<>();

    private volatile boolean shutdown = false;

    /**
     * @param limits max number of concurrently running tasks per tier, from the lowest tier
     *               (index 0) to the highest; must not be null, must not be empty, each value
     *               must be &gt;= 0 and the sum must be &gt; 0
     */
    public TieredExecutor(int... limits)
    {
        if (limits == null)
        {
            throw new NullPointerException("limits must not be null");
        }
        if (limits.length == 0)
        {
            throw new IllegalArgumentException("at least one level is required");
        }
        int total = 0;
        for (int limit : limits)
        {
            if (limit < 0)
            {
                throw new IllegalArgumentException("limits must be >= 0: " + limit);
            }
            total += limit;
        }
        if (total <= 0)
        {
            throw new IllegalArgumentException("the sum of limits must be > 0");
        }
        this.limits = limits.clone();
        this.prefixLimits = new int[limits.length];
        int sum = 0;
        for (int i = 0; i < limits.length; i++)
        {
            sum += limits[i];
            prefixLimits[i] = sum;
        }
        this.running = new int[limits.length];
        // The pool only ever runs tasks that already own a reserved slot (see tryStart), so it
        // never needs more threads than the grand total.
        this.pool = Executors.newFixedThreadPool(total, new TieredThreadFactory());
    }

    // ---------------------------------------------------------------- public API

    public Future<Void> submit(int level, Runnable task)
    {
        return submit(level, toSupplier(task));
    }

    public <T> Future<T> submit(int level, Supplier<T> task)
    {
        checkLevel(level);
        QueuedTask<T> queued = new QueuedTask<>(level, task);
        lock.lock();
        try
        {
            if (shutdown)
            {
                throw new IllegalStateException("TieredExecutor is shut down");
            }
            if (!tryStart(queued))
            {
                queue.addLast(queued);
            }
        }
        finally
        {
            lock.unlock();
        }
        return queued.future;
    }

    /**
     * Number of tasks currently running at the given level.
     *
     * @param level the level index, from 0 (lowest) to {@code levelCount()-1} (highest)
     * @return the number of running tasks of that level
     * @throws ArrayIndexOutOfBoundsException if level is out of range
     */
    public int active(int level)
    {
        checkLevel(level);
        lock.lock();
        try
        {
            return running[level];
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Number of levels of this executor.
     *
     * @return the number of levels
     */
    public int levelCount()
    {
        return limits.length;
    }

    /**
     * Returns a copy of the per-level slot limits.
     *
     * @return the per-level limits
     */
    public int[] getLimits()
    {
        return limits.clone();
    }

    /**
     * Number of tasks waiting for a free slot, across all levels.
     *
     * @return the number of queued tasks
     */
    public int pending()
    {
        lock.lock();
        try
        {
            return queue.size();
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Stops accepting new tasks; the underlying pool shuts down once running tasks finish.
     *
     * <p>Any task still waiting in the internal queue at the time of this call is never started;
     * its {@link Future} is completed exceptionally with a {@link CancellationException} instead.
     * Tasks that are already running are left to finish normally.
     */
    public void shutdown()
    {
        lock.lock();
        try
        {
            shutdown = true;
            QueuedTask<?> queuedTask;
            while ((queuedTask = queue.pollFirst()) != null)
            {
                queuedTask.future.completeExceptionally(
                        new CancellationException("TieredExecutor was shut down while task was queued"));
            }
        }
        finally
        {
            lock.unlock();
        }
        pool.shutdown();
    }

    // ---------------------------------------------------------------- internals

    private void checkLevel(int level)
    {
        if (level < 0 || level >= limits.length)
        {
            throw new ArrayIndexOutOfBoundsException("level out of range: " + level);
        }
    }

    private static Supplier<Void> toSupplier(Runnable task)
    {
        return () -> { task.run(); return null; };
    }

    /** Must hold {@link #lock}. Whether a task of the given level fits within every prefix limit. */
    private boolean fits(int level)
    {
        int used = 0;
        for (int j = 0; j <= level; j++)
        {
            used += running[j];
        }
        if (used + 1 > prefixLimits[level])
        {
            return false;
        }
        for (int j = level + 1; j < running.length; j++)
        {
            used += running[j];
            if (used + 1 > prefixLimits[j])
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Must hold {@link #lock}. If capacity allows, reserves the slot and hands the task to the pool.
     */
    private boolean tryStart(QueuedTask<?> task)
    {
        if (!fits(task.level))
        {
            return false;
        }
        running[task.level]++;
        try
        {
            pool.execute(() -> run(task));
        }
        catch (RejectedExecutionException e)
        {
            // Defensive fallback: should not happen in practice, since shutdown() drains the
            // queue and submit() rejects new tasks while holding the same lock used here, but
            // if it ever does, don't leak the reserved slot or leave the future unresolved.
            running[task.level]--;
            task.future.completeExceptionally(e);
        }
        return true;
    }

    private <T> void run(QueuedTask<T> task)
    {
        try
        {
            T result = task.task.get();
            task.future.complete(result);
        }
        catch (Throwable t)
        {
            task.future.completeExceptionally(t);
        }
        finally
        {
            lock.lock();
            try
            {
                running[task.level]--;
                dispatchQueued();
            }
            finally
            {
                lock.unlock();
            }
        }
    }

    /**
     * Must hold {@link #lock}. Scans the queue once, front-to-back, starting every task that
     * currently fits and leaving the rest in place, in order. A single forward pass is enough:
     * starting a task can only ever consume capacity, never free it, so a task that doesn't fit
     * at some point in the scan won't fit later in the same scan either.
     */
    private void dispatchQueued()
    {
        Iterator<QueuedTask<?>> it = queue.iterator();
        while (it.hasNext())
        {
            if (tryStart(it.next()))
            {
                it.remove();
            }
        }
    }

    private static final class QueuedTask<T>
    {
        final int level;
        final Supplier<T> task;
        final SettableFuture<T> future = new SettableFuture<>();

        QueuedTask(int level, Supplier<T> task)
        {
            this.level = level;
            this.task = task;
        }
    }

    private static final class TieredThreadFactory implements ThreadFactory
    {
        private final AtomicInteger n = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r)
        {
            Thread t = new Thread(r, "tiered-executor-" + n.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * Minimal {@link Future} whose result can be set from another thread once the task finishes.
     * Cancellation of not-yet-started or running tasks is not supported (always returns false).
     */
    private static final class SettableFuture<T> implements Future<T>
    {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile T value;
        private volatile Throwable error;
        private volatile boolean done = false;

        void complete(T value)
        {
            this.value = value;
            this.done = true;
            latch.countDown();
        }

        void completeExceptionally(Throwable t)
        {
            this.error = t;
            this.done = true;
            latch.countDown();
        }

        @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
        @Override public boolean isCancelled() { return false; }
        @Override public boolean isDone() { return done; }

        @Override
        public T get() throws InterruptedException, ExecutionException 
        {
            latch.await();
            return getResultOrThrow();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            if (!latch.await(timeout, unit))
            {
                throw new TimeoutException();
            }
            return getResultOrThrow();
        }

        private T getResultOrThrow() throws ExecutionException
        {
            if (error != null)
            {
                throw new ExecutionException(error);
            }
            return value;
        }
    }
}