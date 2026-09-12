/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Unbuffered channel without closing: it lives as long as the JVM lives (or
 * as long as the object itself is referenced). There is no close() operation,
 * so put()/get() delegate directly to SynchronousQueue, adding only
 * interruption-tracking state.
 */
public final class UnbufferedChannel<E> extends Channel<E>
{
    private final SynchronousQueue<E> queue = new SynchronousQueue<>();

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
