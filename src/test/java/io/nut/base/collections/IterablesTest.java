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

        assertThrows(NullPointerException.class, () -> Iterables.any(null, x -> true));
        assertThrows(NullPointerException.class, () -> Iterables.any(Collections.emptyList(), null));

        assertThrows(NullPointerException.class, () -> Iterables.all(null, x -> true));
        assertThrows(NullPointerException.class, () -> Iterables.all(Collections.emptyList(), null));

        assertThrows(NullPointerException.class, () -> Iterables.none(null, x -> true));
        assertThrows(NullPointerException.class, () -> Iterables.none(Collections.emptyList(), null));

        assertThrows(NullPointerException.class, () -> Iterables.count(null));
        assertThrows(NullPointerException.class, () -> Iterables.count(null, x -> true));
        assertThrows(NullPointerException.class, () -> Iterables.count(Collections.emptyList(), (java.util.function.Predicate<Object>) null));

        assertThrows(NullPointerException.class, () -> Iterables.concat((Iterable<Object>[]) null));
        assertThrows(NullPointerException.class, () -> Iterables.concat(Collections.emptyList(), null));

        assertThrows(NullPointerException.class, () -> Iterables.filter(null, x -> true));
        assertThrows(NullPointerException.class, () -> Iterables.filter(Collections.emptyList(), null));
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

    @Test
    @DisplayName("any returns true if at least one element satisfies predicate")
    void testAny()
    {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        assertTrue(Iterables.any(list, s -> s.startsWith("b")));
        assertFalse(Iterables.any(list, s -> s.startsWith("z")));
        assertFalse(Iterables.any(Collections.emptyList(), s -> true));
    }

    @Test
    @DisplayName("all returns true if all elements satisfy predicate")
    void testAll()
    {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        assertTrue(Iterables.all(list, s -> s.length() >= 5));
        assertFalse(Iterables.all(list, s -> s.startsWith("a")));
        assertTrue(Iterables.all(Collections.emptyList(), s -> false));
    }

    @Test
    @DisplayName("none returns true if no elements satisfy predicate")
    void testNone()
    {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        assertTrue(Iterables.none(list, s -> s.startsWith("z")));
        assertFalse(Iterables.none(list, s -> s.startsWith("a")));
        assertTrue(Iterables.none(Collections.emptyList(), s -> true));
    }

    @Test
    @DisplayName("count returns correct element count")
    void testCount()
    {
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        assertEquals(3, Iterables.count(list));
        assertEquals(2, Iterables.count(list, s -> s.length() == 6));
        assertEquals(0, Iterables.count(list, s -> s.startsWith("z")));

        // Test non-Collection Iterable
        Iterable<String> iterable = () -> list.iterator();
        assertEquals(3, Iterables.count(iterable));
        assertEquals(2, Iterables.count(iterable, s -> s.length() == 6));

        assertEquals(0, Iterables.count(Collections.emptyList()));
        assertEquals(0, Iterables.count(Collections.emptyList(), s -> true));
    }

    @Test
    @DisplayName("concat combines multiple iterables lazily")
    void testConcat()
    {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        List<Integer> list3 = Collections.singletonList(5);

        Iterable<Integer> concatenated = Iterables.concat(list1, list2, list3);

        Iterator<Integer> iterator = concatenated.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(1), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(2), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(3), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(4), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(5), iterator.next());
        assertFalse(iterator.hasNext());

        // Test with empty iterables
        Iterable<Integer> concatWithEmpty = Iterables.concat(
            Collections.emptyList(),
            Arrays.asList(1, 2),
            Collections.emptyList(),
            Collections.singletonList(3)
        );
        Iterator<Integer> emptyIterator = concatWithEmpty.iterator();
        assertTrue(emptyIterator.hasNext());
        assertEquals(Integer.valueOf(1), emptyIterator.next());
        assertTrue(emptyIterator.hasNext());
        assertEquals(Integer.valueOf(2), emptyIterator.next());
        assertTrue(emptyIterator.hasNext());
        assertEquals(Integer.valueOf(3), emptyIterator.next());
        assertFalse(emptyIterator.hasNext());
    }

    @Test
    @DisplayName("filter lazily matches elements according to predicate")
    void testFilter()
    {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "apricot");
        Iterable<String> filtered = Iterables.filter(list, s -> s.startsWith("a"));

        Iterator<String> iterator = filtered.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("apple", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("apricot", iterator.next());
        assertFalse(iterator.hasNext());

        // Test with no matches
        Iterable<String> noMatches = Iterables.filter(list, s -> s.startsWith("z"));
        assertFalse(noMatches.iterator().hasNext());

        // Test with empty collection
        Iterable<String> emptyFilter = Iterables.filter(Collections.emptyList(), s -> true);
        assertFalse(emptyFilter.iterator().hasNext());
    }
}
