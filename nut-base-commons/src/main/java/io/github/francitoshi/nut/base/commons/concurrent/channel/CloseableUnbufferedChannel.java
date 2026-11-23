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
 * blocked {@code put} fails with {@link IllegalStateException} without
 * delivering its value (never silently dropped, so a producer that accounted
 * for it can balance its counters) and the timed variant returns
 * {@code false}.
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

    /**
     * Mailbox mode: a producer's own interruption does not abort its put and
     * does not close the channel — the hand-off wait survives the interrupt
     * (the listener is still notified and the interrupt flag consumed). This
     * is the flavour an {@code Actor} uses for its mailbox, where
     * {@code accept()} waits for a taker permanently and is not abortable by
     * interrupt. Reader interruptions keep the regular abort-and-close
     * semantics.
     */
    private final boolean mailbox;

    /** Package-private constructor used by {@code Channel.closeableMailboxOf}. */
    CloseableUnbufferedChannel(boolean mailbox)
    {
        super();
        this.mailbox = mailbox;
    }

    public CloseableUnbufferedChannel()
    {
        super();
        this.mailbox = false;
    }

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
                if (mailbox)
                {
                    // The producer's own interruption does not abort the
                    // hand-off (mailbox contract): consume the flag and
                    // retry the rendezvous.
                    Thread.interrupted();
                    continue;
                }
                closeIfCloseable();
                // The value was not delivered: like the buffered engine, the
                // blocking put must either deliver the value or fail loudly.
                // A silent return is indistinguishable from a delivered put
                // (the return type is void), and a producer that accounted
                // for the message before the put — as AsyncActor.accept does
                // with its pending counter — would leak the entry forever,
                // leaving the actor never idle and its workers spinning.
                throw new IllegalStateException("closed");
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
                return rendez.offer(value, Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
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
                return (E) item;
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
