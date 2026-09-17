/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import io.nut.base.math.Nums;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A pipeline stage that accumulates individual messages into a pending batch
 * and forwards the entire batch as a single {@code List<T>} message.
 * <p>
 * It is a {@link LinkableActor} whose worker is a single-threaded actor (like
 * {@link SingleActor}): a single thread owns the read loop, and the read loop
 * is the one inherited from the async actor machinery, so it reuses the hub's
 * thread-demand semantics. The thread stays parked between batches, keeping a
 * ready reader on the channel at all times; a timed read only bounds the time
 * a partial batch is held before {@code maxWaitMillis} of inactivity expires.
 * No external scheduler or reordering buffer is needed, because a single
 * thread consumes messages in order.
 * <p>
 * The batch is forwarded when the accumulated count reaches {@code maxSize}
 * or when {@code maxWaitMillis} of inactivity expires, whichever comes first.
 * An explicit {@link #flush()} forces the current partial batch out.
 * <p>
 * The owning hub is never {@code null}: when constructed without a hub, this
 * stage creates (and later closes) its own single-thread hub, so the
 * single worker always has an executor to run on, whether the stage is
 * attached to a hub or used stand-alone.
 *
 * @param <T> the type of individual messages accumulated into batches
 */
public class BatchActor<T> extends LinkableActor<T, List<T>>
{
    private final int maxSize;
    private final long maxWaitMillis;
    private final long maxWaitNanos;
    private final List<T> pending = new ArrayList<>();
    private long batchStartNanos;
    private final ActorHub ownHub;
    private final boolean syncMode;

    /**
     * @param actorHub     the hub the worker runs on, or {@code null} to have
     *                     this stage create its own single-thread hub
     * @param threads      the requested thread count; {@code 0} selects the
     *                     synchronous flavor, any value {@code >= 1} selects a
     *                     single worker thread (unless the hub is synchronous,
     *                     in which case the stage stays synchronous)
     * @param queueSize    the internal queue capacity (0 = rendezvous)
     * @param maxSize      the batch size that triggers an immediate flush
     * @param maxWaitMillis the maximum time a partial batch is held before it
     *                     is flushed, in milliseconds; {@code 0} disables the
     *                     time-based flush
     */
    public BatchActor(ActorHub actorHub, int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        super(null);
        if (maxSize <= 0)
        {
            throw new IllegalArgumentException("maxSize must be > 0");
        }
        if (maxWaitMillis < 0)
        {
            throw new IllegalArgumentException("maxWaitMillis must not be negative");
        }
        this.maxSize = maxSize;
        this.maxWaitMillis = maxWaitMillis;
        this.maxWaitNanos = TimeUnit.MILLISECONDS.toNanos(maxWaitMillis);

        final boolean synchronous = threads == 0;
        this.ownHub = synchronous ? null : (actorHub != null ? null : new ActorHub(1));
        final ActorHub hubToUse = this.ownHub != null ? this.ownHub : actorHub;
        this.syncMode = synchronous || hubToUse == null || hubToUse.isSynchronous();

        Runnable onTerminate = () ->
        {
            if (BatchActor.this.ownHub != null)
            {
                BatchActor.this.ownHub.close(true);
            }
        };

        if (syncMode)
        {
            this.inner = Actor.create(hubToUse, 0, 0,
                    m -> BatchActor.this.accumulate(m), onTerminate, ex -> BatchActor.this.handleException(ex));
        }
        else
        {
            this.inner = new BatchDrainer(hubToUse, queueSize);
        }
    }

    public BatchActor(ActorHub actorHub, int maxSize, long maxWaitMillis)
    {
        this(actorHub, 1, 0, maxSize, maxWaitMillis);
    }

    public BatchActor(int maxSize, long maxWaitMillis)
    {
        this((ActorHub) null, 1, 0, maxSize, maxWaitMillis);
    }

    public int getMaxSize()
    {
        return maxSize;
    }

    public long getMaxWaitMillis()
    {
        return maxWaitMillis;
    }

    /**
     * Returns the number of messages currently accumulated in the pending
     * batch. Exposed solely for tests to inspect the stage in-flight.
     *
     * @return the current pending batch size
     */
    protected int pending()
    {
        synchronized (pending)
        {
            return pending.size();
        }
    }

    /**
     * Forces the current partial batch out immediately, regardless of how
     * many messages it holds and of {@code maxWaitMillis}. Safe to call from
     * any thread; the pending items are moved to the downstream stage.
     */
    public void flush()
    {
        if (!syncMode)
        {
            ((BatchDrainer) inner).flush();
        }
        else
        {
            List<T> batch;
            synchronized (pending)
            {
                batch = pending.isEmpty() ? null : drainLocked();
            }
            if (batch != null)
            {
                forward(batch);
            }
        }
    }

    // -----------------------------------------------------------------
    // Shared accumulation state
    // -----------------------------------------------------------------

    /**
     * Copies and clears the pending batch; the caller must hold
     * {@code synchronized (pending)}.
     */
    private List<T> drainLocked()
    {
        List<T> batch = new ArrayList<>(pending);
        pending.clear();
        return batch;
    }

    /**
     * Synchronous-flavor accumulation: every {@link SynchronousActor#accept}
     * runs in the calling thread, so there is no read loop and no timer; the
     * batch is forwarded when it reaches {@code maxSize}, or when a new
     * message arrives after {@code maxWaitMillis} have passed since the batch
     * was started.
     */
    private void accumulate(T m)
    {
        List<T> stale = null;
        List<T> full = null;
        synchronized (pending)
        {
            if (pending.isEmpty())
            {
                batchStartNanos = System.nanoTime();
            }
            else if (maxWaitNanos > 0 && System.nanoTime() - batchStartNanos >= maxWaitNanos)
            {
                stale = drainLocked();
                batchStartNanos = System.nanoTime();
            }
            pending.add(m);
            if (pending.size() >= maxSize)
            {
                full = drainLocked();
            }
        }
        if (stale != null)
        {
            forward(stale);
        }
        if (full != null)
        {
            forward(full);
        }
    }

    // -----------------------------------------------------------------
    // Single-threaded batch drainer
    // -----------------------------------------------------------------

    private final class BatchDrainer extends AsyncActor<T>
    {
        BatchDrainer(ActorHub hub, int queueSize)
        {
            super(hub, 1, queueSize);
        }

        /**
         * The regular batch loop reads through {@link #push} (which manages the
         * counters itself), but base drain paths — e.g. {@link #closeNow()}
         * draining leftovers — still route through the standard receive, so it
         * accumulates into the pending batch too.
         */
        @Override
        protected void receive(T m)
        {
            accumulate(m);
        }

        @Override
        protected void terminate()
        {
            if (BatchActor.this.ownHub != null)
            {
                BatchActor.this.ownHub.close(true);
            }
        }

        @Override
        protected void exception(Exception ex)
        {
            BatchActor.this.handleException(ex);
        }

        /**
         * The permanent worker reads messages and accumulates them until the
         * batch reaches {@code maxSize} items or {@code maxWaitMillis} have
         * elapsed since the first item of the batch, then flushes it and does
         * the same for the next batch. It blocks on the channel between batches,
         * so a ready reader is always parked; only the close of the channel ends
         * the loop.
         */
        @Override
        protected void permanentLoop()
        {
            try
            {
                drainBatches();
            }
            finally
            {
                workerDone(true);
            }
        }

        private void drainBatches()
        {
            while (true)
            {
                T m = channel.get();
                if (m == null)
                {
                    if (channel.isClosed())
                    {
                        flush();
                    }
                    return;
                }
                int batchSize = push(m);
                if (maxWaitNanos > 0)
                {
                    long deadline;
                    synchronized (pending)
                    {
                        deadline = Nums.saturatedAdd(batchStartNanos, maxWaitNanos);
                    }
                    while (batchSize < maxSize)
                    {
                        long remaining = deadline - System.nanoTime();
                        if (remaining <= 0)
                        {
                            break;
                        }
                        m = channel.get(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining) + 1), TimeUnit.MILLISECONDS);
                        if (m == null)
                        {
                            break;
                        }
                        batchSize = push(m);
                    }
                    flush();
                }
                else if (batchSize >= maxSize)
                {
                    flush();
                }
            }
        }

        private int push(T m)
        {
            int size;
            synchronized (pending)
            {
                if (pending.isEmpty())
                {
                    batchStartNanos = System.nanoTime();
                }
                pending.add(m);
                size = pending.size();
            }
            counters.addFirstToSecond(1);
            countProcessed();
            return size;
        }

        private void flush()
        {
            List<T> batch;
            synchronized (pending)
            {
                if (pending.isEmpty())
                {
                    return;
                }
                batch = drainLocked();
            }
            forward(batch);
            for (int i = 0; i < batch.size(); i++)
            {
                counters.decrementSecond();
            }
            if (counters.isZero())
            {
                synchronized (lock)
                {
                    lock.notifyAll();
                    if (shutdownWhenEmpty)
                    {
                        closeNow();
                    }
                }
            }
        }
    }

}