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
import java.util.function.Supplier;

/**
 * A thread-safe object pool that recycles {@link Poolable} instances, avoiding
 * the cost of repeated allocation while bounding the number of idle objects
 * kept in memory.
 *
 * <p>Obtain an instance with {@link #acquire()}. When it is no longer needed,
 * return it to the pool by calling {@link Poolable#recycle()} on it; the
 * instance automatically goes back to the pool it was acquired from.</p>
 *
 * @param <T> the type of pooled objects
 * @author franci
 * @since 1.8
 */
public final class ObjectPool<T extends Poolable<T>> implements PoolOwner<T>
{
    private static final int STASH_CAP = 8;

    private final Supplier<T> factory;
    private final int maxIdle;
    private final int stashCap;
    private final ArrayBlockingQueue<T> idle;
    private final ThreadLocal<ArrayDeque<T>> localStash = ThreadLocal.withInitial(ArrayDeque::new);
    private final AtomicInteger idleCount = new AtomicInteger();

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
        obj.attach(this);
        return obj;
    }

    public void recycle(T obj)
    {
        obj.reset();
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