/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class EmptyTest
{
    @Test
    public void testEmptyArrays()
    {
        assertEquals(0, Empty.BYTES.length);
        assertEquals(0, Empty.INTS.length);
        assertEquals(0, Empty.LONGS.length);
        assertEquals(0, Empty.CHARS.length);
        assertEquals(0, Empty.BOOLEANS.length);
        assertEquals(0, Empty.SHORTS.length);
        assertEquals(0, Empty.FLOATS.length);
        assertEquals(0, Empty.DOUBLES.length);

        assertEquals(0, Empty.OBJECTS.length);
        assertEquals(0, Empty.STRINGS.length);
        assertEquals(0, Empty.CLASSES.length);
        assertEquals(0, Empty.THROWABLES.length);
    }

    @Test
    public void testEmptyCollections()
    {
        assertTrue(Empty.COLLECTION.isEmpty());
        assertFalse(Empty.ITERABLE.iterator().hasNext());
        assertFalse(Empty.ITERATOR.hasNext());

        assertTrue(Empty.LIST.isEmpty());
        assertTrue(Empty.SET.isEmpty());
        assertTrue(Empty.MAP.isEmpty());

        assertTrue(Empty.SORTED_SET.isEmpty());
        assertTrue(Empty.SORTED_MAP.isEmpty());
        assertTrue(Empty.NAVIGABLE_SET.isEmpty());
        assertTrue(Empty.NAVIGABLE_MAP.isEmpty());

        assertTrue(Empty.QUEUE.isEmpty());
        assertTrue(Empty.DEQUE.isEmpty());
    }

    @Test
    public void testCollectionsAreImmutable()
    {
        assertThrows(UnsupportedOperationException.class, () -> ((List<String>) Empty.LIST).add("x"));
        assertThrows(UnsupportedOperationException.class, () -> ((Set<String>) Empty.SET).add("x"));
        assertThrows(UnsupportedOperationException.class, () -> ((Map<String, String>) Empty.MAP).put("k", "v"));
        assertThrows(UnsupportedOperationException.class, () -> ((SortedSet<String>) Empty.SORTED_SET).add("x"));
        assertThrows(UnsupportedOperationException.class, () -> ((SortedMap<String, String>) Empty.SORTED_MAP).put("k", "v"));
        assertThrows(UnsupportedOperationException.class, () -> ((NavigableSet<String>) Empty.NAVIGABLE_SET).add("x"));
        assertThrows(UnsupportedOperationException.class, () -> ((NavigableMap<String, String>) Empty.NAVIGABLE_MAP).put("k", "v"));
        assertThrows(UnsupportedOperationException.class, () -> ((Queue<String>) Empty.QUEUE).add("x"));
        assertThrows(UnsupportedOperationException.class, () -> ((Deque<String>) Empty.DEQUE).addFirst("x"));
        assertThrows(UnsupportedOperationException.class, () -> ((Deque<String>) Empty.DEQUE).addLast("x"));
    }

    @Test
    public void testCollectionsAreEmpty()
    {
        assertEquals(0, ((Collection<?>) Empty.LIST).size());
        assertEquals(0, Empty.SET.size());
        assertEquals(0, Empty.MAP.size());
        assertEquals(0, Empty.SORTED_SET.size());
        assertEquals(0, Empty.SORTED_MAP.size());
        assertEquals(0, Empty.NAVIGABLE_SET.size());
        assertEquals(0, Empty.NAVIGABLE_MAP.size());
        assertEquals(0, Empty.QUEUE.size());
        assertEquals(0, Empty.DEQUE.size());
        assertFalse(Empty.DEQUE.descendingIterator().hasNext());
    }
}