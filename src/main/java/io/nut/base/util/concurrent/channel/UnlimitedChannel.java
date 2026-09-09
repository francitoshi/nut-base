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
                markInterrupted();
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
                markInterrupted();
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
                markInterrupted();
            }
        }
    }
}
