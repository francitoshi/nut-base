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
}
