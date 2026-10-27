/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.stats;

import java.util.List;

/**
 * Keeps a bounded random sample of a stream of values.
 * <p>
 * A reservoir never holds more than {@link #capacity()} elements, no matter how
 * many are offered with {@link #add(Object)}, so memory usage is constant.
 * Instances are created with {@link Reservoirs}.
 * <p>
 * Implementations returned by {@link Reservoirs} are thread-safe.
 *
 * @param <E> the type of sampled values
 * @author franci
 */
public interface Reservoir<E>
{
    /**
     * Offers a value to the reservoir. Depending on the sampling policy the
     * value may be stored (possibly replacing another one) or discarded.
     *
     * @param item the value to offer; must not be {@code null}
     * @throws NullPointerException if {@code item} is {@code null}
     */
    void add(E item);

    /**
     * Returns a copy of the values currently held. The order is unspecified.
     * The returned list is independent of the reservoir: later calls to
     * {@link #add(Object)} do not modify it.
     *
     * @return a new list with the current sample
     */
    List<E> snapshot();

    /**
     * Returns the number of values currently held.
     *
     * @return a value between 0 and {@link #capacity()}
     */
    int size();

    /**
     * Returns the maximum number of values the reservoir can hold.
     *
     * @return the capacity
     */
    int capacity();

    /**
     * Returns the total number of values offered since creation or the last
     * {@link #clear()}, including the ones that were discarded.
     *
     * @return the number of offered values
     */
    long seen();

    /**
     * Removes all values and resets the counters, as if the reservoir had just
     * been created.
     */
    void clear();
}
