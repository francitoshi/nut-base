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

class BooleanKeyTest
{
    @Test
    void equalsComparesContentInsteadOfArrayReference()
    {
        BooleanKey a = new BooleanKey(new boolean[] {true, false});
        BooleanKey b = new BooleanKey(new boolean[] {true, false});
        BooleanKey c = new BooleanKey(new boolean[] {true, true});

        assertNotSame(a, b);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void equalKeysProduceEqualHashCodes()
    {
        BooleanKey a = new BooleanKey(new boolean[] {false, true});
        BooleanKey b = new BooleanKey(new boolean[] {false, true});

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void compareToOrdersByContent()
    {
        BooleanKey small = new BooleanKey(new boolean[] {false});
        BooleanKey same = new BooleanKey(new boolean[] {false});
        BooleanKey large = new BooleanKey(new boolean[] {true});

        assertEquals(0, small.compareTo(same));
        assertTrue(small.compareTo(large) < 0);
        assertTrue(large.compareTo(small) > 0);
    }

    @Test
    void worksAsHashMapKey()
    {
        Map<BooleanKey, String> map = new HashMap<>();
        map.put(new BooleanKey(new boolean[] {true, false}), "value");

        assertEquals("value", map.get(new BooleanKey(new boolean[] {true, false})));
        assertNull(map.get(new BooleanKey(new boolean[] {true, true})));
    }

    @Test
    void stringConstructorParsesCommaSeparatedValues()
    {
        BooleanKey key = new BooleanKey(new boolean[]{true, false});

        assertArrayEquals(new boolean[] {true, false}, key.getBooleans());
    }

    @Test
    void getBooleansReturnsACopy()
    {
        boolean[] data = new boolean[] {true};
        BooleanKey key = new BooleanKey(data);
        boolean[] copy = key.getBooleans();

        assertNotSame(data, copy);
        copy[0] = false;
        assertArrayEquals(new boolean[] {true}, key.getBooleans());
    }
}
