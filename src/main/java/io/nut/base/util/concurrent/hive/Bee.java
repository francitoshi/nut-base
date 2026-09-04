/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import io.nut.base.math.Nums;
import io.nut.base.util.concurrent.channel.Channel;
import io.nut.base.util.concurrent.channel.CloseableChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The fundamental building block of the Hive concurrency framework.
 * A {@code Bee<M>} is an asynchronous message-processing stage: it accepts
 * messages via {@link #accept(Object)}, buffers them in a
 * {@link CloseableChannel}, and dispatches them to
 * {@link #receive(Object)} on a worker thread supplied by an attached
 * {@link Hive}.
 * <p>
 * When no {@link Hive} is attached, or when constructed with
 * {@code threads == 0}, {@link #accept(Object)} executes
 * {@link #receive(Object)} synchronously in the calling thread.
 * <p>
 * <strong>Worker model</strong>: when a Hive is attached and {@code threads > 0},
 * the first accepted message starts a single <em>permanent</em> worker that
 * stays alive for the entire lifetime of the Bee, blocking on the internal
 * channel waiting for new messages and processing them as they arrive; it only
 * exits when the Bee is shut down. With {@code threads == 1} that is the only
 * worker. With {@code threads > 1}, additional temporary "rush" workers are
 * started as messages arrive (up to the configured maximum) and return to the
 * pool whenever there is nothing left to drain, so the Bee can process
 * messages in parallel.
 * <p>
 * <strong>Lifecycle</strong>: a Bee starts active. {@link #shutdown()} (or
 * {@link #shutdown(boolean)}) closes the internal channel; messages buffered
 * before the close are still delivered. Once the channel is drained and every
 * worker has returned, {@link #terminate()} is invoked and the Bee is
 * terminated. {@link #awaitTermination(int)} blocks until that point.
 *
 * @param <M> the type of messages this Bee processes
 */
public abstract class Bee<M> implements Consumer<M>
{
    private static final Logger LOG = Logger.getLogger(Bee.class.getName());

    private static final int DEFAULT_QUEUE_SIZE = Short.MAX_VALUE;

    /**
     * How long the permanent worker waits for the next message before giving
     * its thread back to the pool. Bounded so that an idle permanent worker
     * never holds a pool thread forever (which would saturate the pool and
     * deadlock bounded pipelines under the pool's CallerRuns policy); a
     * subsequent {@link #accept(Object)} re-submits it cheaply.
     */
    private static final long PERMANENT_WAIT_MILLIS = 400;

    private final CloseableChannel<M> channel;

    /** Maximum number of concurrently running workers. */
    private final int threads;

    /**
     * When {@code true}, {@link #accept(Object)} invokes {@link #receive(Object)}
     * directly in the calling thread, bypassing the internal channel and worker
     * pool entirely. Set when the constructor receives {@code threads == 0}.
     */
    private final boolean synchronous;

    /**
     * Permits one worker per free concurrent slot. The permanent worker holds
     * one permit for the whole lifetime of the Bee (re-acquiring it each time
     * it goes back to blocking), and temporary "rush" workers acquire a permit
     * (atomically, so no {@link #lock} is needed) before starting and hold it
     * until they have drained everything and are about to return. This provides
     * a reliable bound on the number of concurrently running workers.
     */
    private final Semaphore workerSlots;

    // All lifecycle / scheduling decisions are made under this monitor.
    private final Object lock = new Object();

    /** Messages accepted but not yet received. */
    private final AtomicInteger pending = new AtomicInteger();

    /**
     * Messages currently being processed by a worker, i.e. pulled from the
     * channel but not yet returned from {@link #receive(Object, long)}. Used
     * by {@link #isIdle()} so a Bee whose permanent worker is mid-{@code receive}
     * is never reported idle, even though {@code pending} is already back to
     * zero for that message.
     */
    private final AtomicInteger processing = new AtomicInteger();

    /**
     * Monotonic position assigned to messages as they are pulled from the
     * channel, so subclasses that care about arrival order (e.g. classes that
     * re-assemble ordered output from parallel workers) can reconstruct it:
     * the channel is FIFO, so the {@code k}-th successful pull corresponds to
     * the {@code k}-th accepted message.
     */
    private final AtomicLong sequenceCounter = new AtomicLong();

    /** Workers currently running (submitted to the Hive pool but not yet done). */
    private final AtomicInteger activeWorkers = new AtomicInteger();

    /**
     * Temporary ("rush") workers currently running, for {@code threads > 1}.
     * The permanent worker is not counted here, so a Bee waiting for new
     * messages on its permanent worker can still report itself idle.
     */
    private final AtomicInteger rushWorkers = new AtomicInteger();

    /** {@code true} once the permanent worker has been started (threads >= 1). */
    private boolean permanentWorkerStarted;

    /** {@code true} once the internal channel has been closed. */
    private volatile boolean closed;

    /** {@code true} when {@link #shutdown(boolean)} was asked to close only once idle. */
    private boolean shutdownWhenEmpty;

    /** Messages this Bee has processed (each invocation of {@link #receive}). */
    private final AtomicInteger processedCount = new AtomicInteger();

    /** {@code true} after {@link #terminate()} has run. */
    private boolean terminated;

    private volatile boolean allowLogger = true;
    private final Hive hive;
    private volatile Exception ex;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * @param threads   the maximum number of concurrent worker threads;
     *                  {@code 0} enables synchronous mode (no channel, no
     *                  workers; {@link #accept} calls {@link #receive} directly)
     * @param hive      the Hive thread pool, or {@code null} for synchronous mode
     * @param queueSize buffer capacity of the internal channel; {@code <= 0}
     *                  means unbounded; ignored when {@code threads == 0}
     * @throws IllegalArgumentException if {@code threads < 0} or {@code queueSize < 0}
     */
    public Bee(Hive hive, int threads, int queueSize)
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
        this.synchronous = threads == 0;
        if (this.synchronous)
        {
            this.threads = 0;
            this.workerSlots = null;
            this.channel = null;
        }
        else
        {
            this.threads = threads;
            this.workerSlots = new Semaphore(this.threads);
            this.channel = queueSize > 0 ? Channel.closeableBuffered(queueSize) : Channel.closeableBuffered(Queen.CORES);
            registerHive();
        }
    }

    /**
     * Registers this (non-synchronous) Bee into the {@link Hive} it is attached
     * to, so the Hive can track all its active stages. Bees without an attached
     * Hive (or constructed with {@code threads == 0}) are not registered.
     */
    private void registerHive()
    {
        if (hive != null)
        {
            hive.registerBee(this);
        }
    }

    public Bee(Hive hive)
    {
        this(hive, 1, DEFAULT_QUEUE_SIZE);
    }

    public Bee(int threads, int queueSize)
    {
        this(null, threads, queueSize);
    }

    public Bee()
    {
        this(null, 1, DEFAULT_QUEUE_SIZE);
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

    public Hive getHive()
    {
        return hive;
    }

    /**
     * Returns whether this Bee runs synchronously: either it was constructed
     * with {@code threads == 0}, no Hive is attached, or the attached Hive is
     * itself synchronous (constructed with
     * {@code corePoolSize == 0}).
     *
     * @return {@code true} if {@link #accept} runs {@link #receive} directly
     */
    private boolean isSynchronous()
    {
        if (synchronous || hive == null)
        {
            return true;
        }
        return hive != null && hive.isSynchronous();
    }

    // -------------------------------------------------------------------------
    // Message API
    // -------------------------------------------------------------------------

    /**
     * Called once for each message delivered to this Bee. Must be implemented
     * by subclasses.
     *
     * @param m the message to process
     */
    protected abstract void receive(M m);

    /**
     * Ordered variant of {@link #receive(Object)}: {@code seq} is the
     * acceptance position of the message (1-based, in {@link #accept} order)
     * supplied by this Bee when running with an attached Hive. The default
     * implementation ignores the sequence and delegates to
     * {@link #receive(Object)}; subclasses that must preserve arrival order
     * (such as {@code BatchBee}) can override this to reassemble ordered
     * output even when {@code receive} is invoked concurrently by several
     * workers.
     *
     * @param m   the message to process
     * @param seq the 1-based acceptance position of {@code m}
     */
    protected void receive(M m, long seq)
    {
        receive(m);
    }

    /**
     * Called once after the channel is closed and drained, as the final step
     * of the shutdown sequence. Subclasses may override to release resources.
     */
    protected void terminate()
    {
    }

    /**
     * Called whenever an unhandled exception escapes from {@link #receive(Object)}
     * or from a lifecycle hook. The exception is also stored in
     * {@link #getException()} and, unless suppressed by {@link #dryLogger()},
     * logged at {@code SEVERE} level.
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

            if (isSynchronous())
            {
                countProcessed();
                receive(message);
                return;
            }

            // Enqueue without holding the lock: channel.put may block on a full
            // bounded queue, and holding the lock here would deadlock with the
            // workers that need the same lock to drain the queue.
            pending.incrementAndGet();
            boolean queued = false;
            try
            {
                channel.put(message);
                queued = true;
            }
            finally
            {
                if (!queued)
                {
                    pending.decrementAndGet();
                }
            }

            if (queued)
            {
                // Start the permanent worker on the first message. It holds one
                // worker permit for the whole lifetime of the Bee.
                initPermanentWorker();
                // When threads > 1, additional temporary "rush" workers may be
                // started in parallel. The short critical section guarantees
                // that a worker about to release its slot will not abandon a
                // message we have just enqueued: the release and this tryAcquire
                // both happen under the lock, so a slot freed here is always
                // observed. When threads == 1 the permanent worker already holds
                // the only permit, so no rush worker can start.
                if (threads > 1)
                {
                    synchronized (lock)
                    {
                        if (workerSlots.tryAcquire())
                        {
                            try
                            {
                                startWorker();
                            }
                            catch (Exception ex)
                            {
                                workerSlots.release();
                                throw ex;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            handleException(ex);
        }
    }

    // -------------------------------------------------------------------------
    // Worker
    // -------------------------------------------------------------------------

    /**
     * Submits a worker to the Hive pool. Must be called only after a worker
     * permit has been acquired from {@link #workerSlots}; the worker keeps the
     * permit for its entire {@link #workerLoop()} (or {@link #permanentWorkerLoop()}).
     * If the submission fails (or there is no Hive to submit to), the permit is
     * returned.
     */
    private void startWorker()
    {
        activeWorkers.incrementAndGet();
        rushWorkers.incrementAndGet();
        try
        {
            Executor h = hive;
            if (h != null)
            {
                h.execute(this::workerLoop);
            }
            else
            {
                activeWorkers.decrementAndGet();
                rushWorkers.decrementAndGet();
                workerSlots.release();
            }
        }
        catch (Exception ex)
        {
            activeWorkers.decrementAndGet();
            rushWorkers.decrementAndGet();
            workerSlots.release();
            throw ex;
        }
    }

    /**
     * Starts the single permanent worker for this Bee, if it has not been
     * started yet. The permanent worker is started lazily on the first accepted
     * message, holds one worker permit for the lifetime of the Bee while it is
     * active, and waits up to {@link #PERMANENT_WAIT_MILLIS} for new messages
     * between bursts so it is not destroyed and recreated per message; once the
     * Bee has been idle for that window it yields its thread back to the pool
     * (a later {@link #accept(Object)} re-submits it). Used when
     * {@code threads >= 1}; the {@code threads == 1} case has only this
     * permanent worker, while {@code threads > 1} adds temporary "rush" workers
     * on top of it.
     * <p>
     * The task is submitted to the Hive pool <em>outside</em> the {@link #lock}:
     * {@link Executor#execute} can block (e.g. under the pool's CallerRunsPolicy
     * when the pool is saturated) and the permanent worker waits for messages,
     * so running it while holding the lock would deadlock every other thread that
     * needs the lock (they would wait on the lock never released by the caller
     * that is hijacked to drain).
     */
    private void initPermanentWorker()
    {
        synchronized (lock)
        {
            if (permanentWorkerStarted || closed || terminated)
            {
                return;
            }
            permanentWorkerStarted = true;
            if (!workerSlots.tryAcquire())
            {
                permanentWorkerStarted = false;
                return;
            }
            activeWorkers.incrementAndGet();
        }
        try
        {
            hive.execute(this::permanentWorkerLoop);
        }
        catch (Exception ex)
        {
            synchronized (lock)
            {
                permanentWorkerStarted = false;
                activeWorkers.decrementAndGet();
                workerSlots.release();
            }
            throw ex;
        }
    }

    /**
     * Drains every available message into {@link #receive(Object)}, returns to
     * the pool, and hands the scheduling decision back to {@link #workerDone()}.
     * The worker permit acquired from {@link #workerSlots} is kept for the whole
     * call and released by {@link #workerDone()} once the worker truly returns,
     * so the configured {@code threads} limit is respected even while draining
     * inline.
     */
    private void workerLoop()
    {
        try
        {
            drain();
        }
        finally
        {
            workerDone(false);
        }
    }

    /**
     * The permanent worker loop: keeps a thread alive across bursts of messages,
     * waiting up to {@link #PERMANENT_WAIT_MILLIS} for new work, and processing
     * messages as they arrive. It yields its thread back to the pool once the
     * Bee has been idle for the wait window and only truly exits on shutdown.
     */
    private void permanentWorkerLoop()
    {
        try
        {
            drainBlocking();
        }
        finally
        {
            workerDone(true);
        }
    }

    /**
     * Records that this Bee is about to process one message: bumps the per-Bee
     * counter and, unless it aliases the per-Bee counter (a Bee without an
     * attached {@link Hive}, where {@link #globalProcessedCount} falls back to
     * this Bee's own counter), the Hive-wide counter shared with peer Bees.
     */
    private void countProcessed()
    {
        processedCount.incrementAndGet();
        if (hive != null)
        {
            hive.processedCount().incrementAndGet();
        }
    }

    /**
     * Processes every message currently buffered in the channel, one at a time.
     * The channel is closed (drain-only) during shutdown, so an explicit call
     * here guarantees that buffered messages are received even if no worker
     * could be submitted to the pool.
     */
    private void drain()
    {
        M m;
        while ((m = channel.get(0, TimeUnit.MILLISECONDS)) != null)
        {
            pending.decrementAndGet();
            long seq = sequenceCounter.incrementAndGet();
            processing.incrementAndGet();
            countProcessed();
            try
            {
                receive(m, seq);
            }
            catch (Exception ex)
            {
                handleException(ex);
            }
            finally
            {
                processing.decrementAndGet();
            }
        }
    }

    /**
     * Waits up to {@link #PERMANENT_WAIT_MILLIS} for a message, processing every
     * message that arrives, and returns when the channel has been quiet (or
     * closed) for that long. Used by the permanent worker: it keeps a thread
     * alive across bursts of messages (avoiding per-message thread churn) but
     * yields the thread once the Bee has been idle for the wait window, so it
     * never blocks a pool thread indefinitely.
     */
    private void drainBlocking()
    {
        M m;
        while ((m = channel.get(PERMANENT_WAIT_MILLIS, TimeUnit.MILLISECONDS)) != null)
        {
            pending.decrementAndGet();
            long seq = sequenceCounter.incrementAndGet();
            processing.incrementAndGet();
            countProcessed();
            try
            {
                receive(m, seq);
            }
            catch (Exception ex)
            {
                handleException(ex);
            }
            finally
            {
                processing.decrementAndGet();
            }
        }
    }

    /**
     * Runs when a worker returns: drains anything that arrived while it was
     * processing, closes or terminates the Bee when required, and finally hands
     * back its worker permit. Must be race-free: the permit is released under
     * {@link #lock}, matching the {@code tryAcquire} in {@link #accept(Object)},
     * so a worker that frees a slot is never missed by a producer that has just
     * enqueued a message.
     * <p>
     * The drain runs deliberately <em>outside</em> the lock: {@link #receive(Object)}
     * may forward messages to other Bees and block on their full channels, and
     * holding this Bee's lock across such a block would park every other worker
     * and producer of this Bee on that lock, stalling the whole Hive once the
     * pool is exhausted.
     *
     * @param permanent {@code true} for the Bee's single permanent worker,
     *                  {@code false} for temporary "rush" workers
     */
    private void workerDone(boolean permanent)
    {
        while (true)
        {
            // Drains every message that is pending while still holding this
            // worker's permit, without the lock. A producer that enqueues while
            // the drain is running cannot acquire the permit (see accept), so it
            // does not start a worker; the loop below only exits once pending is
            // zero under the lock, guaranteeing those messages are not abandoned.
            while (pending.get() > 0)
            {
                drain();
            }

            boolean close = false;
            synchronized (lock)
            {
                if (pending.get() > 0)
                {
                    // A message arrived between the drain and the lock: keep the
                    // permit and drain again.
                    continue;
                }
                // Count the worker out only now, when it truly stops running. A
                // worker that is still looping (draining, forwarding to other
                // bees) must keep isIdle() from reporting this bee idle:
                // otherwise shutdown(true) could close the bee and terminate it
                // while a worker was still delivering downstream, silently
                // dropping those messages. Terminating also races in-flight
                // forwards being cut off at the next stage.
                boolean last = activeWorkers.decrementAndGet() == 0;
                if (closed)
                {
                    if (permanent)
                    {
                        rushWorkers.set(0);
                    }
                    if (last)
                    {
                        doTerminate();
                    }
                    workerSlots.release();
                    lock.notifyAll();
                    return;
                }
                if (shutdownWhenEmpty)
                {
                    close = true;
                }
                else
                {
                    // Both the permanent and the rush worker give their thread
                    // back to the pool once the Bee has no pending work. The
                    // permanent worker has already waited up to
                    // PERMANENT_WAIT_MILLIS in drainBlocking, so it is "sticky"
                    // across bursts but never holds a pool thread indefinitely:
                    // a later accept() re-submits it cheaply. Resetting
                    // permanentWorkerStarted lets accept() restart it.
                    if (permanent)
                    {
                        permanentWorkerStarted = false;
                    }
                    else
                    {
                        rushWorkers.decrementAndGet();
                    }
                    workerSlots.release();
                    lock.notifyAll();
                    return;
                }
            }

            // Closes on behalf of an empty-when-shutdown request. Runs outside
            // the lock because closeNow may drain, and drain's receive may block
            // forwarding to a full downstream Bee.
            if (close)
            {
                closeNow();
                if (permanent)
                {
                    rushWorkers.set(0);
                    permanentWorkerStarted = false;
                }
                else
                {
                    rushWorkers.decrementAndGet();
                }
                synchronized (lock)
                {
                    workerSlots.release();
                    lock.notifyAll();
                }
                return;
            }
        }
    }

    private void closeNow()
    {
        closed = true;
        if (synchronous)
        {
            doTerminate();
            return;
        }
        channel.close();
        if (activeWorkers.get() == 0)
        {
            drain();
            doTerminate();
        }
    }

    private void doTerminate()
    {
        // Now reachable without holding the lock (workerDone closes outside
        // the lock), so the once-only guarantee is enforced here.
        synchronized (lock)
        {
            if (terminated)
            {
                return;
            }
            terminated = true;
            unregisterFromHive();
            try
            {
                terminate();
            }
            catch (Exception ex)
            {
                handleException(ex);
            }
            lock.notifyAll();
        }
    }

    /**
     * Removes this Bee from the {@link Hive} instance list it was registered
     * in, if any.
     */
    private void unregisterFromHive()
    {
        if (hive instanceof Hive)
        {
            ((Hive) hive).unregisterBee(this);
        }
    }

    private void handleException(Exception ex)
    {
        this.ex = ex;
        if (allowLogger)
        {
            LOG.log(Level.SEVERE, "Bee", ex);
        }
        exception(ex);
    }

    // -------------------------------------------------------------------------
    // Idle / lifecycle
    // -------------------------------------------------------------------------

    /**
     * Returns the number of messages accepted but not yet received, plus the
     * number of active worker threads.
     *
     * @return approximate pending work count
     */
    public int getPendingCount()
    {
        return pending.get() + activeWorkers.get();
    }

    /**
     * Returns {@code true} if no messages are pending or being processed. 
     */
    public boolean isIdle()
    {
        return pending.get() <= 0 && processing.get() == 0;
    }

    /**
     * Blocks until this Bee is idle (no pending messages, no temporary workers
     * active).
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
                    lock.wait();
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
     * Closes the internal channel, causing workers to finish and exit. No new
     * messages can be accepted after this call.
     *
     * @return this Bee, for fluent chaining
     */
    public Bee<M> shutdown()
    {
        return shutdown(false);
    }

    /**
     * Initiates shutdown. If {@code onlyWhenEmpty}, the channel is closed only
     * once all pending messages have been processed.
     *
     * @param onlyWhenEmpty if {@code true}, defers close until idle
     * @return this Bee, for fluent chaining
     */
    public Bee<M> shutdown(boolean onlyWhenEmpty)
    {
        synchronized (lock)
        {
            if (!closed && !terminated)
            {
                if (onlyWhenEmpty)
                {
                    if (isIdle())
                    {
                        closeNow();
                    }
                    else
                    {
                        shutdownWhenEmpty = true;
                    }
                }
                else
                {
                    closeNow();
                }
            }
            lock.notifyAll();
        }
        return this;
    }

    /**
     * Shuts down this Bee and blocks until it has terminated. Equivalent to
     * {@link #close(boolean) close(false)}.
     *
     * @return this Bee, for fluent chaining
     */
    public Bee<M> close()
    {
        return close(false);
    }

    /**
     * Shuts down this Bee and blocks until it has terminated. Provides a
     * {@code boolean} variant of {@link #close()} for symmetry with
     * {@link #shutdown(boolean)}.
     *
     * @param onlyWhenEmpty if {@code true}, defers close until idle (see
     *                      {@link #shutdown(boolean)}); if {@code false}, closes
     *                      immediately
     * @return this Bee, for fluent chaining
     */
    public Bee<M> close(boolean onlyWhenEmpty)
    {
        shutdown(onlyWhenEmpty);
        awaitTermination(Integer.MAX_VALUE);
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
        long deadline = Nums.saturatedAdd(System.nanoTime(), TimeUnit.MILLISECONDS.toNanos(millis));
        return awaitTerminationUntilNanos(deadline);
    }

    /**
     * Blocks until the Bee is terminated or the absolute deadline (in
     * nanoseconds) is reached.
     *
     * @param untilNanos the absolute deadline
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
                    handleException(ex);
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
