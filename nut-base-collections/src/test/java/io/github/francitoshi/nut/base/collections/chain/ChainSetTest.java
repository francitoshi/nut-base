/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.collections.chain;

import io.github.francitoshi.nut.base.collections.chain.ChainSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ChainSetTest
{
    private static ChainSet<String> chain()
    {
        Set<String> high = new LinkedHashSet<>();
        high.add("a");
        high.add("b");
        Set<String> low = new LinkedHashSet<>();
        low.add("b");
        low.add("c");
        return new ChainSet<>(high, low);
    }

    @Test
    public void testConstructorEmpty()
    {
        assertThrows(IllegalArgumentException.class, () -> new ChainSet<String>());
    }

    @Test
    public void testConstructorNullElement()
    {
        Set<String> s = new HashSet<>();
        assertThrows(NullPointerException.class, () -> new ChainSet<>(s, null));
    }

    @Test
    public void testSets()
    {
        ChainSet<String> instance = chain();

        assertEquals(2, instance.sets().size());
        assertTrue(instance.first().contains("a"));
        assertThrows(UnsupportedOperationException.class, () -> instance.sets().clear());
    }

    @Test
    public void testContains()
    {
        ChainSet<String> instance = chain();

        assertTrue(instance.contains("a"));
        assertTrue(instance.contains("b"));
        assertTrue(instance.contains("c"));
        assertFalse(instance.contains("z"));
        assertFalse(instance.contains(null));
    }

    @Test
    public void testSize()
    {
        ChainSet<String> instance = chain();

        assertEquals(3, instance.size());
        assertFalse(instance.isEmpty());

        instance.clear();

        assertEquals(0, instance.size());
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testAddGoesToFirstSet()
    {
        Set<String> high = new LinkedHashSet<>();
        Set<String> low = new LinkedHashSet<>();
        low.add("c");
        ChainSet<String> instance = new ChainSet<>(high, low);

        assertTrue(instance.add("c"));
        assertTrue(instance.add("d"));

        assertTrue(high.contains("c"));
        assertTrue(high.contains("d"));
        assertTrue(low.contains("c"));
        assertEquals(2, instance.size());
    }

    @Test
    public void testAddShadowsLowerSet()
    {
        Set<String> high = new LinkedHashSet<>();
        Set<String> low = new LinkedHashSet<>();
        low.add("c");
        ChainSet<String> instance = new ChainSet<>(high, low);

        assertTrue(instance.add("c"));
        assertTrue(high.contains("c"));
        assertTrue(low.contains("c"));
        assertEquals(1, instance.size());
    }

    @Test
    public void testAddAllGoesToFirstSet()
    {
        Set<String> high = new LinkedHashSet<>();
        Set<String> low = new LinkedHashSet<>();
        low.add("c");
        ChainSet<String> instance = new ChainSet<>(high, low);

        Collection<String> extra = new ArrayList<>();
        extra.add("c");
        extra.add("d");

        assertTrue(instance.addAll(extra));

        assertEquals(2, high.size());
        assertTrue(high.contains("c"));
        assertTrue(high.contains("d"));
        assertEquals(1, low.size());
        assertEquals(2, instance.size());
    }

    @Test
    public void testRemoveUnshadows()
    {
        ChainSet<String> instance = chain();

        assertTrue(instance.remove("b"));

        assertTrue(instance.contains("b"));
        assertFalse(instance.first().contains("b"));
        assertEquals(3, instance.size());
    }

    @Test
    public void testRemoveFromLowerSet()
    {
        ChainSet<String> instance = chain();

        assertTrue(instance.remove("c"));

        assertFalse(instance.contains("c"));
        assertEquals(2, instance.size());
    }

    @Test
    public void testRemoveMissing()
    {
        ChainSet<String> instance = chain();

        assertFalse(instance.remove("z"));
        assertEquals(3, instance.size());
    }

    @Test
    public void testClear()
    {
        Set<String> high = new LinkedHashSet<>();
        high.add("a");
        Set<String> low = new LinkedHashSet<>();
        low.add("c");
        ChainSet<String> instance = new ChainSet<>(high, low);

        instance.clear();

        assertTrue(high.isEmpty());
        assertTrue(low.isEmpty());
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testIterator()
    {
        ChainSet<String> instance = chain();

        Iterator<String> it = instance.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testIteratorRemove()
    {
        ChainSet<String> instance = chain();

        Iterator<String> it = instance.iterator();
        assertEquals("a", it.next());
        it.remove();

        assertFalse(instance.contains("a"));
        assertEquals(2, instance.size());
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    public void testIteratorRemovePeelsOneLevel()
    {
        ChainSet<String> instance = chain();

        Iterator<String> it = instance.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        it.remove();

        assertTrue(instance.contains("b"));
        assertFalse(instance.first().contains("b"));
        assertEquals(3, instance.size());
    }

    @Test
    public void testRemoveIf()
    {
        ChainSet<String> instance = chain();

        assertTrue(instance.removeIf(s -> s.equals("a") || s.equals("c")));

        assertFalse(instance.contains("a"));
        assertFalse(instance.contains("c"));
        assertTrue(instance.contains("b"));
        assertEquals("[b]", instance.toString());
    }

    @Test
    public void testRemoveIfPeelsOneLevel()
    {
        ChainSet<String> instance = chain();

        assertTrue(instance.removeIf(s -> s.equals("b")));

        assertTrue(instance.contains("b"));
        assertFalse(instance.first().contains("b"));
        assertEquals(3, instance.size());
    }

    @Test
    public void testContainsAll()
    {
        ChainSet<String> instance = chain();

        Collection<String> some = new ArrayList<>();
        some.add("a");
        some.add("c");
        assertTrue(instance.containsAll(some));

        some.add("z");
        assertFalse(instance.containsAll(some));
    }

    @Test
    public void testRemoveAll()
    {
        ChainSet<String> instance = chain();

        Collection<String> toRemove = new ArrayList<>();
        toRemove.add("a");
        toRemove.add("c");

        assertTrue(instance.removeAll(toRemove));
        assertFalse(instance.contains("a"));
        assertFalse(instance.contains("c"));
        assertEquals("[b]", instance.toString());
        assertFalse(instance.removeAll(toRemove));
    }

    @Test
    public void testRemoveAllPeelsOneLevel()
    {
        ChainSet<String> instance = chain();

        Collection<String> toRemove = new ArrayList<>();
        toRemove.add("b");

        assertTrue(instance.removeAll(toRemove));
        assertTrue(instance.contains("b"));
        assertFalse(instance.first().contains("b"));
        assertEquals(3, instance.size());
    }

    @Test
    public void testRetainAll()
    {
        ChainSet<String> instance = chain();

        Collection<String> toKeep = new ArrayList<>();
        toKeep.add("a");
        toKeep.add("b");

        assertTrue(instance.retainAll(toKeep));
        assertEquals("[a, b]", instance.toString());
        assertFalse(instance.retainAll(toKeep));
    }

    @Test
    public void testToArray()
    {
        ChainSet<String> instance = chain();

        Object[] array = instance.toArray();
        assertEquals(3, array.length);
        assertEquals("a", array[0]);

        String[] typed = instance.toArray(new String[0]);
        assertEquals(3, typed.length);
        assertEquals("c", typed[2]);

        String[] big = instance.toArray(new String[8]);
        assertEquals(8, big.length);
        assertNull(big[3]);
    }

    @Test
    public void testForEach()
    {
        ChainSet<String> instance = chain();

        HashSet<String> seen = new HashSet<>();
        instance.forEach(seen::add);
        assertEquals(3, seen.size());
        assertTrue(seen.contains("b"));
    }

    @Test
    public void testIsLive()
    {
        ChainSet<String> instance = chain();

        assertEquals(3, instance.size());

        instance.first().add("z");

        assertEquals(4, instance.size());
        assertTrue(instance.contains("z"));
    }

    @Test
    public void testEqualsHashCode()
    {
        ChainSet<String> instance = chain();

        Set<String> other = new HashSet<>();
        other.add("a");
        other.add("b");
        other.add("c");

        assertEquals(other, instance);
        assertEquals(instance, other);
        assertEquals(other.hashCode(), instance.hashCode());

        Set<String> different = new HashSet<>();
        different.add("a");
        assertNotEquals(different, instance);
        assertNotEquals(instance, different);
        assertNotEquals(instance, "not a set");
    }

    @Test
    public void testToString()
    {
        ChainSet<String> instance = chain();

        assertEquals("[a, b, c]", instance.toString());
    }
}
