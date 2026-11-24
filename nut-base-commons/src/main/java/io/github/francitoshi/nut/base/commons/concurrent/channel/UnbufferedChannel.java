/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import io.github.francitoshi.nut.base.commons.lang.Saturated;
import io.github.francitoshi.nut.base.commons.time.JavaTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unbuffered (rendezvous) channel: {@code put} and {@code get} are
 * synchronized hand in hand, with no buffer. Direct hand-off between threads
 * (a {@link SynchronousQueue} under the hood).
 * <p>
 * A value is only visible while its writer stays parked inside the
 * {@link SynchronousQueue}, so a close has to reach the parked threads
 * directly: blocked writers are tracked and interrupted, and parked readers
 * are poked with a {@code POISON} value — kept off the writer's monitor
 * (the reader side counts itself in an {@link AtomicInteger}, never under
 * {@code waitLock}) so that the writer/reader hand-off, the hot path of this
 * engine, pays no extra monitor contention. The closing state, {@code join()}
 * and the terminating iteration are shared machinery of the {@link Channel}
 * base class.
 * <p>
 * {@link #close()} marks the channel as closed, unblocks all pending readers
 * (they return {@code null}) and aborts any {@code put} in progress: a
 * blocked {@code put} fails with {@link IllegalStateException} without
 * delivering its value (never silently dropped, so a producer that accounted
 * for it can balance its counters) and the timed variant fails the same way.
 * <p>
 * A writer's or a reader's own interruption aborts its operation (put yields
 * {@code false}, get yields {@code null}) and leaves the channel open: the
 * request is marked and routed to the interruption listener.
 *
 * @param <E> the element type
 */
public final class UnbufferedChannel<E> extends Channel<E>
{
    private static final Object POISON = new Object();

    private final SynchronousQueue<Object> rendez = new SynchronousQueue<>();

    /** Readers parked in take()/poll(), counted lock-free off the writer's monitor. */
    private final AtomicInteger gets = new AtomicInteger();

    private final Object waitLock = new Object();

    private final Set<Thread> writers = new HashSet<>();

    @Override
    @SuppressWarnings("unchecked")
    public boolean put(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        synchronized (waitLock)
        {
            if (closed)
            {
                throw new IllegalStateException("closed");
            }
            writers.add(Thread.currentThread());
        }
        try
        {
            rendez.put(value);
            return true;
        }
        catch (InterruptedException ex)
        {
            if (closed)
            {
                // woken by close(): the channel is closed for the
                // producer, so the put fails loudly instead of passing
                // itself off as a mere interruption
                throw new IllegalStateException("closed");
            }
            // A writer's own interruption aborts the hand-off without
            // delivering the value: mark the request, route it to the
            // interruption listener and return false. The channel is
            // left open.
            handleInterruptedException(ex);
            return false;
        }
        finally
        {
            synchronized (waitLock)
            {
                writers.remove(Thread.currentThread());
                waitLock.notifyAll();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        // a non-positive timeout is a pure non-blocking attempt: no deadline
        // math (the JavaTime.untilNanoTime wrap is kept for t > 0: the
        // SynchronousQueue overflows huge timeout values on its own)
        synchronized (waitLock)
        {
            if (closed)
            {
                throw new IllegalStateException("closed");
            }
            writers.add(Thread.currentThread());
        }
        try
        {
            if (timeout <= 0L)
            {
                return rendez.offer(value);
            }
            long deadline = JavaTime.untilNanoTime(timeout, unit);
            return rendez.offer(value, Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException ex)
        {
            if (closed)
            {
                // woken by close(): see the blocking variant
                throw new IllegalStateException("closed");
            }
            // A writer's own interruption aborts the put without
            // delivering the value: mark the request, route it to the
            // interruption listener and return false. The channel is
            // left open.
            handleInterruptedException(ex);
            return false;
        }
        finally
        {
            synchronized (waitLock)
            {
                writers.remove(Thread.currentThread());
                waitLock.notifyAll();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public E get()
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

            Object item = rendez.take();
            return item == POISON ? null : (E) item;
        }
        catch (InterruptedException ex)
        {
            // A reader's own interruption aborts the read: mark the
            // request, route it to the interruption listener and yield
            // null. The channel is left open.
            handleInterruptedException(ex);
            return null;
        }
        finally
        {
            gets.decrementAndGet();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public E get(long timeout, TimeUnit unit)
    {
        // a non-positive timeout is a pure non-blocking attempt: no deadline
        // math (the JavaTime.untilNanoTime wrap is kept for t > 0: the
        // SynchronousQueue overflows huge timeout values on its own)
        if (closed)
        {
            return drainAfterClosePoll();
        }

        gets.incrementAndGet();
        try
        {
            if (closed)
            {
                return drainAfterClosePoll();
            }

            Object item = timeout <= 0L
                    ? rendez.poll()
                    : rendez.poll(Math.max(0, JavaTime.untilNanoTime(timeout, unit) - System.nanoTime()), TimeUnit.NANOSECONDS);
            if (item == null || item == POISON)
            {
                return null;
            }
            return (E) item;
        }
        catch (InterruptedException ex)
        {
            // A reader's own interruption aborts the read: mark the
            // request, route it to the interruption listener and yield
            // null. The channel is left open.
            handleInterruptedException(ex);
            return null;
        }
        finally
        {
            gets.decrementAndGet();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<E> tryGet()
    {
        Object item = rendez.poll();
        if (item == null || item == POISON)
        {
            return Optional.empty();
        }
        return Optional.of((E) item);
    }

    @Override
    public boolean tryPut(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        synchronized (waitLock)
        {
            if (closed)
            {
                return false;
            }
        }
        return rendez.offer(value);
    }

    @SuppressWarnings("unchecked")
    private E drainAfterClosePoll()
    {
        Object item = rendez.poll();
        if (item == null || item == POISON)
        {
            return null;
        }
        return (E) item;
    }

    /**
     * Drains the rendezvous after a close: values handed off by writers that
     * were already parked when the close happened are still delivered, until
     * the last writer leaves and the queue is empty.
     */
    @SuppressWarnings("unchecked")
    private E drainAfterClose()
    {
        for (int i = 1; ; i++)
        {
            Object item = rendez.poll();
            if (item != null)
            {
                if (item == POISON)
                {
                    return null;
                }
                return (E) item;
            }

            synchronized (waitLock)
            {
                if (writers.isEmpty())
                {
                    return null;
                }
                try
                {
                    waitLock.wait(Math.min(i, 100));
                }
                catch (InterruptedException ex)
                {
                    handleInterruptedException(ex);
                    return null;
                }
            }
        }
    }

    /**
     * Closes the rendezvous channel: marks it closed, interrupts the tracked
     * writers, wakes the parked readers with a POISON and waits for the
     * writers to leave.
     *
     * @param timeout the maximum time to wait for the writers to leave
     * @param unit    the time unit of {@code timeout}
     * @return {@code false} if the writers did not finish within the timeout
     * @throws InterruptedException if the current thread is interrupted while
     *         waiting
     */
    public boolean close(long timeout, TimeUnit unit) throws InterruptedException
    {
        // The irrevocable part of the close (mark closed, abort writers, wake
        // readers) must not be aborted by the caller's own interrupt status:
        // it is deferred and restored before the (interruptible) wait for the
        // writers, so it still makes this method throw as documented.
        boolean interrupted = Thread.interrupted();

        long timeoutNanos = unit.toNanos(timeout);
        long deadline = Saturated.saturatedAdd(System.nanoTime(), timeoutNanos);

        Thread[] blockedWriters;
        synchronized (waitLock)
        {
            markClosed();
            blockedWriters = writers.toArray(new Thread[writers.size()]);
        }
        for (Thread writer : blockedWriters)
        {
            writer.interrupt();
        }

        // Unblock pending readers even if the close times out waiting for the
        // writers below: a reader blocked in take() is only woken by a POISON
        // and, after closed = true, no real value will ever arrive.
        for (int i = 1; gets.get() > 0; i++)
        {
            try
            {
                rendez.offer(POISON, Math.min(i, 100), TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException ex)
            {
                interrupted = true;
            }
        }

        if (interrupted)
        {
            Thread.currentThread().interrupt();
        }

        synchronized (waitLock)
        {
            while (!writers.isEmpty())
            {
                if (timeoutNanos <= 0)
                {
                    return false;
                }
                long timeoutMillis = TimeUnit.NANOSECONDS.toMillis(timeoutNanos);
                if (timeoutMillis <= 0)
                {
                    timeoutMillis = 1;
                }
                waitLock.wait(timeoutMillis);
                timeoutNanos = deadline - System.nanoTime();
            }
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
