/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Once<T>")
class OnceTest
{
    // ------------------------------------------------------------------ //
    // of(T)
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("of(value) returns an already-assigned cell")
    void ofReturnsAssigned()
    {
        Once<String> once = Once.of("hello");
        assertEquals("hello", once.get());
    }

    @Test
    @DisplayName("of(null) rejects the null value")
    void ofNullThrows()
    {
        assertThrows(NullPointerException.class, () -> Once.of(null));
    }

    // ------------------------------------------------------------------ //
    // get()
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("get() returns null on an unassigned cell")
    void getReturnsNullWhenUnassigned()
    {
        Once<String> once = new Once<>();
        assertNull(once.get());
    }

    // ------------------------------------------------------------------ //
    // set(T)
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("first set() wins and returns true")
    void firstSetWins()
    {
        Once<String> once = new Once<>();
        assertTrue(once.set("first"));
        assertEquals("first", once.get());
    }

    @Test
    @DisplayName("second set() is rejected and returns false")
    void secondSetRejected()
    {
        Once<String> once = new Once<>();
        assertTrue(once.set("first"));
        assertFalse(once.set("second"));
        assertEquals("first", once.get());
    }

    @Test
    @DisplayName("set() on an of() cell is rejected")
    void setRejectedAfterOf()
    {
        Once<String> once = Once.of("hello");
        assertFalse(once.set("world"));
        assertEquals("hello", once.get());
    }

    @Test
    @DisplayName("set(null) rejects the null value")
    void setNullThrows()
    {
        Once<String> once = new Once<>();
        assertThrows(NullPointerException.class, () -> once.set((String) null));
        assertNull(once.get());
    }

    // ------------------------------------------------------------------ //
    // set(Supplier<T>)
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("set(Supplier) computes and assigns the value")
    void setSupplierComputes()
    {
        Once<String> once = new Once<>();
        AtomicInteger calls = new AtomicInteger();
        assertTrue(once.set(() ->
        {
            calls.incrementAndGet();
            return "computed";
        }));
        assertEquals("computed", once.get());
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("set(Supplier) does not invoke the supplier when already assigned")
    void setSupplierNotInvokedWhenAssigned()
    {
        Once<String> once = Once.of("hello");
        AtomicInteger calls = new AtomicInteger();
        assertFalse(once.set(() ->
        {
            calls.incrementAndGet();
            return "computed";
        }));
        assertEquals("hello", once.get());
        assertEquals(0, calls.get());
    }

    @Test
    @DisplayName("set(Supplier) rejects a null supplier")
    void setSupplierNullThrows()
    {
        assertThrows(NullPointerException.class, () -> new Once<String>().set((Supplier<String>) null));
    }

    @Test
    @DisplayName("a failed set(Supplier) leaves the cell unassigned and allows retry")
    void setSupplierFailedRetries()
    {
        Once<String> once = new Once<>();
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> once.set(() ->
        {
            calls.incrementAndGet();
            throw new IllegalStateException("boom");
        }));
        assertNull(once.get());
        assertTrue(once.set(() ->
        {
            calls.incrementAndGet();
            return "ok";
        }));
        assertEquals("ok", once.get());
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("a set(Supplier) yielding null leaves the cell unassigned and allows retry")
    void setSupplierNullResultRetries()
    {
        Once<String> once = new Once<>();
        AtomicInteger calls = new AtomicInteger();
        assertThrows(NullPointerException.class, () -> once.set(() ->
        {
            calls.incrementAndGet();
            return null;
        }));
        assertNull(once.get());
        assertTrue(once.set(() ->
        {
            calls.incrementAndGet();
            return "ok";
        }));
        assertEquals("ok", once.get());
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("only one of ten threads wins a set(Supplier) assignment")
    void setSupplierOnlyOneWinsUnderConcurrency() throws Exception
    {
        Once<Integer> once = new Once<>();
        AtomicInteger counter = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try
        {
            CountDownLatch start = new CountDownLatch(1);
            for (int i = 0; i < 10; i++)
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
                    once.set(counter::incrementAndGet);
                });
            }
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            assertEquals(1, counter.get());
            assertEquals(1, once.get().intValue());
        }
        finally
        {
            executor.shutdownNow();
        }
    }

    // ------------------------------------------------------------------ //
    // getOrWait()
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("getOrWait() returns immediately when already assigned")
    void getOrWaitReturnsWhenAssigned() throws InterruptedException
    {
        Once<String> once = Once.of("hello");
        assertEquals("hello", once.getOrWait());
    }

    @Test
    @DisplayName("getOrWait() blocks until another thread assigns")
    void getOrWaitBlocks() throws Exception
    {
        Once<String> once = new Once<>();
        CountDownLatch setterStarted = new CountDownLatch(1);
        CountDownLatch setterDone = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
        {
            AtomicReference<String> result = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            Thread waiter = new Thread(() ->
            {
                try
                {
                    result.set(once.getOrWait());
                }
                catch (Throwable ex)
                {
                    error.set(ex);
                }
            });
            waiter.start();

            Thread.sleep(50);
            assertNull(result.get(), "waiter must not return before assignment");

            setterStarted.countDown();
            executor.execute(() ->
            {
                once.set("late");
                setterDone.countDown();
            });

            assertTrue(setterStarted.await(1, TimeUnit.SECONDS));
            assertTrue(setterDone.await(1, TimeUnit.SECONDS));
            waiter.join(1000);

            assertNull(error.get());
            assertEquals("late", result.get());
        }
        finally
        {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("getOrWait() throws InterruptedException when interrupted while waiting")
    void getOrWaitInterrupted() throws Exception
    {
        Once<String> once = new Once<>();

        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread waiter = new Thread(() ->
        {
            try
            {
                once.getOrWait();
            }
            catch (Throwable ex)
            {
                error.set(ex);
            }
        });
        waiter.start();

        Thread.sleep(50);
        waiter.interrupt();
        waiter.join(1000);

        assertTrue(error.get() instanceof InterruptedException, "expected InterruptedException but got: " + error.get());
        assertFalse(waiter.isInterrupted(), "interrupt status must be cleared");
    }

    // ------------------------------------------------------------------ //
    // getOrSet(Supplier)
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("rejects a null supplier")
    void nullSupplierThrows()
    {
        assertThrows(NullPointerException.class, () -> new Once<String>().getOrSet(null));
    }

    @Test
    @DisplayName("returns the value without invoking the supplier when assigned")
    void supplierNotInvokedWhenAssigned() throws InterruptedException
    {
        Once<String> once = Once.of("hello");
        AtomicInteger calls = new AtomicInteger();
        assertEquals("hello", once.getOrSet(() ->
        {
            calls.incrementAndGet();
            return "computed";
        }));
        assertEquals(0, calls.get());
    }

    @Test
    @DisplayName("computes the value exactly once")
    void computesOnce() throws InterruptedException
    {
        Once<String> once = new Once<>();
        AtomicInteger calls = new AtomicInteger();
        assertEquals("computed", once.getOrSet(() ->
        {
            calls.incrementAndGet();
            return "computed";
        }));
        assertEquals(1, calls.get());
        assertEquals("computed", once.getOrSet(() ->
        {
            calls.incrementAndGet();
            return "other";
        }));
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("only one thread runs the supplier under concurrency")
    void onlyOneSupplierRuns() throws Exception
    {
        Once<String> once = new Once<>();
        AtomicInteger calls = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try
        {
            List<Object> results = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch start = new CountDownLatch(1);
            for (int i = 0; i < 16; i++)
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
                    try
                    {
                        results.add(once.getOrSet(() ->
                        {
                            calls.incrementAndGet();
                            try
                            {
                                Thread.sleep(10);
                            }
                            catch (InterruptedException ex)
                            {
                                Thread.currentThread().interrupt();
                            }
                            return "computed";
                        }));
                    }
                    catch (InterruptedException ex)
                    {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            start.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

            assertEquals(1, calls.get());
            assertEquals(16, results.size());
            for (Object r : results)
            {
                assertEquals("computed", r);
            }
        }
        finally
        {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("a failed supplier leaves the cell unassigned and allows retry")
    void failedSupplierRetries() throws InterruptedException
    {
        Once<String> once = new Once<>();
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> once.getOrSet(() ->
        {
            calls.incrementAndGet();
            throw new IllegalStateException("boom");
        }));
        assertEquals("ok", once.getOrSet(() ->
        {
            calls.incrementAndGet();
            return "ok";
        }));
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("a supplier yielding null leaves the cell unassigned and allows retry")
    void nullSupplierResultRetries() throws InterruptedException
    {
        Once<String> once = new Once<>();
        AtomicInteger calls = new AtomicInteger();
        assertThrows(NullPointerException.class, () -> once.getOrSet(() ->
        {
            calls.incrementAndGet();
            return null;
        }));
        assertEquals("ok", once.getOrSet(() ->
        {
            calls.incrementAndGet();
            return "ok";
        }));
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("getOrSet() throws InterruptedException when interrupted while waiting for another thread")
    void getOrSetInterrupted() throws Exception
    {
        Once<String> once = new Once<>();
        CountDownLatch computing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
        {
            executor.execute(() ->
            {
                try
                {
                    once.getOrSet(() ->
                    {
                        computing.countDown();
                        try
                        {
                            assertTrue(release.await(1, TimeUnit.SECONDS));
                        }
                        catch (InterruptedException ex)
                        {
                            Thread.currentThread().interrupt();
                        }
                        return "computed";
                    });
                }
                catch (InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                }
            });

            assertTrue(computing.await(1, TimeUnit.SECONDS));

            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread waiter = new Thread(() ->
            {
                try
                {
                    once.getOrSet(() -> "other");
                }
                catch (Throwable ex)
                {
                    error.set(ex);
                }
            });
            waiter.start();

            Thread.sleep(50);
            waiter.interrupt();
            waiter.join(1000);

            release.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

            assertTrue(error.get() instanceof InterruptedException, "expected InterruptedException but got: " + error.get());
            assertFalse(waiter.isInterrupted(), "interrupt status must be cleared");
        }
        finally
        {
            executor.shutdownNow();
        }
    }
}