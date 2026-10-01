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

class LongKeyTest
{
    @Test
    void equalsComparesContentInsteadOfArrayReference()
    {
        LongKey a = new LongKey(new long[] {1L, 2L});
        LongKey b = new LongKey(new long[] {1L, 2L});
        LongKey c = new LongKey(new long[] {1L, 3L});

        assertNotSame(a, b);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void equalKeysProduceEqualHashCodes()
    {
        LongKey a = new LongKey(new long[] {5L, 6L});
        LongKey b = new LongKey(new long[] {5L, 6L});

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void compareToOrdersByContent()
    {
        LongKey small = new LongKey(new long[] {1L});
        LongKey same = new LongKey(new long[] {1L});
        LongKey large = new LongKey(new long[] {2L});

        assertEquals(0, small.compareTo(same));
        assertTrue(small.compareTo(large) < 0);
        assertTrue(large.compareTo(small) > 0);
    }

    @Test
    void worksAsHashMapKey()
    {
        Map<LongKey, String> map = new HashMap<>();
        map.put(new LongKey(new long[] {1L, 2L}), "value");

        assertEquals("value", map.get(new LongKey(new long[] {1L, 2L})));
        assertNull(map.get(new LongKey(new long[] {1L, 3L})));
    }

    @Test
    void getLongsReturnsACopy()
    {
        long[] data = new long[] {9L};
        LongKey key = new LongKey(data);
        long[] copy = key.getLongs();

        assertNotSame(data, copy);
        copy[0] = 42L;
        assertArrayEquals(new long[] {9L}, key.getLongs());
    }
}
