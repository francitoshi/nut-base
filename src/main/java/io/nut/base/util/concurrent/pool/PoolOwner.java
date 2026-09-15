/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.pool;

/**
 * Common owner contract implemented by the pool implementations. A
 * {@link Poolable} holds a reference to its owner so that {@code recycle()}
 * can return the object to the pool it came from.
 *
 * @param <T> the concrete pooled type
 */
interface PoolOwner<T extends Poolable<T>>
{
    void recycle(T obj);
}