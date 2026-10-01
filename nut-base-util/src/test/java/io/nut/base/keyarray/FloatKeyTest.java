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

class FloatKeyTest
{
    @Test
    void equalsComparesContentInsteadOfArrayReference()
    {
        FloatKey a = new FloatKey(new float[] {1.5f, 2.5f});
        FloatKey b = new FloatKey(new float[] {1.5f, 2.5f});
        FloatKey c = new FloatKey(new float[] {1.5f, 3.5f});

        assertNotSame(a, b);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void equalKeysProduceEqualHashCodes()
    {
        FloatKey a = new FloatKey(new float[] {0.1f});
        FloatKey b = new FloatKey(new float[] {0.1f});

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void compareToOrdersByContent()
    {
        FloatKey small = new FloatKey(new float[] {1.0f});
        FloatKey same = new FloatKey(new float[] {1.0f});
        FloatKey large = new FloatKey(new float[] {2.0f});

        assertEquals(0, small.compareTo(same));
        assertTrue(small.compareTo(large) < 0);
        assertTrue(large.compareTo(small) > 0);
    }

    @Test
    void worksAsHashMapKey()
    {
        Map<FloatKey, String> map = new HashMap<>();
        map.put(new FloatKey(new float[] {1.5f, 2.5f}), "value");

        assertEquals("value", map.get(new FloatKey(new float[] {1.5f, 2.5f})));
        assertNull(map.get(new FloatKey(new float[] {1.5f, 3.5f})));
    }

    @Test
    void getFloatsReturnsACopy()
    {
        float[] data = new float[] {9.0f};
        FloatKey key = new FloatKey(data);
        float[] copy = key.getFloats();

        assertNotSame(data, copy);
        copy[0] = 42.0f;
        assertArrayEquals(new float[] {9.0f}, key.getFloats());
    }
}
