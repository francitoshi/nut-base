/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import io.github.francitoshi.nut.base.commons.time.JavaTime;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Closeable unbounded channel.
 * <p>
 * The engine keeps the queue and the closed flag under a single
 * {@link ReentrantLock}, the same invariant of the {@code concurrent.chann}
 * family: {@code put} never waits for room, so there is no {@code POISON}, no
 * reader counters, no writer bookkeeping and no barrier lock — the
 * drain-after-close is just an ordinary predicate checked under the lock.
 * The closing state, {@link #join()} and the terminating iteration are
 * inherited from the {@link CloseableChannel} support in {@link Channel}.
 * <p>
 * {@link #close()} marks the channel as closed, unblocks all pending readers
 * (they drain the remaining buffered elements and then return {@code null})
 * and rejects further {@code put}s (blocking variant throws
 * {@link IllegalStateException}, timed variant returns {@code false}).
 * Nothing can prevent the close from completing: a put never blocks, so the
 * {@code close(timeout, unit)} barrier of the previous engine became an
 * immediate true (the timeout parameter is kept for API symmetry).
 *
 * @param <E> the element type
 */
public final class CloseableUnlimitedChannel<E> extends UnlimitedChannel<E> implements CloseableChannel<E>
{
    private final ArrayDeque<E> items = new ArrayDeque<>();

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition notEmpty = lock.newCondition();

    @Override
    public void put(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        lock.lock();
        try
        {
            if (closed)
            {
                throw new IllegalStateException("closed");
            }
            items.addLast(value);
            notEmpty.signalAll();
        }
        finally
        {
            lock.unlock();
        }
        notifyOperation(Operation.PUT, value);
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        // unbounded: the value is delivered immediately unless the channel is
        // closed; there is nothing the timeout could bound
        lock.lock();
        try
        {
            if (closed)
            {
                return false;
            }
            items.addLast(value);
            notEmpty.signalAll();
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
        return getWaiting(-1L, TimeUnit.NANOSECONDS, true);
    }

    @Override
    public E get(long timeout, TimeUnit unit)
    {
        return getWaiting(timeout, unit, false);
    }

    /**
     * Bounded get with a single lock acquisition. A negative (or zero, in
     * the timed variants) timeout makes an immediate non-blocking attempt;
     * the untimed {@code get()} blocks forever. An open channel waits for
     * items; a closed one drains the remaining items one per call and then
     * yields {@code null}.
     */
    private E getWaiting(long timeout, TimeUnit unit, boolean block)
    {
        long deadline = block ? -1L : JavaTime.untilNanoTime(timeout, unit);

        E result = null;
        boolean received = false;
        InterruptedException interrupted = null;

        lock.lock();
        try
        {
            while (items.isEmpty())
            {
                if (closed || (!block && timeout <= 0L))
                {
                    return null;
                }
                if (block)
                {
                    notEmpty.await();
                }
                else
                {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0L)
                    {
                        return null;
                    }
                    notEmpty.awaitNanos(remaining);
                }
            }
            result = items.removeFirst();
            received = true;
            return result;
        }
        catch (InterruptedException ex)
        {
            interrupted = ex;
            return null;
        }
        finally
        {
            lock.unlock();
            if (interrupted != null)
            {
                handleInterruptedException(interrupted);
                closeIfCloseable();
            }
            if (received)
            {
                notifyOperation(Operation.GET, result);
            }
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
            if (!items.isEmpty())
            {
                result = items.removeFirst();
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

    @Override
    public boolean tryPut(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        boolean inserted = false;

        lock.lock();
        try
        {
            if (!closed)
            {
                items.addLast(value);
                notEmpty.signalAll();
                inserted = true;
            }
        }
        finally
        {
            lock.unlock();
        }
        if (inserted)
        {
            notifyOperation(Operation.PUT, value);
        }
        return inserted;
    }

    /**
     * Closes the channel: marks it closed and wakes every waiter. There is
     * nothing to wait for on this engine ({@code put} never blocks for
     * room), so the close always completes instantly; the timeout parameter
     * is kept for API symmetry and the result is always {@code true}. The
     * caller's own interrupt status is preserved across the call.
     *
     * @return {@code true}, always
     */
    public boolean close(long timeout, TimeUnit unit) throws InterruptedException
    {
        // An interruption of the caller must not abort the irrevocable part
        // of the close; it is deferred and restored before returning.
        boolean interrupted = Thread.interrupted();

        lock.lock();
        try
        {
            markClosed();
            notEmpty.signalAll();
        }
        finally
        {
            lock.unlock();
        }
        notifyOperation(Operation.CLOSE, null);

        if (interrupted)
        {
            Thread.currentThread().interrupt();
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
}
