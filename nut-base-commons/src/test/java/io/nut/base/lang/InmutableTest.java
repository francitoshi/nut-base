/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class InmutableTest
{
    @Test
    public void testCollection()
    {
        Collection<String> source = new ArrayList<>(Arrays.asList("a"));
        Collection<String> view = Inmutable.collection(source);

        assertEquals(1, view.size());
        assertTrue(view.contains("a"));
        assertThrows(UnsupportedOperationException.class, () -> view.add("b"));
        assertThrows(UnsupportedOperationException.class, () -> view.clear());
    }

    @Test
    public void testList()
    {
        List<String> view = Inmutable.list(new ArrayList<>(Arrays.asList("a", "b")));

        assertEquals(2, view.size());
        assertEquals("a", view.get(0));
        assertThrows(UnsupportedOperationException.class, () -> view.add("c"));
        assertThrows(UnsupportedOperationException.class, () -> view.set(0, "c"));
        assertThrows(UnsupportedOperationException.class, () -> view.remove(0));
    }

    @Test
    public void testSet()
    {
        Set<String> view = Inmutable.set(new HashSet<>(Arrays.asList("a")));

        assertTrue(view.contains("a"));
        assertThrows(UnsupportedOperationException.class, () -> view.add("b"));
        assertThrows(UnsupportedOperationException.class, () -> view.remove("a"));
    }

    @Test
    public void testSortedSet()
    {
        SortedSet<String> view = Inmutable.sortedSet(new TreeSet<>(Arrays.asList("b", "a")));

        assertEquals("a", view.first());
        assertEquals("b", view.last());
        assertThrows(UnsupportedOperationException.class, () -> view.add("c"));
    }

    @Test
    public void testNavigableSet()
    {
        NavigableSet<String> view = Inmutable.navigableSet(new TreeSet<>(Arrays.asList("a", "c")));

        assertEquals("a", view.floor("b"));
        assertThrows(UnsupportedOperationException.class, () -> view.pollFirst());
    }

    @Test
    public void testMap()
    {
        Map<String, Integer> source = new HashMap<>();
        source.put("k", 1);
        Map<String, Integer> view = Inmutable.map(source);

        assertEquals(Integer.valueOf(1), view.get("k"));
        assertThrows(UnsupportedOperationException.class, () -> view.put("x", 2));
        assertThrows(UnsupportedOperationException.class, () -> view.remove("k"));
    }

    @Test
    public void testSortedMap()
    {
        SortedMap<String, Integer> source = new TreeMap<>();
        source.put("b", 2);
        source.put("a", 1);
        SortedMap<String, Integer> view = Inmutable.sortedMap(source);

        assertEquals("a", view.firstKey());
        assertThrows(UnsupportedOperationException.class, () -> view.put("c", 3));
    }

    @Test
    public void testNavigableMap()
    {
        NavigableMap<String, Integer> source = new TreeMap<>();
        source.put("a", 1);
        source.put("c", 3);
        NavigableMap<String, Integer> view = Inmutable.navigableMap(source);

        assertEquals(Integer.valueOf(1), view.floorEntry("b").getValue());
        assertThrows(UnsupportedOperationException.class, () -> view.pollFirstEntry());
    }

    @Test
    public void testQueue()
    {
        Queue<String> view = Inmutable.queue(new LinkedList<>(Arrays.asList("a", "b")));

        assertEquals(2, view.size());
        assertEquals("a", view.peek());
        assertThrows(UnsupportedOperationException.class, () -> view.add("c"));
        assertThrows(UnsupportedOperationException.class, () -> view.offer("c"));
        assertThrows(UnsupportedOperationException.class, () -> view.poll());
        assertThrows(UnsupportedOperationException.class, () -> view.remove());
        assertThrows(UnsupportedOperationException.class, () -> view.clear());
    }

    @Test
    public void testDeque()
    {
        Deque<String> view = Inmutable.deque(new ArrayDeque<>(Arrays.asList("a", "b")));

        assertEquals("a", view.getFirst());
        assertEquals("b", view.getLast());
        assertEquals("b", view.peekLast());
        assertThrows(UnsupportedOperationException.class, () -> view.addFirst("z"));
        assertThrows(UnsupportedOperationException.class, () -> view.addLast("z"));
        assertThrows(UnsupportedOperationException.class, () -> view.removeFirst());
        assertThrows(UnsupportedOperationException.class, () -> view.removeLast());
        assertThrows(UnsupportedOperationException.class, () -> view.pollFirst());
        assertThrows(UnsupportedOperationException.class, () -> view.pop());
        assertThrows(UnsupportedOperationException.class, () -> view.push("z"));
    }

    @Test
    public void testIterable()
    {
        Iterable<String> view = Inmutable.iterable(Arrays.asList("a", "b"));

        List<String> collected = new ArrayList<>();
        for (String s : view)
        {
            collected.add(s);
        }
        assertEquals(Arrays.asList("a", "b"), collected);
    }

    @Test
    public void testIterator()
    {
        Iterator<String> view = Inmutable.iterator(Arrays.asList("a", "b").iterator());

        assertTrue(view.hasNext());
        assertEquals("a", view.next());
        assertThrows(UnsupportedOperationException.class, view::remove);
    }

    @Test
    public void testViewsAreBackedBySource()
    {
        List<String> source = new ArrayList<>();
        source.add("a");
        List<String> view = Inmutable.list(source);

        source.add("b");
        assertEquals(2, view.size());
    }

    @Test
    public void testNullArguments()
    {
        assertThrows(NullPointerException.class, () -> Inmutable.collection(null));
        assertThrows(NullPointerException.class, () -> Inmutable.list(null));
        assertThrows(NullPointerException.class, () -> Inmutable.set(null));
        assertThrows(NullPointerException.class, () -> Inmutable.map(null));
        assertThrows(NullPointerException.class, () -> Inmutable.queue(null));
        assertThrows(NullPointerException.class, () -> Inmutable.deque(null));
        assertThrows(NullPointerException.class, () -> Inmutable.iterable(null));
        assertThrows(NullPointerException.class, () -> Inmutable.iterator(null));
    }
}
