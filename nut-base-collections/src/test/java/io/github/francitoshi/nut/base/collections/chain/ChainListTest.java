/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.collections.chain;

import io.github.francitoshi.nut.base.collections.chain.ChainList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ChainListTest
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

    private static ChainList<String> chain()
    {
        return new ChainList<>(list("a","b"), list(), list("c","d"));
    }

    @Test
    public void testConstructorEmpty()
    {
        assertThrows(IllegalArgumentException.class, () -> new ChainList<String>());
    }

    @Test
    public void testConstructorNullElement()
    {
        List<String> l = list("a");
        assertThrows(NullPointerException.class, () -> new ChainList<>(l, null));
    }

    @Test
    public void testLists()
    {
        ChainList<String> instance = chain();

        assertEquals(3, instance.lists().size());
        assertEquals("[a, b]", instance.first().toString());
        assertEquals("[c, d]", instance.last().toString());
        assertThrows(UnsupportedOperationException.class, () -> instance.lists().clear());
    }

    @Test
    public void testGetAcrossLists()
    {
        ChainList<String> instance = chain();

        assertEquals("a", instance.get(0));
        assertEquals("b", instance.get(1));
        assertEquals("c", instance.get(2));
        assertEquals("d", instance.get(3));
    }

    @Test
    public void testGetNegative()
    {
        ChainList<String> instance = chain();

        assertThrows(IndexOutOfBoundsException.class, () -> instance.get(-1));
    }

    @Test
    public void testGetOutOfBounds()
    {
        ChainList<String> instance = chain();

        assertThrows(IndexOutOfBoundsException.class, () -> instance.get(4));
    }

    @Test
    public void testSize()
    {
        ChainList<String> instance = chain();

        assertEquals(4, instance.size());
        assertFalse(instance.isEmpty());

        instance.clear();

        assertEquals(0, instance.size());
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testLiveView()
    {
        List<String> middle = list();
        ChainList<String> instance = new ChainList<>(list("a","b"), middle, list("c"));
        assertEquals(3, instance.size());

        middle.add("x");

        assertEquals(4, instance.size());
        assertEquals("x", instance.get(2));
        assertEquals("c", instance.get(3));

        middle.add("y");

        assertEquals(5, instance.size());
        assertEquals("[a, b, x, y, c]", instance.toString());
    }

    @Test
    public void testIsEmpty()
    {
        ChainList<String> instance = new ChainList<>(list(), list());

        assertTrue(instance.isEmpty());
        assertEquals(0, instance.size());
    }

    @Test
    public void testIndexOf()
    {
        ChainList<String> instance = new ChainList<>(list("a","b"), list("b","c"));

        assertEquals(0, instance.indexOf("a"));
        assertEquals(1, instance.indexOf("b"));
        assertEquals(3, instance.indexOf("c"));
        assertEquals(-1, instance.indexOf("z"));
        assertEquals(-1, instance.indexOf(null));
    }

    @Test
    public void testLastIndexOf()
    {
        ChainList<String> instance = new ChainList<>(list("a","b"), list("b","c"));

        assertEquals(2, instance.lastIndexOf("b"));
        assertEquals(0, instance.lastIndexOf("a"));
        assertEquals(3, instance.lastIndexOf("c"));
        assertEquals(-1, instance.lastIndexOf("z"));
    }

    @Test
    public void testContains()
    {
        ChainList<String> instance = chain();

        assertTrue(instance.contains("a"));
        assertTrue(instance.contains("d"));
        assertFalse(instance.contains("z"));

        Collection<String> all = list("a","c");
        assertTrue(instance.containsAll(all));

        all.add("z");
        assertFalse(instance.containsAll(all));
    }

    @Test
    public void testSet()
    {
        List<String> first = list("a","b");
        List<String> last = list("c","d");
        ChainList<String> instance = new ChainList<>(first, list(), last);

        assertEquals("c", instance.set(2,"C"));

        assertEquals("[C, d]", last.toString());
        assertEquals("C", instance.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> instance.set(-1,"x"));
        assertThrows(IndexOutOfBoundsException.class, () -> instance.set(4,"x"));
    }

    @Test
    public void testRemoveByIndex()
    {
        List<String> last = list("c","d");
        ChainList<String> instance = new ChainList<>(list("a","b"), list(), last);

        assertEquals("c", instance.remove(2));

        assertEquals("[d]", last.toString());
        assertEquals("[a, b, d]", instance.toString());
        assertEquals(3, instance.size());
        assertThrows(IndexOutOfBoundsException.class, () -> instance.remove(3));
    }

    @Test
    public void testRemoveByObject()
    {
        List<String> first = list("a","b");
        ChainList<String> instance = new ChainList<>(first, list("b","c"));

        assertTrue(instance.remove("b"));

        assertEquals("[a]", first.toString());
        assertEquals("[a, b, c]", instance.toString());
        assertFalse(instance.remove("z"));
    }

    @Test
    public void testAddAppendsToLastList()
    {
        List<String> last = list("c","d");
        ChainList<String> instance = new ChainList<>(list("a","b"), list(), last);

        assertTrue(instance.add("e"));

        assertEquals("[c, d, e]", last.toString());
        assertEquals("[a, b, c, d, e]", instance.toString());
        assertEquals("e", instance.get(4));
    }

    @Test
    public void testAddAtIndex()
    {
        List<String> first = list("a","b");
        List<String> last = list("c","d");
        ChainList<String> instance = new ChainList<>(first, list(), last);

        instance.add(1,"x");
        assertEquals("[a, x, b]", first.toString());
        assertEquals("[a, x, b, c, d]", instance.toString());

        instance.add(3,"y");
        assertEquals("[y, c, d]", last.toString());
        assertEquals("[a, x, b, y, c, d]", instance.toString());

        instance.add(6,"z");
        assertEquals("[y, c, d, z]", last.toString());
        assertEquals(7, instance.size());
    }

    @Test
    public void testAddAtIndexOutOfBounds()
    {
        ChainList<String> instance = chain();

        assertThrows(IndexOutOfBoundsException.class, () -> instance.add(-1,"x"));
        assertThrows(IndexOutOfBoundsException.class, () -> instance.add(5,"x"));
    }

    @Test
    public void testAddAll()
    {
        List<String> last = list("c");
        ChainList<String> instance = new ChainList<>(list("a"), list(), last);

        assertTrue(instance.addAll(list("d","e")));

        assertEquals("[c, d, e]", last.toString());
        assertEquals("[a, c, d, e]", instance.toString());
        assertFalse(instance.addAll(list()));
    }

    @Test
    public void testAddAllAtIndex()
    {
        List<String> first = list("a","b");
        ChainList<String> instance = new ChainList<>(first, list("c"));

        assertTrue(instance.addAll(1, list("x","y")));

        assertEquals("[a, x, y, b]", first.toString());
        assertEquals("[a, x, y, b, c]", instance.toString());

        assertTrue(instance.addAll(5, list("z")));
        assertEquals("[a, x, y, b, c, z]", instance.toString());

        assertThrows(IndexOutOfBoundsException.class, () -> instance.addAll(-1, list("w")));
        assertThrows(IndexOutOfBoundsException.class, () -> instance.addAll(7, list("w")));
    }

    @Test
    public void testRemoveAll()
    {
        List<String> first = list("a","b");
        List<String> last = list("c","d");
        ChainList<String> instance = new ChainList<>(first, list(), last);

        assertTrue(instance.removeAll(list("b","c")));

        assertEquals("[a]", first.toString());
        assertEquals("[d]", last.toString());
        assertEquals("[a, d]", instance.toString());
        assertFalse(instance.removeAll(list("b","c")));
    }

    @Test
    public void testRetainAll()
    {
        List<String> first = list("a","b");
        List<String> last = list("c","d");
        ChainList<String> instance = new ChainList<>(first, list(), last);

        assertTrue(instance.retainAll(list("a","d")));

        assertEquals("[a]", first.toString());
        assertEquals("[d]", last.toString());
        assertEquals("[a, d]", instance.toString());
        assertFalse(instance.retainAll(list("a","d")));
    }

    @Test
    public void testClear()
    {
        List<String> first = list("a","b");
        List<String> last = list("c");
        ChainList<String> instance = new ChainList<>(first, list(), last);

        instance.clear();

        assertTrue(first.isEmpty());
        assertTrue(last.isEmpty());
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testIterator()
    {
        ChainList<String> instance = chain();

        Iterator<String> it = instance.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertEquals("d", it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testIteratorRemove()
    {
        List<String> first = list("a","b");
        ChainList<String> instance = new ChainList<>(first, list("b","c"));

        Iterator<String> it = instance.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        it.remove();

        assertEquals("[a]", first.toString());
        assertEquals("[a, b, c]", instance.toString());
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    public void testListIteratorBidirectional()
    {
        ChainList<String> instance = chain();

        ListIterator<String> it = instance.listIterator();
        assertEquals(0, it.nextIndex());
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals(1, it.previousIndex());
        assertEquals("b", it.previous());
        assertEquals("a", it.previous());
        assertFalse(it.hasPrevious());
        assertThrows(NoSuchElementException.class, it::previous);

        ListIterator<String> tail = instance.listIterator(3);
        assertEquals("d", tail.next());
        assertFalse(tail.hasNext());
        assertEquals("d", tail.previous());
        assertEquals(2, tail.previousIndex());
        assertEquals("c", tail.previous());
        assertEquals(1, tail.previousIndex());
    }

    @Test
    public void testListIteratorSetRemoveAdd()
    {
        List<String> last = list("c","d");
        ChainList<String> instance = new ChainList<>(list("a"), list(), last);

        ListIterator<String> it = instance.listIterator();
        it.next();
        it.set("A");
        assertEquals("[A]", instance.first().toString());

        it.next();
        it.remove();
        assertEquals("[A, d]", instance.toString());

        it.add("x");
        assertEquals("[A, x, d]", instance.toString());

        assertThrows(IllegalStateException.class, () -> it.set("y"));
        assertEquals("x", it.previous());
        assertEquals("[A, x, d]", instance.toString());
    }

    @Test
    public void testListIteratorInitialState()
    {
        ChainList<String> instance = chain();

        ListIterator<String> it = instance.listIterator();
        assertThrows(IllegalStateException.class, it::remove);
        assertThrows(IllegalStateException.class, () -> it.set("x"));
    }

    @Test
    public void testListIteratorBounds()
    {
        ChainList<String> instance = chain();

        assertThrows(IndexOutOfBoundsException.class, () -> instance.listIterator(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> instance.listIterator(5));
    }

    @Test
    public void testSubList()
    {
        ChainList<String> instance = chain();

        List<String> sub = instance.subList(1,4);

        assertEquals(3, sub.size());
        assertEquals("[b, c, d]", sub.toString());
        assertEquals("c", sub.get(1));

        instance.set(2,"C");
        assertEquals("[b, C, d]", sub.toString());

        sub.set(0,"B");
        assertEquals("B", instance.get(1));
    }

    @Test
    public void testSubListEmptyRange()
    {
        ChainList<String> instance = chain();

        assertEquals("[]", instance.subList(0,0).toString());
        assertEquals("[]", instance.subList(2,2).toString());
        assertEquals("[]", instance.subList(4,4).toString());
    }

    @Test
    public void testSubListBounds()
    {
        ChainList<String> instance = chain();

        assertThrows(IndexOutOfBoundsException.class, () -> instance.subList(-1,2));
        assertThrows(IndexOutOfBoundsException.class, () -> instance.subList(0,5));
        assertThrows(IllegalArgumentException.class, () -> instance.subList(3,1));
    }

    @Test
    public void testToArray()
    {
        ChainList<String> instance = chain();

        Object[] array = instance.toArray();
        assertEquals(4, array.length);
        assertEquals("a", array[0]);
        assertEquals("d", array[3]);

        String[] typed = instance.toArray(new String[0]);
        assertEquals(4, typed.length);
        assertEquals("c", typed[2]);

        String[] big = instance.toArray(new String[8]);
        assertEquals(8, big.length);
        assertNull(big[4]);
    }

    @Test
    public void testReplaceAll()
    {
        ChainList<String> instance = chain();

        instance.replaceAll(String::toUpperCase);

        assertEquals("[A, B, C, D]", instance.toString());
        assertEquals("[A, B]", instance.first().toString());
        assertEquals("[C, D]", instance.last().toString());
    }

    @Test
    public void testEqualsHashCode()
    {
        ChainList<String> instance = chain();

        List<String> other = list("a","b","c","d");

        assertEquals(other, instance);
        assertEquals(instance, other);
        assertEquals(other.hashCode(), instance.hashCode());

        List<String> different = list("a","b");
        assertNotEquals(different, instance);
        assertNotEquals(instance, different);
        assertNotEquals(instance, "not a list");
    }

    @Test
    public void testToString()
    {
        ChainList<String> instance = chain();

        assertEquals("[a, b, c, d]", instance.toString());
    }
}
