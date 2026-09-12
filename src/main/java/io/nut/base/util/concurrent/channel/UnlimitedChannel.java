/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Unbounded and non-closeable channel: it lives as long as the JVM lives (or
 * as long as the object itself is referenced). There is no close() operation,
 * so put()/get() delegate directly to LinkedBlockingQueue, adding only
 * interruption-tracking state.
 *
 * Since it has no bounded capacity, put() never blocks waiting for room: the
 * value is always enqueued and returns immediately (except for memory
 * exhaustion).
 */
public final class UnlimitedChannel<E> extends Channel<E>
{
    private final LinkedBlockingQueue<E> queue = new LinkedBlockingQueue<>();

    @Override
    public void put(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        try
        {
            queue.put(value);
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
        }
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        long deadline = toDeadline(timeout, unit);
        try
        {
            return queue.offer(value, Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return false;
        }
    }

    @Override
    public E get()
    {
        try
        {
            return queue.take();
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return null;
        }
    }

    @Override
    public E get(long timeout, TimeUnit unit)
    {
        long deadline = toDeadline(timeout, unit);
        try
        {
            return queue.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return null;
        }
    }
}
