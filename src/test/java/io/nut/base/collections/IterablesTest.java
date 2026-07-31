/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Iterables")
class IterablesTest
{
    @Test
    @DisplayName("chunked splits collections into correct chunk sizes")
    void testChunked()
    {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

        // Exact chunking
        Iterator<List<Integer>> chunks = Iterables.chunked(list, 2).iterator();
        assertTrue(chunks.hasNext());
        assertEquals(Arrays.asList(1, 2), chunks.next());
        assertTrue(chunks.hasNext());
        assertEquals(Arrays.asList(3, 4), chunks.next());
        assertTrue(chunks.hasNext());
        assertEquals(Collections.singletonList(5), chunks.next());
        assertFalse(chunks.hasNext());

        // Empty source chunking
        assertFalse(Iterables.chunked(Collections.emptyList(), 3).iterator().hasNext());
    }

    @Test
    @DisplayName("chunked produces unmodifiable chunks")
    void testChunkedImmutability()
    {
        Iterable<List<Integer>> chunked = Iterables.chunked(Arrays.asList(1, 2), 2);
        List<Integer> firstChunk = chunked.iterator().next();
        assertThrows(UnsupportedOperationException.class, () -> firstChunk.add(3));
    }

    @Test
    @DisplayName("windowed produces correct sliding windows")
    void testWindowed()
    {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);

        Iterator<List<Integer>> windows = Iterables.windowed(list, 2).iterator();
        assertTrue(windows.hasNext());
        assertEquals(Arrays.asList(1, 2), windows.next());
        assertTrue(windows.hasNext());
        assertEquals(Arrays.asList(2, 3), windows.next());
        assertTrue(windows.hasNext());
        assertEquals(Arrays.asList(3, 4), windows.next());
        assertFalse(windows.hasNext());

        // Window size larger than source size produces empty output
        assertFalse(Iterables.windowed(list, 5).iterator().hasNext());
    }

    @Test
    @DisplayName("windowed produces unmodifiable windows")
    void testWindowedImmutability()
    {
        Iterable<List<Integer>> windowed = Iterables.windowed(Arrays.asList(1, 2, 3), 2);
        List<Integer> firstWindow = windowed.iterator().next();
        assertThrows(UnsupportedOperationException.class, () -> firstWindow.add(4));
    }

    @Test
    @DisplayName("zip combines pairwise elements of two iterables")
    void testZip()
    {
        List<String> first = Arrays.asList("a", "b", "c");
        List<Integer> second = Arrays.asList(1, 2);

        Iterator<String> zipped = Iterables.zip(first, second, (s, i) -> s + i).iterator();
        assertTrue(zipped.hasNext());
        assertEquals("a1", zipped.next());
        assertTrue(zipped.hasNext());
        assertEquals("b2", zipped.next());
        assertFalse(zipped.hasNext());
    }

    @Test
    @DisplayName("validates arguments strictly")
    void testArgumentValidation()
    {
        assertThrows(NullPointerException.class, () -> Iterables.chunked(null, 3));
        assertThrows(IllegalArgumentException.class, () -> Iterables.chunked(Collections.emptyList(), 0));
        assertThrows(IllegalArgumentException.class, () -> Iterables.chunked(Collections.emptyList(), -1));

        assertThrows(NullPointerException.class, () -> Iterables.windowed(null, 3));
        assertThrows(IllegalArgumentException.class, () -> Iterables.windowed(Collections.emptyList(), 0));
        assertThrows(IllegalArgumentException.class, () -> Iterables.windowed(Collections.emptyList(), -1));

        assertThrows(NullPointerException.class, () -> Iterables.zip(null, Collections.emptyList(), (a, b) -> a));
        assertThrows(NullPointerException.class, () -> Iterables.zip(Collections.emptyList(), null, (a, b) -> a));
        assertThrows(NullPointerException.class, () -> Iterables.zip(Collections.emptyList(), Collections.emptyList(), null));

        assertThrows(NullPointerException.class, () -> Iterables.associateBy(null, Object::toString));
        assertThrows(NullPointerException.class, () -> Iterables.associateBy(Collections.emptyList(), null));

        assertThrows(NullPointerException.class, () -> Iterables.associateBy(null, Object::toString, Object::toString));
        assertThrows(NullPointerException.class, () -> Iterables.associateBy(Collections.emptyList(), null, Object::toString));
        assertThrows(NullPointerException.class, () -> Iterables.associateBy(Collections.emptyList(), Object::toString, (java.util.function.Function<Object, Object>) null));

        assertThrows(NullPointerException.class, () -> 
        {
            java.util.function.Supplier<Map<String, Object>> sup = () -> new HashMap<>();
            Iterables.associateBy(null, Object::toString, sup);
        });
        assertThrows(NullPointerException.class, () ->
        {
            java.util.function.Supplier<Map<String, Object>> sup = () -> new HashMap<>();
            Iterables.associateBy(Collections.emptyList(), null, sup);
        });
        assertThrows(NullPointerException.class, () -> 
        {
            java.util.function.Supplier<Map<String, Object>> sup = null;
            Iterables.associateBy(Collections.emptyList(), Object::toString, sup);
        });

        assertThrows(NullPointerException.class, () -> Iterables.associateBy(null, Object::toString, Object::toString, () -> new HashMap<>()));
        assertThrows(NullPointerException.class, () -> Iterables.associateBy(Collections.emptyList(), null, Object::toString, () -> new HashMap<>()));
        assertThrows(NullPointerException.class, () -> Iterables.associateBy(Collections.emptyList(), Object::toString, null, () -> new HashMap<>()));
        assertThrows(NullPointerException.class, () -> Iterables.associateBy(Collections.emptyList(), Object::toString, Object::toString, null));

        assertThrows(NullPointerException.class, () -> Iterables.associateBy(null, Object::toString, (a, b) -> a));
        assertThrows(NullPointerException.class, () -> Iterables.associateBy(Collections.emptyList(), null, (a, b) -> a));
        assertThrows(NullPointerException.class, () -> Iterables.associateBy(Collections.emptyList(), Object::toString, (java.util.function.BinaryOperator<Object>) null));
    }

    @Test
    @DisplayName("associateBy builds maps correctly with various overloads")
    void testAssociateBy()
    {
        List<String> list = Arrays.asList("apple", "banana", "cherry");

        // 1. associateBy(source, keyFunction)
        Map<Integer, String> map1 = Iterables.associateBy(list, String::length);
        assertEquals(2, map1.size());
        assertEquals("apple", map1.get(5));
        assertEquals("cherry", map1.get(6));

        // Duplicate keys: last one wins
        List<String> listWithDuplicates = Arrays.asList("apple", "apricot", "banana");
        Map<Character, String> mapDup = Iterables.associateBy(listWithDuplicates, s -> s.charAt(0));
        assertEquals(2, mapDup.size());
        assertEquals("apricot", mapDup.get('a')); // apricot replaced apple

        // 2. associateBy(source, keyFunction, valueFunction)
        Map<Integer, Character> map2 = Iterables.associateBy(list, String::length, s -> s.charAt(0));
        assertEquals(2, map2.size());
        assertEquals('a', map2.get(5));
        assertEquals('c', map2.get(6));

        // 3. associateBy(source, keyFunction, mapSupplier)
        java.util.function.Supplier<Map<Integer, String>> mapSupplier3 = () -> new TreeMap<>();
        Map<Integer, String> map3 = Iterables.associateBy(list, String::length, mapSupplier3);
        assertTrue(map3 instanceof TreeMap);
        assertEquals("apple", map3.get(5));
        assertEquals("cherry", map3.get(6));

        // 4. associateBy(source, keyFunction, valueFunction, mapSupplier)
        java.util.function.Supplier<Map<Integer, Character>> mapSupplier4 = () -> new TreeMap<>();
        Map<Integer, Character> map4 = Iterables.associateBy(list, String::length, s -> s.charAt(0), mapSupplier4);
        assertTrue(map4 instanceof TreeMap);
        assertEquals('a', map4.get(5));
        assertEquals('c', map4.get(6));

        // 5. associateBy(source, keyMapper, mergeFunction)
        List<String> listForMerge = Arrays.asList("apple", "peach", "banana");
        Map<Integer, String> map5 = Iterables.associateBy(listForMerge, String::length, (s1, s2) -> s1 + "," + s2);
        assertEquals(2, map5.size());
        assertEquals("apple,peach", map5.get(5));
        assertEquals("banana", map5.get(6));
    }
}
