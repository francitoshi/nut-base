/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.tuple;

/**
 * A specialization of {@link Pair} in which the key and the value share
 * the same type {@code E}.
 * <p>
 * {@code Peer} is convenient for representing a pair of homogeneous
 * elements, such as an edge between two nodes of the same type, a
 * before/after value, or any other relationship between two instances
 * of the same class.
 *
 * @param <E> the common type of both elements of the peer
 */
public class Peer<E> extends Pair<E,E>
{
    /**
     * Creates a new peer holding the given key and value, both of the
     * same type {@code E}.
     *
     * @param key the key (first element) to store
     * @param val the value (second element) to store
     */
    public Peer(E key, E val)
    {
        super(key, val);
    }

    /**
     * Returns a new peer with the key and value swapped. Since both
     * elements share the same type, the returned object is also a
     * {@code Peer}. This peer is not modified.
     *
     * @return a new {@code Peer} with key and value swapped
     */
    public Peer<E> inverse()
    {
        return new Peer<>(val, key);
    }
    
}
