/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import io.nut.base.math.Nums;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class CloseableBufferedChannel<E> extends CloseableChannel<E>
{
    private static final Object POISON = new Object();

    private final AtomicInteger gets = new AtomicInteger();

    private final ArrayBlockingQueue<Object> queue;

    private volatile boolean closed;

    private final Object lock = new Object();

    private int activeWriters;

    public CloseableBufferedChannel(int capacity)
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
            synchronized (lock)
            {
                if (closed)
                {
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
                    throw new IllegalStateException("closed");
                }
                activeWriters++;
            }
            try
            {
                try
                {
                    queue.put(value);
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
                    return;
                }
                catch (InterruptedException ex)
                {
                    markInterrupted();
                    wasInterrupted = true;
                }
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
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        if (closed)
        {
            return false;
        }

        if (timeout == 0)
        {
            return queue.offer(value);
        }

        boolean wasInterrupted = false;
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (true)
        {
            synchronized (lock)
            {
                if (closed)
                {
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
                    return false;
                }
                activeWriters++;
            }
            try
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
            finally
            {
                synchronized (lock)
                {
                    activeWriters--;
                    lock.notifyAll();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get()
    {
        boolean wasInterrupted = false;
        while (true)
        {
            if (closed)
            {
                if (wasInterrupted)
                {
                    Thread.currentThread().interrupt();
                }
                return drainAfterClose();
            }

            gets.incrementAndGet();
            try
            {
                if (closed)
                {
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
                    return drainAfterClose();
                }

                try
                {
                    Object item = queue.take();
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
                    return item == POISON ? null : (E) item;
                }
                catch (InterruptedException ex)
                {
                    markInterrupted();
                    wasInterrupted = true;
                }
            }
            finally
            {
                gets.decrementAndGet();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get(long timeout, TimeUnit unit)
    {
        if (closed)
        {
            return drainAfterClosePoll();
        }

        if (timeout == 0)
        {
            return drainAfterClosePoll();
        }

        boolean wasInterrupted = false;
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (true)
        {
            if (closed)
            {
                if (wasInterrupted)
                {
                    Thread.currentThread().interrupt();
                }
                return drainAfterClosePoll();
            }

            gets.incrementAndGet();
            try
            {
                if (closed)
                {
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
                    return drainAfterClosePoll();
                }

                try
                {
                    Object item = queue.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
                    if (item == null || item == POISON)
                    {
                        return null;
                    }
                    return (E) item;
                }
                catch (InterruptedException ex)
                {
                    markInterrupted();
                    wasInterrupted = true;
                }
            }
            finally
            {
                gets.decrementAndGet();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private E drainAfterClosePoll()
    {
        Object item = queue.poll();
        if (item == null || item == POISON)
        {
            return null;
        }
        @SuppressWarnings("unchecked")
        E result = (E) item;
        return result;
    }

    @SuppressWarnings("unchecked")
    private E drainAfterClose()
    {
        boolean wasInterrupted = false;
        while (true)
        {
            Object item = queue.poll();
            if (item != null)
            {
                if (wasInterrupted)
                {
                    Thread.currentThread().interrupt();
                }
                return (item == POISON) ? null : (E) item;
            }

            synchronized (lock)
            {
                if (activeWriters == 0)
                {
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }
                try
                {
                    lock.wait(100);
                }
                catch (InterruptedException ex)
                {
                    markInterrupted();
                    wasInterrupted = true;
                }
            }
        }
    }

    public boolean close(long timeout, TimeUnit unit) throws InterruptedException
    {
        long timeoutNanos = unit.toNanos(timeout);
        long deadline = Nums.saturatedAdd(System.nanoTime(),timeoutNanos);

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
                long ms = TimeUnit.NANOSECONDS.toMillis(timeoutNanos);
                if (ms <= 0)
                {
                    ms = 1;
                }
                lock.wait(ms);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void join() throws InterruptedException
    {
        synchronized (lock)
        {
            while (!closed)
            {
                lock.wait();
            }
        }
    }
}
