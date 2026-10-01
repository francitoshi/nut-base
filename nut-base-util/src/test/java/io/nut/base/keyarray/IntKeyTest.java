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

class IntKeyTest
{
    @Test
    void equalsComparesContentInsteadOfArrayReference()
    {
        IntKey a = new IntKey(new int[] {1, 2, 3});
        IntKey b = new IntKey(new int[] {1, 2, 3});
        IntKey c = new IntKey(new int[] {1, 2, 4});

        assertNotSame(a, b);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void equalKeysProduceEqualHashCodes()
    {
        IntKey a = new IntKey(new int[] {10, 20});
        IntKey b = new IntKey(new int[] {10, 20});

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void compareToOrdersByContent()
    {
        IntKey small = new IntKey(new int[] {1});
        IntKey same = new IntKey(new int[] {1});
        IntKey large = new IntKey(new int[] {2});

        assertEquals(0, small.compareTo(same));
        assertTrue(small.compareTo(large) < 0);
        assertTrue(large.compareTo(small) > 0);
    }

    @Test
    void worksAsHashMapKey()
    {
        Map<IntKey, String> map = new HashMap<>();
        map.put(new IntKey(new int[] {100, 200}), "value");

        assertEquals("value", map.get(new IntKey(new int[] {100, 200})));
        assertNull(map.get(new IntKey(new int[] {100, 300})));
    }

    @Test
    void getIntsReturnsACopy()
    {
        int[] data = new int[] {9};
        IntKey key = new IntKey(data);
        int[] copy = key.getInts();

        assertNotSame(data, copy);
        copy[0] = 42;
        assertArrayEquals(new int[] {9}, key.getInts());
    }
}
