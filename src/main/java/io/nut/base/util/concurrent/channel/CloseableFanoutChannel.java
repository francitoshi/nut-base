/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A {@link FanoutChannel} that can be closed, propagating end-of-data to all
 * registered {@link ChannelCloser} targets.
 * <p>
 * Closing this fan-out sets a closed flag (preventing further writes) and then
 * closes every target that implements {@link ChannelCloser}. Targets that do
 * not implement {@link ChannelCloser} are left untouched — they will stop
 * receiving values because {@link #put} throws
 * {@link IllegalStateException} after close.
 * <p>
 * A {@code put} that is blocked on a full target at the moment of closing is
 * aborted: it returns without delivering to the remaining destinations, and
 * the close proceeds without waiting for the in-flight broadcast to finish.
 * <p>
 * <strong>Ownership:</strong> this class assumes it is the sole owner of its
 * closeable targets. If targets are shared with other producers, do not close
 * this fan-out — remove the targets with {@link #addTarget} /
 * {@link #removeTarget} instead.
 * <p>
 * Example:
 * <pre>{@code
 * CloseableChannel<String> dest1 = Channel.closeableOf(10);
 * CloseableChannel<String> dest2 = Channel.closeableOf(10);
 *
 * CloseableFanoutChannel<String> fan = new CloseableFanoutChannel<>(dest1, dest2);
 * fan.put("hello");   // broadcast to dest1 and dest2
 * fan.put("world");
 * fan.close();         // closes dest1 and dest2 → consumers get null
 * }</pre>
 *
 * @param <E> the element type
 * @see FanoutChannel
 * @see ChannelCloser
 */
public final class CloseableFanoutChannel<E> extends FanoutChannel<E> implements ChannelCloser
{
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
     * Creates a closeable fan-out channel with the given initial destinations.
     *
     * @param targets the downstream channels to broadcast to; individual
     *                elements must not be {@code null}
     */
    @SafeVarargs
    public CloseableFanoutChannel(ChannelWriter<E>... targets)
    {
        super(targets);
    }

    /**
     * Writes {@code value} to every registered target, in registration order.
     * After this channel has been closed, throws {@link IllegalStateException}.
     * <p>
     * If the current thread is interrupted while broadcasting, the interrupted
     * target aborts its {@code put} (closing itself if it is closeable) and the
     * broadcast returns without completing the remaining destinations.
     * <p>
     * A concurrent {@link #close()} never deadlocks with a {@code put} blocked
     * on a full target: close() marks the channel closed, interrupts the
     * in-flight broadcasters and closes the targets once they have drained.
     *
     * @param value the value to broadcast
     * @throws IllegalStateException if this fan-out has been closed
     */
    @Override
    public void put(E value)
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
            broadcast(value);
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
     * timeout. Returns {@code false} if this fan-out has been closed or if
     * any target rejects the value.
     *
     * @param value   the value to broadcast
     * @param timeout maximum time to wait per target
     * @param unit    the time unit of the timeout
     * @return {@code true} if the value was written to all targets;
     *         {@code false} otherwise
     */
    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        synchronized (closeLock)
        {
            if (closed)
            {
                return false;
            }
            writers.add(Thread.currentThread());
        }
        try
        {
            for (ChannelWriter<E> target : targets)
            {
                if (closed)
                {
                    return false;
                }
                if (!target.put(value, timeout, unit))
                {
                    return false;
                }
            }
            return true;
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

    private void broadcast(E value)
    {
        for (ChannelWriter<E> target : targets)
        {
            target.put(value);
            if (closed)
            {
                return;
            }
        }
    }

    /**
     * Closes this fan-out and propagates end-of-data to all registered
     * targets that implement {@link ChannelCloser}.
     * <p>
     * Passing puts are aborted first: close() marks the channel closed under
     * the same monitor used to register in-flight broadcasts, interrupts the
     * registered writers (each blocked target put aborts per the interruption
     * contract) and waits until no broadcast is in flight before closing the
     * targets, so the close never waits for a put that blocks forever.
     * <p>
     * After this call:
     * <ul>
     *   <li>Any subsequent {@link #put} throws {@link IllegalStateException}.</li>
     *   <li>{@link ChannelReader#get} on each closeable target drains
     *       remaining buffered elements and then returns {@code null}.</li>
     * </ul>
     * This method is idempotent: subsequent calls return the same result as
     * the first call.
     *
     * @return {@code true} if all closeable targets were closed successfully;
     *         {@code false} if the close could not complete (e.g. interrupted)
     */
    @Override
    public boolean close()
    {
        synchronized (closeLock)
        {
            if (closed)
            {
                return closeResult;
            }
            closed = true;
            closeLock.notifyAll();
            for (Thread writer : writers)
            {
                writer.interrupt();
            }
            while (!writers.isEmpty())
            {
                try
                {
                    closeLock.wait();
                }
                catch (InterruptedException ex)
                {
                    closeResult = false;
                    return false;
                }
            }
        }

        boolean allClosed = true;
        for (ChannelWriter<E> target : targets)
        {
            if (target instanceof ChannelCloser)
            {
                if (!((ChannelCloser) target).close())
                {
                    allClosed = false;
                }
            }
        }
        closeResult = allClosed;
        return allClosed;
    }

    /**
     * Returns whether this fan-out has been closed via {@link #close()}.
     *
     * @return {@code true} if closed; {@code false} otherwise
     */
    @Override
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
    @Override
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