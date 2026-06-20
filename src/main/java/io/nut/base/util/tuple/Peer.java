/*
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
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
