/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.atomic;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicCounterPairTest
{
    @Test
    void initialValuesAreReadable()
    {
        AtomicCounterPair pair = new AtomicCounterPair(7, -3);
        assertEquals(7, pair.getFirst());
        assertEquals(-3, pair.getSecond());
        assertEquals(7L << 32 | (0xFFFFFFFFL & -3), pair.get());
        assertFalse(pair.isZero());
    }

    @Test
    void defaultConstructorIsZero()
    {
        AtomicCounterPair pair = new AtomicCounterPair();
        assertTrue(pair.isZero());
        assertEquals(0, pair.getFirst());
        assertEquals(0, pair.getSecond());
    }

    @Test
    void setOverwritesBothCounters()
    {
        AtomicCounterPair pair = new AtomicCounterPair();
        pair.set(11, 22);
        assertEquals(11, pair.getFirst());
        assertEquals(22, pair.getSecond());
        assertFalse(pair.isZero());
        pair.set(0, 0);
        assertTrue(pair.isZero());
    }

    @Test
    void singleCounterUpdatesReturnNewValue()
    {
        AtomicCounterPair pair = new AtomicCounterPair(1, 2);

        assertEquals(2, pair.incrementFirst());
        assertEquals(3, pair.incrementSecond());
        assertEquals(1, pair.decrementFirst());
        assertEquals(2, pair.decrementSecond());

        assertEquals(1, pair.getFirst());
        assertEquals(2, pair.getSecond());
        assertEquals("1/2", pair.toString());
    }

    @Test
    void addFirstToSecondMovesUnitsAtomically()
    {
        AtomicCounterPair pair = new AtomicCounterPair(10, 4);

        pair.addFirstToSecond(3);
        assertEquals(7, pair.getFirst());
        assertEquals(7, pair.getSecond());

        pair.addFirstToSecond(-2);
        assertEquals(9, pair.getFirst());
        assertEquals(5, pair.getSecond());

        pair.addFirstToSecond(4);
        assertEquals(5, pair.getFirst());
        assertEquals(9, pair.getSecond());
        assertEquals(14, pair.getFirst() + pair.getSecond());
    }

    @Test
    void decrementSecondAndAllZeroOnlyWhenBothReachZero()
    {
        AtomicCounterPair pair = new AtomicCounterPair(5, 1);

        assertFalse(pair.decrementSecondAndAllZero());
        assertEquals(5, pair.getFirst());
        assertEquals(0, pair.getSecond());
        assertFalse(pair.isZero());

        pair.set(0, 2);
        assertFalse(pair.decrementSecondAndAllZero());
        assertTrue(pair.decrementSecondAndAllZero());
        assertTrue(pair.isZero());
    }

    @Test
    void decrementSecondAndAllZeroIgnoresLeftOversOnFirst()
    {
        AtomicCounterPair pair = new AtomicCounterPair(3, 1);

        assertFalse(pair.decrementSecondAndAllZero());
        assertEquals(3, pair.getFirst());
        assertEquals(0, pair.getSecond());
        assertFalse(pair.isZero());
    }

    @Test
    void decrementFirstAndAllZeroWorksSymmetric()
    {
        AtomicCounterPair pair = new AtomicCounterPair(1, 2);

        assertFalse(pair.decrementFirstAndAllZero());
        assertEquals(0, pair.getFirst());
        assertEquals(2, pair.getSecond());

        pair.set(2, 0);
        assertFalse(pair.decrementFirstAndAllZero());
        assertTrue(pair.decrementFirstAndAllZero());
        assertTrue(pair.isZero());
    }

    @Test
    void updateAppliesTransformationAndObservesNewState()
    {
        AtomicCounterPair pair = new AtomicCounterPair(4, 0);

        Boolean bothZero = pair.update(
                (f, s) -> f - 1,
                (f, s) -> s + 1,
                (nf, ns) -> (nf == 0 && ns == 0));

        assertEquals(3, pair.getFirst());
        assertEquals(1, pair.getSecond());
        assertFalse(bothZero);

        bothZero = pair.update(
                (f, s) -> f - 3,
                (f, s) -> s - 1,
                (nf, ns) -> nf == 0 && ns == 0);

        assertTrue(bothZero);
        assertTrue(pair.isZero());
    }

    @Test
    void updateWithoutObserverReturnsNull()
    {
        AtomicCounterPair pair = new AtomicCounterPair(2, 0);
        Object result = pair.update((f, s) -> f - 1, (f, s) -> s + 1, null);
        assertEquals(null, result);
        assertEquals(1, pair.getFirst());
        assertEquals(1, pair.getSecond());
    }

    @Test
    void concurrentTransfersPreserveTheTotal()
    {
        final int start = 500;
        AtomicCounterPair pair = new AtomicCounterPair(start, 0);
        int threads = 8;
        int iterations = 50_000;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        try
        {
            for (int t = 0; t < threads; t++)
            {
                executor.submit(() ->
                {
                    ready.countDown();
                    go.await();
                    for (int i = 0; i < iterations; i++)
                    {
                        pair.addFirstToSecond(1);
                        pair.addFirstToSecond(-1);
                    }
                    return null;
                });
            }
            ready.await();
            go.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }

        assertEquals(start, pair.getFirst());
        assertEquals(0, pair.getSecond());
        assertFalse(pair.isZero());
    }
}