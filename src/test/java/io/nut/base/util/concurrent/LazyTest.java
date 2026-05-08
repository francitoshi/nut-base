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
    // ================================================================== //
    // PERMANENT MODE
    // ================================================================== //

    // ------------------------------------------------------------------ //
    // Construction — permanent
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("constructor (permanent)")
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

        @Test
        @DisplayName("isEphemeral() returns false")
        void isNotEphemeral()
        {
            assertFalse(new Lazy<>(() -> "x").isEphemeral());
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

        @Test
        @DisplayName("isEphemeral() returns false")
        void isNotEphemeral()
        {
            assertFalse(Lazy.of("x").isEphemeral());
        }
    }

    // ------------------------------------------------------------------ //
    // get() — permanent
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("get() (permanent)")
    class Get
    {
        @Test
        @DisplayName("returns the supplier's value")
        void returnsSupplierValue()
        {
            assertEquals("hello", new Lazy<>(() -> "hello").get());
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
            Lazy<Integer> lazy = new Lazy<>(calls::incrementAndGet);

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

            assertEquals("ok", lazy.get());
            assertTrue(lazy.isInitialized());
            assertEquals(3, calls.get());
        }
    }

    // ------------------------------------------------------------------ //
    // isInitialized() — permanent
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("isInitialized() (permanent)")
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
    // getIfInitialized() — permanent
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getIfInitialized() (permanent)")
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
    // toString() — permanent
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("toString() (permanent)")
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
    // Concurrency — permanent
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("concurrency (permanent)")
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
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
            }

            ready.await();
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

            assertEquals(THREADS, results.size());
            assertEquals(1, calls.get(), "supplier must be called exactly once");
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
                    finally { done.countDown(); }
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
                    catch (RuntimeException e) { failures.add(e); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
            }

            ready.await();
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

            assertFalse(successes.isEmpty());
            assertTrue(lazy.isInitialized());
            assertEquals("ok", lazy.get());
        }
    }

    // ================================================================== //
    // EPHEMERAL MODE
    // ================================================================== //

    // ------------------------------------------------------------------ //
    // Construction — ephemeral
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("constructor (ephemeral)")
    class EphemeralConstructor
    {
        @Test
        @DisplayName("rejects ttl = 0")
        void rejectsTtlZero()
        {
            assertThrows(IllegalArgumentException.class,
                    () -> new Lazy<>(0L, () -> "x"));
        }

        @Test
        @DisplayName("rejects negative ttl")
        void rejectsNegativeTtl()
        {
            assertThrows(IllegalArgumentException.class,
                    () -> new Lazy<>(-1L, () -> "x"));
        }

        @Test
        @DisplayName("rejects null supplier (millis constructor)")
        void rejectsNullSupplierMillis()
        {
            assertThrows(NullPointerException.class,
                    () -> new Lazy<String>(1_000L, (java.util.function.Supplier<String>) null));
        }

        @Test
        @DisplayName("rejects null TimeUnit")
        void rejectsNullTimeUnit()
        {
            assertThrows(NullPointerException.class,
                    () -> new Lazy<>(1L, null, () -> "x"));
        }

        @Test
        @DisplayName("rejects null supplier (TimeUnit constructor)")
        void rejectsNullSupplierTimeUnit()
        {
            assertThrows(NullPointerException.class,
                    () -> new Lazy<String>(1L, TimeUnit.SECONDS, null));
        }

        @Test
        @DisplayName("constructs successfully with millis constructor")
        void constructsWithMillis()
        {
            assertDoesNotThrow(() -> new Lazy<>(500L, () -> 42));
        }

        @Test
        @DisplayName("constructs successfully with TimeUnit constructor")
        void constructsWithTimeUnit()
        {
            assertDoesNotThrow(() -> new Lazy<>(1L, TimeUnit.SECONDS, () -> 42));
        }

        @Test
        @DisplayName("isEphemeral() returns true")
        void isEphemeral()
        {
            assertTrue(new Lazy<>(1_000L, () -> "x").isEphemeral());
        }

        @Test
        @DisplayName("does not invoke the supplier on construction")
        void supplierNotCalledOnConstruction()
        {
            AtomicInteger calls = new AtomicInteger();
            new Lazy<>(1_000L, () -> { calls.incrementAndGet(); return "v"; });
            assertEquals(0, calls.get());
        }

        @Test
        @DisplayName("isInitialized() returns false before first get()")
        void notInitializedBeforeGet()
        {
            assertFalse(new Lazy<>(1_000L, () -> "x").isInitialized());
        }
    }

    // ------------------------------------------------------------------ //
    // get() — ephemeral, within TTL
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("get() within TTL (ephemeral)")
    class EphemeralGetWithinTtl
    {
        @Test
        @DisplayName("returns the supplier's value on first call")
        void returnsValueOnFirstCall()
        {
            assertEquals("hello", new Lazy<>(1_000L, () -> "hello").get());
        }

        @Test
        @DisplayName("supplier is called exactly once within TTL")
        void supplierCalledOnceWithinTtl()
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<Integer> lazy = new Lazy<>(10_000L, calls::incrementAndGet);

            lazy.get();
            lazy.get();
            lazy.get();

            assertEquals(1, calls.get());
        }

        @Test
        @DisplayName("all calls within TTL return the same cached value")
        void returnsSameCachedValue()
        {
            AtomicInteger counter = new AtomicInteger();
            Lazy<Integer> lazy = new Lazy<>(10_000L, counter::incrementAndGet);

            Integer first  = lazy.get();
            Integer second = lazy.get();
            Integer third  = lazy.get();

            assertEquals(first, second);
            assertEquals(first, third);
        }

        @Test
        @DisplayName("accepts a null return value from the supplier")
        void acceptsNullValue()
        {
            Lazy<String> lazy = new Lazy<>(1_000L, () -> null);
            assertNull(lazy.get());
            assertTrue(lazy.isInitialized());
        }

        @Test
        @DisplayName("isInitialized() returns true immediately after get()")
        void isInitializedAfterGet()
        {
            Lazy<String> lazy = new Lazy<>(10_000L, () -> "x");
            lazy.get();
            assertTrue(lazy.isInitialized());
        }
    }

    // ------------------------------------------------------------------ //
    // get() — ephemeral, after TTL expires
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("get() after TTL expiry (ephemeral)")
    class EphemeralGetAfterExpiry
    {
        @Test
        @DisplayName("supplier is called again after TTL elapses")
        void supplierCalledAgainAfterTtl() throws InterruptedException
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<Integer> lazy = new Lazy<>(50L, calls::incrementAndGet);

            lazy.get();
            Thread.sleep(100);
            lazy.get();

            assertEquals(2, calls.get());
        }

        @Test
        @DisplayName("returns updated value after TTL elapses")
        void returnsUpdatedValueAfterTtl() throws InterruptedException
        {
            AtomicInteger counter = new AtomicInteger();
            Lazy<Integer> lazy = new Lazy<>(50L, counter::incrementAndGet);

            int first = lazy.get();
            Thread.sleep(100);
            int second = lazy.get();

            assertEquals(1, first);
            assertEquals(2, second);
        }

        @Test
        @DisplayName("supplier is not called again before TTL elapses")
        void supplierNotCalledBeforeTtl() throws InterruptedException
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<Integer> lazy = new Lazy<>(10_000L, calls::incrementAndGet);

            lazy.get();
            Thread.sleep(20);   // well within TTL
            lazy.get();

            assertEquals(1, calls.get());
        }

        @Test
        @DisplayName("isInitialized() returns false after TTL elapses")
        void isInitializedFalseAfterExpiry() throws InterruptedException
        {
            Lazy<String> lazy = new Lazy<>(50L, () -> "x");
            lazy.get();
            Thread.sleep(100);
            assertFalse(lazy.isInitialized());
        }

        @Test
        @DisplayName("value refreshes repeatedly on each expiry cycle")
        void refreshesOnEachExpiryCycle() throws InterruptedException
        {
            AtomicInteger counter = new AtomicInteger();
            Lazy<Integer> lazy = new Lazy<>(50L, counter::incrementAndGet);

            assertEquals(1, (int) lazy.get());
            Thread.sleep(100);
            assertEquals(2, (int) lazy.get());
            Thread.sleep(100);
            assertEquals(3, (int) lazy.get());
        }
    }

    // ------------------------------------------------------------------ //
    // isInitialized() — ephemeral
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("isInitialized() (ephemeral)")
    class EphemeralIsInitialized
    {
        @Test
        @DisplayName("returns false before any get()")
        void falseBeforeGet()
        {
            assertFalse(new Lazy<>(1_000L, () -> "x").isInitialized());
        }

        @Test
        @DisplayName("returns true immediately after a successful get()")
        void trueAfterGet()
        {
            Lazy<String> lazy = new Lazy<>(10_000L, () -> "x");
            lazy.get();
            assertTrue(lazy.isInitialized());
        }

        @Test
        @DisplayName("returns false after TTL elapses")
        void falseAfterTtlElapses() throws InterruptedException
        {
            Lazy<String> lazy = new Lazy<>(50L, () -> "x");
            lazy.get();
            Thread.sleep(100);
            assertFalse(lazy.isInitialized());
        }

        @Test
        @DisplayName("returns true again after refresh following expiry")
        void trueAgainAfterRefresh() throws InterruptedException
        {
            Lazy<String> lazy = new Lazy<>(50L, () -> "x");
            lazy.get();
            Thread.sleep(100);
            lazy.get();         // triggers refresh
            assertTrue(lazy.isInitialized());
        }

        @Test
        @DisplayName("returns false after a failed get()")
        void falseAfterFailedGet()
        {
            Lazy<String> lazy = new Lazy<>(1_000L, () -> { throw new RuntimeException(); });
            assertThrows(RuntimeException.class, lazy::get);
            assertFalse(lazy.isInitialized());
        }
    }

    // ------------------------------------------------------------------ //
    // getIfInitialized() — ephemeral
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getIfInitialized() (ephemeral)")
    class EphemeralGetIfInitialized
    {
        @Test
        @DisplayName("returns empty before any get()")
        void emptyBeforeGet()
        {
            assertEquals(Optional.empty(),
                    new Lazy<>(1_000L, () -> "x").getIfInitialized());
        }

        @Test
        @DisplayName("returns the value within TTL")
        void presentWithinTtl()
        {
            Lazy<String> lazy = new Lazy<>(10_000L, () -> "hello");
            lazy.get();
            assertEquals(Optional.of("hello"), lazy.getIfInitialized());
        }

        @Test
        @DisplayName("returns empty after TTL elapses")
        void emptyAfterExpiry() throws InterruptedException
        {
            Lazy<String> lazy = new Lazy<>(50L, () -> "hello");
            lazy.get();
            Thread.sleep(100);
            assertEquals(Optional.empty(), lazy.getIfInitialized());
        }

        @Test
        @DisplayName("does not trigger initialization or refresh")
        void doesNotTriggerRefresh()
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<String> lazy = new Lazy<>(1_000L, () ->
            {
                calls.incrementAndGet();
                return "x";
            });
            lazy.getIfInitialized();
            assertEquals(0, calls.get());
        }
    }

    // ------------------------------------------------------------------ //
    // Error handling — ephemeral
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("error handling (ephemeral)")
    class EphemeralErrorHandling
    {
        @Test
        @DisplayName("propagates supplier exception to the caller")
        void propagatesException()
        {
            RuntimeException cause = new RuntimeException("boom");
            Lazy<String> lazy = new Lazy<>(1_000L, () -> { throw cause; });

            RuntimeException thrown = assertThrows(RuntimeException.class, lazy::get);
            assertSame(cause, thrown);
        }

        @Test
        @DisplayName("remains uninitialized after a supplier exception")
        void remainsUninitializedAfterException()
        {
            Lazy<String> lazy = new Lazy<>(1_000L, () -> { throw new RuntimeException(); });
            assertThrows(RuntimeException.class, lazy::get);
            assertFalse(lazy.isInitialized());
        }

        @Test
        @DisplayName("retries on next get() after a supplier exception")
        void retriesAfterException()
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<String> lazy = new Lazy<>(10_000L, () ->
            {
                if (calls.incrementAndGet() < 3) throw new RuntimeException("not yet");
                return "ok";
            });

            assertThrows(RuntimeException.class, lazy::get);
            assertThrows(RuntimeException.class, lazy::get);
            assertFalse(lazy.isInitialized());

            assertEquals("ok", lazy.get());
            assertTrue(lazy.isInitialized());
            assertEquals(3, calls.get());
        }
    }

    // ------------------------------------------------------------------ //
    // toString() — ephemeral
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("toString() (ephemeral)")
    class EphemeralToStr
    {
        @Test
        @DisplayName("indicates uninitialized state before first get()")
        void uninitializedMessage()
        {
            String s = new Lazy<>(1_000L, () -> "x").toString();
            assertTrue(s.contains("uninitialized"), "expected 'uninitialized' in: " + s);
        }

        @Test
        @DisplayName("includes the value within TTL")
        void valueWithinTtl()
        {
            Lazy<String> lazy = new Lazy<>(10_000L, () -> "hello");
            lazy.get();
            assertTrue(lazy.toString().contains("hello"));
        }

        @Test
        @DisplayName("indicates expired state after TTL elapses")
        void expiredMessage() throws InterruptedException
        {
            Lazy<String> lazy = new Lazy<>(50L, () -> "hello");
            lazy.get();
            Thread.sleep(100);
            String s = lazy.toString();
            assertTrue(s.contains("expired"), "expected 'expired' in: " + s);
        }

        @Test
        @DisplayName("handles null value in toString")
        void nullValue()
        {
            Lazy<String> lazy = new Lazy<>(10_000L, () -> null);
            lazy.get();
            assertDoesNotThrow(lazy::toString);
        }
    }

    // ------------------------------------------------------------------ //
    // TimeUnit constructor — ephemeral
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("TimeUnit constructor (ephemeral)")
    class EphemeralTimeUnitConstructor
    {
        @Test
        @DisplayName("1-second TTL does not expire after 50 ms")
        void doesNotExpireWithinTtl() throws InterruptedException
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<Integer> lazy = new Lazy<>(1L, TimeUnit.SECONDS, calls::incrementAndGet);

            lazy.get();
            Thread.sleep(50);
            lazy.get();

            assertEquals(1, calls.get());
        }

        @Test
        @DisplayName("50-ms TTL expressed via TimeUnit expires correctly")
        void expiresWithTimeUnit() throws InterruptedException
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<Integer> lazy =
                    new Lazy<>(50L, TimeUnit.MILLISECONDS, calls::incrementAndGet);

            lazy.get();
            Thread.sleep(100);
            lazy.get();

            assertEquals(2, calls.get());
        }
    }

    // ------------------------------------------------------------------ //
    // Concurrency — ephemeral
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("concurrency (ephemeral)")
    class EphemeralConcurrency
    {
        private static final int THREADS = 50;
        private static final int REPETITIONS = 5;

        @RepeatedTest(REPETITIONS)
        @DisplayName("supplier is called exactly once within TTL under contention")
        void supplierCalledOnceUnderContentionWithinTtl() throws InterruptedException
        {
            AtomicInteger calls = new AtomicInteger();
            Lazy<Integer> lazy = new Lazy<>(10_000L, () ->
            {
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
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
            }

            ready.await();
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

            assertEquals(THREADS, results.size());
            assertEquals(1, calls.get(), "supplier must be called exactly once within TTL");
            assertTrue(results.stream().allMatch(v -> v.equals(results.get(0))));
        }

        @RepeatedTest(REPETITIONS)
        @DisplayName("supplier is re-invoked after expiry under contention")
        void supplierReInvokedAfterExpiryUnderContention() throws InterruptedException
        {
            AtomicInteger calls = new AtomicInteger();
            // Large TTL so the first call caches, then we force expiry via sleep
            Lazy<Integer> lazy = new Lazy<>(50L, calls::incrementAndGet);

            lazy.get();               // first computation
            Thread.sleep(100);        // let it expire

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
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
            }

            ready.await();
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

            assertEquals(THREADS, results.size());
            // All threads must have received the refreshed value (2)
            assertTrue(results.stream().allMatch(v -> v.equals(2)));
        }
    }
}
