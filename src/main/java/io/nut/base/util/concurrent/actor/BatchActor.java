/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
 * internal to this {@code BatchActor} and is independent of the owning
 * {@link ActorHub}'s thread pool. {@link #flush()} can also be called manually at
 * any time — for example, to force out a partial batch before shutting down the
 * pipeline.
 * <p>
 * The next stage is wired with {@link #linkTo}. Because the output type differs
 * from the input type ({@code List<T>} vs {@code T}), the next stage must be a
 * {@link java.util.function.Consumer}{@code <List<T>>}.
 * <p>
 * <strong>Thread safety:</strong> the internal batch and the reordering buffer
 * are guarded by {@code batchLock}, so concurrent calls to
 * {@link #receive(Object)}, {@link #flush()}, and {@link #pending()} are all
 * safe. When this Actor runs with an attached ActorHub, several workers may process
 * messages concurrently; messages are still assembled into each batch in
 * acceptance order, so the emitted batches keep their input order regardless
 * of the processing order.
 *
 * @param <T> the type of individual messages accumulated into batches
 */
public class BatchActor<T> extends LinkableActor<T,List<T>>
{
    private final int maxSize;
    private final Object batchLock = new Object();
    private List<T> batch;

    /**
     * 1-based sequence of the next message expected in the current batch (see
     * {@link Actor#receive(Object, long)}). When actual arrival order differs
     * from acceptance order, late-arriving messages wait in
     * {@link #buffered} until their predecessors show up.
     */
    private final AtomicLong expectedSeq = new AtomicLong(1);

    /** Out-of-order arrivals keyed by their acceptance sequence. */
    private final TreeMap<Long, T> buffered = new TreeMap<>();

    private final ScheduledExecutorService scheduler;

    /**
     * Full constructor.
     *
     * @param threads        the maximum number of concurrent worker threads
     * @param actorHub           the ActorHub thread pool, or {@code null} for synchronous
     *                       mode
     * @param queueSize      the internal queue capacity (0 = default)
     * @param maxSize        the number of messages that trigger an immediate flush;
     *                       must be positive
     * @param maxWaitMillis  the maximum interval between flushes, in milliseconds;
     *                       pass {@code 0} to disable periodic flushing
     * @throws IllegalArgumentException if {@code maxSize <= 0}
     */
    public BatchActor(ActorHub actorHub, int threads, int queueSize, int maxSize, long maxWaitMillis)
    {
        super(actorHub, threads, queueSize);
        if (maxSize <= 0)
        {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.batch = new ArrayList<>(maxSize);
        if (maxWaitMillis > 0)
        {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(BatchActor::newDaemonThread);
            this.scheduler.scheduleWithFixedDelay(this::flush, maxWaitMillis, maxWaitMillis, TimeUnit.MILLISECONDS);
        }
        else
        {
            this.scheduler = null;
        }
    }

    /**
     * Constructs a BatchActor with the given thread count and ActorHub, using the
     * default queue size.
     *
     * @param threads       the maximum number of concurrent worker threads
     * @param actorHub          the ActorHub thread pool, or {@code null} for synchronous mode
     * @param maxSize       the number of messages that trigger an immediate flush
     * @param maxWaitMillis the maximum interval between flushes (0 = disabled)
     */
    public BatchActor(ActorHub actorHub, int threads, int maxSize, long maxWaitMillis)
    {
        this(actorHub, threads, 0, maxSize, maxWaitMillis);
    }

    /**
     * Constructs a BatchActor attached to the given ActorHub with the default thread
     * count and queue size.
     *
     * @param actorHub          the ActorHub thread pool, or {@code null} for synchronous mode
     * @param maxSize       the number of messages that trigger an immediate flush
     * @param maxWaitMillis the maximum interval between flushes (0 = disabled)
     */
    public BatchActor(ActorHub actorHub, int maxSize, long maxWaitMillis)
    {
        this(actorHub, 1, 0, maxSize, maxWaitMillis);
    }

    /**
     * Constructs a standalone BatchActor with the given thread count but no ActorHub.
     * A ActorHub is attached at construction time and cannot be changed during the lifecycle of the instance.
     *
     * @param threads       the maximum number of concurrent worker threads
     * @param maxSize       the number of messages that trigger an immediate flush
     * @param maxWaitMillis the maximum interval between flushes (0 = disabled)
     */
    public BatchActor(int threads, int maxSize, long maxWaitMillis)
    {
        this(null, threads, 0, maxSize, maxWaitMillis);
    }

    /**
     * Constructs a standalone BatchActor with the default thread count and no
     * ActorHub. A ActorHub is attached at construction time and cannot be changed during the lifecycle of the instance.
     *
     * @param maxSize       the number of messages that trigger an immediate flush
     * @param maxWaitMillis the maximum interval between flushes (0 = disabled)
     */
    public BatchActor(int maxSize, long maxWaitMillis)
    {
        this(null, 1, 0, maxSize, maxWaitMillis);
    }

    /**
     * Thread factory used for the internal flush scheduler. Creates a single
     * named daemon thread so it does not prevent JVM shutdown.
     *
     * @param r the runnable to wrap
     * @return a new daemon thread named {@code "BatchActor-flush-timer"}
     */
    private static Thread newDaemonThread(Runnable r)
    {
        Thread t = new Thread(r, "BatchActor-flush-timer");
        t.setDaemon(true);
        return t;
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
        receive(m, expectedSeq.get());
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
     * Flushes any remaining pending batch and shuts down the internal flush
     * scheduler (if one was created). Called automatically by {@link Actor}'s
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
