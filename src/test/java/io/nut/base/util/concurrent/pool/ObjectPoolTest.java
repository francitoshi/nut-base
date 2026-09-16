/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.pool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ObjectPool<T>")
class ObjectPoolTest
{
    static class Item
    {
        final AtomicBoolean busy = new AtomicBoolean();
        int value;
    }

    static Supplier<Item> countingFactory(AtomicInteger created)
    {
        return () ->
        {
            created.incrementAndGet();
            return new Item();
        };
    }

    @Test
    @DisplayName("rejects a null factory")
    void testNullFactory()
    {
        assertThrows(NullPointerException.class, () -> new ObjectPool<>(null, 4));
    }

    @Test
    @DisplayName("rejects a negative maxIdle but accepts zero")
    void testNegativeMaxIdle()
    {
        assertThrows(IllegalArgumentException.class, () -> new ObjectPool<>(Item::new, -1));
        assertDoesNotThrow(() -> new ObjectPool<>(Item::new, 0));
    }

    @Test
    @DisplayName("creates a new object through the factory when the pool is empty")
    void testAcquireCreatesWhenEmpty()
    {
        AtomicInteger created = new AtomicInteger();
        ObjectPool<Item> pool = new ObjectPool<>(countingFactory(created), 4);
        Item item = pool.acquire();
        assertNotNull(item);
        assertEquals(1, created.get());
        assertEquals(0, pool.idleCount());
    }

    @Test
    @DisplayName("reuses a recycled instance instead of creating a new one")
    void testRecycleReusesInstance()
    {
        AtomicInteger created = new AtomicInteger();
        ObjectPool<Item> pool = new ObjectPool<>(countingFactory(created), 4);
        Item first = pool.acquire();
        pool.recycle(first);
        assertEquals(1, pool.idleCount());

        Item second = pool.acquire();
        assertSame(first, second);
        assertEquals(1, created.get(), "no new instance must be created while an idle object is available");
        assertEquals(0, pool.idleCount());
    }

    @Test
    @DisplayName("does not touch the object when it is recycled")
    void testRecycleLeavesObjectUntouched()
    {
        ObjectPool<Item> pool = new ObjectPool<>(Item::new, 4);
        Item item = pool.acquire();
        item.value = 42;
        pool.recycle(item);
        assertEquals(42, item.value, "clearing state is the caller's responsibility");

        Item again = pool.acquire();
        assertSame(item, again);
    }

    @Test
    @DisplayName("supports extended reuse without creating new instances")
    void testExtendedReuse()
    {
        AtomicInteger created = new AtomicInteger();
        ObjectPool<Item> pool = new ObjectPool<>(countingFactory(created), 4);
        Item item = pool.acquire();
        for (int i = 0; i < 1000; i++)
        {
            item.busy.set(true);
            item.value = i;
            Item previous = item;
            item.busy.set(false);
            pool.recycle(item);
            item = pool.acquire();
            assertSame(previous, item, "the recycled object must be handed out again");
        }
        assertEquals(1, created.get());
    }

    @Test
    @DisplayName("keeps at most maxIdle idle objects and discards the excess")
    void testMaxIdleBounds()
    {
        AtomicInteger created = new AtomicInteger();
        ObjectPool<Item> pool = new ObjectPool<>(countingFactory(created), 2);
        Item a = pool.acquire();
        Item b = pool.acquire();
        Item c = pool.acquire();
        Item d = pool.acquire();
        assertEquals(4, created.get());

        pool.recycle(a);
        pool.recycle(b);
        pool.recycle(c);
        pool.recycle(d);

        assertEquals(2, pool.idleCount());

        Item a2 = pool.acquire();
        Item b2 = pool.acquire();
        assertTrue(a2 == a || a2 == b);
        assertTrue(b2 == a || b2 == b);
        assertNotSame(a2, b2);

        pool.acquire();
        pool.acquire();
        assertEquals(6, created.get(), "pool must fall back to the factory once idle objects run out");
        assertEquals(0, pool.idleCount());
    }

    @Test
    @DisplayName("setReset returns the same pool instance")
    void testSetResetReturnsThis()
    {
        ObjectPool<Item> pool = new ObjectPool<>(Item::new, 4);
        assertSame(pool, pool.setReset(item -> {}));
    }

    @Test
    @DisplayName("recycle applies the reset consumer when one is set")
    void testRecycleCallsResetConsumer()
    {
        AtomicReference<Item> seen = new AtomicReference<>();
        ObjectPool<Item> pool = new ObjectPool<>(Item::new, 4)
                .setReset(seen::set);
        Item item = pool.acquire();
        item.value = 42;

        pool.recycle(item);

        assertSame(item, seen.get());
        assertEquals(42, seen.get().value, "reset consumer is the one responsible for clearing state");
        assertEquals(1, pool.idleCount());
    }

    @Test
    @DisplayName("recycle skips reset when no consumer is set")
    void testRecycleSkipsResetWhenNull()
    {
        ObjectPool<Item> pool = new ObjectPool<>(Item::new, 4)
                .setReset(null);
        Item item = pool.acquire();
        item.value = 42;

        pool.recycle(item);

        assertEquals(42, item.value, "object must be untouched");
        assertEquals(1, pool.idleCount());
    }

    @Test
    @DisplayName("setReset replaces the previously configured consumer")
    void testSetResetReplacesConsumer()
    {
        AtomicReference<Item> first = new AtomicReference<>();
        AtomicReference<Item> second = new AtomicReference<>();
        ObjectPool<Item> pool = new ObjectPool<>(Item::new, 4)
                .setReset(first::set)
                .setReset(second::set);

        Item item = pool.acquire();
        pool.recycle(item);

        assertNull(first.get(), "old consumer must not be invoked");
        assertSame(item, second.get());
    }

    @Test
    @DisplayName("never hands out the same object twice under concurrent traffic")
    void testConcurrentAcquireRecycle() throws Exception
    {
        AtomicInteger created = new AtomicInteger();
        ObjectPool<Item> pool = new ObjectPool<>(countingFactory(created), 8);
        AtomicInteger doubleAcquire = new AtomicInteger();
        int threads = 8;
        int iterations = 2000;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try
        {
            CountDownLatch start = new CountDownLatch(1);
            for (int t = 0; t < threads; t++)
            {
                executor.execute(() ->
                {
                    try
                    {
                        start.await();
                    }
                    catch (InterruptedException ex)
                    {
                        Thread.currentThread().interrupt();
                    }
                    for (int i = 0; i < iterations; i++)
                    {
                        Item item = pool.acquire();
                        if (item.busy.getAndSet(true))
                        {
                            doubleAcquire.incrementAndGet();
                        }
                        item.value = i;
                        item.busy.set(false);
                        pool.recycle(item);
                    }
                });
            }
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

            assertEquals(0, doubleAcquire.get(), "no object must be handed out while it is still in use");
            int idle = pool.idleCount();
            assertTrue(idle >= 0 && idle <= 8, "idleCount must stay within bounds but was " + idle);
        }
        finally
        {
            executor.shutdownNow();
        }
    }
}