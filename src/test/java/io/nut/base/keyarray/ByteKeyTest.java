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

class ByteKeyTest
{
    @Test
    void equalsComparesContentInsteadOfArrayReference()
    {
        ByteKey a = new ByteKey(new byte[] {1, 2, 3});
        ByteKey b = new ByteKey(new byte[] {1, 2, 3});
        ByteKey c = new ByteKey(new byte[] {1, 2, 4});

        assertNotSame(a, b);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void equalKeysProduceEqualHashCodes()
    {
        ByteKey a = new ByteKey(new byte[] {5, 6});
        ByteKey b = new ByteKey(new byte[] {5, 6});

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void compareToOrdersByContent()
    {
        ByteKey small = new ByteKey(new byte[] {1});
        ByteKey same = new ByteKey(new byte[] {1});
        ByteKey large = new ByteKey(new byte[] {2});

        assertEquals(0, small.compareTo(same));
        assertTrue(small.compareTo(large) < 0);
        assertTrue(large.compareTo(small) > 0);
    }

    @Test
    void worksAsHashMapKey()
    {
        Map<ByteKey, String> map = new HashMap<>();
        map.put(new ByteKey(new byte[] {1, 2}), "value");

        assertEquals("value", map.get(new ByteKey(new byte[] {1, 2})));
        assertNull(map.get(new ByteKey(new byte[] {1, 3})));
    }

    @Test
    void stringConstructorDecodesHex()
    {
        ByteKey key = new ByteKey("0102ff");

        assertArrayEquals(new byte[] {1, 2, (byte) 0xff}, key.getBytes());
        assertEquals("0102ff", key.toString());
    }

    @Test
    void getBytesReturnsACopy()
    {
        byte[] data = new byte[] {9};
        ByteKey key = new ByteKey(data);
        byte[] copy = key.getBytes();

        assertNotSame(data, copy);
        copy[0] = 42;
        assertArrayEquals(new byte[] {9}, key.getBytes());
    }
}
