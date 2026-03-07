/*
 *  NullCache.java
 *
 *  Copyright (C) 2025-2026 francitoshi@gmail.com
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

/**
 * A no-op implementation of the {@link Cache} interface that never stores
 * anything.
 * <p>
 * {@code NullCache} follows the
 * <a href="https://en.wikipedia.org/wiki/Null_object_pattern">Null Object
 * pattern</a>: every {@link #put(Object, Object)} call is silently discarded,
 * every {@link #get(Object)} call returns {@code null}, and the cache always
 * reports itself as empty. No data is ever retained in memory.
 * <p>
 * This class is useful in two common scenarios:
 * <ul>
 *   <li><b>Disabling caching at runtime</b> – inject a {@code NullCache}
 *       wherever a {@link Cache} is expected to effectively turn off caching
 *       without changing any surrounding logic.</li>
 *   <li><b>Testing</b> – use {@code NullCache} in unit tests to isolate the
 *       system under test from any caching side-effects, ensuring that each
 *       call always reaches the underlying data source.</li>
 * </ul>
 * <p>
 * All operations complete in O(1) constant time and allocate no additional
 * memory. This class is not thread-safe, but since it holds no mutable state
 * it can safely be shared across threads without synchronization.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 * @see Cache
 * @see AbstractCache
 */
public class NullCache<K,V> extends AbstractCache<K, V> implements Cache<K, V>
{

    /**
     * Constructs a new {@code NullCache} instance.
     * <p>
     * No resources are allocated; the cache is ready to use immediately and
     * will never hold any entries.
     */
    public NullCache()
    {
    }

    /**
     * Returns {@code true} always, since this cache never stores any entries.
     *
     * @return {@code true}
     */
    @Override
    public boolean isEmpty()
    {
        return true;
    }

    /**
     * Returns {@code 0} always, since this cache never stores any entries.
     *
     * @return {@code 0}
     */
    @Override
    public int size()
    {
        return 0;
    }

    /**
     * Does nothing. There are no entries to remove.
     */
    @Override
    public void clear()
    {
    }

    /**
     * Returns {@code null} always, regardless of the key.
     * <p>
     * Because this cache never stores any entries, every lookup is a cache
     * miss by definition.
     *
     * @param key the key to look up (ignored)
     * @return {@code null}
     */
    @Override
    public V get(K key)
    {
        return null;
    }

    /**
     * Does nothing. The key-value pair is silently discarded.
     * <p>
     * Subsequent calls to {@link #get(Object)} for the same key will still
     * return {@code null}.
     *
     * @param key   the key (ignored)
     * @param value the value (ignored)
     */
    @Override
    public void put(K key, V value)
    {        
    }

}
