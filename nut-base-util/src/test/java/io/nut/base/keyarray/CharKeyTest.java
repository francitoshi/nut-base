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

class CharKeyTest
{
    @Test
    void equalsComparesContentInsteadOfArrayReference()
    {
        CharKey a = new CharKey(new char[] {'a', 'b'});
        CharKey b = new CharKey(new char[] {'a', 'b'});
        CharKey c = new CharKey(new char[] {'a', 'c'});

        assertNotSame(a, b);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void equalKeysProduceEqualHashCodes()
    {
        CharKey a = new CharKey(new char[] {'x', 'y'});
        CharKey b = new CharKey(new char[] {'x', 'y'});

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void compareToOrdersByContent()
    {
        CharKey small = new CharKey(new char[] {'a'});
        CharKey same = new CharKey(new char[] {'a'});
        CharKey large = new CharKey(new char[] {'b'});

        assertEquals(0, small.compareTo(same));
        assertTrue(small.compareTo(large) < 0);
        assertTrue(large.compareTo(small) > 0);
    }

    @Test
    void worksAsHashMapKey()
    {
        Map<CharKey, String> map = new HashMap<>();
        map.put(new CharKey(new char[] {'h', 'i'}), "value");

        assertEquals("value", map.get(new CharKey(new char[] {'h', 'i'})));
        assertNull(map.get(new CharKey(new char[] {'h', 'o'})));
    }

    @Test
    void stringConstructorUsesCharacters()
    {
        CharKey key = new CharKey("ab");

        assertArrayEquals(new char[] {'a', 'b'}, key.getChars());
        assertEquals("ab", key.toString());
    }

    @Test
    void getCharsReturnsACopy()
    {
        char[] data = new char[] {'z'};
        CharKey key = new CharKey(data);
        char[] copy = key.getChars();

        assertNotSame(data, copy);
        copy[0] = 'a';
        assertArrayEquals(new char[] {'z'}, key.getChars());
    }
}
