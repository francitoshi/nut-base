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
        while (true)
        {
            try
            {
                queue.put(value);
                break;
            }
            catch (InterruptedException ex)
            {
                handleInterruptedException(ex);
            }
        }
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (true)
        {
            try
            {
                return queue.offer(value, Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            }
            catch (InterruptedException ex)
            {
                handleInterruptedException(ex);
            }
        }
    }

    @Override
    public E get()
    {
        while (true)
        {
            try
            {
                return queue.take();
            }
            catch (InterruptedException ex)
            {
                handleInterruptedException(ex);
            }
        }
    }

    @Override
    public E get(long timeout, TimeUnit unit)
    {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (true)
        {
            try
            {
                return queue.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            }
            catch (InterruptedException ex)
            {
                handleInterruptedException(ex);
            }
        }
    }
}
