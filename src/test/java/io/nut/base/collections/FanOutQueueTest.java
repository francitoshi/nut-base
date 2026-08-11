/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FanOutQueueTest
{
    @Test
    void testFanOutToInitialSubscribers()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<>(a, b);
        putUnchecked(fan, 1);

        assertEquals(1, a.poll());
        assertEquals(1, b.poll());
    }

    @Test
    void testSubscribeAddsNewSubscriber()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<>(a);
        fan.subscribe(b);
        putUnchecked(fan, 1);

        assertEquals(1, a.poll());
        assertEquals(1, b.poll());
    }

    @Test
    void testSubscribeWithPredicateFiltersElements()
    {
        BlockingQueue<Integer> all = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> even = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<>(all)
            .subscribe(even, n -> n % 2 == 0);

        putUnchecked(fan, 1);
        putUnchecked(fan, 2);

        assertEquals(Integer.valueOf(1), all.poll());
        assertEquals(Integer.valueOf(2), all.poll());
        assertEquals(Integer.valueOf(2), even.poll());
        assertNull(even.poll());
        assertTrue(all.isEmpty());
        assertTrue(even.isEmpty());
    }

    @Test
    void testSubscribeReturnsThisForChaining()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<>();
        assertSame(fan, fan.subscribe(a));
        assertSame(fan, fan.subscribe(a, n -> true));
    }

    @Test
    void testUnsubscribeRemovesSubscriber()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<>(a);
        fan.subscribe(b);
        putUnchecked(fan, 1);

        assertTrue(fan.unsubscribe(a));
        assertFalse(fan.unsubscribe(a));

        // the element delivered before unsubscribing is still in a
        assertEquals(Integer.valueOf(1), a.poll());
        assertTrue(a.isEmpty());

        putUnchecked(fan, 2);
        assertNull(a.poll());

        assertEquals(Integer.valueOf(1), b.poll());
        assertEquals(Integer.valueOf(2), b.poll());
    }

    @Test
    void testUnsubscribeReturnsFalseWhenNotSubscribed()
    {
        FanOutQueue<Integer> fan = new FanOutQueue<>();
        assertFalse(fan.unsubscribe(new LinkedBlockingQueue<>()));
        assertFalse(fan.unsubscribe(null));
    }

    @Test
    void testUnsubscribeRemovesOnlyFirstSubscription()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<>(a).subscribe(a, n -> n > 0);

        assertTrue(fan.unsubscribe(a));
        // the second subscription remains active
        putUnchecked(fan, 1);
        assertEquals(Integer.valueOf(1), a.poll());
    }

    @Test
    void testAddDelegatesToAllSubscribers()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<>(a, b);
        assertTrue(fan.add(1));

        assertEquals(Integer.valueOf(1), a.poll());
        assertEquals(Integer.valueOf(1), b.poll());
    }

    @Test
    void testLimitZeroBehavesAsUnlimited()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<>(0, a, b);
        putUnchecked(fan, 1);

        assertEquals(Integer.valueOf(1), a.poll());
        assertEquals(Integer.valueOf(1), b.poll());
    }

    @Test
    void testLimitGreaterOrEqualThanSubscribersBehavesAsUnlimited()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<>(3, a, b);
        putUnchecked(fan, 1);

        assertEquals(Integer.valueOf(1), a.poll());
        assertEquals(Integer.valueOf(1), b.poll());
    }

    @Test
    void testLimitedDeliversExactlyLimitDistinctQueues()
    {
        int n = 5;
        int limit = 2;
        int messages = 1000;
        BlockingQueue<Integer>[] queues = newLinkedQueues(n);

        FanOutQueue<Integer> fan = newLimited(limit, queues);
        for (int i = 0; i < messages; i++)
        {
            putUnchecked(fan, i);
        }

        int total = 0;
        for (BlockingQueue<Integer> queue : queues)
        {
            total += queue.size();
            assertTrue(queue.size() > 0, "every queue should receive some element");
        }
        // exactly 'limit' distinct queues receive each of the 'messages' elements
        assertEquals(messages * limit, total);
    }

    @Test
    void testLimitedPrioritizesEmptiestQueue()
    {
        BlockingQueue<Integer> a = newLinkedQueue(5, 5, 5, 5, 5); // size 5
        BlockingQueue<Integer> b = newLinkedQueue(2, 2);           // size 2

        FanOutQueue<Integer> fan = new FanOutQueue<>(1, a, b);
        putUnchecked(fan, 1);
        putUnchecked(fan, 2);
        putUnchecked(fan, 3);

        // b stays the emptiest until it reaches a's size, so every message goes to b
        assertEquals(5, a.size());
        assertEquals(5, b.size());
    }

    @Test
    void testLimitedCompletesWithQueuesThatHaveNotReceived()
    {
        // a: full (size 2 == capacity) -> must be skipped
        // b: size 2, capacity 10 -> has room
        // c: empty and unbounded
        BlockingQueue<Integer> a = new ArrayBlockingQueue<>(2);
        a.add(1);
        a.add(2);
        BlockingQueue<Integer> b = new ArrayBlockingQueue<>(10);
        b.add(1);
        b.add(2);
        BlockingQueue<Integer> c = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<>(2, a, b, c);
        putUnchecked(fan, 3);

        assertEquals(2, a.size()); // never received the element
        assertEquals(3, b.size()); // a was skipped, delivery completed with b
        assertEquals(1, c.size());
    }

    @Test
    void testLimitedMayReachNoQueueWhenAllAreFilteredByPredicate()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> c = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<Integer>(2)
            .subscribe(a, n -> false)
            .subscribe(b, n -> false);
        fan.subscribe(c, n -> false);

        assertFalse(fan.offer(1));
        assertFalse(fan.add(1));
        putUnchecked(fan, 2);

        assertTrue(a.isEmpty());
        assertTrue(b.isEmpty());
        assertTrue(c.isEmpty());
    }

    @Test
    void testLimitedSkipsPredicateRejectingQueues()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> c = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<Integer>(2)
            .subscribe(a)
            .subscribe(b)
            .subscribe(c, n -> n % 2 == 0);

        putUnchecked(fan, 1); // odd -> only a and b receive it
        putUnchecked(fan, 2); // even -> c and one of a/b (the emptiest) receive it

        // the odd message can only be delivered to a and b (limit is 2)
        assertTrue(a.size() >= 1);
        assertTrue(b.size() >= 1);
        // the even message reaches the empty queue c plus one of a or b
        assertEquals(1, c.size());
        assertEquals(3, a.size() + b.size());
    }

    @Test
    void testRejectsNegativeLimit()
    {
        assertThrows(IllegalArgumentException.class,
            () -> new FanOutQueue<>(-1, new LinkedBlockingQueue<Integer>()));
    }

    @Test
    void testLimitedBreaksSizeTiesByReceivedCounter()
    {
        BlockingQueue<Integer> a = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> b = new LinkedBlockingQueue<>();

        FanOutQueue<Integer> fan = new FanOutQueue<>(1, a, b);

        // first message goes to a or b at random; then empty one of the two
        putUnchecked(fan, 1);
        int[] sizes = { a.size(), b.size() };
        BlockingQueue<Integer> received = sizes[0] == 1 ? a : b;
        BlockingQueue<Integer> other = sizes[0] == 1 ? b : a;
        received.poll();

        // sizes tie (0, 0) but the received counter is lower for the other queue
        putUnchecked(fan, 2);
        assertEquals(1, other.size());
        assertEquals(0, received.size());
    }

    @Test
    void testLimitedLoadBalancesByCounter()
    {
        int limit = 2;
        int n = 4;
        BlockingQueue<Integer>[] queues = newLinkedQueues(n);

        FanOutQueue<Integer> fan = newLimited(limit, queues);
        for (int i = 0; i < 2000; i++)
        {
            putUnchecked(fan, i);
        }

        // each message is delivered to exactly 'limit' queues (unbounded), so the
        // sort by size and counter keeps the queues evenly balanced
        int total = 0;
        int max = 0;
        int min = Integer.MAX_VALUE;
        for (BlockingQueue<Integer> queue : queues)
        {
            int s = queue.size();
            total += s;
            max = Math.max(max, s);
            min = Math.min(min, s);
        }
        assertEquals(2000 * limit, total);
        // with n queues and limit picks per message the greedy balance stays tight
        assertTrue(max - min <= limit + 1, "max=" + max + " min=" + min);
    }

    @Test
    void testOfferReturnsFalseWhenSubscriberIsFull()
    {
        BlockingQueue<Integer> bounded = new ArrayBlockingQueue<>(1);
        FanOutQueue<Integer> fan = new FanOutQueue<>(bounded);

        assertTrue(fan.offer(1));
        assertFalse(fan.offer(2));
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testPutBlocksUntilAllSubscribersAccept() throws InterruptedException
    {
        BlockingQueue<Integer> bounded = new ArrayBlockingQueue<>(1);
        FanOutQueue<Integer> fan = new FanOutQueue<>(bounded);

        fan.put(1);

        Thread t = new Thread(() -> putUnchecked(fan, 2));
        t.start();
        Thread.sleep(200);
        assertTrue(t.isAlive());

        assertEquals(1, bounded.poll());
        t.join(1000);
        assertEquals(2, bounded.poll());
    }

    @Test
    void testRemainingCapacityIsSmallestOfSubscribers()
    {
        BlockingQueue<Integer> unbounded = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> bounded = new ArrayBlockingQueue<>(3);

        FanOutQueue<Integer> fan = new FanOutQueue<>(unbounded, bounded);
        assertEquals(3, fan.remainingCapacity());

        bounded.add(1);
        assertEquals(2, fan.remainingCapacity());
    }

    @Test
    void testOfferWithTimeoutRespectsSharedBudget() throws InterruptedException
    {
        BlockingQueue<Integer> bounded = new ArrayBlockingQueue<>(1);
        FanOutQueue<Integer> fan = new FanOutQueue<>(bounded);

        bounded.add(1);
        long start = System.nanoTime();
        assertFalse(fan.offer(2, 50, TimeUnit.MILLISECONDS));
        assertTrue(System.nanoTime() - start < TimeUnit.MILLISECONDS.toNanos(2000));
    }

    @Test
    void testRetrievalOperationsAreNotSupported()
    {
        FanOutQueue<Integer> fan = new FanOutQueue<>(new LinkedBlockingQueue<>());

        assertThrows(UnsupportedOperationException.class, fan::take);
        assertThrows(UnsupportedOperationException.class, fan::poll);
        assertThrows(UnsupportedOperationException.class, () -> fan.poll(1, TimeUnit.SECONDS));
        assertThrows(UnsupportedOperationException.class, fan::remove);
        assertThrows(UnsupportedOperationException.class, fan::element);
        assertThrows(UnsupportedOperationException.class, fan::peek);
        assertThrows(UnsupportedOperationException.class, () -> fan.drainTo(new java.util.ArrayList<>()));
    }

    @Test
    void testCollectionInspectionMethods()
    {
        FanOutQueue<Integer> fan = new FanOutQueue<>(new LinkedBlockingQueue<>());

        assertEquals(0, fan.size());
        assertTrue(fan.isEmpty());
        assertFalse(fan.contains(1));
        assertFalse(fan.containsAll(Arrays.asList(1)));
        assertTrue(fan.containsAll(Arrays.asList()));
        assertFalse(fan.remove(1));
        assertFalse(fan.removeAll(Arrays.asList(1)));
        assertFalse(fan.removeIf(n -> true));
        assertFalse(fan.retainAll(Arrays.asList(1)));
        assertEquals(0, fan.toArray().length);
        Integer[] arr = new Integer[1];
        assertSame(arr, fan.toArray(arr));
        assertNull(arr[0]);
        assertFalse(fan.iterator().hasNext());
    }

    private static void putUnchecked(FanOutQueue<Integer> fan, int value)
    {
        try
        {
            fan.put(value);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    @SafeVarargs
    private static FanOutQueue<Integer> newLimited(int limit, BlockingQueue<Integer>... queues)
    {
        return new FanOutQueue<>(limit, queues);
    }

    private static BlockingQueue<Integer>[] newLinkedQueues(int n)
    {
        @SuppressWarnings("unchecked")
        BlockingQueue<Integer>[] queues = new BlockingQueue[n];
        for (int i = 0; i < n; i++)
        {
            queues[i] = new LinkedBlockingQueue<>();
        }
        return queues;
    }

    private static BlockingQueue<Integer> newLinkedQueue(int... elements)
    {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        for (int value : elements)
        {
            queue.add(value);
        }
        return queue;
    }
}