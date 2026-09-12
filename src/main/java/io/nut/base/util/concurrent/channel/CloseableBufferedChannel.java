/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import io.nut.base.math.Nums;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Closeable channel with a fixed-capacity buffer.
 * <p>
 * {@link #close()} marks the channel as closed, unblocks all pending readers
 * (they drain the remaining buffered elements and then return {@code null})
 * and aborts any {@link #put} in progress: a blocked {@code put} returns
 * without delivering its value and the timed variant returns {@code false}.
 */
public final class CloseableBufferedChannel<E> extends CloseableChannel<E>
{
    private static final Object POISON = new Object();

    private final AtomicInteger gets = new AtomicInteger();

    private final ArrayBlockingQueue<Object> queue;

    private volatile boolean closed;

    private final Object lock = new Object();

    private int activeWriters;

    private final Set<Thread> writers = new HashSet<>();

    public CloseableBufferedChannel(int capacity)
    {
        if (capacity <= 0)
        {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    @Override
    public void put(E value)
    {
        Objects.requireNonNull(value, "value must not be null");

        while (true)
        {
            synchronized (lock)
            {
                if (closed)
                {
                    throw new IllegalStateException("closed");
                }
                activeWriters++;
                writers.add(Thread.currentThread());
            }
            boolean interrupted = false;
            try
            {
                queue.put(value);
                return;
            }
            catch (InterruptedException ex)
            {
                handleInterruptedException(ex);
                interrupted = true;
            }
            finally
            {
                synchronized (lock)
                {
                    activeWriters--;
                    writers.remove(Thread.currentThread());
                    lock.notifyAll();
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
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        if (closed)
        {
            return false;
        }

        if (timeout == 0)
        {
            return queue.offer(value);
        }

        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (true)
        {
            synchronized (lock)
            {
                if (closed)
                {
                    return false;
                }
                activeWriters++;
                writers.add(Thread.currentThread());
            }
            boolean interrupted = false;
            try
            {
                boolean result = queue.offer(value, Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                return result;
            }
            catch (InterruptedException ex)
            {
                handleInterruptedException(ex);
                interrupted = true;
            }
            finally
            {
                synchronized (lock)
                {
                    activeWriters--;
                    writers.remove(Thread.currentThread());
                    lock.notifyAll();
                }
            }
            if (interrupted)
            {
                closeIfCloseable();
                return false;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
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

                try
                {
                    Object item = queue.take();
                    return item == POISON ? null : (E) item;
                }
                catch (InterruptedException ex)
                {
                    handleInterruptedException(ex);
                    interrupted = true;
                }
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

    @SuppressWarnings("unchecked")
    @Override
    public E get(long timeout, TimeUnit unit)
    {
        if (closed)
        {
            return drainAfterClosePoll();
        }

        if (timeout == 0)
        {
            return drainAfterClosePoll();
        }

        long deadline = System.nanoTime() + unit.toNanos(timeout);
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

                try
                {
                    Object item = queue.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
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

    @SuppressWarnings("unchecked")
    private E drainAfterClosePoll()
    {
        Object item = queue.poll();
        if (item == null || item == POISON)
        {
            return null;
        }
        @SuppressWarnings("unchecked")
        E result = (E) item;
        return result;
    }

    @SuppressWarnings("unchecked")
    private E drainAfterClose()
    {
        while (true)
        {
            Object item = queue.poll();
            if (item != null)
            {
                return (item == POISON) ? null : (E) item;
            }

            synchronized (lock)
            {
                if (activeWriters == 0)
                {
                    return null;
                }
                try
                {
                    lock.wait(100);
                }
                catch (InterruptedException ex)
                {
                    handleInterruptedException(ex);
                    return null;
                }
            }
        }
    }

    public boolean close(long timeout, TimeUnit unit) throws InterruptedException
    {
        long timeoutNanos = unit.toNanos(timeout);
        long deadline = Nums.saturatedAdd(System.nanoTime(),timeoutNanos);

        Thread[] blockedWriters;
        synchronized (lock)
        {
            closed = true;
            lock.notifyAll();
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
            queue.offer(POISON, Math.min(i, 100), TimeUnit.MILLISECONDS);
        }

        synchronized (lock)
        {
            while (activeWriters > 0)
            {
                if (timeoutNanos <= 0)
                {
                    return false;
                }
                long ms = TimeUnit.NANOSECONDS.toMillis(timeoutNanos);
                if (ms <= 0)
                {
                    ms = 1;
                }
                lock.wait(ms);
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

    @Override
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void join() throws InterruptedException
    {
        synchronized (lock)
        {
            while (!closed)
            {
                lock.wait();
            }
        }
    }
}
