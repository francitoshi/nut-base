/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MultiExchangerTest
{
    @Test
    void testConstructorPartiesValidation()
    {
        assertThrows(IllegalArgumentException.class, () -> new MultiExchanger<Integer>(0));
        assertThrows(IllegalArgumentException.class, () -> new MultiExchanger<Integer>(1));
        assertThrows(IllegalArgumentException.class, () -> new MultiExchanger<Integer>(-5));
    }

    @Test
    void testTwoPartyExchange() throws Exception
    {
        MultiExchanger<String> exchanger = new MultiExchanger<>(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
        {
            Future<String> f1 = executor.submit(() -> exchanger.exchange("A"));
            Future<String> f2 = executor.submit(() -> exchanger.exchange("B"));

            String res1 = f1.get(2, TimeUnit.SECONDS);
            String res2 = f2.get(2, TimeUnit.SECONDS);

            assertEquals("B", res1);
            assertEquals("A", res2);
        }
        finally
        {
            executor.shutdown();
        }
    }

    @Test
    void testTwoPartyExchangeSecure() throws Exception
    {
        MultiExchanger<String> exchanger = new MultiExchanger<>(2, true);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
        {
            Future<String> f1 = executor.submit(() -> exchanger.exchange("A"));
            Future<String> f2 = executor.submit(() -> exchanger.exchange("B"));

            String res1 = f1.get(2, TimeUnit.SECONDS);
            String res2 = f2.get(2, TimeUnit.SECONDS);

            assertEquals("B", res1);
            assertEquals("A", res2);
        }
        finally
        {
            executor.shutdown();
        }
    }

    @Test
    void testNPartyExchangeDerangement() throws Exception
    {
        int parties = 5;
        MultiExchanger<Integer> exchanger = new MultiExchanger<>(parties);
        ExecutorService executor = Executors.newFixedThreadPool(parties);
        try
        {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < parties; i++)
            {
                final int offeredValue = i;
                futures.add(executor.submit(() -> exchanger.exchange(offeredValue)));
            }

            Set<Integer> receivedValues = new HashSet<>();
            for (int i = 0; i < parties; i++)
            {
                int received = futures.get(i).get(5, TimeUnit.SECONDS);
                // Confirm no thread receives its own offered value
                assertNotEquals(i, received, "A thread received its own offered value: " + i);
                receivedValues.add(received);
            }

            // Confirm that all offered values were received (permutation conservation)
            assertEquals(parties, receivedValues.size());
            for (int i = 0; i < parties; i++)
            {
                assertTrue(receivedValues.contains(i));
            }
        }
        finally
        {
            executor.shutdown();
        }
    }

    @Test
    void testSequentialMultipleGroups() throws Exception
    {
        int parties = 3;
        MultiExchanger<String> exchanger = new MultiExchanger<>(parties);
        ExecutorService executor = Executors.newFixedThreadPool(parties);
        try
        {
            // First group
            Future<String> f1 = executor.submit(() -> exchanger.exchange("A"));
            Future<String> f2 = executor.submit(() -> exchanger.exchange("B"));
            Future<String> f3 = executor.submit(() -> exchanger.exchange("C"));

            Set<String> firstGroupResults = new HashSet<>();
            firstGroupResults.add(f1.get(2, TimeUnit.SECONDS));
            firstGroupResults.add(f2.get(2, TimeUnit.SECONDS));
            firstGroupResults.add(f3.get(2, TimeUnit.SECONDS));

            assertTrue(firstGroupResults.contains("A"));
            assertTrue(firstGroupResults.contains("B"));
            assertTrue(firstGroupResults.contains("C"));

            // Second group
            Future<String> f4 = executor.submit(() -> exchanger.exchange("D"));
            Future<String> f5 = executor.submit(() -> exchanger.exchange("E"));
            Future<String> f6 = executor.submit(() -> exchanger.exchange("F"));

            Set<String> secondGroupResults = new HashSet<>();
            secondGroupResults.add(f4.get(2, TimeUnit.SECONDS));
            secondGroupResults.add(f5.get(2, TimeUnit.SECONDS));
            secondGroupResults.add(f6.get(2, TimeUnit.SECONDS));

            assertTrue(secondGroupResults.contains("D"));
            assertTrue(secondGroupResults.contains("E"));
            assertTrue(secondGroupResults.contains("F"));
        }
        finally
        {
            executor.shutdown();
        }
    }

    @Test
    void testTimeoutException() throws Exception
    {
        MultiExchanger<String> exchanger = new MultiExchanger<>(3);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
        {
            Future<String> f1 = executor.submit(() -> exchanger.exchange("A", 100, TimeUnit.MILLISECONDS));
            Future<String> f2 = executor.submit(() -> exchanger.exchange("B", 100, TimeUnit.MILLISECONDS));

            assertThrows(Exception.class, () -> f1.get(2, TimeUnit.SECONDS));
            assertThrows(Exception.class, () -> f2.get(2, TimeUnit.SECONDS));
        }
        finally
        {
            executor.shutdown();
        }
    }

    @Test
    void testInterruptedException() throws Exception
    {
        MultiExchanger<String> exchanger = new MultiExchanger<>(3);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> exceptionThrown = new AtomicReference<>();

        Thread t = new Thread(() -> {
            try
            {
                latch.countDown();
                exchanger.exchange("A");
                fail("Should have thrown InterruptedException");
            }
            catch (InterruptedException e)
            {
                exceptionThrown.set(e);
            }
        });

        t.start();
        latch.await();
        // Give the thread a short moment to block on exchange
        Thread.sleep(100);

        t.interrupt();
        t.join(1000);

        assertTrue(exceptionThrown.get() instanceof InterruptedException);
    }

    @Test
    void testNullUnitThrows()
    {
        MultiExchanger<String> exchanger = new MultiExchanger<>(2);
        assertThrows(NullPointerException.class, () -> exchanger.exchange("A", 1, null));
    }
}
