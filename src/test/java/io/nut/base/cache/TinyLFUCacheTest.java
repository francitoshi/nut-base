/*
 *  TinyLFUCacheTest.java
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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TinyLFUCacheTest
{
    @Test
    public void testMain()
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(3);

        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        // Access key 1 multiple times - increases frequency
        cache.get(1);
        cache.get(1);
        cache.get(1);

        // Access key 2 once
        cache.get(2);

        // Add new item - should evict key 3 (lowest frequency, least recent)
        cache.put(4, "four");

        assertNotNull(cache.get(1));    // "one" - kept due to high frequency
        assertNotNull(cache.get(2));    // "two" - kept
        assertNull(cache.get(3));       // null - evicted
        assertNotNull(cache.get(4));    // "four" - new entry
    }

    @Test
    public void testGet()
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(3);
        assertNull(cache.get(1));

        cache.put(1, "one");
        assertNotNull(cache.get(1));
    }

    @Test
    public void testSize()
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(3);
        assertEquals(0, cache.size());

        cache.put(1, "one");
        assertEquals(1, cache.size());
    }

    @Test
    public void testClear()
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(3);
        cache.put(1, "one");
        assertEquals(1, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    public void testIsEmpty()
    {
        Cache<Integer, String> cache = new TinyLFUCache<>(3);
        assertTrue(cache.isEmpty());

        cache.put(1, "one");
        assertFalse(cache.isEmpty());

        cache.clear();
        assertTrue(cache.isEmpty());
    }

}
