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
 * Channel with a fixed-capacity buffer: at most {@code capacity} elements in
 * transit; {@code put} blocks while the buffer is full. Suited for work
 * queues / pipelines.
 * <p>
 * The engine keeps the queue and the count of puts in flight under a single
 * {@link ReentrantLock}: checking {@code closed} while holding the lock that
 * also guards the queue turns the drain-after-close into an ordinary
 * predicate, so the hot path carries no {@code POISON}, no reader counters
 * and no thread bookkeeping. The closing state, {@code join()} and the
 * terminating iteration are shared machinery of the {@link Channel} base
 * class.
 * <p>
 * {@link #close()} marks the channel as closed, unblocks all pending readers
 * (they drain the remaining buffered elements and then return {@code null})
 * and aborts any {@code put} in progress: a blocked {@code put} fails with
 * {@link IllegalStateException} without delivering its value.
 * <p>
 * A writer's or a reader's own interruption aborts its operation (put yields
 * {@code false}, get yields {@code null}) and leaves the channel open: the
 * request is marked and routed to the interruption listener.
 *
 * @param <E> the element type
 */
public final class BufferedChannel<E> extends Channel<E>
{
    private final int capacity;

    private final ArrayDeque<E> items;

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition notEmpty = lock.newCondition();

    private final Condition notFull = lock.newCondition();

    /**
     * Signalled when the count of puts in flight reaches zero: it wakes the
     * {@code close(timeout, unit)} waiters.
     */
    private final Condition drained = lock.newCondition();

    /** Puts in flight, guarded by {@link #lock}. */
    private int activeWriters;

    /**
     * @param capacity the buffer capacity; must be positive
     */
    public BufferedChannel(int capacity)
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
        return putBounded(value, -1L);
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        return putBounded(value, JavaTime.untilNanoTime(timeout, unit));
    }

    // -------------------------------------------------------------------
    // Core engine
    // -------------------------------------------------------------------

    /**
     * Bounded put with a single lock acquisition. When {@code deadline} is
     * negative the put blocks until room appears; otherwise it blocks at most
     * until the deadline (a non-positive timeout is an immediate attempt).
     * A writer's own interruption aborts the put without delivering the
     * value: the request is marked and routed to the interruption listener.
     * {@link #activeWriters} counts the put from right after the closed
     * check until it finishes, so {@code close(timeout, unit)} can wait for
     * the puts in flight without tracking threads.
     *
     * @param deadline absolute deadline in nanos; negative blocks forever
     * @return {@code true} if the value was delivered; {@code false} on
     *         interruption or timeout
     * @throws IllegalStateException if the channel is (or becomes) closed
     */
    private boolean putBounded(E value, long deadline)
    {
        lock.lock();
        try
        {
            activeWriters++;
            if (closed)
            {
                // closed means closed for the producer, in both variants
                throw new IllegalStateException("closed");
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
                    // A writer's own interruption aborts the put without
                    // delivering the value: mark the request, route it to
                    // the interruption listener and return. The channel is
                    // left open.
                    handleInterruptedException(ex);
                    return false;
                }
                if (closed)
                {
                    // woken by close(): fresh out of the wait. The put must
                    // not silently drop its value (that would leave the
                    // producer's pending count incremented forever), so it
                    // fails with the same IllegalStateException a put on a
                    // closed channel raises.
                    throw new IllegalStateException("closed");
                }
            }
            items.addLast(value);
            // signalAll on purpose: one put adds one item, so a signal()
            // would be sufficient (the ArrayBlockingQueue pattern); the
            // conservative wake-everyone keeps the channel correct under any
            // future evolution of the closing machinery, at the cost of
            // parked readers re-parking when several workers wait on an
            // empty buffer.
            notEmpty.signalAll();
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
        }
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

    /**
     * Bounded get with a single lock acquisition. A negative (or zero, in
     * the timed variants) timeout makes an immediate non-blocking attempt;
     * the untimed {@code get()} blocks forever. An open channel waits for
     * items; a closed one drains the remaining items one per call and then
     * yields {@code null}.
     */
    private E getBounded(long timeout, TimeUnit unit, boolean block)
    {
        // a negative (or zero, in the timed variants) timeout makes an
        // immediate non-blocking attempt; the untimed get() blocks forever
        long deadline = block ? -1L : JavaTime.untilNanoTime(timeout, unit);

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
            E result = items.removeFirst();
            notFull.signalAll();
            return result;
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
                notFull.signalAll();
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
            if (!closed && items.size() < capacity)
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
     * Closes the channel: marks it closed and wakes every waiter. A put in
     * flight aborts itself on its next wake-up (it re-checks {@code closed}
     * while holding {@link #lock}), so the close never has to interrupt any
     * thread. The wait below is for the puts in flight to reach their
     * {@code finally}.
     *
     * @param timeout the maximum time to wait for the puts in flight
     * @param unit    the time unit of {@code timeout}
     * @return {@code true} if the puts in flight finished within the timeout
     * @throws InterruptedException if the current thread is interrupted while
     *         waiting
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
