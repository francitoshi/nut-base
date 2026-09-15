/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.pool;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for objects that can be pooled by an {@link ObjectPool}.
 *
 * <p>Subclasses must implement {@link #reset()} to clear the object's internal
 * state, so that a recycled instance can be safely handed out again. Once the
 * instance is no longer needed, call {@link #recycle()} to return it to the
 * pool it was acquired from.</p>
 *
 * <p>The single {@code owner} field doubles as both the pool reference and the
 * recycling state: it holds the owning pool while the object is in use and is
 * {@code null} while the object is idle in the pool. {@link #recycle()} clears
 * it atomically, which also makes a double recycle a no-op.</p>
 *
 * @param <T> the concrete pooled type
 * @author franci
 * @since 1.8
 */
public abstract class Poolable<T extends Poolable<T>>
{
    private final AtomicReference<PoolOwner<T>> owner = new AtomicReference<>();

    /**
     * Clears the content / internal state of this object so it can be reused.
     */
    public abstract void reset();

    /**
     * Returns this object to the pool it was acquired from. Once recycled, the
     * object must not be used by the caller anymore.
     *
     * <p>If this object was not acquired from a pool, or it was already
     * recycled, this method does nothing.</p>
     */
    public final void recycle()
    {
        PoolOwner<T> pool = owner.getAndSet(null);
        if (pool != null)
        {
            pool.recycle(self());
        }
    }

    @SuppressWarnings("unchecked")
    private T self()
    {
        return (T) this;
    }

    final void attach(PoolOwner<T> pool)
    {
        owner.set(pool);
    }
}