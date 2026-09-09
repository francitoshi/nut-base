/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Closeable conflated channel: only the latest value is retained, and the
 * channel can be explicitly closed to signal end-of-data.
 * Equivalent to Kotlin's {@code Channel(CONFLATED)} with close support.
 *
 * <p>When a producer calls {@link #put} while the previous value has not yet
 * been consumed, the old value is silently overwritten.
 *
 * <p>{@link #close()} marks the channel as closed. No further {@link #put}
 * calls are accepted (they return {@code false} or throw
 * {@link IllegalStateException}). Consumers can still drain any remaining
 * value; once empty, {@link #get} returns {@code null}.
 *
 * @param <E> the element type
 */
public final class CloseableConflatedChannel<E> extends CloseableChannel<E>
{
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition closed = lock.newCondition();

    private E value;
    private boolean hasValue;
    private volatile boolean closedFlag;
    private int activeWriters;

    @Override
    public void put(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        while (true)
        {
            try
            {
                lock.lockInterruptibly();
                try
                {
                    if (closedFlag)
                    {
                        throw new IllegalStateException("closed");
                    }
                    this.value = value;
                    this.hasValue = true;
                    notEmpty.signal();
                    return;
                }
                finally
                {
                    lock.unlock();
                }
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
        while (true)
        {
            try
            {
                lock.lockInterruptibly();
                try
                {
                    if (closedFlag)
                    {
                        return false;
                    }
                    this.value = value;
                    this.hasValue = true;
                    notEmpty.signal();
                    return true;
                }
                finally
                {
                    lock.unlock();
                }
            }
            catch (InterruptedException ex)
            {
                markInterrupted();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get()
    {
        while (true)
        {
            try
            {
                lock.lockInterruptibly();
                try
                {
                    while (!hasValue)
                    {
                        if (closedFlag)
                        {
                            return null;
                        }
                        try
                        {
                            notEmpty.await();
                        }
                        catch (InterruptedException ex)
                        {
                            markInterrupted();
                            continue;
                        }
                    }
                    E result = (E) this.value;
                    this.value = null;
                    hasValue = false;
                    return result;
                }
                finally
                {
                    lock.unlock();
                }
            }
            catch (InterruptedException ex)
            {
                markInterrupted();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get(long timeout, TimeUnit unit)
    {
        if (timeout == 0)
        {
            lock.lock();
            try
            {
                if (!hasValue)
                {
                    return null;
                }
                E result = (E) this.value;
                this.value = null;
                hasValue = false;
                return result;
            }
            finally
            {
                lock.unlock();
            }
        }

        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (true)
        {
            try
            {
                lock.lockInterruptibly();
                try
                {
                    while (!hasValue)
                    {
                        if (closedFlag)
                        {
                            return null;
                        }
                        long remaining = deadline - System.nanoTime();
                        if (remaining <= 0)
                        {
                            return null;
                        }
                        try
                        {
                            notEmpty.await(remaining, TimeUnit.NANOSECONDS);
                        }
                        catch (InterruptedException ex)
                        {
                            markInterrupted();
                            continue;
                        }
                    }
                    E result = (E) this.value;
                    this.value = null;
                    hasValue = false;
                    return result;
                }
                finally
                {
                    lock.unlock();
                }
            }
            catch (InterruptedException ex)
            {
                markInterrupted();
            }
        }
    }

    @Override
    public boolean close()
    {
        lock.lock();
        try
        {
            if (closedFlag)
            {
                return true;
            }
            closedFlag = true;
            notEmpty.signalAll();
            closed.signalAll();
            return true;
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public boolean isClosed()
    {
        return closedFlag;
    }

    @Override
    public void join() throws InterruptedException
    {
        lock.lockInterruptibly();
        try
        {
            while (!closedFlag)
            {
                closed.await();
            }
        }
        finally
        {
            lock.unlock();
        }
    }
}
