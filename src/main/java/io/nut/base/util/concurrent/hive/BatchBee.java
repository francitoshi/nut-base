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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A BatchBee is a Bee&lt;T&gt; that accumulates received messages into a
 * pending batch and forwards the batch, as a single {@code List<T>}
 * message, to the next stage as soon as either:
 * <ul>
 * <li>the batch reaches {@code maxSize} elements, or</li>
 * <li>{@code maxWaitMillis} milliseconds have elapsed since the last
 * flush, if {@code maxWaitMillis > 0},</li>
 * </ul>
 * whichever happens first. This is useful to amortize the cost of an
 * expensive downstream operation (a DB write, a network call...) over
 * several messages instead of doing it once per message.
 * <p>
 * The time-based flush, when enabled, is driven by a single daemon
 * thread internal to this BatchBee and is independent of the owning
 * Hive's thread pool. {@link #flush()} can also be called manually at
 * any time, e.g. to force out a partial batch before shutting down.
 *
 * @param <T> the type of individual messages accumulated into batches
 */
public class BatchBee<T> extends Bee<T>
{
    private final int maxSize;
    private final Object batchLock = new Object();
    private List<T> batch;
    protected volatile Sendable<List<T>> next;
    private final ScheduledExecutorService scheduler;

    public BatchBee(int threads, Hive hive, int queueSize, int maxSize, long maxWaitMillis)
    {
        super(threads, hive, queueSize);
        if (maxSize <= 0)
        {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.batch = new ArrayList<>(maxSize);
        if (maxWaitMillis > 0)
        {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(BatchBee::newDaemonThread);
            this.scheduler.scheduleWithFixedDelay(this::flush, maxWaitMillis, maxWaitMillis, TimeUnit.MILLISECONDS);
        }
        else
        {
            this.scheduler = null;
        }
    }

    public BatchBee(int threads, Hive hive, int maxSize, long maxWaitMillis)
    {
        this(threads, hive, 0, maxSize, maxWaitMillis);
    }

    public BatchBee(Hive hive, int maxSize, long maxWaitMillis)
    {
        this(0, hive, 0, maxSize, maxWaitMillis);
    }

    public BatchBee(int threads, int maxSize, long maxWaitMillis)
    {
        this(threads, null, 0, maxSize, maxWaitMillis);
    }

    public BatchBee(int maxSize, long maxWaitMillis)
    {
        this(0, null, 0, maxSize, maxWaitMillis);
    }

    private static Thread newDaemonThread(Runnable r)
    {
        Thread t = new Thread(r, "BatchBee-flush-timer");
        t.setDaemon(true);
        return t;
    }

    /**
     * Links this BatchBee to the next stage of the chain (the
     * continuation), invoked with every completed batch. The next stage
     * is returned as-is, so calls can be fluently chained:
     * {@code batchBee.linkTo(pipeOfLists).linkTo(bee);}
     *
     * @param next the next Sendable&lt;List&lt;T&gt;&gt; that will
     *             receive each completed batch
     * @return the same {@code next} instance passed in, typed as given,
     *         so the next {@code linkTo} call can be chained on it
     */
    public <S extends Sendable<List<T>>> S linkTo(S next)
    {
        this.next = Objects.requireNonNull(next, "next must not be null");
        return next;
    }

    /**
     * @return the next stage in the chain, or null if none is linked
     */
    protected Sendable<List<T>> getNext()
    {
        return next;
    }

    @Override
    protected void receive(T m)
    {
        List<T> full = null;
        synchronized (batchLock)
        {
            batch.add(m);
            if (batch.size() >= maxSize)
            {
                full = batch;
                batch = new ArrayList<>(maxSize);
            }
        }
        if (full != null)
        {
            forward(full);
        }
    }

    /**
     * Forces the current pending batch, if non-empty, to be forwarded
     * immediately regardless of its size, and resets the batch. Called
     * periodically by the internal scheduler when a maximum wait time
     * was configured; can also be called manually at any time.
     */
    public void flush()
    {
        List<T> pending = null;
        synchronized (batchLock)
        {
            if (!batch.isEmpty())
            {
                pending = batch;
                batch = new ArrayList<>(maxSize);
            }
        }
        if (pending != null)
        {
            forward(pending);
        }
    }

    /**
     * @return the number of messages currently waiting in the pending
     *         batch, not yet forwarded
     */
    public int pending()
    {
        synchronized (batchLock)
        {
            return batch.size();
        }
    }

    private void forward(List<T> values)
    {
        Sendable<List<T>> n = this.next;
        if (n != null)
        {
            n.send(values);
        }
    }

    @Override
    protected void terminate()
    {
        flush();
        if (scheduler != null)
        {
            scheduler.shutdownNow();
        }
    }
}
