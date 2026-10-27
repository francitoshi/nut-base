/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.collections.chain;

import io.github.francitoshi.nut.base.collections.chain.ChainIterable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ChainIterableTest
{
    private static List<String> list(String... elements)
    {
        List<String> l = new ArrayList<>();
        for(String e : elements)
        {
            l.add(e);
        }
        return l;
    }

    private static <E> List<E> collect(Iterable<E> source)
    {
        ArrayList<E> result = new ArrayList<>();
        for(E e : source)
        {
            result.add(e);
        }
        return result;
    }

    private static ChainIterable<String> chain()
    {
        return new ChainIterable<>(list("a","b"), Collections.<String>emptyList(), list("c"));
    }

    @Test
    public void testConstructorEmpty()
    {
        assertThrows(IllegalArgumentException.class, () -> new ChainIterable<String>());
    }

    @Test
    public void testConstructorEmptyCollection()
    {
        assertThrows(IllegalArgumentException.class, () -> new ChainIterable<String>(Collections.<Iterable<String>>emptyList()));
    }

    @Test
    public void testConstructorNullArray()
    {
        assertThrows(NullPointerException.class, () -> new ChainIterable<String>((Iterable<String>[]) null));
    }

    @Test
    public void testConstructorNullCollection()
    {
        assertThrows(NullPointerException.class, () -> new ChainIterable<String>((Collection<? extends Iterable<String>>) null));
    }

    @Test
    public void testConstructorNullElement()
    {
        assertThrows(NullPointerException.class, () -> new ChainIterable<>(Collections.emptyList(), null));
    }

    @Test
    public void testIterables()
    {
        List<String> first = list("a","b");
        ChainIterable<String> instance = new ChainIterable<>(first, list("c"));

        assertEquals(2, instance.iterables().size());
        assertSame(first, instance.first());
        assertThrows(UnsupportedOperationException.class, () -> instance.iterables().clear());
    }

    @Test
    public void testIteration()
    {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        List<Integer> list3 = Collections.singletonList(5);

        ChainIterable<Integer> instance = new ChainIterable<>(list1, list2, list3);

        Iterator<Integer> iterator = instance.iterator();
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
    }

    @Test
    public void testIterationSkipsEmptySources()
    {
        ChainIterable<Integer> instance = new ChainIterable<>(
            Collections.emptyList(),
            Arrays.asList(1, 2),
            Collections.emptyList(),
            Collections.singletonList(3));

        assertEquals(Arrays.asList(1, 2, 3), collect(instance));
    }

    @Test
    public void testReiteration()
    {
        ChainIterable<String> instance = chain();

        assertEquals("[a, b, c]", collect(instance).toString());
        assertEquals("[a, b, c]", collect(instance).toString());
    }

    @Test
    public void testLiveView()
    {
        List<String> middle = list("b");
        ChainIterable<String> instance = new ChainIterable<>(list("a"), middle, list("c"));
        assertEquals("[a, b, c]", collect(instance).toString());

        middle.add("x");

        assertEquals("[a, b, x, c]", collect(instance).toString());
    }

    @Test
    public void testAllSourcesEmpty()
    {
        ChainIterable<String> instance = new ChainIterable<>(list(), list());

        Iterator<String> it = instance.iterator();
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testNextWithoutHasNext()
    {
        ChainIterable<String> instance = chain();

        Iterator<String> it = instance.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testIteratorRemove()
    {
        List<String> first = list("a","b");
        List<String> last = list("c");
        ChainIterable<String> instance = new ChainIterable<>(first, last);

        Iterator<String> it = instance.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        it.remove();

        assertEquals("[a]", first.toString());
        assertEquals("[a, c]", collect(instance).toString());
    }

    @Test
    public void testIteratorRemoveAcrossSources()
    {
        List<String> first = list("a");
        List<String> last = list("b","c");
        ChainIterable<String> instance = new ChainIterable<>(first, last);

        Iterator<String> it = instance.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        it.remove();

        assertEquals("[c]", last.toString());
        assertEquals("[a, c]", collect(instance).toString());
    }

    @Test
    public void testIteratorRemoveUnsupported()
    {
        ChainIterable<String> instance = new ChainIterable<>(Arrays.asList("x","y"));

        Iterator<String> it = instance.iterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    public void testIteratorRemoveState()
    {
        ChainIterable<String> instance = chain();

        Iterator<String> it = instance.iterator();
        assertThrows(IllegalStateException.class, it::remove);

        it.next();
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);
    }
}
