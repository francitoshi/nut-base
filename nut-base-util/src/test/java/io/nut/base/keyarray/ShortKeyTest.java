/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.keyarray;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShortKeyTest
{
    @Test
    void equalsComparesContentInsteadOfArrayReference()
    {
        ShortKey a = new ShortKey(new short[] {1, 2});
        ShortKey b = new ShortKey(new short[] {1, 2});
        ShortKey c = new ShortKey(new short[] {1, 3});

        assertNotSame(a, b);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void equalKeysProduceEqualHashCodes()
    {
        ShortKey a = new ShortKey(new short[] {5});
        ShortKey b = new ShortKey(new short[] {5});

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void compareToOrdersByContent()
    {
        ShortKey small = new ShortKey(new short[] {1});
        ShortKey same = new ShortKey(new short[] {1});
        ShortKey large = new ShortKey(new short[] {2});

        assertEquals(0, small.compareTo(same));
        assertTrue(small.compareTo(large) < 0);
        assertTrue(large.compareTo(small) > 0);
    }

    @Test
    void worksAsHashMapKey()
    {
        Map<ShortKey, String> map = new HashMap<>();
        map.put(new ShortKey(new short[] {7, 8}), "value");

        assertEquals("value", map.get(new ShortKey(new short[] {7, 8})));
        assertNull(map.get(new ShortKey(new short[] {7, 9})));
    }

    @Test
    void getShortsReturnsACopy()
    {
        short[] data = new short[] {9};
        ShortKey key = new ShortKey(data);
        short[] copy = key.getShorts();

        assertNotSame(data, copy);
        copy[0] = 42;
        assertArrayEquals(new short[] {9}, key.getShorts());
    }
}
