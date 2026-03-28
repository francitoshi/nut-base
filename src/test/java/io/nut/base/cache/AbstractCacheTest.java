/*
 *  AbstractCacheTest.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class AbstractCacheTest
{
    /**
     * Test of synchronizedCache method, of class AbstractCache.
     */
    @Test
    public void testSynchronizedCache()
    {
        TinyLFUCache<String, String> instance = new TinyLFUCache<>(10);

        Cache<String,String> result1 = instance.synchronizedCache();
        assertNotNull(result1);
        Cache<String,String> result2 = result1.synchronizedCache();
        assertNotNull(result2);
    }
}
