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
public final class CloseableConflatedChannel<E> extends ConflatedChannel<E> implements CloseableChannel<E>
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
            // The value was not stored: like the closeable unbuffered and
            // buffered engines, the put must fail loudly instead of returning
            // as if it had delivered it (the return type is void, so a silent
            // return is indistinguishable from a delivered put for the
            // caller).
            throw new IllegalStateException("closed");
        }
        try
        {
            if (closed)
            {
                throw new IllegalStateException("closed");
            }
            this.value = value;
            this.hasValue = true;
            notEmpty.signal();
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
        try
        {
            lock.lockInterruptibly();
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return false;
        }
        try
        {
            if (closed)
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

    @SuppressWarnings("unchecked")
    @Override
    public Optional<E> tryGet()
    {
        lock.lock();
        try
        {
            if (hasValue)
            {
                E result = (E) this.value;
                this.value = null;
                hasValue = false;
                return Optional.of(result);
            }
            return Optional.empty();
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public boolean tryPut(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        if (closed)
        {
            return false;
        }
        return put(value, 0L, TimeUnit.NANOSECONDS);
    }

    @SuppressWarnings("unchecked")
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
        boolean interrupted = false;
        E result = null;
        try
        {
            while (!hasValue)
            {
                if (closed)
                {
                    return null;
                }
                try
                {
                    notEmpty.await();
                }
                catch (InterruptedException ex)
                {
                    handleInterruptedException(ex);
                    interrupted = true;
                    break;
                }
            }
            if (!interrupted)
            {
                result = (E) this.value;
                this.value = null;
                hasValue = false;
            }
        }
        finally
        {
            lock.unlock();
        }
        // The lock is released before requesting the shutdown.
        if (interrupted)
        {
            closeIfCloseable();
            return null;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public E get(long timeout, TimeUnit unit)
    {
        if (timeout == 0)
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
        boolean interrupted = false;
        E result = null;
        try
        {
            while (!hasValue)
            {
                if (closed)
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
                    handleInterruptedException(ex);
                    interrupted = true;
                    break;
                }
            }
            if (!interrupted)
            {
                result = (E) this.value;
                this.value = null;
                hasValue = false;
            }
        }
        finally
        {
            lock.unlock();
        }
        // The lock is released before requesting the shutdown.
        if (interrupted)
        {
            closeIfCloseable();
            return null;
        }
        return result;
    }

    @Override
    public boolean close()
    {
        lock.lock();
        try
        {
            if (closed)
            {
                return true;
            }
            // the notifying of the join() waiters goes through markClosed()
            markClosed();
            notEmpty.signalAll();
            return true;
        }
        finally
        {
            lock.unlock();
        }
    }
}
