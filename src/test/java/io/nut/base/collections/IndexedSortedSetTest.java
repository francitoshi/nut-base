/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import java.util.Arrays;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IndexedSortedSetTest
{
    @Test
    public void testBasicGetAndLazyCaching()
    {
        IndexedSortedSet<String> set = new IndexedSortedSet<>();
        set.add("banana");
        set.add("apple");
        set.add("cherry");

        // Elements are sorted: apple, banana, cherry
        assertEquals("apple", set.get(0));
        assertEquals("banana", set.get(1));
        assertEquals("cherry", set.get(2));

        // Test out of bounds
        assertThrows(IndexOutOfBoundsException.class, () -> set.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.get(3));
    }

    @Test
    public void testInvalidationOnAdd()
    {
        IndexedSortedSet<Integer> set = new IndexedSortedSet<>();
        set.addAll(Arrays.asList(10, 30, 20));

        // Populate cache up to index 1 (10, 20)
        assertEquals(10, set.get(0));
        assertEquals(20, set.get(1));

        // Add element that goes in between (15)
        set.add(15);

        // Cache should be invalidated and start fresh
        assertEquals(10, set.get(0));
        assertEquals(15, set.get(1));
        assertEquals(20, set.get(2));
        assertEquals(30, set.get(3));
    }

    @Test
    public void testInvalidationOnRemove()
    {
        IndexedSortedSet<Integer> set = new IndexedSortedSet<>(Arrays.asList(10, 20, 30));

        assertEquals(10, set.get(0));
        assertEquals(20, set.get(1));

        set.remove(20);

        assertEquals(10, set.get(0));
        assertEquals(30, set.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.get(2));
    }

    @Test
    public void testInvalidationOnClear()
    {
        IndexedSortedSet<Integer> set = new IndexedSortedSet<>(Arrays.asList(10, 20, 30));

        assertEquals(10, set.get(0));
        set.clear();

        assertTrue(set.isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> set.get(0));
    }

    @Test
    public void testIteratorRemoveInvalidation()
    {
        IndexedSortedSet<Integer> set = new IndexedSortedSet<>(Arrays.asList(10, 20, 30));

        assertEquals(10, set.get(0));
        assertEquals(20, set.get(1));

        Iterator<Integer> it = set.iterator();
        while (it.hasNext())
        {
            if (it.next() == 20)
            {
                it.remove();
            }
        }

        assertEquals(10, set.get(0));
        assertEquals(30, set.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.get(2));
    }

    @Test
    public void testSubSetInvalidationPropagation()
    {
        IndexedSortedSet<Integer> set = new IndexedSortedSet<>(Arrays.asList(10, 20, 30, 40, 50));

        IndexedSortedSet<Integer> subSet = set.subSet(20, 45); // [20, 30, 40]

        // Populate both caches
        assertEquals(10, set.get(0));
        assertEquals(20, set.get(1));

        assertEquals(20, subSet.get(0));
        assertEquals(30, subSet.get(1));

        // Mutate subSet
        subSet.add(25); // Set is now [10, 20, 25, 30, 40, 50], SubSet is [20, 25, 30, 40]

        // Both caches must be invalidated!
        assertEquals(20, subSet.get(0));
        assertEquals(25, subSet.get(1));
        assertEquals(30, subSet.get(2));

        assertEquals(10, set.get(0));
        assertEquals(20, set.get(1));
        assertEquals(25, set.get(2));
    }

    @Test
    public void testParentInvalidationPropagationToSubSet()
    {
        IndexedSortedSet<Integer> set = new IndexedSortedSet<>(Arrays.asList(10, 20, 30, 40, 50));

        IndexedSortedSet<Integer> subSet = set.subSet(20, 45); // [20, 30, 40]

        assertEquals(20, subSet.get(0));
        assertEquals(30, subSet.get(1));

        // Mutate parent
        set.add(35); // Set is now [10, 20, 30, 35, 40, 50], SubSet is [20, 30, 35, 40]

        // SubSet cache should be invalidated dynamically when we access it
        assertEquals(20, subSet.get(0));
        assertEquals(30, subSet.get(1));
        assertEquals(35, subSet.get(2));
        assertEquals(40, subSet.get(3));
    }

    @Test
    public void testFirstAndLastOptimizations()
    {
        IndexedSortedSet<Integer> set = new IndexedSortedSet<>(Arrays.asList(10, 20, 30));

        assertEquals(10, set.first());
        assertEquals(30, set.last());

        // Fully cache the set
        assertEquals(30, set.get(2));

        // Should return from cache now
        assertEquals(10, set.first());
        assertEquals(30, set.last());
    }

    @Test
    public void testToArrayAndSizeWhenFullyCached()
    {
        IndexedSortedSet<Integer> set = new IndexedSortedSet<>(Arrays.asList(10, 20, 30));

        assertEquals(3, set.size());
        assertFalse(set.isEmpty());

        // Cache fully
        assertEquals(30, set.get(2));

        // Test optimized toArray and size methods
        assertEquals(3, set.size());
        assertArrayEquals(new Object[]{10, 20, 30}, set.toArray());
        assertArrayEquals(new Integer[]{10, 20, 30}, set.toArray(new Integer[0]));
    }

    @Test
    public void testSynchronizedViewBasicAndConcurrency() throws InterruptedException
    {
        IndexedSortedSet<Integer> set = new IndexedSortedSet<>();
        IndexedSortedSet<Integer> syncSet = set.synchronizedView();

        // Check identity on double wrap
        assertSame(syncSet, syncSet.synchronizedView());

        // Concurrent additions
        int threadCount = 4;
        int itemsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++)
        {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < itemsPerThread; j++)
                {
                    syncSet.add(threadId * 1000 + j);
                }
            });
            threads[i].start();
        }

        for (Thread t : threads)
        {
            t.join();
        }

        assertEquals(threadCount * itemsPerThread, syncSet.size());

        // Verify indexing works on the thread-safe version
        for (int i = 0; i < syncSet.size(); i++)
        {
            assertNotNull(syncSet.get(i));
        }

        // Test subset returned by syncSet is also a synchronized view
        IndexedSortedSet<Integer> sub = syncSet.subSet(0, 500);
        assertTrue(sub.getClass().getName().contains("SynchronizedIndexedSortedSet"));
    }
}

