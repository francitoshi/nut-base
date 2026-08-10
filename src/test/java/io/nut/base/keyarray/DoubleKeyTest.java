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

class DoubleKeyTest
{
    @Test
    void equalsComparesContentInsteadOfArrayReference()
    {
        DoubleKey a = new DoubleKey(new double[] {1.5, 2.5});
        DoubleKey b = new DoubleKey(new double[] {1.5, 2.5});
        DoubleKey c = new DoubleKey(new double[] {1.5, 3.5});

        assertNotSame(a, b);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void equalKeysProduceEqualHashCodes()
    {
        DoubleKey a = new DoubleKey(new double[] {0.1});
        DoubleKey b = new DoubleKey(new double[] {0.1});

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void compareToOrdersByContent()
    {
        DoubleKey small = new DoubleKey(new double[] {1.0});
        DoubleKey same = new DoubleKey(new double[] {1.0});
        DoubleKey large = new DoubleKey(new double[] {2.0});

        assertEquals(0, small.compareTo(same));
        assertTrue(small.compareTo(large) < 0);
        assertTrue(large.compareTo(small) > 0);
    }

    @Test
    void worksAsHashMapKey()
    {
        Map<DoubleKey, String> map = new HashMap<>();
        map.put(new DoubleKey(new double[] {1.5, 2.5}), "value");

        assertEquals("value", map.get(new DoubleKey(new double[] {1.5, 2.5})));
        assertNull(map.get(new DoubleKey(new double[] {1.5, 3.5})));
    }

    @Test
    void getDoublesReturnsACopy()
    {
        double[] data = new double[] {9.0};
        DoubleKey key = new DoubleKey(data);
        double[] copy = key.getDoubles();

        assertNotSame(data, copy);
        copy[0] = 42.0;
        assertArrayEquals(new double[] {9.0}, key.getDoubles());
    }
}
