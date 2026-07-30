/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import io.nut.base.cache.CacheType;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import static org.junit.jupiter.api.Assertions.*;

public class MemoizerTest
{

    @Test
    public void testSupplierMemoization()
    {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = new Memoizer().threadSafe().memoize(counter::incrementAndGet);

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void testThreadSafeSupplierMemoization()
    {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = new Memoizer().threadSafe().memoize(counter::incrementAndGet);

        assertEquals(1, memoized.get());
        assertEquals(1, memoized.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void testFunctionMemoization()
    {
        Memoizer memoizer = new Memoizer(CacheType.HASH_MAP, 100);
        AtomicInteger counter = new AtomicInteger(0);
        Function<String, Integer> func = s -> {
            counter.incrementAndGet();
            return s.length();
        };

        Function<String, Integer> memoized = memoizer.memoize(func);

        assertEquals(5, memoized.apply("hello"));
        assertEquals(5, memoized.apply("hello"));
        assertEquals(3, memoized.apply("foo"));
        assertEquals(5, memoized.apply("hello"));

        assertEquals(2, counter.get());
    }

    @Test
    public void testThreadSafeFunctionMemoization()
    {
        Memoizer memoizer = new Memoizer(CacheType.HASH_MAP, 100).threadSafe();
        AtomicInteger counter = new AtomicInteger(0);
        Function<String, Integer> func = s -> 
        {
            counter.incrementAndGet();
            return s.length();
        };

        Function<String, Integer> memoized = memoizer.memoize(func);

        assertEquals(5, memoized.apply("hello"));
        assertEquals(5, memoized.apply("hello"));
        assertEquals(3, memoized.apply("foo"));

        assertEquals(2, counter.get());
    }

    @Test
    public void testBiFunctionMemoization()
    {
        Memoizer memoizer = new Memoizer();
        AtomicInteger counter = new AtomicInteger(0);
        BiFunction<Integer, Integer, Integer> adder = (a, b) -> 
        {
            counter.incrementAndGet();
            return a + b;
        };

        BiFunction<Integer, Integer, Integer> memoized = memoizer.memoize(adder);

        assertEquals(5, memoized.apply(2, 3));
        assertEquals(5, memoized.apply(2, 3));
        assertEquals(6, memoized.apply(3, 3));
        assertEquals(5, memoized.apply(2, 3));

        assertEquals(2, counter.get());
    }

    @Test
    public void testThreadSafeBiFunctionMemoization()
    {
        Memoizer memoizer = new Memoizer().threadSafe();
        AtomicInteger counter = new AtomicInteger(0);
        BiFunction<Integer, Integer, Integer> adder = (a, b) -> {
            counter.incrementAndGet();
            return a + b;
        };

        BiFunction<Integer, Integer, Integer> memoized = memoizer.memoize(adder);

        assertEquals(5, memoized.apply(2, 3));
        assertEquals(5, memoized.apply(2, 3));
        assertEquals(2, memoized.apply(1, 1));

        assertEquals(2, counter.get());
    }

    @Test
    public void testPredicateMemoization()
    {
        Memoizer memoizer = new Memoizer();
        AtomicInteger counter = new AtomicInteger(0);
        Predicate<String> pred = s -> {
            counter.incrementAndGet();
            return s.startsWith("a");
        };

        Predicate<String> memoized = memoizer.memoize(pred);

        assertTrue(memoized.test("apple"));
        assertTrue(memoized.test("apple"));
        assertFalse(memoized.test("banana"));
        assertTrue(memoized.test("apple"));

        assertEquals(2, counter.get());
    }

    @Test
    public void testThreadSafePredicateMemoization()
    {
        Memoizer memoizer = new Memoizer().threadSafe();
        AtomicInteger counter = new AtomicInteger(0);
        Predicate<String> pred = s -> {
            counter.incrementAndGet();
            return s.startsWith("a");
        };

        Predicate<String> memoized = memoizer.memoize(pred);

        assertTrue(memoized.test("apple"));
        assertTrue(memoized.test("apple"));
        assertFalse(memoized.test("banana"));

        assertEquals(2, counter.get());
    }

    @Test
    public void testUnaryOperatorMemoization()
    {
        Memoizer memoizer = new Memoizer();
        AtomicInteger counter = new AtomicInteger(0);
        UnaryOperator<String> op = s -> 
        {
            counter.incrementAndGet();
            return s.toUpperCase();
        };

        UnaryOperator<String> memoized = memoizer.memoize(op);

        assertEquals("HELLO", memoized.apply("hello"));
        assertEquals("HELLO", memoized.apply("hello"));
        assertEquals("WORLD", memoized.apply("world"));

        assertEquals(2, counter.get());
    }

    @Test
    public void testThreadSafeUnaryOperatorMemoization()
    {
        Memoizer memoizer = new Memoizer().threadSafe();
        AtomicInteger counter = new AtomicInteger(0);
        UnaryOperator<String> op = s -> 
        {
            counter.incrementAndGet();
            return s.toUpperCase();
        };

        UnaryOperator<String> memoized = memoizer.memoize(op);

        assertEquals("HELLO", memoized.apply("hello"));
        assertEquals("HELLO", memoized.apply("hello"));

        assertEquals(1, counter.get());
    }

    @Test
    public void testIntPredicateMemoization()
    {
        Memoizer memoizer = new Memoizer();
        AtomicInteger counter = new AtomicInteger(0);
        IntPredicate pred = i -> 
        {
            counter.incrementAndGet();
            return i % 2 == 0;
        };

        IntPredicate memoized = memoizer.memoize(pred);

        assertTrue(memoized.test(4));
        assertTrue(memoized.test(4));
        assertFalse(memoized.test(5));
        assertTrue(memoized.test(4));

        assertEquals(2, counter.get());
    }

    @Test
    public void testThreadSafeIntPredicateMemoization()
    {
        Memoizer memoizer = new Memoizer().threadSafe();
        AtomicInteger counter = new AtomicInteger(0);
        IntPredicate pred = i -> {
            counter.incrementAndGet();
            return i % 2 == 0;
        };

        IntPredicate memoized = memoizer.memoize(pred);

        assertTrue(memoized.test(4));
        assertTrue(memoized.test(4));
        assertFalse(memoized.test(5));

        assertEquals(2, counter.get());
    }

    @Test
    public void testThreadSafeMemoizerMethod()
    {
        Memoizer memoizer = new Memoizer(CacheType.HASH_MAP, 100);
        Memoizer tsMemoizer = memoizer.threadSafe();
        assertNotSame(memoizer, tsMemoizer);
        assertSame(tsMemoizer.threadSafe(), tsMemoizer);

        AtomicInteger counter = new AtomicInteger(0);
        Function<String, Integer> func = s -> 
        {
            counter.incrementAndGet();
            return s.length();
        };

        // Calling memoize (not memoizeThreadSafe) on tsMemoizer should return a thread-safe wrapper
        Function<String, Integer> memoized = tsMemoizer.memoize(func);
        assertEquals(5, memoized.apply("hello"));
        assertEquals(5, memoized.apply("hello"));
        assertEquals(1, counter.get());
    }
}
