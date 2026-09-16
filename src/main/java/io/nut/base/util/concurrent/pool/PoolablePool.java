/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.pool;

import java.util.Objects;
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
public final class PoolablePool<T extends Poolable<T>> implements PoolOwner<T>
{
    private final ObjectPool<T> pool;

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
    public PoolablePool(Supplier<T> factory, int maxIdle)
    {
        this.pool = new ObjectPool<>(Objects.requireNonNull(factory, "factory must not be null"), maxIdle);
    }

    /**
     * Acquires an object from the pool, or creates a new one via the factory
     * when the pool is empty.
     *
     * @return a pooled object ready for use
     */
    public T acquire()
    {
        T obj = pool.acquire();
        obj.attach(this);
        return obj;
    }

    @Override
    public void recycle(T obj)
    {
        obj.reset();
        pool.recycle(obj);
    }

    /**
     * Returns the number of idle objects currently held by this pool.
     *
     * @return the number of idle objects
     */
    public int idleCount()
    {
        return pool.idleCount();
    }
}