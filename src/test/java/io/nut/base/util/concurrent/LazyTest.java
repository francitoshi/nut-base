/*
 *  LazyTest.java
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
package io.nut.base.util.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Lazy<T>")
class LazyTest 
{

    // ------------------------------------------------------------------ //
    // Construction
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("constructor")
    class Constructor 
    {

        @Test
        @DisplayName("rejects a null supplier")
        void nullSupplierThrows() 
        {
            assertThrows(NullPointerException.class, () -> new Lazy<>(null));
        }

        @Test
        @DisplayName("does not invoke the supplier on construction")
        void supplierNotCalledOnConstruction() 
        {
            AtomicInteger calls = new AtomicInteger();
            new Lazy<>(() -> 
            { 
                calls.incrementAndGet(); 
                return "value";
            });
            assertEquals(0, calls.get());
        }
    }

    // ------------------------------------------------------------------ //
    // Lazy.of
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("Lazy.of(value)")
    class Of 
    {

        @Test
        @DisplayName("is immediately initialized")
        void isInitialized() 
        {
            assertTrue(Lazy.of("hello").isInitialized());
        }

        @Test
        @DisplayName("returns the wrapped value")
        void returnsValue() 
        {
            assertEquals("hello", Lazy.of("hello").get());
        }

        @Test
        @DisplayName("accepts null value")
        void acceptsNull() 
        {
            Lazy<String> lazy = Lazy.of(null);
            assertTrue(lazy.isInitialized());
            assertNull(lazy.get());
        }
    }

    // ------------------------------------------------------------------ //
    // get()
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("get()")
    class Get 
    {

        @Test
        @DisplayName("returns the supplier's value")
        void returnsSupplierValue() 
        {
            Lazy<String> lazy = new Lazy<>(() -> "hello");
            assertEquals("hello", lazy.get());
        }

        @Test
        @DisplayName("accepts a null return value from the supplier")
        void acceptsNullValue() 
        {
            Lazy<String> lazy = new Lazy<>(() -> null);
            assertNull(lazy.get());
            assertTrue(lazy.isInitialized());
        }

        @Test
        @DisplayName("invokes the supplier exactly once across repeated calls")
        void supplierCalledOnce() 
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<Integer> lazy = new Lazy<>(() -> calls.incrementAndGet());

            lazy.get();
            lazy.get();
            lazy.get();

            assertEquals(1, calls.get());
        }

        @Test
        @DisplayName("always returns the same reference")
        void returnsSameReference() 
        {
            Lazy<List<String>> lazy = new Lazy<>(ArrayList::new);
            assertSame(lazy.get(), lazy.get());
        }

        @Test
        @DisplayName("propagates supplier exception to the caller")
        void propagatesException() 
        {
            RuntimeException cause = new RuntimeException("boom");
            Lazy<String> lazy = new Lazy<>(() -> { throw cause; });

            RuntimeException thrown = assertThrows(RuntimeException.class, lazy::get);
            assertSame(cause, thrown);
        }

        @Test
        @DisplayName("remains uninitialized after a supplier exception")
        void remainsUninitializedAfterException() 
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<String> lazy = new Lazy<>(() -> 
            {
                if (calls.incrementAndGet() < 3) throw new RuntimeException("not yet");
                return "ok";
            });

            assertThrows(RuntimeException.class, lazy::get);
            assertThrows(RuntimeException.class, lazy::get);
            assertFalse(lazy.isInitialized());

            assertEquals("ok", lazy.get());   // third call succeeds
            assertTrue(lazy.isInitialized());
            assertEquals(3, calls.get());
        }
    }

    // ------------------------------------------------------------------ //
    // isInitialized()
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("isInitialized()")
    class IsInitialized 
    {

        @Test
        @DisplayName("returns false before first get()")
        void falseBeforeGet() 
        {
            assertFalse(new Lazy<>(() -> "x").isInitialized());
        }

        @Test
        @DisplayName("returns true after successful get()")
        void trueAfterGet() 
        {
            Lazy<String> lazy = new Lazy<>(() -> "x");
            lazy.get();
            assertTrue(lazy.isInitialized());
        }

        @Test
        @DisplayName("returns false after a failed get()")
        void falseAfterFailedGet() 
        {
            Lazy<String> lazy = new Lazy<>(() -> { throw new RuntimeException(); });
            assertThrows(RuntimeException.class, lazy::get);
            assertFalse(lazy.isInitialized());
        }
    }

    // ------------------------------------------------------------------ //
    // getIfInitialized()
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getIfInitialized()")
    class GetIfInitialized 
    {

        @Test
        @DisplayName("returns empty before initialization")
        void emptyBeforeInit() 
        {
            assertEquals(Optional.empty(), new Lazy<>(() -> "x").getIfInitialized());
        }

        @Test
        @DisplayName("returns the value after initialization")
        void presentAfterInit() 
        {
            Lazy<String> lazy = new Lazy<>(() -> "hello");
            lazy.get();
            assertEquals(Optional.of("hello"), lazy.getIfInitialized());
        }

        @Test
        @DisplayName("returns empty Optional for a null value (not absent)")
        void emptyOptionalForNullValue() 
        {
            Lazy<String> lazy = new Lazy<>(() -> null);
            lazy.get();
            // isInitialized is true, but Optional.ofNullable(null) == empty
            assertTrue(lazy.isInitialized());
            assertEquals(Optional.empty(), lazy.getIfInitialized());
        }

        @Test
        @DisplayName("does not trigger initialization")
        void doesNotTriggerInit() 
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<String> lazy = new Lazy<>(() -> 
            {
                calls.incrementAndGet();
                return "x";
            });
            lazy.getIfInitialized();
            assertEquals(0, calls.get());
        }
    }

    // ------------------------------------------------------------------ //
    // toString()
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("toString()")
    class ToStr 
    {

        @Test
        @DisplayName("indicates uninitialized state")
        void uninitializedMessage() 
        {
            String s = new Lazy<>(() -> "x").toString();
            assertTrue(s.contains("uninitialized"), "expected 'uninitialized' in: " + s);
        }

        @Test
        @DisplayName("includes the value after initialization")
        void valueAfterInit() 
        {
            Lazy<String> lazy = new Lazy<>(() -> "hello");
            lazy.get();
            assertTrue(lazy.toString().contains("hello"));
        }

        @Test
        @DisplayName("handles null value in toString")
        void nullValue() 
        {
            Lazy<String> lazy = new Lazy<>(() -> null);
            lazy.get();
            assertDoesNotThrow(lazy::toString);
        }
    }

    // ------------------------------------------------------------------ //
    // Concurrency
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("concurrency")
    class Concurrency 
    {

        private static final int THREADS = 50;
        private static final int REPETITIONS = 5;

        @RepeatedTest(REPETITIONS)
        @DisplayName("supplier is called exactly once under contention")
        void supplierCalledOnceUnderContention() throws InterruptedException 
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<Integer> lazy = new Lazy<>(() -> 
            {
                // Simulate work to increase race window
                try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return calls.incrementAndGet();
            });

            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch start = new CountDownLatch(1);
            List<Integer> results = Collections.synchronizedList(new ArrayList<>());

            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            for (int i = 0; i < THREADS; i++) 
            {
                pool.submit(() -> 
                {
                    ready.countDown();
                    try 
                    {
                        start.await();
                        results.add(lazy.get());
                    } 
                    catch (InterruptedException e) 
                    {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            ready.await();
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

            assertEquals(THREADS, results.size());
            assertEquals(1, calls.get(), "supplier must be called exactly once");
            // All threads must see the same value
            assertTrue(results.stream().allMatch(v -> v.equals(results.get(0))));
        }

        @RepeatedTest(REPETITIONS)
        @DisplayName("isInitialized is consistent with get() under contention")
        void isInitializedConsistency() throws InterruptedException 
        {
            Lazy<String> lazy = new Lazy<>(() -> "value");
            CountDownLatch done = new CountDownLatch(THREADS);
            ExecutorService pool = Executors.newFixedThreadPool(THREADS);

            for (int i = 0; i < THREADS; i++) 
            {
                pool.submit(() -> 
                {
                    try 
                    {
                        lazy.get();
                        assertTrue(lazy.isInitialized());
                    } 
                    finally 
                    {
                        done.countDown();
                    }
                });
            }

            assertTrue(done.await(5, TimeUnit.SECONDS));
            pool.shutdown();
        }

        @RepeatedTest(REPETITIONS)
        @DisplayName("retries after intermittent supplier failure under contention")
        void retryAfterFailureUnderContention() throws InterruptedException 
        {
            AtomicInteger attempts = new AtomicInteger();
            Lazy<String> lazy = new Lazy<>(() -> 
            {
                if (attempts.incrementAndGet() == 1) throw new RuntimeException("first attempt fails");
                return "ok";
            });

            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch start = new CountDownLatch(1);
            List<String> successes = Collections.synchronizedList(new ArrayList<>());
            List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            for (int i = 0; i < THREADS; i++) 
            {
                pool.submit(() -> 
                {
                    ready.countDown();
                    try 
                    {
                        start.await();
                        successes.add(lazy.get());
                    } 
                    catch (RuntimeException e) 
                    {
                        failures.add(e);
                    } 
                    catch (InterruptedException e) 
                    {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            ready.await();
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

            // At least one thread must have succeeded eventually
            assertFalse(successes.isEmpty());
            assertTrue(lazy.isInitialized());
            assertEquals("ok", lazy.get());
        }
    }
}