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
 * Closeable unbuffered (rendezvous) channel.
 * <p>
 * A value is only visible while its writer stays parked inside the
 * {@link SynchronousQueue} counterpart slot, so the close has to reach the
 * parked threads directly: readers are poked with a {@code POISON} value and
 * blocked writers are tracked and interrupted, as in the non-closeable
 * {@link UnbufferedChannel} engine this class extends. The closing state,
 * {@link #join()} and the terminating iteration are inherited from the
 * {@link CloseableChannel} support in {@link Channel}.
 * <p>
 * {@link #close()} marks the channel as closed, unblocks all pending readers
 * (they return {@code null}) and aborts any {@link #put} in progress: a
 * blocked {@code put} returns without delivering its value and the timed
 * variant returns {@code false}.
 *
 * @param <E> the element type
 */
public final class CloseableUnbufferedChannel<E> extends UnbufferedChannel<E> implements CloseableChannel<E>
{
    private static final Object POISON = new Object();

    private final SynchronousQueue<Object> rendez = new SynchronousQueue<>();

    private final AtomicInteger gets = new AtomicInteger();

    private final Object waitLock = new Object();

    private final Set<Thread> writers = new HashSet<>();

    @Override
    @SuppressWarnings("unchecked")
    public void put(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        while (true)
        {
            synchronized (waitLock)
            {
                if (closed)
                {
                    throw new IllegalStateException("closed");
                }
                writers.add(Thread.currentThread());
            }
            boolean interrupted = false;
            try
            {
                rendez.put(value);
                notifyOperation(Operation.PUT, value);
                return;
            }
            catch (InterruptedException ex)
            {
                handleInterruptedException(ex);
                interrupted = true;
            }
            finally
            {
                synchronized (waitLock)
                {
                    writers.remove(Thread.currentThread());
                    waitLock.notifyAll();
                }
            }
            if (interrupted)
            {
                closeIfCloseable();
                return;
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        long deadline = JavaTime.untilNanoTime(timeout, unit);
        while (true)
        {
            synchronized (waitLock)
            {
                if (closed)
                {
                    return false;
                }
                writers.add(Thread.currentThread());
            }
            boolean interrupted = false;
            try
            {
                boolean result = rendez.offer(value, Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (result)
                {
                    notifyOperation(Operation.PUT, value);
                }
                return result;
            }
            catch (InterruptedException ex)
            {
                handleInterruptedException(ex);
                interrupted = true;
            }
            finally
            {
                synchronized (waitLock)
                {
                    writers.remove(Thread.currentThread());
                    waitLock.notifyAll();
                }
            }
            if (interrupted)
            {
                closeIfCloseable();
                return false;
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public E get()
    {
        while (true)
        {
            if (closed)
            {
                return drainAfterClose();
            }

            gets.incrementAndGet();
            boolean interrupted = false;
            try
            {
                if (closed)
                {
                    return drainAfterClose();
                }

                Object item = rendez.take();
                E result = item == POISON ? null : (E) item;
                if (result != null)
                {
                    notifyOperation(Operation.GET, result);
                }
                return result;
            }
            catch (InterruptedException ex)
            {
                handleInterruptedException(ex);
                interrupted = true;
            }
            finally
            {
                gets.decrementAndGet();
            }
            if (interrupted)
            {
                closeIfCloseable();
                return null;
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public E get(long timeout, TimeUnit unit)
    {
        long deadline = JavaTime.untilNanoTime(timeout, unit);
        while (true)
        {
            if (closed)
            {
                return drainAfterClosePoll();
            }

            gets.incrementAndGet();
            boolean interrupted = false;
            try
            {
                if (closed)
                {
                    return drainAfterClosePoll();
                }

                Object item = rendez.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (item == null || item == POISON)
                {
                    return null;
                }
                E result = (E) item;
                notifyOperation(Operation.GET, result);
                return result;
            }
            catch (InterruptedException ex)
            {
                handleInterruptedException(ex);
                interrupted = true;
            }
            finally
            {
                gets.decrementAndGet();
            }
            if (interrupted)
            {
                closeIfCloseable();
                return null;
            }
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
        E value = (E) item;
        notifyOperation(Operation.GET, value);
        return Optional.of(value);
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
        if (rendez.offer(value))
        {
            notifyOperation(Operation.PUT, value);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private E drainAfterClosePoll()
    {
        Object item = rendez.poll();
        if (item == null || item == POISON)
        {
            return null;
        }
        E result = (E) item;
        notifyOperation(Operation.GET, result);
        return result;
    }

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
                notifyOperation(Operation.GET, (E) item);
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
     * @return {@code false} if the writers did not finish within the timeout
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
            waitLock.notifyAll();
            blockedWriters = writers.toArray(new Thread[writers.size()]);
        }
        notifyOperation(Operation.CLOSE, null);
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
