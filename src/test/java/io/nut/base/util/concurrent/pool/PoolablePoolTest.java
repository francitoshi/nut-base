/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.pool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PoolablePool<T>")
class PoolablePoolTest
{
    static class Item extends Poolable<Item>
    {
        final AtomicInteger resetCount = new AtomicInteger();
        final AtomicInteger value = new AtomicInteger();

        @Override
        public void reset()
        {
            value.set(0);
            resetCount.incrementAndGet();
        }
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
        assertThrows(NullPointerException.class, () -> new PoolablePool<Item>(null, 4));
    }

    @Test
    @DisplayName("rejects a negative maxIdle but accepts zero")
    void testNegativeMaxIdle()
    {
        assertThrows(IllegalArgumentException.class, () -> new PoolablePool<>(Item::new, -1));
        assertDoesNotThrow(() -> new PoolablePool<>(Item::new, 0));
    }

    @Test
    @DisplayName("creates a new object through the factory when the pool is empty")
    void testAcquireCreatesWhenEmpty()
    {
        AtomicInteger created = new AtomicInteger();
        PoolablePool<Item> pool = new PoolablePool<>(countingFactory(created), 4);
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
        PoolablePool<Item> pool = new PoolablePool<>(countingFactory(created), 4);
        Item first = pool.acquire();
        first.recycle();
        assertEquals(1, pool.idleCount());

        Item second = pool.acquire();
        assertSame(first, second);
        assertEquals(1, created.get(), "no new instance must be created while an idle object is available");
        assertEquals(0, pool.idleCount());
    }

    @Test
    @DisplayName("resets the object before it is placed back in the pool")
    void testRecycleResetsObject()
    {
        PoolablePool<Item> pool = new PoolablePool<>(Item::new, 4);
        Item item = pool.acquire();
        item.value.set(42);
        item.recycle();
        assertEquals(1, item.resetCount.get());
        assertEquals(0, item.value.get());
    }

    @Test
    @DisplayName("a second recycle() of the same object is a no-op")
    void testDoubleRecycleIsNoOp()
    {
        PoolablePool<Item> pool = new PoolablePool<>(Item::new, 4);
        Item item = pool.acquire();
        item.recycle();
        item.recycle();
        assertEquals(1, pool.idleCount());
    }

    @Test
    @DisplayName("an object returns to the pool it was acquired from")
    void testRecycleReturnsToOwningPool()
    {
        PoolablePool<Item> poolA = new PoolablePool<>(Item::new, 4);
        PoolablePool<Item> poolB = new PoolablePool<>(Item::new, 4);
        Item item = poolA.acquire();
        item.recycle();
        assertEquals(1, poolA.idleCount());
        assertEquals(0, poolB.idleCount());
    }

    @Test
    @DisplayName("keeps at most maxIdle idle objects and discards the excess")
    void testMaxIdleBounds()
    {
        AtomicInteger created = new AtomicInteger();
        PoolablePool<Item> pool = new PoolablePool<>(countingFactory(created), 2);
        Item a = pool.acquire();
        Item b = pool.acquire();
        Item c = pool.acquire();
        Item d = pool.acquire();
        assertEquals(4, created.get());

        a.recycle();
        b.recycle();
        c.recycle();
        d.recycle();

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
    @DisplayName("supports extended reuse without creating new instances")
    void testExtendedReuse()
    {
        AtomicInteger created = new AtomicInteger();
        PoolablePool<Item> pool = new PoolablePool<>(countingFactory(created), 4);
        Item item = pool.acquire();
        for (int i = 0; i < 1000; i++)
        {
            item.value.set(i);
            item.recycle();
            item = pool.acquire();
            assertEquals(0, item.value.get(), "recycled object must come back reset");
        }
        assertEquals(1, created.get());
        assertEquals(1000, item.resetCount.get());
    }
}