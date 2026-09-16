/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.pool;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A thread-safe object pool that reuses arbitrary instances, avoiding the cost
 * of repeated allocation while bounding the number of idle objects kept in
 * memory.
 *
 * <p>Obtain an instance with {@link #acquire()}. When it is no longer needed,
 * return it to the pool with {@link #recycle(Object)}. The caller must know
 * which pool the object came from, so pooled objects stay completely decoupled
 * from the pooling machinery and do not need to implement or extend anything.
 * A reset consumer may be registered with {@link #setReset(java.util.function.Consumer)}
 * so the pool itself clears the object's state; otherwise the caller is
 * responsible for clearing any state before recycling.</p>
 *
 * <p>For objects that know how to return themselves to their pool, see
 * {@link PoolablePool}.</p>
 *
 * @param <T> the type of pooled objects
 * @author franci
 * @since 1.8
 */
public final class ObjectPool<T>
{
    private static final int STASH_CAP = 8;

    private final Supplier<T> factory;
    private final int maxIdle;
    private final int stashCap;
    private final ArrayBlockingQueue<T> idle;
    private final ThreadLocal<ArrayDeque<T>> localStash = ThreadLocal.withInitial(ArrayDeque::new);
    private final AtomicInteger idleCount = new AtomicInteger();
    private volatile Consumer<T> reset;

    /**
     * Creates a new object pool.
     *
     * @param factory the supplier used to create new instances when the pool
     *                is empty; must not be {@code null}
     * @param maxIdle the maximum number of idle objects to keep in the pool;
     *                must not be negative
     * @throws NullPointerException if factory is null
     * @throws IllegalArgumentException if maxIdle is negative
     */
    public ObjectPool(Supplier<T> factory, int maxIdle)
    {
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
        if (maxIdle < 0)
        {
            throw new IllegalArgumentException("maxIdle must not be negative");
        }
        this.maxIdle = maxIdle;
        this.stashCap = Math.min(maxIdle, STASH_CAP);
        this.idle = new ArrayBlockingQueue<>(Math.max(1, maxIdle));
    }

    /**
     * Acquires an object from the pool, or creates a new one via the factory
     * when the pool is empty.
     *
     * @return a pooled object ready for use
     */
    public T acquire()
    {
        ArrayDeque<T> stash = localStash.get();
        T obj = stash.pollLast();
        if (obj == null)
        {
            obj = idle.poll();
            if (obj == null)
            {
                obj = factory.get();
            }
            else
            {
                idleCount.decrementAndGet();
            }
        }
        else
        {
            idleCount.decrementAndGet();
        }
        return obj;
    }

    /**
     * Sets a consumer that resets an object's state before it is placed back
     * in the pool. It is optional; when set, {@link #recycle(Object)} applies
     * it to the recycled object. Passing {@code null} clears it.
     *
     * @param reset the reset consumer, or {@code null} to disable it
     * @return this pool, for chaining
     */
    public ObjectPool<T> setReset(Consumer<T> reset)
    {
        this.reset = reset;
        return this;
    }

    /**
     * Returns an object previously obtained from {@link #acquire()} back to
     * this pool so it can be reused.
     *
     * <p>If a reset consumer was set with {@link #setReset(Consumer)}, it is
     * applied to the object before it is stored. Otherwise the pool does not
     * inspect or modify the object and the caller is responsible for clearing
     * any state that must be reset before reuse. The object must have been
     * acquired from this pool and must not be recycled more than once.</p>
     *
     * @param obj the object to recycle
     */
    public void recycle(T obj)
    {
        Consumer<T> reset = this.reset;
        if (reset != null)
        {
            reset.accept(obj);
        }
        if (idleCount.incrementAndGet() > maxIdle)
        {
            idleCount.decrementAndGet();
            return;
        }
        ArrayDeque<T> stash = localStash.get();
        if (stash.size() < stashCap)
        {
            stash.addLast(obj);
        }
        else if (!idle.offer(obj))
        {
            idleCount.decrementAndGet();
        }
    }

    /**
     * Returns the number of idle objects currently held by this pool.
     *
     * @return the number of idle objects
     */
    public int idleCount()
    {
        return idleCount.get();
    }
}