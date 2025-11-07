/*
 *  Cache.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.cache;

import java.util.function.Function;

/**
 * A temporary, in-memory storage for key-value pairs designed to reduce the
 * cost of retrieving data. A cache is similar to a {@link java.util.Map}, but
 * its primary purpose is performance optimization by storing data that is
 * expensive to compute or retrieve.
 * <p>
 * Implementations may have policies for evicting entries automatically, such as
 * size limits, time-to-live (TTL), or least-recently-used (LRU) strategies.
 * This interface, however, only defines the core contract for interacting with
 * the cache.
 * <p>
 * Implementations may or may not permit {@code null} keys or values. It is
 * recommended that implementations document their specific null-handling
 * behavior.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public interface Cache<K, V>
{

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this cache contains no mapping for the key.
     * <p>
     * A return value of {@code null} does not necessarily indicate that the
     * cache contains no mapping for the key; it's also possible that the cache
     * explicitly maps the key to {@code null}.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null}
     * if the key is not present
     */
    V get(K key);

    /**
     * Returns the value associated with the {@code key} in this cache,
     * obtaining that value from the {@code mappingFunction} if necessary.
     * <p>
     * If the key is not already associated with a value, this method attempts
     * to compute its value using the given mapping function, enters it into
     * this cache, and returns it. The entire method invocation should be
     * performed atomically. The mapping function is invoked only if the key is
     * not present in the cache.
     *
     * @param key the key whose associated value is to be returned
     * @param create the function to compute a value if the key is not
     * present. This function must not return {@code null}.
     * @return the current (existing or computed) value associated with the
     * specified key
     */
    V get(K key, Function<? super K, ? extends V> create);

    /**
     * Associates the specified value with the specified key in this cache. If
     * the cache previously contained a mapping for the key, the old value is
     * replaced by the specified value.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    void put(K key, V value);

    /**
     * Returns the number of key-value mappings in this cache.
     *
     * @return the number of entries in the cache
     */
    int size();
    
    /**
     * Returns {@code true} if this cache contains no key-value mappings.
     *
     * @return {@code true} if the cache is empty, {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * Removes all of the mappings from this cache. The cache will be empty
     * after this call returns.
     */
    void clear();
}
