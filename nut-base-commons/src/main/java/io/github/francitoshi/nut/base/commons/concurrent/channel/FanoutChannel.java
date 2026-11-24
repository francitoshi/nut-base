/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * The additional, non-basic type of the package: the classic fan-out of
 * queue systems. Everything it receives is delivered to all the channels
 * registered on it; targets can be added and removed dynamically.
 * <p>
 * {@code FanoutChannel} implements {@link ChannelWriter} so it can be passed
 * anywhere a single write target is expected, while internally dispatching
 * to multiple destinations. It is a write endpoint: it forwards, it does not
 * buffer for its own readers. It manages the lifecycle of its targets:
 * closing the fan-out closes every registered target, propagating
 * end-of-data to their consumers.
 * <p>
 * {@link #put} returns {@code true} only when every target accepted the
 * value. {@link #tryPut} is a best-effort broadcast: it offers the value to
 * every target without blocking.
 * <p>
 * A {@code put} that is blocked on a full target at the moment of closing is
 * aborted: it fails with {@link IllegalStateException} without delivering to
 * the remaining destinations, and the close proceeds without waiting for the
 * in-flight broadcast to finish (the in-flight writers are interrupted).
 * <p>
 * <strong>Ownership:</strong> this class assumes it is the sole owner of its
 * targets. If targets are shared with other producers, do not close this
 * fan-out — remove the targets with {@link #addTarget} /
 * {@link #removeTarget} instead.
 * <p>
 * <strong>Thread safety:</strong> targets are stored in a
 * {@link CopyOnWriteArrayList}, so {@link #addTarget} and {@link #removeTarget}
 * may be called concurrently with {@link #put}.
 * <p>
 * Example:
 * <pre>{@code
 * Channel<String> dest1 = Channel.buffered(10);
 * Channel<String> dest2 = Channel.buffered(10);
 *
 * FanoutChannel<String> fan = new FanoutChannel<>(dest1, dest2);
 * fan.put("hello");   // broadcast to dest1 and dest2
 * fan.put("world");
 * fan.close();        // closes dest1 and dest2 → consumers get null
 * }</pre>
 *
 * @param <E> the element type
 * @see ChannelWriter
 */
public class FanoutChannel<E> implements ChannelWriter<E>
{
    final CopyOnWriteArrayList<ChannelWriter<E>> targets = new CopyOnWriteArrayList<>();

    /**
     * Guards {@link #closed}, the {@link #writers} set and the close() wait.
     * The broadcast itself happens outside this monitor so that a blocked put
     * never prevents close() from marking the channel closed.
     */
    private final Object closeLock = new Object();

    /**
     * Threads currently broadcasting. Tracked so that close() can interrupt
     * them and wait until no broadcast is in flight before closing targets.
     */
    private final Set<Thread> writers = new HashSet<>();

    private volatile boolean closed;

    /**
     * Result of the last close() attempt, returned by subsequent idempotent
     * close() calls so they do not claim success after a failed close.
     */
    private volatile boolean closeResult;

    /**
     * Creates a fan-out channel with the given initial destinations.
     *
     * @param targets the downstream channels to broadcast to; individual
     *                elements must not be {@code null}
     */
    @SafeVarargs
    public FanoutChannel(ChannelWriter<E>... targets)
    {
        for (ChannelWriter<E> t : targets)
        {
            addTarget(t);
        }
    }

    /**
     * Registers a new downstream target that will receive every value written
     * to this fan-out from this point forward.
     *
     * @param target the channel to add; must not be {@code null}
     * @return this fan-out, for fluent chaining
     */
    public FanoutChannel<E> addTarget(ChannelWriter<E> target)
    {
        this.targets.add(Objects.requireNonNull(target, "target must not be null"));
        return this;
    }

    /**
     * Removes a previously registered target so that it no longer receives
     * values from this fan-out.
     *
     * @param target the channel to remove
     * @return {@code true} if the target was present and has been removed;
     *         {@code false} if it was not found
     */
    public boolean removeTarget(ChannelWriter<E> target)
    {
        return this.targets.remove(target);
    }

    /**
     * Writes {@code value} to every registered target, in registration order.
     * After this fan-out has been closed, throws {@link IllegalStateException}.
     * <p>
     * If any target's {@link ChannelWriter#put} blocks (e.g. the target is
     * full and bounded), subsequent targets wait until the previous write
     * completes, providing natural back-pressure from the slowest consumer.
     * <p>
     * If the current thread is interrupted while broadcasting, the interrupted
     * target aborts its {@code put} (it yields {@code false}) and the
     * broadcast returns {@code false} without completing the remaining
     * destinations.
     * <p>
     * A concurrent {@link #close()} never deadlocks with a {@code put} blocked
     * on a full target: close() marks the fan-out closed, interrupts the
     * in-flight broadcasters and closes the targets once they have drained.
     *
     * @param value the value to broadcast
     * @return {@code true} if the value was written to all targets;
     *         {@code false} otherwise
     * @throws IllegalStateException if this fan-out has been closed
     */
    @Override
    public boolean put(E value)
    {
        synchronized (closeLock)
        {
            if (closed)
            {
                throw new IllegalStateException("closed");
            }
            writers.add(Thread.currentThread());
        }
        try
        {
            boolean all = true;
            for (ChannelWriter<E> target : targets)
            {
                if (closed)
                {
                    // aborted by a concurrent close(): the fanout is closed
                    // for the producer, so the put fails loudly instead of
                    // passing itself off as a mere timeout or interruption
                    throw new IllegalStateException("closed");
                }
                all &= target.put(value);
            }
            if (closed)
            {
                throw new IllegalStateException("closed");
            }
            return all;
        }
        finally
        {
            synchronized (closeLock)
            {
                writers.remove(Thread.currentThread());
                closeLock.notifyAll();
            }
        }
    }

    /**
     * Writes {@code value} to every registered target with a per-target
     * timeout. Throws {@link IllegalStateException} if this fan-out has been
     * closed, and returns {@code false} if any target rejects the value
     * (interruption or timeout).
     *
     * @param value   the value to broadcast
     * @param timeout maximum time to wait per target
     * @param unit    the time unit of the timeout
     * @return {@code true} if the value was written to all targets;
     *         {@code false} otherwise
     * @throws IllegalStateException if this fan-out has been closed
     */
    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        synchronized (closeLock)
        {
            if (closed)
            {
                throw new IllegalStateException("closed");
            }
            writers.add(Thread.currentThread());
        }
        try
        {
            boolean all = true;
            for (ChannelWriter<E> target : targets)
            {
                if (closed)
                {
                    // aborted by a concurrent close(): see the blocking variant
                    throw new IllegalStateException("closed");
                }
                all &= target.put(value, timeout, unit);
            }
            if (closed)
            {
                throw new IllegalStateException("closed");
            }
            return all;
        }
        finally
        {
            synchronized (closeLock)
            {
                writers.remove(Thread.currentThread());
                closeLock.notifyAll();
            }
        }
    }

    /**
     * Best-effort non-blocking broadcast: offers {@code value} to every
     * registered target without blocking and returns {@code true} only when
     * all of them accepted it.
     */
    @Override
    public boolean tryPut(E value)
    {
        synchronized (closeLock)
        {
            if (closed)
            {
                return false;
            }
        }
        boolean delivered = true;
        for (ChannelWriter<E> target : targets)
        {
            delivered &= target.tryPut(value);
        }
        return delivered;
    }

    /**
     * Closes this fan-out and propagates end-of-data to all registered
     * targets.
     * <p>
     * In-flight puts are aborted first: close() marks the fan-out closed under
     * the same monitor used to register in-flight broadcasts, interrupts the
     * registered writers (each blocked target put aborts per the interruption
     * contract of the target) and waits until no broadcast is in flight
     * before closing the targets, so the close never waits for a put that
     * blocks forever.
     * <p>
     * After this call:
     * <ul>
     *   <li>Any subsequent {@link #put(Object)} throws {@link IllegalStateException}.</li>
     *   <li>{@link ChannelReader#get} on each target drains remaining
     *       elements and then returns {@code null}.</li>
     * </ul>
     * This method is idempotent: subsequent calls return the same result as
     * the first call.
     *
     * @return {@code true} if all targets were closed successfully;
     *         {@code false} if the close could not complete (e.g. interrupted)
     */
    @Override
    public boolean close()
    {
        // If the caller is interrupted while waiting for the writers it stops
        // waiting, but it still closes the targets (that is also what unblocks
        // writers stuck in a target) and restores its interrupt status. Giving up
        // here would leave the fanout flagged as closed and its targets open
        // forever, because later close() calls just return the cached result.
        boolean interrupted = Thread.interrupted();
        boolean writersDrained = true;
        synchronized (closeLock)
        {
            if (closed)
            {
                if (interrupted)
                {
                    Thread.currentThread().interrupt();
                }
                return closeResult;
            }
            closed = true;
            closeLock.notifyAll();
            for (Thread writer : writers)
            {
                writer.interrupt();
            }
            if (interrupted)
            {
                // do not wait: the caller asked to be left alone
                writersDrained = writers.isEmpty();
            }
            else
            {
                while (!writers.isEmpty())
                {
                    try
                    {
                        closeLock.wait();
                    }
                    catch (InterruptedException ex)
                    {
                        interrupted = true;
                        writersDrained = false;
                        break;
                    }
                }
            }
        }

        boolean allClosed = writersDrained;
        for (ChannelWriter<E> target : targets)
        {
            if (!target.close())
            {
                allClosed = false;
            }
        }
        closeResult = allClosed;
        if (interrupted)
        {
            Thread.currentThread().interrupt();
        }
        return allClosed;
    }

    /**
     * Returns whether this fan-out has been closed via {@link #close()}.
     *
     * @return {@code true} if closed; {@code false} otherwise
     */
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * Blocks the current thread until this fan-out is closed. If it is
     * already closed, returns immediately.
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public void join() throws InterruptedException
    {
        synchronized (closeLock)
        {
            while (!closed)
            {
                closeLock.wait();
            }
        }
    }
}
