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
 * Conflated channel: keeps only the {@code N} last values, dropping the
 * oldest pending ones as new values arrive. {@code new ConflatedChannel()}
 * is N = 1, so only the latest value is retained — equivalent to Kotlin's
 * {@code Channel(CONFLATED)}; {@code Channel.conflated(n)} is a sliding
 * window of {@code n} values.
 * <p>
 * When a producer calls {@code put} while the previous values have not yet
 * been consumed, the oldest ones are silently dropped. This makes the
 * channel ideal for signal / state-update patterns where only the most
 * recent values matter (e.g. UI state, configuration updates, sensor
 * readings). {@code put} never blocks waiting for a consumer, and the timed
 * variant ignores the timeout (there is nothing it could bound).
 * <p>
 * {@link #close()} marks the channel as closed. No further {@code put} calls
 * are accepted (they throw {@link IllegalStateException}). Consumers can
 * still drain the remaining values; once empty, {@code get} returns
 * {@code null}. The closing state, {@code join()} and the terminating
 * iteration are shared machinery of the {@link Channel} base class.
 * <p>
 * A reader's own interruption aborts the read: the request is marked and
 * routed to the interruption listener, the read yields {@code null} and the
 * channel is left open.
 *
 * @param <E> the element type
 */
public final class ConflatedChannel<E> extends Channel<E>
{
    private final int capacity;

    private final ArrayDeque<E> items;

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition notEmpty = lock.newCondition();

    /** Creates a conflated channel that keeps only the latest value. */
    public ConflatedChannel()
    {
        this(1);
    }

    /**
     * Creates a conflated channel that keeps the {@code capacity} last
     * values.
     *
     * @param capacity the number of latest values to keep; must be positive
     */
    public ConflatedChannel(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.capacity = capacity;
        this.items = new ArrayDeque<>(capacity);
    }

    @Override
    public boolean put(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        lock.lock();
        try
        {
            if (Thread.interrupted())
            {
                // pre-marked, or interrupted while queued for the lock: the
                // status is consumed and the put aborts without touching the
                // window, exactly per the pre-acquisition contract
                handleInterruptedException(new InterruptedException("interrupted before delivering"));
                return false;
            }
            if (closed)
            {
                throw new IllegalStateException("closed");
            }
            if (items.size() == capacity)
            {
                // conflation: the oldest pending value is dropped to make
                // room for the new one; put never blocks and never fails
                // for room
                items.removeFirst();
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
        // conflation never blocks for room: the timeout has nothing to bound
        return put(value);
    }

    @Override
    public E get()
    {
        lock.lock();
        try
        {
            if (Thread.interrupted())
            {
                // pre-marked, or interrupted while queued for the lock: the
                // status is consumed and the get aborts without touching the
                // window, so it can never deliver a value it should not
                handleInterruptedException(new InterruptedException("interrupted before receiving"));
                return null;
            }
            while (items.isEmpty())
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
                    // A reader's own interruption aborts the read: mark the
                    // request, route it to the interruption listener and
                    // yield null. The channel is left open.
                    handleInterruptedException(ex);
                    return null;
                }
            }
            return items.removeFirst();
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
        lock.lock();
        try
        {
            if (Thread.interrupted())
            {
                // same post-acquisition contract as the untimed get()
                handleInterruptedException(new InterruptedException("interrupted before receiving"));
                return null;
            }
            while (items.isEmpty())
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
                    // A reader's own interruption aborts the read: mark the
                    // request, route it to the interruption listener and
                    // yield null. The channel is left open.
                    handleInterruptedException(ex);
                    return null;
                }
            }
            return items.removeFirst();
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
                return Optional.of(items.removeFirst());
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
            if (closed)
            {
                return false;
            }
            if (items.size() == capacity)
            {
                items.removeFirst();
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

    /**
     * Closes the channel: marks it closed and wakes every waiter. There is
     * nothing to wait for on this engine ({@code put} never blocks), so the
     * close always completes instantly; the timeout parameter is kept for
     * API symmetry and the result is always {@code true}.
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
        lock.lock();
        try
        {
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
