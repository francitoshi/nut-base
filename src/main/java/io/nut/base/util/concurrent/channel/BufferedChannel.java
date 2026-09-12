/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Channel with a fixed-capacity buffer without closing: it lives as long as
 * the JVM lives (or as long as the object itself is referenced). There is no
 * close() operation, so put()/get() delegate directly to
 * ArrayBlockingQueue, adding only interruption-tracking state.
 *
 * Unlike UnbufferedChannel, put() does not block until a get() is waiting: the
 * value is stored in the buffer and put() returns as soon as there is room
 * available (up to "capacity" elements in transit).
 */
public final class BufferedChannel<E> extends Channel<E>
{
    private final ArrayBlockingQueue<E> queue;

    public BufferedChannel(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

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
        if (timeout <= 0)
        {
            return queue.offer(value);
        }

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
        if (timeout <= 0)
        {
            return queue.poll();
        }

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
