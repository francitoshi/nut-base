/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.github.francitoshi.nut.base.commons.time.JavaTime;

/**
 * Unbounded channel: no capacity limit, {@code put} never blocks for room,
 * memory grows with unconsumed data. Suited when the producer must never
 * block.
 * <p>
 * The engine keeps the queue and the closed flag under a single
 * {@link ReentrantLock}: {@code put} never waits for room, so there is no
 * {@code POISON}, no reader counters and no writer bookkeeping — the
 * drain-after-close is just an ordinary predicate checked under the lock.
 * The closing state, {@code join()} and the terminating iteration are shared
 * machinery of the {@link Channel} base class.
 * <p>
 * {@link #close()} marks the channel as closed, unblocks all pending readers
 * (they drain the remaining elements and then return {@code null}) and
 * rejects further {@code put}s (both variants throw
 * {@link IllegalStateException}). Nothing can prevent the close from
 * completing: a put never blocks, so {@code close(timeout, unit)} always
 * completes instantly (the timeout parameter is kept for API symmetry).
 * <p>
 * A writer's or a reader's own interruption aborts its operation (put yields
 * {@code false}, get yields {@code null}) and leaves the channel open: the
 * request is marked and routed to the interruption listener.
 *
 * @param <E> the element type
 */
public final class UnlimitedChannel<E> extends Channel<E>
{
    private final ArrayDeque<E> items = new ArrayDeque<>();

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition notEmpty = lock.newCondition();

    @Override
    public boolean put(E value)
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
        return true;
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
                throw new IllegalStateException("closed");
            }
            items.addLast(value);
            notEmpty.signalAll();
        }
        finally
        {
            lock.unlock();
        }
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
            return items.removeFirst();
        }
        catch (InterruptedException ex)
        {
            // A reader's own interruption aborts the read: mark the request,
            // route it to the interruption listener and yield null. The
            // channel is left open.
            handleInterruptedException(ex);
            return null;
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public Optional<E> tryGet()
    {
        lock.lock();
        try
        {
            if (!items.isEmpty())
            {
                E result = items.removeFirst();
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

        lock.lock();
        try
        {
            if (!closed)
            {
                items.addLast(value);
                notEmpty.signalAll();
                return true;
            }
            return false;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Closes the channel: marks it closed and wakes every waiter. There is
     * nothing to wait for on this engine ({@code put} never blocks for
     * room), so the close always completes instantly; the timeout parameter
     * is kept for API symmetry and the result is always {@code true}. The
     * caller's own interrupt status is preserved across the call.
     *
     * @param timeout ignored; kept for API symmetry with the other engines
     * @param unit    ignored; kept for API symmetry with the other engines
     * @return {@code true}, always
     * @throws InterruptedException if the current thread is interrupted; the
     *         close has already taken effect
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
