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
 * A pipeline stage that accumulates individual messages into a pending batch
 * and forwards the entire batch, as a single {@code List<T>} message, to the
 * next stage as soon as either:
 * <ul>
 *   <li>the batch reaches {@code maxSize} elements, or</li>
 *   <li>{@code maxWaitMillis} milliseconds have elapsed since the last flush
 *       (only when {@code maxWaitMillis > 0}).</li>
 * </ul>
 * whichever condition is met first. This is useful to amortize the cost of an
 * expensive downstream operation (a database write, a network call, etc.) over
 * several messages instead of paying that cost once per message.
 * <p>
 * The time-based flush, when enabled, is driven by a single daemon thread
 * internal to this {@code BatchBee} and is independent of the owning
 * {@link Hive}'s thread pool. {@link #flush()} can also be called manually at
 * any time — for example, to force out a partial batch before shutting down the
 * pipeline.
 * <p>
 * The next stage is wired with {@link #linkTo}. Because the output type differs
 * from the input type ({@code List<T>} vs {@code T}), the next stage must be a
 * {@code Sendable<List<T>>}.
 * <p>
 * <strong>Thread safety:</strong> the internal batch is guarded by
 * {@code batchLock}, so concurrent calls to {@link #receive(Object)},
 * {@link #flush()}, and {@link #pending()} are all safe.
 *
 * @param <T> the type of individual messages accumulated into batches
 */
public class BatchBee<T> extends Bee<T>
{
    private final int maxSize;
    private final Object batchLock = new Object();
    private List<T> batch;

    /**
     * The next stage in the chain that will receive each completed batch.
     * Declared {@code volatile} so that a call to {@link #linkTo} from one
     * thread is immediately visible to the worker and scheduler threads that
     * call {@link #forward(List)}.
     */
    protected volatile Sendable<List<T>> next;

    private final ScheduledExecutorService scheduler;

    /**
     * Full constructor.
     *
     * @param threads        the maximum number of concurrent worker threads
     * @param hive           the Hive thread pool, or {@code null} for synchronous
     *                       mode
     * @param queueSize      the internal queue capacity (0 = default)
     * @param maxSize        the number of messages that trigger an immediate flush;
     *                       must be positive
     * @param maxWaitMillis  the maximum interval between flushes, in milliseconds;
     *                       pass {@code 0} to disable periodic flushing
     * @throws IllegalArgumentException if {@code maxSize <= 0}
     */
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

    /**
     * Constructs a BatchBee with the given thread count and Hive, using the
     * default queue size.
     *
     * @param threads       the maximum number of concurrent worker threads
     * @param hive          the Hive thread pool, or {@code null} for synchronous mode
     * @param maxSize       the number of messages that trigger an immediate flush
     * @param maxWaitMillis the maximum interval between flushes (0 = disabled)
     */
    public BatchBee(int threads, Hive hive, int maxSize, long maxWaitMillis)
    {
        this(threads, hive, 0, maxSize, maxWaitMillis);
    }

    /**
     * Constructs a BatchBee attached to the given Hive with the default thread
     * count and queue size.
     *
     * @param hive          the Hive thread pool, or {@code null} for synchronous mode
     * @param maxSize       the number of messages that trigger an immediate flush
     * @param maxWaitMillis the maximum interval between flushes (0 = disabled)
     */
    public BatchBee(Hive hive, int maxSize, long maxWaitMillis)
    {
        this(0, hive, 0, maxSize, maxWaitMillis);
    }

    /**
     * Constructs a standalone BatchBee with the given thread count but no Hive.
     * A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param threads       the maximum number of concurrent worker threads
     * @param maxSize       the number of messages that trigger an immediate flush
     * @param maxWaitMillis the maximum interval between flushes (0 = disabled)
     */
    public BatchBee(int threads, int maxSize, long maxWaitMillis)
    {
        this(threads, null, 0, maxSize, maxWaitMillis);
    }

    /**
     * Constructs a standalone BatchBee with the default thread count and no
     * Hive. A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param maxSize       the number of messages that trigger an immediate flush
     * @param maxWaitMillis the maximum interval between flushes (0 = disabled)
     */
    public BatchBee(int maxSize, long maxWaitMillis)
    {
        this(0, null, 0, maxSize, maxWaitMillis);
    }

    /**
     * Thread factory used for the internal flush scheduler. Creates a single
     * named daemon thread so it does not prevent JVM shutdown.
     *
     * @param r the runnable to wrap
     * @return a new daemon thread named {@code "BatchBee-flush-timer"}
     */
    private static Thread newDaemonThread(Runnable r)
    {
        Thread t = new Thread(r, "BatchBee-flush-timer");
        t.setDaemon(true);
        return t;
    }

    /**
     * Links this BatchBee to the next stage of the chain (the continuation),
     * which will be invoked with every completed batch. The returned value is
     * {@code next} itself, allowing fluent chaining:
     * <pre>{@code
     * batchBee.linkTo(pipeOfLists).linkTo(sink);
     * }</pre>
     *
     * @param <S>  the concrete type of the next stage (must extend
     *             {@code Sendable<List<T>>})
     * @param next the stage that will receive completed batches; must not be
     *             {@code null}
     * @return {@code next}, typed as {@code S}, enabling fluent chaining
     */
    public <S extends Sendable<List<T>>> S linkTo(S next)
    {
        this.next = Objects.requireNonNull(next, "next must not be null");
        return next;
    }

    /**
     * Returns the next stage in the chain, or {@code null} if none has been
     * linked yet. Used by {@link Hive#shutdown(Sendable, boolean, boolean)}
     * to traverse the chain.
     *
     * @return the linked next stage, or {@code null}
     */
    protected Sendable<List<T>> getNext()
    {
        return next;
    }

    /**
     * Adds {@code m} to the pending batch. If the batch has reached
     * {@code maxSize} after the addition, the batch is atomically swapped for a
     * new empty one and forwarded to the next stage.
     *
     * @param m the message to accumulate
     */
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
     * immediately regardless of its current size, and resets the internal batch
     * to a fresh empty list.
     * <p>
     * This method is called periodically by the internal scheduler when a
     * maximum wait time was configured, and can also be called manually at any
     * time — for example to ensure a partial batch is not lost during shutdown.
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
     * Returns the number of messages currently waiting in the pending batch,
     * not yet forwarded to the next stage.
     *
     * @return the current pending batch size (between 0 and {@code maxSize - 1})
     */
    public int pending()
    {
        synchronized (batchLock)
        {
            return batch.size();
        }
    }

    /**
     * Sends {@code values} to the linked next stage. If no next stage is linked,
     * the batch is silently discarded.
     *
     * @param values the batch to forward; never {@code null}
     */
    private void forward(List<T> values)
    {
        Sendable<List<T>> n = this.next;
        if (n != null)
        {
            n.send(values);
        }
    }

    /**
     * Flushes any remaining pending batch and shuts down the internal flush
     * scheduler (if one was created). Called automatically by {@link Bee}'s
     * shutdown sequence after the last message has been processed.
     */
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
