/*
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent.hive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link QueueBee}: delegation to an underlying
 * BlockingQueue, BlockingQueue interface implementation, receive()
 * put() semantics, and the collection of factory methods on {@link Hive}.
 */
class QueueBeeTest
{
    private Hive hive;

    @BeforeEach
    void setUp()
    {
        hive = Hive.hive(2);
    }

    @AfterEach
    void tearDown()
    {
        hive.shutdown();
        try
        {
            hive.awaitTermination(2000);
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void directSendPutsMessageIntoTheQueue()
    {
        BlockingQueue<String> underlying = new LinkedBlockingQueue<>();
        QueueBee<String> qb = new QueueBee<>(underlying);

        assertTrue(qb.send("hello"));

        assertEquals(1, qb.size());
        assertEquals("hello", underlying.peek());
    }

    @Test
    void implementsBlockingQueueInterface()
    {
        BlockingQueue<Integer> underlying = new LinkedBlockingQueue<>();
        QueueBee<Integer> qb = new QueueBee<>(underlying);

        assertTrue(qb instanceof BlockingQueue);
    }

    @Test
    void takeBehavesLikeUnderlyingQueue() throws InterruptedException
    {
        BlockingQueue<String> underlying = new LinkedBlockingQueue<>();
        QueueBee<String> qb = new QueueBee<>(underlying);

        qb.send("item");

        assertEquals("item", qb.take());
    }

    @Test
    void pollWithTimeoutBehavesLikeUnderlyingQueue() throws InterruptedException
    {
        BlockingQueue<String> underlying = new LinkedBlockingQueue<>();
        QueueBee<String> qb = new QueueBee<>(underlying);

        qb.send("item");

        assertEquals("item", qb.poll(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void offerWithTimeoutBehavesLikeUnderlyingQueue() throws InterruptedException
    {
        BlockingQueue<String> underlying = new LinkedBlockingQueue<>(2);
        QueueBee<String> qb = new QueueBee<>(underlying);

        assertTrue(qb.offer("a", 100, TimeUnit.MILLISECONDS));
        assertTrue(qb.offer("b", 100, TimeUnit.MILLISECONDS));
        assertFalse(qb.offer("c", 50, TimeUnit.MILLISECONDS));
    }

    @Test
    void remainingCapacityDelegates()
    {
        BlockingQueue<String> underlying = new LinkedBlockingQueue<>(5);
        QueueBee<String> qb = new QueueBee<>(underlying);

        qb.send("a");
        qb.send("b");

        assertEquals(3, qb.remainingCapacity());
    }

    @Test
    void drainToDelegates()
    {
        BlockingQueue<String> underlying = new LinkedBlockingQueue<>();
        QueueBee<String> qb = new QueueBee<>(underlying);

        qb.send("a");
        qb.send("b");

        java.util.List<String> drained = new java.util.ArrayList<>();
        int count = qb.drainTo(drained);

        assertEquals(2, count);
        assertEquals(2, drained.size());
    }

    @Test
    void sizeIsEmptyContainsDelegateToo()
    {
        BlockingQueue<Integer> underlying = new LinkedBlockingQueue<>();
        QueueBee<Integer> qb = new QueueBee<>(underlying);

        assertTrue(qb.isEmpty());
        assertEquals(0, qb.size());
        assertFalse(qb.contains(1));

        qb.send(1);

        assertFalse(qb.isEmpty());
        assertEquals(1, qb.size());
        assertTrue(qb.contains(1));
    }

    @Test
    void collectionMethodsDelegate()
    {
        BlockingQueue<Integer> underlying = new LinkedBlockingQueue<>();
        QueueBee<Integer> qb = new QueueBee<>(underlying);

        qb.addAll(java.util.Arrays.asList(1, 2, 3));

        assertEquals(3, qb.size());
        assertTrue(qb.containsAll(java.util.Arrays.asList(1, 2)));
    }

    @Test
    void constructorRejectsNullQueue()
    {
        assertThrows(NullPointerException.class, () -> new QueueBee<>(null));
        assertThrows(NullPointerException.class, () -> new QueueBee<>(hive, null));
        assertThrows(NullPointerException.class, () -> new QueueBee<>(2, hive, null));
    }

    @Test
    void hiveQueueFactoryCreatesBoundQueueBee()
    {
        QueueBee<String> qb = hive.queue(new LinkedBlockingQueue<>());

        qb.send("test");
        qb.shutdown().awaitTermination(1);

        assertEquals(1, qb.size());
    }

    @Test
    void hiveQueueFactoryWithThreadsParameter()
    {
        QueueBee<String> qb = hive.queue(2, new LinkedBlockingQueue<>());

        assertTrue(qb.send("test"));
        qb.shutdown().awaitTermination(1);

        assertEquals(1, qb.size());
    }

    @Test
    void hiveQueueFactoryWithQueueSizeParameter()
    {
        QueueBee<String> qb = hive.queue(2, 10, new LinkedBlockingQueue<>());

        assertTrue(qb.send("test"));
        qb.shutdown().awaitTermination(1);

        assertEquals(1, qb.size());
    }

    @Test
    void hiveQueueFactoryRejectsNullQueue()
    {
        assertThrows(NullPointerException.class, () -> hive.queue(null));
        assertThrows(NullPointerException.class, () -> hive.queue(2, null));
        assertThrows(NullPointerException.class, () -> hive.queue(2, 10, null));
    }
}
