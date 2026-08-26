/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import io.nut.base.util.concurrent.channel.CloseableUnlimitedChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The fundamental building block of the Hive concurrency framework.
 * A {@code Bee<M>} is an asynchronous message-processing stage: it accepts
 * messages via {@link #accept(Object)}, queues them internally via a
 * {@link CloseableUnlimitedChannel}, and dispatches them to
 * {@link #receive(Object)} on worker threads supplied by an attached
 * {@link Hive}.
 * <p>
 * When no {@link Hive} is attached, {@link #accept(Object)} executes
 * {@link #receive(Object)} synchronously in the calling thread.
 * <p>
 * <strong>Lifecycle</strong>: a Bee starts in the {@code RUNNING} state.
 * Calling {@link #close()} closes the internal channel, which causes all
 * active workers to finish processing and then exit. After the channel is
 * closed and drained, {@link #terminate()} is called. Callers can block on
 * completion with {@link #awaitTermination(int)}.
 * <p>
 * <strong>Concurrency</strong>: at most one worker is submitted per
 * {@link #accept(Object)} call when no worker is active. Each worker loops
 * on the internal channel, processing messages until the channel is closed
 * and empty.
 *
 * @param <M> the type of messages this Bee processes
 */
public abstract class Bee<M> implements Consumer<M>
{
    private static final Logger LOG = Logger.getLogger(Bee.class.getName());

    private static final int DEFAULT_QUEUE_SIZE = Short.MAX_VALUE;

    private final CloseableUnlimitedChannel<M> channel;
    private final Object lock = new Object();
    private final AtomicInteger activeWorkers = new AtomicInteger();
    private final AtomicInteger pendingCount = new AtomicInteger();

    private volatile boolean closed;
    private volatile boolean shutdownWhenEmpty;
    private volatile boolean terminated;
    private volatile boolean allowLogger = true;
    private volatile Executor hive;
    private volatile Exception ex;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Bee(int threads, Hive hive, int queueSize)
    {
        if (threads < 0)
        {
            throw new IllegalArgumentException("threads < 0");
        }
        if (queueSize < 0)
        {
            throw new IllegalArgumentException("queueSize < 0");
        }
        this.hive = hive;
        this.channel = new CloseableUnlimitedChannel<>();
    }

    public Bee(int threads, Hive hive)
    {
        this(threads, hive, DEFAULT_QUEUE_SIZE);
    }

    public Bee(Hive hive)
    {
        this(0, hive, DEFAULT_QUEUE_SIZE);
    }

    public Bee(int threads)
    {
        this(threads, null, DEFAULT_QUEUE_SIZE);
    }

    public Bee()
    {
        this(0, null, DEFAULT_QUEUE_SIZE);
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    public Bee<M> dryLogger()
    {
        this.allowLogger = false;
        return this;
    }

    public Exception getException()
    {
        return ex;
    }

    public void setHive(Hive hive)
    {
        this.hive = hive;
    }

    // -------------------------------------------------------------------------
    // Message API
    // -------------------------------------------------------------------------

    /**
     * Called by the worker thread for each message dequeued from the internal
     * channel. Subclasses must implement the actual message-processing logic.
     *
     * @param m the message to process
     */
    protected abstract void receive(M m);

    /**
     * Called once after the channel is closed and fully drained, as the final
     * step of the shutdown sequence. Subclasses may override to release
     * resources.
     */
    protected void terminate()
    {
    }

    /**
     * Called whenever an unhandled exception escapes from {@link #receive(Object)}
     * or from a lifecycle hook.
     *
     * @param ex the exception that was thrown
     */
    protected void exception(Exception ex)
    {
    }

    /**
     * Sends a message to this Bee for processing.
     *
     * @param message the message to deliver
     * @throws IllegalStateException if this Bee has been closed
     */
    @Override
    public void accept(M message)
    {
        try
        {
            if (closed)
            {
                throw new IllegalStateException("closed");
            }

            if (hive != null)
            {
                pendingCount.incrementAndGet();
                channel.put(message);
                if (activeWorkers.get() == 0)
                {
                    submitWorker();
                }
            }
            else
            {
                receive(message);
            }
        }
        catch (Exception ex)
        {
            this.ex = ex;
            if (allowLogger)
            {
                LOG.log(Level.SEVERE, "Bee.accept()", ex);
            }
            exception(ex);
        }
    }

    // -------------------------------------------------------------------------
    // Worker
    // -------------------------------------------------------------------------

    private void submitWorker()
    {
        activeWorkers.incrementAndGet();
        hive.execute(this::workerLoop);
    }

    private void workerLoop()
    {
        try
        {
            M m;
            while ((m = channel.get(0, java.util.concurrent.TimeUnit.SECONDS)) != null)
            {
                pendingCount.decrementAndGet();
                try
                {
                    receive(m);
                }
                catch (Exception ex)
                {
                    this.ex = ex;
                    if (allowLogger)
                    {
                        LOG.log(Level.SEVERE, "Bee.receive()", ex);
                    }
                    exception(ex);
                }
            }
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            this.ex = ex;
            if (allowLogger)
            {
                LOG.log(Level.SEVERE, "Bee.worker interrupted", ex);
            }
            exception(ex);
        }
        finally
        {
            activeWorkers.decrementAndGet();
            synchronized (lock)
            {
                if (shutdownWhenEmpty && !closed && pendingCount.get() <= 0 && activeWorkers.get() == 0)
                {
                    closed = true;
                    channel.close();
                    doTerminate();
                }
                else if (closed && activeWorkers.get() == 0)
                {
                    doTerminate();
                }
                else if (!closed && !channel.isClosed() && pendingCount.get() > 0 && activeWorkers.get() == 0)
                {
                    try
                    {
                        submitWorker();
                    }
                    catch (Exception ignored) {}
                }
                lock.notifyAll();
            }
        }
    }

    private void doTerminate()
    {
        synchronized (lock)
        {
            if (terminated)
            {
                return;
            }
            terminated = true;
            try
            {
                terminate();
            }
            catch (Exception ex)
            {
                this.ex = ex;
                if (allowLogger)
                {
                    LOG.log(Level.SEVERE, "Bee.terminate()", ex);
                }
                exception(ex);
            }
            finally
            {
                lock.notifyAll();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Idle / lifecycle
    // -------------------------------------------------------------------------

    /**
     * Returns the number of messages currently in the internal channel waiting
     * to be processed, plus the number of active worker threads.
     *
     * @return approximate pending work count
     */
    public int getPendingCount()
    {
        return pendingCount.get() + activeWorkers.get();
    }

    /**
     * Returns {@code true} if no messages are pending and no workers are
     * active.
     */
    public boolean isIdle()
    {
        return pendingCount.get() <= 0 && activeWorkers.get() == 0;
    }

    /**
     * Blocks until this Bee is idle (no pending messages, no active workers).
     *
     * @return this Bee, for fluent chaining
     */
    public Bee<M> waitForIdle()
    {
        synchronized (lock)
        {
            try
            {
                while (!isIdle())
                {
                    lock.wait(100);
                }
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
        }
        return this;
    }

    /**
     * Closes the internal channel, causing active workers to finish and exit.
     * No new messages can be accepted after this call.
     *
     * @return this Bee, for fluent chaining
     */
    public Bee<M> shutdown()
    {
        return shutdown(false);
    }

    /**
     * Initiates shutdown. If {@code onlyWhenEmpty}, the channel is closed
     * only once all pending messages have been processed.
     *
     * @param onlyWhenEmpty if {@code true}, defers close until idle
     * @return this Bee, for fluent chaining
     */
    public Bee<M> shutdown(boolean onlyWhenEmpty)
    {
        synchronized (lock)
        {
            if (closed)
            {
                return this;
            }

            if (onlyWhenEmpty)
            {
                if (isIdle())
                {
                    closed = true;
                    channel.close();
                    if (activeWorkers.get() == 0)
                    {
                        Executor h = hive;
                        if (h != null)
                        {
                            h.execute(this::doTerminate);
                        }
                        else
                        {
                            doTerminate();
                        }
                    }
                }
                else
                {
                    shutdownWhenEmpty = true;
                }
            }
            else
            {
                closed = true;
                channel.close();
                if (activeWorkers.get() == 0)
                {
                    Executor h = hive;
                    if (h != null)
                    {
                        h.execute(this::doTerminate);
                    }
                    else
                    {
                        doTerminate();
                    }
                }
            }
        }
        return this;
    }

    /**
     * Returns whether {@link #shutdown()} has been called.
     */
    public boolean isShutdown()
    {
        return closed;
    }

    /**
     * Returns whether this Bee has fully terminated.
     */
    public boolean isTerminated()
    {
        return terminated;
    }

    /**
     * Blocks until the Bee is terminated or the timeout elapses.
     *
     * @param millis maximum time to wait
     * @return {@code true} if terminated within the timeout
     */
    public boolean awaitTermination(int millis)
    {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(millis);
        return awaitTerminationUntilNanos(deadline);
    }

    /**
     * Blocks until the Bee is terminated or the deadline elapses.
     *
     * @param untilNanos the absolute deadline in nanoseconds
     * @return {@code true} if terminated within the deadline
     */
    public boolean awaitTerminationUntilNanos(long untilNanos)
    {
        synchronized (lock)
        {
            while (!terminated)
            {
                long remaining = untilNanos - System.nanoTime();
                if (remaining <= 0)
                {
                    return false;
                }
                try
                {
                    lock.wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                    this.ex = ex;
                    if (allowLogger)
                    {
                        LOG.log(Level.SEVERE, "Bee.awaitTermination()", ex);
                    }
                    exception(ex);
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Returns a collection of downstream target consumers linked to this stage.
     * The default implementation returns an empty collection.
     *
     * @return the collection of downstream target consumers
     */
    public Collection<Consumer<?>> getLinkedTargets()
    {
        return Collections.emptyList();
    }

    /**
     * Subscribes this Bee to {@code topic} on the attached {@link Hive}.
     *
     * @param topic the topic name
     * @return this Bee, for fluent chaining
     * @throws IllegalStateException if no Hive has been attached
     */
    @SuppressWarnings("unchecked")
    public Bee<M> sub(String topic)
    {
        if (!(this.hive instanceof Hive))
        {
            throw new IllegalStateException("No Hive attached.");
        }
        ((Hive) this.hive).sub(topic, this);
        return this;
    }
}
