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
 * Closeable channel with a fixed-capacity buffer (capacity must be positive;
 * for an unbuffered rendezvous channel see {@link CloseableUnbufferedChannel}).
 * <p>
 * The engine keeps the queue and the count of puts in flight under a single
 * {@link ReentrantLock}, the same invariant of the {@code concurrent.chann}
 * family: checking {@code closed} while holding the lock that also guards the
 * queue turns the drain-after-close into an ordinary predicate, so the hot
 * path carries no {@code POISON}, no reader counters and no thread
 * bookkeeping. The closing state, {@link #join()} and the terminating
 * iteration are inherited from {@link CloseableChannel} support in
 * {@link Channel}.
 * <p>
 * {@link #close()} marks the channel as closed, unblocks all pending readers
 * (they drain the remaining buffered elements and then return {@code null})
 * and aborts any {@link #put} in progress: a blocked {@code put} returns
 * without delivering its value and the timed variant returns {@code false}.
 *
 * @param <E> the element type
 */
public class CloseableBufferedChannel<E> extends BufferedChannel<E> implements CloseableChannel<E>
{
    private final int capacity;

    private final ArrayDeque<E> items;

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition notEmpty = lock.newCondition();

    private final Condition notFull = lock.newCondition();

    /**
     * Signalled when the count of puts in flight reaches zero: it wakes the
     * {@code close(timeout, unit)} waiters and the {@link #join()} callers.
     */
    private final Condition drained = lock.newCondition();

    /** Puts in flight, guarded by {@link #lock}. */
    private int activeWriters;

    /**
     * @param capacity the buffer capacity; must be positive
     */
    public CloseableBufferedChannel(int capacity)
    {
        super(capacity);
        this.capacity = capacity;
        this.items = new ArrayDeque<>(capacity);
    }

    @Override
    public void put(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        putBounded(value, -1L);
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        if (closed)
        {
            return false;
        }
        if (timeout == 0)
        {
            return tryPut(value);
        }
        return putBounded(value, JavaTime.untilNanoTime(timeout, unit));
    }

    @Override
    public E get()
    {
        return getBounded(-1L, TimeUnit.NANOSECONDS, true);
    }

    @Override
    public E get(long timeout, TimeUnit unit)
    {
        return getBounded(timeout, unit, false);
    }

    // -------------------------------------------------------------------
    // Core engine
    // -------------------------------------------------------------------

    /**
     * Bounded put with a single lock acquisition. When {@code deadline} is
     * negative the put blocks until interrupted, closed, or room appears.
     * {@link #activeWriters} counts the put from right after the closed
     * check until it finishes, so {@code close(timeout, unit)} can wait for
     * the puts in flight without tracking threads.
     *
     * @param deadline absolute deadline in nanos; negative blocks forever
     * @return {@code true} if the value was delivered; {@code false} on
     *         interruption, timeout or close (the blocking variant also
     *         handles the delegated {@link #put(Object)} case with its
     *         {@link IllegalStateException})
     */
    private boolean putBounded(E value, long deadline)
    {
        boolean inserted = false;
        boolean aborted = false;
        InterruptedException interrupted = null;

        lock.lock();
        try
        {
            activeWriters++;
            if (closed)
            {
                if (deadline < 0L)
                {
                    throw new IllegalStateException("closed");
                }
                return false;
            }
            while (items.size() >= capacity)
            {
                try
                {
                    if (deadline < 0L)
                    {
                        notFull.await();
                    }
                    else
                    {
                        long remaining = deadline - System.nanoTime();
                        if (remaining <= 0L)
                        {
                            return false;
                        }
                        notFull.awaitNanos(remaining);
                    }
                }
                catch (InterruptedException ex)
                {
                    interrupted = ex;
                    return false;
                }
                if (closed)
                {
                    // woken by close(): fresh out of the wait, same outcome
                    // as an interruption per the interruption contract
                    aborted = true;
                    return false;
                }
            }
            items.addLast(value);
            notEmpty.signalAll();
            inserted = true;
            return true;
        }
        finally
        {
            activeWriters--;
            if (activeWriters == 0)
            {
                drained.signalAll();
            }
            lock.unlock();
            // decision points run outside the lock: they may close the
            // channel or dispatch listeners
            if (interrupted != null)
            {
                handleInterruptedException(interrupted);
                closeIfCloseable();
            }
            if (aborted)
            {
                // the put was ended by close(), not by a real interrupt, but
                // the observable contract is the one of an aborted operation
                handleInterruptedException(new InterruptedException("aborted by close()"));
            }
            if (inserted)
            {
                notifyOperation(Operation.PUT, value);
            }
        }
    }

    private E getBounded(long timeout, TimeUnit unit, boolean block)
    {
        // a negative (or zero, in the timed variants) timeout makes an
        // immediate non-blocking attempt; the untimed get() blocks forever
        long deadline = block ? -1L : JavaTime.untilNanoTime(timeout, unit);

        E result = null;
        boolean received = false;
        InterruptedException interrupted = null;

        lock.lock();
        try
        {
            // An open channel waits for items; a closed one drains the
            // remaining items one per call and then yields null.
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
            notFull.signalAll();
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
                notFull.signalAll();
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
            if (!closed && items.size() < capacity)
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
     * Closes the channel: marks it closed and wakes every waiter. A put in
     * flight aborts itself on its next wake-up (it re-checks {@code closed}
     * while holding {@link #lock}), so the close never has to interrupt any
     * thread. The wait below is for the puts in flight to reach their
     * {@code finally}, the same contract of the previous engine.
     *
     * @return {@code true} if the puts in flight finished within the timeout
     */
    public boolean close(long timeout, TimeUnit unit) throws InterruptedException
    {
        // The irrevocable part of the close (mark closed, wake readers and
        // writers) must not be aborted by the caller's own interrupt status:
        // it is deferred and restored right before the one interruptible
        // wait, so it still surfaces as documented.
        boolean interrupted = Thread.interrupted();

        long nanos = unit.toNanos(timeout);

        lock.lock();
        try
        {
            markClosed();
            notEmpty.signalAll();
            notFull.signalAll();
            drained.signalAll();
        }
        finally
        {
            lock.unlock();
        }
        notifyOperation(Operation.CLOSE, null);

        lock.lock();
        try
        {
            if (interrupted)
            {
                // already processed: surface it in the interruptible wait
                Thread.currentThread().interrupt();
            }
            while (activeWriters > 0)
            {
                if (nanos <= 0)
                {
                    return false;
                }
                nanos = drained.awaitNanos(nanos);
            }
            return true;
        }
        finally
        {
            lock.unlock();
        }
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
