/*
 *  IterableQueueTest.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.collections;

import io.nut.base.util.As;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IterableQueue}.
 *
 * <p>
 * Naming convention: {@code methodUnderTest_scenario_expectedOutcome}.
 */
@Timeout(value = 5, unit = TimeUnit.SECONDS) // guard against blocking forever
class IterableQueueTest
{

    private static final String POISON = "__POISON__";

    private IterableQueue<String> iterableQueue;

    @BeforeEach
    void setUp()
    {
        iterableQueue = new IterableQueue<>(new LinkedBlockingQueue<>(), POISON);
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
    @Test
    void constructor_nullQueue_throwsNullPointerException()
    {
        assertThrows(NullPointerException.class, () -> new IterableQueue<>(null, POISON));
    }

    @Test
    void constructor_nullPoison_throwsNullPointerException()
    {
        assertThrows(NullPointerException.class, () -> new IterableQueue<>(new LinkedBlockingQueue<>(), null));
    }

    // -------------------------------------------------------------------------
    // add()
    // -------------------------------------------------------------------------
    @Test
    void add_nullElement_throwsNullPointerException()
    {
        assertThrows(NullPointerException.class, () -> iterableQueue.add(null));
    }

    @Test
    void add_validElement_elementIsAvailableViaIterator() throws InterruptedException
    {
        iterableQueue.add("hello");
        iterableQueue.poison();

        assertTrue(iterableQueue.hasNext());
        assertEquals("hello", iterableQueue.next());
    }

    // -------------------------------------------------------------------------
    // hasNext() / next() — basic contract
    // -------------------------------------------------------------------------
    @Test
    void hasNext_emptyQueueThenPoison_returnsFalse() throws InterruptedException
    {
        iterableQueue.poison();

        assertFalse(iterableQueue.hasNext());
    }

    @Test
    void hasNext_calledMultipleTimes_idempotent() throws InterruptedException
    {
        iterableQueue.add("a");
        iterableQueue.poison();

        assertTrue(iterableQueue.hasNext());
        assertTrue(iterableQueue.hasNext()); // must not consume the element
        assertTrue(iterableQueue.hasNext());
        assertEquals("a", iterableQueue.next());
        assertFalse(iterableQueue.hasNext());
    }

    @Test
    void next_noElements_throwsNoSuchElementException() throws InterruptedException
    {
        iterableQueue.poison();

        assertThrows(NoSuchElementException.class, iterableQueue::next);
    }

    @Test
    void next_afterExhaustion_throwsNoSuchElementException() throws InterruptedException
    {
        iterableQueue.add("only");
        iterableQueue.poison();

        assertEquals("only", iterableQueue.next());
        assertThrows(NoSuchElementException.class, iterableQueue::next);
    }

    // -------------------------------------------------------------------------
    // Iteration order and completeness
    // -------------------------------------------------------------------------
    @Test
    void iteration_multipleElements_preservesInsertionOrder() throws InterruptedException
    {
        iterableQueue.add("a");
        iterableQueue.add("b");
        iterableQueue.add("c");
        iterableQueue.poison();

        List<String> result = collectAll(iterableQueue);

        assertEquals(As.list("a", "b", "c"), result);
    }

    @Test
    void forEachLoop_multipleElements_consumesAll() throws InterruptedException
    {
        iterableQueue.add("x");
        iterableQueue.add("y");
        iterableQueue.poison();

        List<String> result = new ArrayList<>();
        for (String s : iterableQueue)
        {
            result.add(s);
        }

        assertEquals(As.list("x", "y"), result);
    }

    @Test
    void iteration_singleElement_thenPoison_returnsOneElement() throws InterruptedException
    {
        iterableQueue.add("solo");
        iterableQueue.poison();

        List<String> result = collectAll(iterableQueue);

        assertEquals(As.list("solo"), result);
    }

    // -------------------------------------------------------------------------
    // Poison detection
    // -------------------------------------------------------------------------
    @Test
    void hasNext_poisonDetectedByEquals_notByReference() throws InterruptedException
    {
        // Use a poison whose equal twin is a distinct object instance.
        String poisonA = new String("END");
        String poisonB = new String("END"); // same value, different reference
        IterableQueue<String> q = new IterableQueue<>(new LinkedBlockingQueue<>(), poisonA);

        q.add("item");
        q.getQueue().put(poisonB); // bypass add() to inject equal-but-not-same poison

        List<String> result = collectAll(q);

        assertEquals(As.list("item"), result);
    }

    // -------------------------------------------------------------------------
    // iterator()
    // -------------------------------------------------------------------------
    @Test
    void iterator_returnsSameInstance()
    {
        assertSame(iterableQueue, iterableQueue.iterator());
    }

    // -------------------------------------------------------------------------
    // remove()
    // -------------------------------------------------------------------------
    @Test
    void remove_alwaysThrowsUnsupportedOperationException()
    {
        assertThrows(UnsupportedOperationException.class, iterableQueue::remove);
    }

    // -------------------------------------------------------------------------
    // Concurrent producer / consumer
    // -------------------------------------------------------------------------
    @Test
    void concurrentProducer_itemsConsumedInOrder() throws InterruptedException
    {
        int count = 1_000;

        Thread producer = new Thread(() ->
        {
            try
            {
                for (int i = 0; i < count; i++)
                {
                    iterableQueue.add(String.valueOf(i));
                }
                iterableQueue.poison();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();

        List<String> result = collectAll(iterableQueue);
        producer.join();

        assertEquals(count, result.size());
        for (int i = 0; i < count; i++)
        {
            assertEquals(String.valueOf(i), result.get(i));
        }
    }

    @Test
    void interrupt_whileBlocking_throwsIllegalStateAndRestoresFlag() throws InterruptedException
    {
        // The queue is empty and no poison is sent, so hasNext() will block.
        Thread consumer = new Thread(() ->
        {
            assertThrows(IllegalStateException.class, iterableQueue::hasNext);
            assertTrue(Thread.currentThread().isInterrupted(), "interrupt flag must be restored after IllegalStateException");
        });

        consumer.start();
        // Give the consumer time to block on queue.take()
        Thread.sleep(100);
        consumer.interrupt();
        consumer.join();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private static <E> List<E> collectAll(IterableQueue<E> q)
    {
        List<E> list = new ArrayList<>();
        q.forEach(list::add);
        return list;
    }
}
