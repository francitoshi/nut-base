/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

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
 * {@link Consumer}{@code <List<T>>}.
 * <p>
 * <strong>Thread safety:</strong> the internal batch and the reordering buffer
 * are guarded by {@code batchLock}, so concurrent calls to
 * {@link #receive(Object)}, {@link #flush()}, and {@link #pending()} are all
 * safe. When this Bee runs with an attached Hive, several workers may process
 * messages concurrently; messages are still assembled into each batch in
 * acceptance order, so the emitted batches keep their input order regardless
 * of the processing order.
 *
 * @param <T> the type of individual messages accumulated into batches
 */
public class BatchBee<T> extends Bee<T>
{
    private final int maxSize;
    private final Object batchLock = new Object();
    private List<T> batch;

    /**
     * 1-based sequence of the next message expected in the current batch (see
     * {@link Bee#receive(Object, long)}). When actual arrival order differs
     * from acceptance order, late-arriving messages wait in
     * {@link #buffered} until their predecessors show up.
     */
    private final AtomicLong expectedSeq = new AtomicLong(1);

    /** Out-of-order arrivals keyed by their acceptance sequence. */
    private final TreeMap<Long, T> buffered = new TreeMap<>();

    /**
     * The next stage in the chain that will receive each completed batch.
     * Declared {@code volatile} so that a call to {@link #linkTo} from one
     * thread is immediately visible to the worker and scheduler threads that
     * call {@link #forward(List)}.
     */
    protected volatile Consumer<List<T>> next;

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
    public BatchBee(Hive hive, int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        super(hive, threads, queueSize);
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
    public BatchBee(Hive hive, int threads, int maxSize, long maxWaitMillis)
    {
        this(hive, threads, 0, maxSize, maxWaitMillis);
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
        this(hive, 1, 0, maxSize, maxWaitMillis);
    }

    /**
     * Constructs a standalone BatchBee with the given thread count but no Hive.
     * A Hive is attached at construction time and cannot be changed during the lifecycle of the instance.
     *
     * @param threads       the maximum number of concurrent worker threads
     * @param maxSize       the number of messages that trigger an immediate flush
     * @param maxWaitMillis the maximum interval between flushes (0 = disabled)
     */
    public BatchBee(int threads, int maxSize, long maxWaitMillis)
    {
        this(null, threads, 0, maxSize, maxWaitMillis);
    }

    /**
     * Constructs a standalone BatchBee with the default thread count and no
     * Hive. A Hive is attached at construction time and cannot be changed during the lifecycle of the instance.
     *
     * @param maxSize       the number of messages that trigger an immediate flush
     * @param maxWaitMillis the maximum interval between flushes (0 = disabled)
     */
    public BatchBee(int maxSize, long maxWaitMillis)
    {
        this(null, 1, 0, maxSize, maxWaitMillis);
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
     *             {@link Consumer}{@code <List<T>>})
     * @param next the stage that will receive completed batches; must not be
     *             {@code null}
     * @return {@code next}, typed as {@code S}, enabling fluent chaining
     */
    public <S extends Consumer<List<T>>> S linkTo(S next)
    {
        this.next = Objects.requireNonNull(next, "next must not be null");
        return next;
    }

    /**
     * Returns the next stage in the chain, or {@code null} if none has been
     * linked yet.
     *
     * @return the linked next stage, or {@code null}
     */
    protected Consumer<List<T>> getNext()
    {
        return next;
    }

    /**
     * Adds {@code m} to the pending batch, using its acceptance sequence so
     * that batches are assembled in acceptance order even when {@code receive}
     * is invoked concurrently. If the batch has reached {@code maxSize} after
     * the addition, the batch is atomically swapped for a new empty one and
     * returned so the caller can forward it.
     *
     * @param m the message to accumulate
     * @param seq the 1-based acceptance position of {@code m}
     * @return the completed batch to forward, or {@code null}
     */
    private List<T> addInOrder(T m, long seq)
    {
        synchronized (batchLock)
        {
            if (seq == expectedSeq.get())
            {
                batch.add(m);
                expectedSeq.incrementAndGet();
                while (true)
                {
                    T next = buffered.remove(expectedSeq.get());
                    if (next == null)
                    {
                        break;
                    }
                    batch.add(next);
                    expectedSeq.incrementAndGet();
                }
                if (batch.size() >= maxSize)
                {
                    List<T> full = batch;
                    batch = new ArrayList<>(maxSize);
                    return full;
                }
            }
            else if (seq > expectedSeq.get())
            {
                buffered.put(seq, m);
            }
            return null;
        }
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
        List<T> full = addInOrder(m, expectedSeq.get());
        if (full != null)
        {
            forward(full);
        }
    }

    /**
     * Adds {@code m} to the pending batch, honoring its acceptance order so
     * that batches are assembled in input order even when processed in
     * parallel by several workers.
     *
     * @param m   the message to accumulate
     * @param seq the 1-based acceptance position of {@code m}
     */
    @Override
    protected void receive(T m, long seq)
    {
        List<T> full = addInOrder(m, seq);
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
        Consumer<List<T>> n = this.next;
        if (n != null)
        {
            n.accept(values);
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

    @Override
    public Collection<Consumer<?>> getLinkedTargets()
    {
        return next != null ? Collections.singletonList(next) : Collections.emptyList();
    }
}
