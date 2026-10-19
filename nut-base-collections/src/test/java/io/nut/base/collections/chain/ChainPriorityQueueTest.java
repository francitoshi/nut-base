/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections.chain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ChainPriorityQueue}.
 *
 * @author franci
 */
public class ChainPriorityQueueTest
{
    private static Queue<String> pq(String... elements)
    {
        return new PriorityQueue<>(Arrays.asList(elements));
    }

    /**
     * A queue that preserves insertion order, for order-sensitive assertions.
     */
    private static Queue<String> fifo(String... elements)
    {
        return new ArrayDeque<>(Arrays.asList(elements));
    }

    private static ChainPriorityQueue<String> chain()
    {
        return new ChainPriorityQueue<>(pq("b", "d"), pq("a", "c", "e"));
    }

    @Test
    public void testConstructorEmpty()
    {
        assertThrows(IllegalArgumentException.class, () -> new ChainPriorityQueue<String>());
        assertThrows(IllegalArgumentException.class,
                     () -> new ChainPriorityQueue<String>(Collections.<Queue<String>>emptyList()));
        assertThrows(IllegalArgumentException.class,
                     () -> new ChainPriorityQueue<String>(Collections.reverseOrder(),
                                                          Collections.<Queue<String>>emptyList()));
    }

    @Test
    public void testConstructorNullElement()
    {
        assertThrows(NullPointerException.class,
                     () -> new ChainPriorityQueue<String>((Queue<String>[]) null));
        assertThrows(NullPointerException.class,
                     () -> new ChainPriorityQueue<>(null, (Collection<? extends Queue<String>>) null));
        assertThrows(NullPointerException.class,
                     () -> new ChainPriorityQueue<String>(pq("a"), null));
        assertThrows(NullPointerException.class,
                     () -> new ChainPriorityQueue<String>(Collections.<Queue<String>>singletonList(null)));
    }

    @Test
    public void testQueuesAndFirst()
    {
        ChainPriorityQueue<String> instance = chain();
        assertEquals(2, instance.queues().size());
        assertSame(instance.queues().get(0), instance.first());
        assertEquals(2, instance.first().size());
        assertThrows(UnsupportedOperationException.class, () -> instance.queues().clear());
    }

    @Test
    public void testComparator()
    {
        assertEquals(null, chain().comparator());

        Comparator<String> byLength = Comparator.comparingInt(String::length);
        ChainPriorityQueue<String> instance = new ChainPriorityQueue<>(byLength, pq("bb", "a"), pq("ccc"));
        assertSame(byLength, instance.comparator());
        assertEquals("a", instance.peek());
    }

    @Test
    public void testPeekBestAcrossQueues()
    {
        ChainPriorityQueue<String> instance = chain();
        assertEquals("a", instance.peek());
        assertEquals("a", instance.peek());
        assertEquals("a", instance.queues().get(1).poll());
        assertEquals("b", instance.peek());
    }

    @Test
    public void testPollDrainsInPriorityOrder()
    {
        ChainPriorityQueue<String> instance = new ChainPriorityQueue<>(pq("1", "4"), pq("2", "3"));
        assertEquals("1", instance.poll());
        assertEquals("2", instance.poll());
        assertEquals("3", instance.poll());
        assertEquals("4", instance.poll());
        assertEquals(null, instance.poll());
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testElementAndRemoveWhenEmpty()
    {
        ChainPriorityQueue<String> instance = new ChainPriorityQueue<>(pq(), pq());
        assertThrows(NoSuchElementException.class, instance::element);
        assertThrows(NoSuchElementException.class, instance::remove);
    }

    @Test
    public void testOfferGoesToFirstQueue()
    {
        ChainPriorityQueue<String> instance = chain();
        assertTrue(instance.offer("z"));
        assertTrue(instance.first().contains("z"));
        assertEquals(3, instance.first().size());
        assertEquals(6, instance.size());
        assertEquals("a", instance.peek());
    }

    @Test
    public void testAddNull()
    {
        ChainPriorityQueue<String> instance = chain();
        assertThrows(NullPointerException.class, () -> instance.add(null));
        assertThrows(NullPointerException.class, () -> instance.offer(null));
        assertThrows(NullPointerException.class, () -> instance.addAll(null));
        assertThrows(NullPointerException.class, () -> instance.addAll(Collections.singletonList(null)));
    }

    @Test
    public void testAddAndAddAll()
    {
        ChainPriorityQueue<String> instance = new ChainPriorityQueue<>(fifo("b", "d"), fifo("a", "c"));
        assertTrue(instance.add("z"));
        assertTrue(instance.addAll(Arrays.asList("y", "x")));
        assertEquals(Arrays.asList("b", "d", "z", "y", "x"), new ArrayList<>(instance.first()));
        assertEquals(7, instance.size());
        assertEquals("a", instance.peek());
    }

    @Test
    public void testSizeAndIsEmptyAreLive()
    {
        ChainPriorityQueue<String> instance = chain();
        assertEquals(5, instance.size());
        assertFalse(instance.isEmpty());

        instance.poll();
        assertEquals(4, instance.size());

        instance.first().add("z");
        assertEquals(5, instance.size());

        instance.queues().get(1).clear();
        assertEquals(3, instance.size());

        instance.clear();
        assertEquals(0, instance.size());
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testContainsAndRemoveObject()
    {
        ChainPriorityQueue<String> instance = chain();
        assertTrue(instance.contains("c"));
        assertTrue(instance.remove("c"));
        assertFalse(instance.contains("c"));
        assertFalse(instance.queues().get(1).contains("c"));
        assertFalse(instance.remove("c"));
        assertEquals(4, instance.size());
    }

    @Test
    public void testRemoveAllRetainAllClear()
    {
        ChainPriorityQueue<String> instance = new ChainPriorityQueue<>(fifo("b", "d"), fifo("a", "c", "e"));
        assertTrue(instance.removeAll(Arrays.asList("a", "c", "z")));
        assertEquals(Arrays.asList("b", "d"), new ArrayList<>(instance.first()));
        assertEquals(Collections.singletonList("e"), new ArrayList<>(instance.queues().get(1)));

        assertTrue(instance.retainAll(Arrays.asList("b", "e")));
        assertFalse(instance.retainAll(Arrays.asList("b", "e")));
        assertEquals(2, instance.size());

        instance.clear();
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testIteratorOrderAndRemove()
    {
        ChainPriorityQueue<String> instance = new ChainPriorityQueue<>(fifo("b", "d"), fifo("a", "c", "e"));
        Iterator<String> iterator = instance.iterator();
        assertEquals(Arrays.asList("b", "d", "a", "c", "e"), collect(iterator));

        Iterator<String> remover = instance.iterator();
        assertEquals("b", remover.next());
        remover.remove();
        assertEquals(4, instance.size());
        assertEquals(Arrays.asList("d", "a", "c", "e"), collect(instance.iterator()));
    }

    @Test
    public void testIteratorExhausted()
    {
        ChainPriorityQueue<String> instance = new ChainPriorityQueue<>(pq(), pq("a"));
        Iterator<String> iterator = instance.iterator();
        assertEquals("a", iterator.next());
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
        iterator.remove();
        assertThrows(IllegalStateException.class, iterator::remove);
    }

    @Test
    public void testToArray()
    {
        ChainPriorityQueue<String> instance = new ChainPriorityQueue<>(fifo("b", "d"), fifo("a", "c", "e"));
        assertEquals(5, instance.toArray().length);
        assertEquals(Arrays.asList("b", "d", "a", "c", "e"), Arrays.asList(instance.toArray(new String[0])));
    }

    @Test
    public void testToString()
    {
        ChainPriorityQueue<String> instance = new ChainPriorityQueue<>(fifo("b", "d"), fifo("a", "c", "e"));
        assertEquals("[b, d, a, c, e]", instance.toString());
        assertEquals("[]", new ChainPriorityQueue<>(pq()).toString());
    }

    @Test
    public void testNaturalOrderingRequiresComparable()
    {
        Queue<Object> q1 = new LinkedList<>(Arrays.asList(new Object()));
        Queue<Object> q2 = new LinkedList<>(Arrays.asList(new Object()));
        ChainPriorityQueue<Object> instance = new ChainPriorityQueue<>(q1, q2);
        assertThrows(ClassCastException.class, instance::peek);
    }

    @Test
    public void testComparatorOrdering()
    {
        ChainPriorityQueue<String> instance =
            new ChainPriorityQueue<>(Collections.reverseOrder(), pq("a"), pq("m", "z"));
        assertEquals("m", instance.peek());
        assertEquals("m", instance.poll());
        assertEquals("z", instance.poll());
        assertEquals("a", instance.poll());
        assertEquals(null, instance.peek());
    }

    @Test
    public void testFifoQueuesCompeteByHead()
    {
        ChainPriorityQueue<String> instance = new ChainPriorityQueue<>(fifo("z", "m"), fifo("y", "n"));
        assertEquals("y", instance.poll());
        assertEquals("n", instance.poll());
        assertEquals("z", instance.poll());
        assertEquals("m", instance.poll());
        assertEquals(null, instance.peek());
    }

    @Test
    public void testEqualsUsesIdentity()
    {
        ChainPriorityQueue<String> a = chain();
        ChainPriorityQueue<String> b = chain();
        assertEquals(a, a);
        assertNotEquals(a, b);
    }

    @Test
    public void testAddToChainFromUnderlyingQueue()
    {
        ChainPriorityQueue<String> instance = chain();
        instance.first().add("a");
        assertTrue(instance.contains("a"));
        assertEquals("a", instance.peek());
    }

    private static List<String> collect(Iterator<String> iterator)
    {
        List<String> collected = new ArrayList<>();
        while(iterator.hasNext())
        {
            collected.add(iterator.next());
        }
        return collected;
    }
}
