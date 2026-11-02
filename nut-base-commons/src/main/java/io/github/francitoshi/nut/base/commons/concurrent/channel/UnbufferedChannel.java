/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import io.github.francitoshi.nut.base.commons.time.JavaTime;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Unbuffered channel without closing: it lives as long as the JVM lives (or
 * as long as the object itself is referenced). There is no close() operation,
 * so put()/get() delegate directly to SynchronousQueue, adding only
 * interruption-tracking state.
 */
public final class UnbufferedChannel<E> extends Channel<E>
{
    private final SynchronousQueue<E> queue = new SynchronousQueue<>();

    @Override
    public void put(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        try
        {
            // A value is only visible while the writer is parked: announce the
            // pending put FIRST (it may wake a selectGet that retries tryGet),
            // then block until the rendezvous happens.
            notifyOperation(Operation.PUT, value);
            queue.put(value);
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
        }
    }

    @Override
    public boolean put(E value, long timeout, TimeUnit unit)
    {
        Objects.requireNonNull(value, "value must not be null");
        long deadline = JavaTime.untilNanoTime(timeout, unit);
        long remaining = Math.max(0, deadline - System.nanoTime());
        if (remaining == 0)
        {
            return tryPut(value);
        }
        try
        {
            notifyOperation(Operation.PUT, value);
            return queue.offer(value, remaining, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return false;
        }
    }

    @Override
    public E get()
    {
        try
        {
            // Announce the pending get FIRST: it may wake a selectPut whose
            // value becomes deliverable as soon as the reader is parked.
            notifyOperation(Operation.GET, null);
            E value = queue.take();
            notifyOperation(Operation.GET, value);
            return value;
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return null;
        }
    }

    @Override
    public E get(long timeout, TimeUnit unit)
    {
        long deadline = JavaTime.untilNanoTime(timeout, unit);
        long remaining = Math.max(0, deadline - System.nanoTime());
        if (remaining == 0)
        {
            return tryGet().orElse(null);
        }
        try
        {
            notifyOperation(Operation.GET, null);
            E value = queue.poll(remaining, TimeUnit.NANOSECONDS);
            if (value != null)
            {
                notifyOperation(Operation.GET, value);
            }
            return value;
        }
        catch (InterruptedException ex)
        {
            handleInterruptedException(ex);
            closeIfCloseable();
            return null;
        }
    }

    @Override
    public Optional<E> tryGet()
    {
        // Non-blocking: a value is only reachable if a writer is already
        // parked, which is observable by poll(0).
        E value = queue.poll();
        if (value != null)
        {
            notifyOperation(Operation.GET, value);
            return Optional.of(value);
        }
        return Optional.empty();
    }

    @Override
    public boolean tryPut(E value)
    {
        Objects.requireNonNull(value, "value must not be null");
        if (queue.offer(value))
        {
            // A waiting plain get() is not notified through notifyOperation
            // by the offering side; the taker notifies on success.
            return true;
        }
        return false;
    }

    @Override
    boolean needsPolling()
    {
        return true;
    }
}
