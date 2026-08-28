/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class CloseableUnlimitedChannel<E> extends CloseableChannel<E>
{
    private static final Object POISON = new Object();

    private final AtomicInteger gets = new AtomicInteger();

    private final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();

    private volatile boolean closed;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

    private final Object closeLock = new Object();

    @Override
    public void put(E value)
    {
        Objects.requireNonNull(value, "value must not be null");

        boolean wasInterrupted = false;
        while (true)
        {
            if (closed)
            {
                if (wasInterrupted)
                {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("closed");
            }

            rwLock.readLock().lock();
            try
            {
                if (closed)
                {
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
                    throw new IllegalStateException("closed");
                }

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
                rwLock.readLock().unlock();
            }
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
            if (closed)
            {
                if (wasInterrupted)
                {
                    Thread.currentThread().interrupt();
                }
                return false;
            }

            rwLock.readLock().lock();
            try
            {
                if (closed)
                {
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
                    return false;
                }

                try
                {
                    boolean result;
                    if (timeout == 0)
                    {
                        result = queue.offer(value);
                    }
                    else
                    {
                        result = queue.offer(value, Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                    }
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
                rwLock.readLock().unlock();
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
        while (true)
        {
            Object item = queue.poll();
            if (item != null)
            {
                return (item == POISON) ? null : (E) item;
            }

            if (rwLock.getReadLockCount() == 0)
            {
                return null;
            }
            Thread.yield();
        }
    }

    public boolean close(long timeout, TimeUnit unit) throws InterruptedException
    {
        synchronized (closeLock)
        {
            closed = true;
            closeLock.notifyAll();

            boolean acquired = rwLock.writeLock().tryLock(timeout, unit);
            if (acquired)
            {
                rwLock.writeLock().unlock();
            }
            if (!acquired)
            {
                return false;
            }

            int count = gets.get();
            for (int i = 0; i < count; i++)
            {
                queue.put(POISON);
            }

            return true;
        }
    }

    @Override
    public boolean close()
    {
        try
        {
            return close(Long.MAX_VALUE/2, TimeUnit.NANOSECONDS);
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
        synchronized (closeLock)
        {
            while (!closed)
            {
                closeLock.wait();
            }
        }
    }
}
