/*
 * Copyright (C) 2007-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.locks;

import java.util.Objects;

/**
 * A simple wait/notify style synchronization utility built on top of
 * the intrinsic monitor of a lock object ({@code synchronized} /
 * {@link Object#wait()} / {@link Object#notify()} /
 * {@link Object#notifyAll()}). By default the lock is a private
 * object created internally, but an external lock object may be
 * supplied via {@link #AwaitSignal(Object)} instead, e.g. to
 * coordinate with other code synchronizing on the same object.
 * <p>
 * Any number of threads may call {@link #await()} (or its timed
 * variant {@link #await(long)}) to block until another thread calls
 * {@link #signal()} or {@link #signalAll()}. {@code signal()} wakes up
 * a single waiting thread, while {@code signalAll()} wakes up all of
 * them.
 * <p>
 * The internal waiting-thread counter is only read and written while
 * holding the lock, and is always decremented in a {@code finally}
 * block, so a thread that is interrupted while waiting cannot leave
 * the counter in an inconsistent state, and a call to {@code signal()}
 * cannot be lost because of a stale read of the counter taken outside
 * the lock.
 * <p>
 * As with any wait/notify-based wait, callers should be prepared for
 * spurious wakeups: {@code await()} may return without a matching
 * {@code signal()}/{@code signalAll()} call, so the woken thread
 * should re-check whatever condition it is waiting for.
 * <p>
 * This class is thread-safe.
 *
 * @author franci
 */
public class AwaitSignal
{
    private final Object lock;
    private int waiting = 0;

    /**
     * Creates a new instance that uses its own private object as the
     * lock for the internal wait/notify synchronization.
     */
    public AwaitSignal()
    {
        this(new Object());
    }

    /**
     * Creates a new instance that uses the given external object as
     * the lock for the internal wait/notify synchronization, instead
     * of a private one.
     * <p>
     * This allows the caller to coordinate this instance's
     * {@code await}/{@code signal} calls with other code that
     * synchronizes on the same {@code lock} object, or with a
     * {@code lock} object shared by several {@code AwaitSignal}
     * instances.
     *
     * @param lock the object to synchronize on; must not be
     * {@code null}
     * @throws NullPointerException if {@code lock} is {@code null}
     */
    public AwaitSignal(Object lock)
    {
        this.lock = Objects.requireNonNull(lock, "lock must not be null");
    }

    /**
     * Blocks the current thread until it is woken up by a call to
     * {@link #signal()} or {@link #signalAll()}, or the thread is
     * interrupted.
     * <p>
     * Note that this method may return due to a spurious wakeup, so
     * callers should not assume that a matching signal was actually
     * sent.
     *
     * @throws InterruptedException if the current thread is
     * interrupted while waiting
     */
    public final void await() throws InterruptedException
    {
        synchronized (lock)
        {
            waiting++;
            try
            {
                lock.wait();
            }
            finally
            {
                waiting--;
            }
        }
    }

    /**
     * Blocks the current thread until it is woken up by a call to
     * {@link #signal()} or {@link #signalAll()}, the given timeout
     * elapses, or the thread is interrupted.
     * <p>
     * Note that this method may return due to a spurious wakeup, so
     * callers should not assume that a matching signal was actually
     * sent or that the timeout has necessarily elapsed.
     *
     * @param timeout the maximum time to wait, in milliseconds
     * @throws InterruptedException if the current thread is
     * interrupted while waiting
     */
    public final void await(long timeout) throws InterruptedException
    {
        synchronized (lock)
        {
            waiting++;
            try
            {
                lock.wait(timeout);
            }
            finally
            {
                waiting--;
            }
        }
    }

    /**
     * Wakes up a single thread that is currently blocked in
     * {@link #await()} or {@link #await(long)}. If no thread is
     * waiting, this call has no effect. If multiple threads are
     * waiting, which one is woken up is unspecified.
     */
    public final void signal()
    {
        synchronized (lock)
        {
            if (waiting > 0)
            {
                lock.notify();
            }
        }
    }

    /**
     * Wakes up all threads that are currently blocked in
     * {@link #await()} or {@link #await(long)}. If no thread is
     * waiting, this call has no effect.
     */
    public final void signalAll()
    {
        synchronized (lock)
        {
            if (waiting > 0)
            {
                lock.notifyAll();
            }
        }
    }
}
