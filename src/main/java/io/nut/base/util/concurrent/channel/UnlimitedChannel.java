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
