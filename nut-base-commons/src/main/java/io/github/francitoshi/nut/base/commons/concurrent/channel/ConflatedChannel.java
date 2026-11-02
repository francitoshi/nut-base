/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import io.github.francitoshi.nut.base.commons.time.JavaTime;
import java.util.Objects;
import java.util.Optional;
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
        try
        {
            lock.lockInterruptibly();
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return;
        }
        try
        {
            this.value = value;
            this.hasValue = true;
            notEmpty.signal();
            notifyOperation(Operation.PUT, value);
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        return tryPut(value);
    }

    @Override
    public boolean tryPut(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        lock.lock();
        try
        {
            this.value = value;
            this.hasValue = true;
            notEmpty.signal();
        }
        finally
        {
            lock.unlock();
        }
        notifyOperation(Operation.PUT, value);
        return true;
    }

    @Override
    public E get()
    {
        try
        {
            lock.lockInterruptibly();
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return null;
        }
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
                    handleInterruptedException(ex);
                    closeIfCloseable();
                    return null;
                }
            }
            E result = value;
            value = null;
            hasValue = false;
            notifyOperation(Operation.GET, result);
            return result;
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public E get(long timeout, TimeUnit unit)
    {
        if (timeout <= 0)
        {
            return tryGet().orElse(null);
        }

        long deadline = JavaTime.untilNanoTime(timeout, unit);
        try
        {
            lock.lockInterruptibly();
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return null;
        }
        try
        {
            while (!hasValue)
            {
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
                    handleInterruptedException(ex);
                    closeIfCloseable();
                    return null;
                }
            }
            E result = value;
            value = null;
            hasValue = false;
            notifyOperation(Operation.GET, result);
            return result;
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public Optional<E> tryGet()
    {
        E result = null;
        boolean received = false;
        lock.lock();
        try
        {
            if (hasValue)
            {
                result = value;
                value = null;
                hasValue = false;
                received = true;
            }
        }
        finally
        {
            lock.unlock();
        }
        if (received)
        {
            notifyOperation(Operation.GET, result);
            return Optional.of(result);
        }
        return Optional.empty();
    }
}
