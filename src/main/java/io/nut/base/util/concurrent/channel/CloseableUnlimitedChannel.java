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
    public void put(E value) throws InterruptedException
    {
        Objects.requireNonNull(value, "value must not be null");

        if (closed)
        {
            throw new IllegalStateException("closed");
        }

        rwLock.readLock().lock();
        try
        {
            if (closed)
            {
                throw new IllegalStateException("closed");
            }

            queue.put(value);
        }
        finally
        {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit) throws InterruptedException
    {
        Objects.requireNonNull(value, "value must not be null");

        if (closed)
        {
            return false;
        }

        rwLock.readLock().lock();
        try
        {
            if (closed)
            {
                return false;
            }

            if (timeout == 0)
            {
                return queue.offer(value);
            }

            return queue.offer(value, timeout, unit);
        }
        finally
        {
            rwLock.readLock().unlock();
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
    @Override
    public E get(long timeout, TimeUnit unit) throws InterruptedException
    {
        if (closed)
        {
            return drainAfterClosePoll();
        }

        if (timeout == 0)
        {
            Object item = queue.poll();
            if (item == null || item == POISON)
            {
                return null;
            }
            return (E) item;
        }

        gets.incrementAndGet();
        try
        {
            if (closed)
            {
                return drainAfterClosePoll();
            }

            Object item = queue.poll(timeout, unit);
            if (item == null || item == POISON)
            {
                return null;
            }
            return (E) item;
        }
        finally
        {
            gets.decrementAndGet();
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
