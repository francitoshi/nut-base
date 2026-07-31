/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PeekableIterator")
class PeekableIteratorTest
{
    @Test
    @DisplayName("constructor validates arguments strictly")
    void testConstructorValidation()
    {
        assertThrows(NullPointerException.class, () -> new PeekableIterator<>(null));
    }

    @Test
    @DisplayName("peek returns next element without consuming it")
    void testPeek()
    {
        List<String> list = Arrays.asList("a", "b", "c");
        PeekableIterator<String> it = new PeekableIterator<>(list.iterator());

        assertTrue(it.hasNext());
        assertEquals("a", it.peek());
        assertEquals("a", it.peek()); // Repeated calls return the same element
        assertTrue(it.hasNext());

        assertEquals("a", it.next()); // Consumed
        assertTrue(it.hasNext());
        assertEquals("b", it.peek());
        assertEquals("b", it.next());

        assertTrue(it.hasNext());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("peek and next throw NoSuchElementException when empty")
    void testEmptyIterator()
    {
        PeekableIterator<String> it = new PeekableIterator<>(Collections.emptyIterator());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::peek);
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    @DisplayName("supports null elements correctly")
    void testNullElements()
    {
        List<String> list = Arrays.asList("a", null, "b");
        PeekableIterator<String> it = new PeekableIterator<>(list.iterator());

        assertTrue(it.hasNext());
        assertEquals("a", it.next());

        assertTrue(it.hasNext());
        assertNull(it.peek()); // peek returned null
        assertTrue(it.hasNext()); // should still have next since it's just a null element
        assertNull(it.next()); // next returns null

        assertTrue(it.hasNext());
        assertEquals("b", it.peek());
        assertEquals("b", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("remove delegates to underlying iterator when no peek is pending")
    void testRemoveSuccess()
    {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        PeekableIterator<String> it = new PeekableIterator<>(list.iterator());

        assertEquals("a", it.next());
        it.remove(); // Removes "a"
        assertEquals(Arrays.asList("b", "c"), list);

        assertEquals("b", it.next());
        assertEquals("c", it.peek());
        // A peek is pending, so remove should fail
        assertThrows(IllegalStateException.class, it::remove);

        assertEquals("c", it.next()); // Consume "c", peek is no longer pending
        it.remove(); // Removes "c"
        assertEquals(Collections.singletonList("b"), list);
    }

    @Test
    @DisplayName("remove throws IllegalStateException if next has not been called")
    void testRemoveBeforeNext()
    {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        PeekableIterator<String> it = new PeekableIterator<>(list.iterator());

        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    @DisplayName("remove throws IllegalStateException if peeked element is pending")
    void testRemoveWhenPeekedPending()
    {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        PeekableIterator<String> it = new PeekableIterator<>(list.iterator());

        assertEquals("a", it.next());
        assertEquals("b", it.peek()); // peeked "b"
        assertThrows(IllegalStateException.class, it::remove); // should throw since "b" is pending
    }

    @Test
    @DisplayName("remove throws UnsupportedOperationException if underlying iterator does not support it")
    void testRemoveUnsupported()
    {
        List<String> list = Collections.unmodifiableList(Arrays.asList("a", "b"));
        PeekableIterator<String> it = new PeekableIterator<>(list.iterator());

        assertEquals("a", it.next());
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    @DisplayName("forEachRemaining processes all remaining elements including peeked one")
    void testForEachRemaining()
    {
        List<String> list = Arrays.asList("a", "b", "c");
        PeekableIterator<String> it = new PeekableIterator<>(list.iterator());

        assertEquals("a", it.peek()); // peek "a"
        List<String> remaining = new ArrayList<>();
        it.forEachRemaining(remaining::add);

        assertEquals(Arrays.asList("a", "b", "c"), remaining);
        assertFalse(it.hasNext());
    }
}
