/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class CloseableUnbufferedChannel<E> extends CloseableChannel<E>
{
    private static final Object POISON = new Object();

    private final AtomicInteger gets = new AtomicInteger();

    private final SynchronousQueue<Object> queue = new SynchronousQueue<>();

    private volatile boolean closed;

    private final Object lock = new Object();

    private int activeWriters;

    @Override
    public void put(E value) throws InterruptedException
    {
        Objects.requireNonNull(value, "value must not be null");

        synchronized (lock)
        {
            if (closed)
            {
                throw new IllegalStateException("closed");
            }
            activeWriters++;
        }
        try
        {
            queue.put(value);
        }
        finally
        {
            synchronized (lock)
            {
                activeWriters--;
                lock.notifyAll();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get() throws InterruptedException
    {
        if (closed)
        {
            return drainAfterClose();
        }

        gets.incrementAndGet();
        try
        {
            if (closed)
            {
                return drainAfterClose();
            }

            Object item = queue.take();
            return item == POISON ? null : (E) item;
        }
        finally
        {
            gets.decrementAndGet();
        }
    }

    @SuppressWarnings("unchecked")
    private E drainAfterClose() throws InterruptedException
    {
        for(int i=1;;i++)
        {
            Object item = queue.poll();
            if (item != null)
            {
                return (item == POISON) ? null : (E) item;
            }

            synchronized (lock)
            {
                if (activeWriters == 0)
                {
                    return null;
                }
                lock.wait(Math.min(i,100));
            }
        }
    }

    public boolean close(long timeout, TimeUnit unit) throws InterruptedException
    {
        long timeoutNanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + timeoutNanos;

        synchronized (lock)
        {
            closed = true;
            lock.notifyAll();

            while (activeWriters > 0)
            {
                if (timeoutNanos <= 0)
                {
                    return false;
                }
                long timeoutMillis = TimeUnit.NANOSECONDS.toMillis(timeoutNanos);
                if (timeoutMillis <= 0)
                {
                    timeoutMillis = 1;
                }
                lock.wait(timeoutMillis);
                timeoutNanos = deadline - System.nanoTime();
            }
        }

        for (int i = 1; gets.get() > 0; i++)
        {
            queue.offer(POISON, Math.min(i, 100), TimeUnit.MILLISECONDS);
        }

        return true;
    }

    @Override
    public boolean close()
    {
        try
        {
            return close(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public boolean isClosed()
    {
        return closed;
    }
}
