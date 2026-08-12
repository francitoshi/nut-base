/*
 *  CachedExpEvalTest.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
 *  SPDX-License-Identifier: GPL-3.0-or-later
 *  See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CachedExpEval}.
 */
public class CachedExpEvalTest
{
    private static final AtomicInteger CALLS = new AtomicInteger();

    public static int tick()
    {
        return CALLS.incrementAndGet();
    }

    private static CachedExpEval newEval()
    {
        return new CachedExpEval(16);
    }

    private static CachedExpEval newEvalWithTick() throws Exception
    {
        CachedExpEval eval = newEval();
        eval.addFunction("tick", CachedExpEvalTest.class.getMethod("tick", new Class<?>[0]));
        return eval;
    }

    private static void assertNumeric(String expected, Object actual)
    {
        BigDecimal e = new BigDecimal(expected);
        BigDecimal a = (actual instanceof BigInteger)
                ? new BigDecimal((BigInteger) actual)
                : (BigDecimal) actual;
        assertEquals(0, e.compareTo(a), () -> "expected " + expected + " but was " + actual);
    }

    // ------------------------------------------------------------------
    // construction
    // ------------------------------------------------------------------

    /**
     * Test of the default constructor using DEFAULT_DECIMALS decimals.
     */
    @Test
    public void testDefaultConstructor() throws Exception
    {
        CachedExpEval instance = newEval();
        Object result = instance.eval("1 / 3");
        assertEquals(new BigDecimal("0.3333333333333333"), result);
    }

    /**
     * Test of the constructor with a custom number of decimals.
     */
    @Test
    public void testConstructorWithDecimals() throws Exception
    {
        CachedExpEval instance = new CachedExpEval(16, 2);
        Object result = instance.eval("1 / 3");
        assertEquals(new BigDecimal("0.33"), result);
    }

    /**
     * Test that a negative number of decimals is rejected.
     */
    @Test
    public void testConstructorNegativeDecimals()
    {
        assertThrows(IllegalArgumentException.class, () -> new CachedExpEval(16, -1));
    }

    /**
     * Test that a non-positive capacity is rejected.
     */
    @Test
    public void testConstructorInvalidCapacity()
    {
        assertThrows(IllegalArgumentException.class, () -> new CachedExpEval(0));
        assertThrows(IllegalArgumentException.class, () -> new CachedExpEval(-1));
    }

    // ------------------------------------------------------------------
    // caching
    // ------------------------------------------------------------------

    /**
     * Test that evaluating the same expression twice returns the cached result
     * without re-evaluating it.
     */
    @Test
    public void testRepeatedEvaluationIsCached() throws Exception
    {
        CALLS.set(0);
        CachedExpEval eval = newEvalWithTick();

        assertNumeric("1", eval.eval("tick()"));
        assertNumeric("1", eval.eval("tick()"));
        assertNumeric("1", eval.eval("tick()"));

        assertEquals(1, CALLS.get());
    }

    /**
     * Test that two different expressions evaluate independently and are kept
     * in the cache.
     */
    @Test
    public void testDistinctExpressionsAreCached() throws Exception
    {
        CALLS.set(0);
        CachedExpEval eval = newEvalWithTick();

        assertNumeric("1", eval.eval("tick()"));
        assertNumeric("3", eval.eval("tick() + 1"));
        assertNumeric("3", eval.eval("tick() + 1"));
        assertNumeric("3", eval.eval("tick() + 1"));

        assertEquals(2, CALLS.get());
    }

    /**
     * Test that expressions are evicted from the cache once the capacity is
     * exceeded, forcing a re-evaluation.
     */
    @Test
    public void testEvictionOverCapacity() throws Exception
    {
        CALLS.set(0);
        CachedExpEval eval = new CachedExpEval(2);
        eval.addFunction("tick", CachedExpEvalTest.class.getMethod("tick", new Class<?>[0]));

        assertNumeric("1", eval.eval("tick()"));
        assertNumeric("12", eval.eval("tick() + 10"));
        assertNumeric("103", eval.eval("tick() + 100"));

        assertNumeric("4", eval.eval("tick()"));

        assertEquals(4, CALLS.get());
    }

    /**
     * Test that arithmetic and functions behave as in ExpEval.
     */
    @Test
    public void testEvaluationDelegatesToExpEval() throws Exception
    {
        CachedExpEval eval = newEval();
        eval.addFunction("sqrt", Math::sqrt);

        assertNumeric("3", eval.eval("sqrt(9)"));
        assertNumeric("3.3333333333333333", eval.eval("1 / 0.3"));
        assertEquals("abcd", eval.eval("\"ab\" + \"cd\""));
        assertTrue(eval.eval("2 + 2") instanceof BigInteger);
    }

    /**
     * Test that a cached result reflects the state at evaluation time and is
     * not refreshed while the variable binding changes.
     */
    @Test
    public void testCachedResultDoesNotFollowVariableChanges() throws Exception
    {
        CachedExpEval eval = newEval();
        eval.addVariable("x", 1);

        assertNumeric("1", eval.eval("$x"));

        eval.addVariable("x", 42);

        assertNumeric("1", eval.eval("$x"));
        assertNumeric("42", eval.eval("$x + 0"));
    }

    /**
     * Test that leading and trailing whitespace is ignored when computing the
     * cache key, so equivalent expressions share the same cached result.
     */
    @Test
    public void testLeadingAndTrailingWhitespaceIsCachedTogether() throws Exception
    {
        CALLS.set(0);
        CachedExpEval eval = newEvalWithTick();

        assertNumeric("1", eval.eval("  tick() "));
        assertNumeric("1", eval.eval("tick()"));
        assertNumeric("1", eval.eval("  tick()  "));

        assertEquals(1, CALLS.get());

        assertNumeric("3", eval.eval(" tick() + 1"));
        assertNumeric("3", eval.eval("tick() + 1 "));
    }

    /**
     * Test that concurrent evaluations of the same expressions compute each
     * of them only once thanks to the synchronized cache.
     */
    @Test
    public void testConcurrentEvaluationsAreComputedOnce() throws Exception
    {
        final String[] expressions = { "tick()", "tick() + 1", "tick() + 2" };
        CALLS.set(0);
        final CachedExpEval eval = newEvalWithTick();

        final int threads = 8;
        final int iterations = 100;
        List<Thread> pool = new ArrayList<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        for (int t = 0; t < threads; t++)
        {
            Thread worker = new Thread(() ->
            {
                try
                {
                    for (int i = 0; i < iterations; i++)
                    {
                        for (String expression : expressions)
                        {
                            eval.eval(expression);
                        }
                    }
                }
                catch (Throwable ex)
                {
                    failure.set(ex);
                }
            });
            pool.add(worker);
            worker.start();
        }
        for (Thread worker : pool)
        {
            worker.join();
        }

        assertNull(failure.get());
        assertEquals(expressions.length, CALLS.get());
    }

    // ------------------------------------------------------------------
    // errors
    // ------------------------------------------------------------------

    /**
     * Test that a null expression is rejected.
     */
    @Test
    public void testNullExpression()
    {
        CachedExpEval eval = newEval();
        assertThrows(NullPointerException.class, () -> eval.eval(null));
    }

    /**
     * Test that a malformed expression is rejected and is not cached.
     */
    @Test
    public void testMalformedExpressionIsNotCached() throws Exception
    {
        CachedExpEval eval = newEval();
        assertThrows(IllegalArgumentException.class, () -> eval.eval("1 +"));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("1 +"));
    }
}