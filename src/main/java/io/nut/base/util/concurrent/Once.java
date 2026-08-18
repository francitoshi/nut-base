/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A thread-safe container that can be written to at most once, similar to
 * {@code std::cell::OnceCell} in Rust.
 *
 * <p>The cell starts unassigned. It can be assigned exactly once, either
 * eagerly with {@link #set(Object)} / {@link #of(Object)}, lazily by the
 * first {@link #set(Supplier)} / {@link #getOrSet(Supplier)} caller, or by
 * another thread via {@link #set(Object)}. Every later assignment attempt is
 * rejected and returns {@code false}.
 *
 * <pre>{@code
 * Once<String> once = new Once<>();
 * once.set("hello");              // true — first assignment wins
 * once.set("world");              // false — already assigned
 * once.get();                     // "hello"
 * }</pre>
 *
 * <h2>Lazy initialization</h2>
 * <p>{@link #getOrSet(Supplier)} computes the value on first use. Only the
 * thread that wins the assignment runs the supplier; every concurrent caller
 * waits and receives the winner's value without invoking the supplier.
 *
 * <pre>{@code
 * Once<Config> once = new Once<>();
 * Config cfg = once.getOrSet(Config::load);   // supplier runs exactly once
 * }</pre>
 *
 * <h2>Blocking on assignment</h2>
 * <p>{@link #getOrWait()} blocks until the cell has been assigned by another
 * thread, then returns the value. If the calling thread is interrupted while
 * waiting, the wait is aborted and {@link InterruptedException} is thrown
 * instead of the wait continuing silently.
 *
 * @param <T> the type of the value
 */
public final class Once<T> implements Supplier<T>
{
    /** Sentinel meaning "no value assigned yet". */
    private static final Object UNSET = new Object();

    /** Sentinel meaning "a thread is currently computing the value". */
    private static final Object COMPUTING = new Object();

    /** The current state: {@link #UNSET}, {@link #COMPUTING}, or the value. */
    private final AtomicReference<Object> ref = new AtomicReference<>(UNSET);

    private final Object lock = new Object();

    /**
     * Creates an empty {@code Once} with no value assigned.
     */
    public Once()
    {
    }

    /**
     * Creates an already-assigned {@code Once} wrapping the given value.
     *
     * @param value the preassigned value; must not be {@code null}
     * @param <T>   the value type
     * @return an assigned {@code Once}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static <T> Once<T> of(T value)
    {
        Objects.requireNonNull(value, "value must not be null");
        Once<T> once = new Once<>();
        once.ref.set(value);
        return once;
    }

    /**
     * Returns the assigned value, or {@code null} if the cell has not been
     * assigned yet.
     *
     * @return the assigned value; {@code null} if unassigned
     */
    @SuppressWarnings("unchecked")
    public T get()
    {
        Object value = ref.get();
        return (value == UNSET || value == COMPUTING) ? null : (T) value;
    }

    /**
     * Attempts to assign {@code value} to this cell.
     *
     * <p>If this thread is the first to assign the cell, the value is stored
     * and {@code true} is returned. If the cell has already been assigned (or
     * is being computed), the value is rejected and {@code false} is returned.
     *
     * @param value the value to assign; must not be {@code null}
     * @return {@code true} if this thread assigned the value; {@code false}
     *         otherwise
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public boolean set(T value)
    {
        Objects.requireNonNull(value, "value must not be null");
        if (ref.compareAndSet(UNSET, value))
        {
            notifyAssigned();
            return true;
        }
        return false;
    }

    /**
     * Returns the assigned value, blocking until the cell has been assigned by
     * another thread.
     *
     * <p>If the calling thread is interrupted while waiting, this method
     * stops waiting immediately and throws {@link InterruptedException}; the
     * thread's interrupt status is left cleared, as is standard for methods
     * that throw {@code InterruptedException}. The cell itself is unaffected
     * and a later call may wait again.
     *
     * @return the assigned value
     * @throws InterruptedException if the calling thread is interrupted while
     *                               waiting for the cell to be assigned
     */
    @SuppressWarnings("unchecked")
    public T getOrWait() throws InterruptedException
    {
        for (;;)
        {
            Object value = ref.get();
            if (value != UNSET && value != COMPUTING)
            {
                return (T) value;
            }
            waitForAssigned();
        }
    }

    /**
     * Returns the assigned value, computing it via {@code supplier} if the
     * cell has not been assigned yet.
     *
     * <p>Only the thread that wins the assignment executes the supplier;
     * concurrent callers wait and receive the same value without invoking it.
     * If the supplier throws, the cell remains unassigned so a later call can
     * retry, and the exception propagates to the caller.
     *
     * <p>If a waiting (non-computing) thread is interrupted, this method
     * stops waiting immediately and throws {@link InterruptedException}; the
     * thread's interrupt status is left cleared. The cell itself is
     * unaffected and a later call may wait or compute again.
     *
     * @param supplier the factory used to compute the value; must not be
     *                 {@code null}
     * @return the assigned value
     * @throws NullPointerException if {@code supplier} is {@code null}, or if
     *                               {@code supplier} returns {@code null}
     * @throws InterruptedException if the calling thread is interrupted while
     *                               waiting for another thread's computation
     */
    @SuppressWarnings("unchecked")
    public T getOrSet(Supplier<T> supplier) throws InterruptedException
    {
        Objects.requireNonNull(supplier, "supplier must not be null");
        for (;;)
        {
            Object value = ref.get();
            if (value != UNSET && value != COMPUTING)
            {
                return (T) value;
            }
            if (value == UNSET && ref.compareAndSet(UNSET, COMPUTING))
            {
                try
                {
                    T computed = Objects.requireNonNull(supplier.get(), "supplier must not return null");
                    ref.set(computed);
                    notifyAssigned();
                    return computed;
                }
                catch (RuntimeException | Error ex)
                {
                    ref.set(UNSET);
                    notifyAssigned();
                    throw ex;
                }
            }
            waitForAssigned();
        }
    }

    /**
     * Wakes up any threads blocked in {@link #getOrWait()} or
     * {@link #getOrSet(Supplier)}.
     */
    private void notifyAssigned()
    {
        synchronized (lock)
        {
            lock.notifyAll();
        }
    }

    /**
     * Blocks the calling thread until the cell holds a real value.
     *
     * <p>If the calling thread is interrupted while waiting, the
     * {@link InterruptedException} propagates to the caller and the thread's
     * interrupt status is left cleared, following the standard convention for
     * methods that declare {@code throws InterruptedException}.
     *
     * @throws InterruptedException if the calling thread is interrupted while
     *                               waiting
     */
    private void waitForAssigned() throws InterruptedException
    {
        synchronized (lock)
        {
            Object value = ref.get();
            if (value != UNSET && value != COMPUTING)
            {
                return;
            }
            lock.wait();
        }
    }

    /**
     * Attempts to assign a lazily computed value to this cell.
     *
     * <p>If this thread is the first to assign the cell, the value returned by
     * {@code supplier} is stored and {@code true} is returned. If the cell has
     * already been assigned (or is being computed), the supplier is not
     * invoked, the value is rejected and {@code false} is returned.
     *
     * <p>If the supplier throws, the cell remains unassigned so a later call
     * can retry, and the exception propagates to the caller.
     *
     * @param supplier the factory used to compute the value; must not be
     *                 {@code null}
     * @return {@code true} if this thread assigned the value; {@code false}
     *         otherwise
     * @throws NullPointerException if {@code supplier} is {@code null}, or if
     *                               {@code supplier} returns {@code null}
     */
    public boolean set(Supplier<T> supplier)
    {
        Objects.requireNonNull(supplier, "supplier must not be null");
        for (;;)
        {
            Object value = ref.get();
            if (value != UNSET)
            {
                return false;
            }
            if (ref.compareAndSet(UNSET, COMPUTING))
            {
                try
                {
                    T computed = Objects.requireNonNull(supplier.get(), "supplier must not return null");
                    ref.set(computed);
                    notifyAssigned();
                    return true;
                }
                catch (RuntimeException | Error ex)
                {
                    ref.set(UNSET);
                    notifyAssigned();
                    throw ex;
                }
            }
        }
    }

    /**
     * Returns a human-readable description of the current state.
     */
    @Override
    public String toString()
    {
        Object value = ref.get();
        if (value == UNSET || value == COMPUTING)
        {
            return "Once[unassigned]";
        }
        return "Once[" + value + "]";
    }
}
