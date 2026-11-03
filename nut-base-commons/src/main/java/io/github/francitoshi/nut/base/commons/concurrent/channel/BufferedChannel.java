/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import io.github.francitoshi.nut.base.commons.time.JavaTime;
import java.util.Objects;
import java.util.Optional;
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
public class BufferedChannel<E> extends Channel<E>
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
            notifyOperation(Operation.PUT, value);
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
            return tryPut(value);
        }

        long deadline = JavaTime.untilNanoTime(timeout, unit);
        try
        {
            return putWithTimeout(value, deadline);
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return false;
        }
    }

    private boolean putWithTimeout(E value, long deadline) throws InterruptedException
    {
        if (queue.offer(value, Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS))
        {
            notifyOperation(Operation.PUT, value);
            return true;
        }
        return false;
    }

    @Override
    public E get()
    {
        try
        {
            E value = queue.take();
            notifyOperation(Operation.GET, value);
            return value;
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
            return tryGet().orElse(null);
        }

        long deadline = JavaTime.untilNanoTime(timeout, unit);
        try
        {
            E value = queue.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            if (value != null)
            {
                notifyOperation(Operation.GET, value);
            }
            return value;
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return null;
        }
    }

    @Override
    public Optional<E> tryGet()
    {
        E value = queue.poll();
        if (value != null)
        {
            notifyOperation(Operation.GET, value);
            return Optional.of(value);
        }
        return Optional.empty();
    }

    @Override
    public boolean tryPut(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        if (queue.offer(value))
        {
            notifyOperation(Operation.PUT, value);
            return true;
        }
        return false;
    }
}
