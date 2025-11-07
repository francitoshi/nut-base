/*
 *  AbstractCache.java
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
 * A skeletal implementation of the {@link Cache} interface to minimize the
 * effort required to implement this interface. This class provides a default
 * implementation for the "get-or-create" logic.
 * <p>
 * To implement a cache, the programmer needs only to extend this class and
 * provide implementations for the core {@link #get(Object)} and
 * {@link #put(Object, Object)} methods, and any other methods from the
 * {@code Cache} interface (e.g., {@code invalidate}, {@code size}, etc.).
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public abstract class AbstractCache<K, V> implements Cache<K, V>
{

    /**
     * Retrieves the value associated with the given key from this cache. If no
     * value is present, it computes a new value using the provided
     * {@code creator} function, stores it in the cache, and then returns the
     * new value.
     * <p>
     * This implementation first calls the abstract {@link #get(Object)} method.
     * If the result is {@code null} (indicating a cache miss), it then calls
     * the {@code creator.apply(key)} to produce a new value. This new value is
     * then inserted into the cache via the abstract
     * {@link #put(Object, Object)} method before being returned.
     *
     * @param key the key whose associated value is to be returned
     * @param creator the function to compute a value if one is not already
     * present. This function must not return {@code null}.
     * @return the current (existing or computed) value associated with the
     * specified key.
     * @see #get(Object)
     * @see #put(Object, Object)
     */
    @Override
    public V get(K key, Function<? super K, ? extends V> creator) 
    {
        // First, try to retrieve the value from the cache using the subclass's implementation.
        V value = get(key);

        // If the value is null, it's a cache miss.
        if (value == null) 
        {
            // Create the new value using the provided function.
            value = creator.apply(key);
            // Store the newly created value in the cache for future requests.
            put(key, value);
        }

        // Return the existing or newly created value.
        return value;
    }    
}
