/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    private final Object closeLock = new Object();

    private volatile boolean closed;

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
     *
     * @param value the value to broadcast
     * @throws IllegalStateException if this fan-out has been closed
     */
    @Override
    public void put(E value)
    {
        rwLock.readLock().lock();
        try
        {
            if (closed)
            {
                throw new IllegalStateException("closed");
            }
            super.put(value);
        }
        finally
        {
            rwLock.readLock().unlock();
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
        rwLock.readLock().lock();
        try
        {
            if (closed)
            {
                return false;
            }
            return super.put(value, timeout, unit);
        }
        finally
        {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Closes this fan-out and propagates end-of-data to all registered
     * targets that implement {@link ChannelCloser}.
     * <p>
     * After this call:
     * <ul>
     *   <li>Any subsequent {@link #put} throws {@link IllegalStateException}.</li>
     *   <li>{@link ChannelReader#get} on each closeable target drains
     *       remaining buffered elements and then returns {@code null}.</li>
     * </ul>
     * This method is idempotent.
     *
     * @return {@code true} if all closeable targets were closed successfully;
     *         {@code false} if the close could not complete (e.g. interrupted)
     */
    @Override
    public boolean close()
    {
        rwLock.writeLock().lock();
        try
        {
            if (closed)
            {
                return true;
            }
            closed = true;
            synchronized (closeLock)
            {
                closeLock.notifyAll();
            }
        }
        finally
        {
            rwLock.writeLock().unlock();
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
