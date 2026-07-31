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
import java.util.Iterator;
import java.util.List;

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
    }
}
