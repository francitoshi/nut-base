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
 * Conflated channel without closing: only the latest value is retained.
 * Equivalent to Kotlin's {@code Channel(CONFLATED)}.
 *
 * <p>When a producer calls {@link #put} while the previous value has not yet
 * been consumed, the old value is silently overwritten. This makes the channel
 * ideal for signal / state-update patterns where only the most recent value
 * matters (e.g. UI state, configuration updates, sensor readings).
 *
 * <p>{@link #get} blocks until a value is available; {@link #get(long, TimeUnit)}
 * with a timeout of 0 returns immediately ({@code null} if empty).
 *
 * <p>{@link #put} never blocks waiting for a consumer: it always returns
 * immediately, replacing any unread value.
 *
 * @param <E> the element type
 */
public final class ConflatedChannel<E> extends Channel<E>
{
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    private E value;
    private boolean hasValue;

    @Override
    public void put(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        boolean wasInterrupted = false;
        while (true)
        {
            try
            {
                lock.lockInterruptibly();
                try
                {
                    this.value = value;
                    this.hasValue = true;
                    notEmpty.signal();
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
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
                wasInterrupted = true;
            }
        }
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        boolean wasInterrupted = false;
        while (true)
        {
            try
            {
                lock.lockInterruptibly();
                try
                {
                    this.value = value;
                    this.hasValue = true;
                    notEmpty.signal();
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
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
                lock.lockInterruptibly();
                try
                {
                    while (!hasValue)
                    {
                        try
                        {
                            notEmpty.await();
                        }
                        catch (InterruptedException ex)
                        {
                            markInterrupted();
                            wasInterrupted = true;
                        }
                    }
                    E result = value;
                    value = null;
                    hasValue = false;
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
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
                wasInterrupted = true;
            }
        }
    }

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
                E result = value;
                value = null;
                hasValue = false;
                return result;
            }
            finally
            {
                lock.unlock();
            }
        }

        boolean wasInterrupted = false;
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
                        long remaining = deadline - System.nanoTime();
                        if (remaining <= 0)
                        {
                            if (wasInterrupted)
                            {
                                Thread.currentThread().interrupt();
                            }
                            return null;
                        }
                        try
                        {
                            notEmpty.await(remaining, TimeUnit.NANOSECONDS);
                        }
                        catch (InterruptedException ex)
                        {
                            markInterrupted();
                            wasInterrupted = true;
                            continue;
                        }
                    }
                    E result = value;
                    value = null;
                    hasValue = false;
                    if (wasInterrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
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
                wasInterrupted = true;
            }
        }
    }
}
