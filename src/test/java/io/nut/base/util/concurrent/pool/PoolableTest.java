/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.pool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Poolable<T>")
class PoolableTest
{
    static class Item extends Poolable<Item>
    {
        final AtomicInteger resetCount = new AtomicInteger();

        @Override
        public void reset()
        {
            resetCount.incrementAndGet();
        }
    }

    @Test
    @DisplayName("recycle() is a no-op on an object that was never pooled")
    void testRecycleOnUnpooledObject()
    {
        Item item = new Item();
        assertDoesNotThrow(item::recycle);
        assertEquals(0, item.resetCount.get());
    }

    @Test
    @DisplayName("recycle() returns the object to its owner")
    void testRecycleReturnsToOwner()
    {
        AtomicInteger recycled = new AtomicInteger();
        PoolOwner<Item> owner = obj -> recycled.incrementAndGet();
        Item item = new Item();
        item.attach(owner);

        item.recycle();

        assertEquals(1, recycled.get());
    }

    @Test
    @DisplayName("a second recycle() does not return the object again")
    void testDoubleRecycleReturnsOnce()
    {
        AtomicInteger recycled = new AtomicInteger();
        PoolOwner<Item> owner = obj -> recycled.incrementAndGet();
        Item item = new Item();
        item.attach(owner);

        item.recycle();
        item.recycle();

        assertEquals(1, recycled.get());
    }

    @Test
    @DisplayName("recycle() does nothing once the owner has been detached")
    void testRecycleAfterDetach()
    {
        AtomicInteger recycled = new AtomicInteger();
        PoolOwner<Item> owner = obj -> recycled.incrementAndGet();
        Item item = new Item();
        item.attach(owner);
        item.attach(null);

        item.recycle();

        assertEquals(0, recycled.get());
        assertEquals(0, item.resetCount.get());
    }
}