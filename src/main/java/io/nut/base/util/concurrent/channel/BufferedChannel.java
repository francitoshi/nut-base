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
 * close() operation, so put()/get() are delegated directly to
 * ArrayBlockingQueue without any additional state or synchronization.
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
        boolean wasInterrupted = false;
        while (true)
        {
            try
            {
                queue.put(value);
                break;
            }
            catch (InterruptedException ex)
            {
                markInterrupted();
                wasInterrupted = true;
            }
        }
        if (wasInterrupted)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit) 
    {
        Objects.requireNonNull(value, "value must not be null");
        boolean wasInterrupted = false;
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (true)
        {
            try
            {
                boolean result = queue.offer(value, Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (wasInterrupted)
                {
                    Thread.currentThread().interrupt();
                }
                return result;
            }
            catch (InterruptedException ex)
            {
                markInterrupted();
                wasInterrupted = true;
            }
        }
    }

    @Override
    public E get() 
    {
        boolean wasInterrupted = false;
        while (true)
        {
            try
            {
                E result = queue.take();
                if (wasInterrupted)
                {
                    Thread.currentThread().interrupt();
                }
                return result;
            }
            catch (InterruptedException ex)
            {
                markInterrupted();
                wasInterrupted = true;
            }
        }
    }

    @Override
    public E get(long timeout, TimeUnit unit) 
    {
        boolean wasInterrupted = false;
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (true)
        {
            try
            {
                E result = queue.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (wasInterrupted)
                {
                    Thread.currentThread().interrupt();
                }
                return result;
            }
            catch (InterruptedException ex)
            {
                markInterrupted();
                wasInterrupted = true;
            }
        }
    }
}
