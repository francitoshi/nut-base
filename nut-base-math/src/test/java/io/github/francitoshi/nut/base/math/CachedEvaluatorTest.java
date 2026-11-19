/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CachedEvaluator}.
 */
public class CachedEvaluatorTest
{
    private static final AtomicInteger CALLS = new AtomicInteger();

    public static int tick()
    {
        return CALLS.incrementAndGet();
    }

    private static CachedEvaluator newEval()
    {
        return new CachedEvaluator(new ExpressionEvaluator(), 16);
    }

    private static CachedEvaluator newEvalWithTick() throws Exception
    {
        ExpressionEvaluator target = new ExpressionEvaluator();
        target.addFunction("tick", CachedEvaluatorTest.class.getMethod("tick", new Class<?>[0]));
        return new CachedEvaluator(target, 16);
    }

    /**
     * Evaluates the expression asserting that there is no error, and returns
     * its value.
     */
    private static Object value(Evaluator eval, String expression)
    {
        Evaluator.Result result = eval.eval(expression);
        assertEquals(Evaluator.ErrorType.NONE, result.error,
                () -> expression + " -> " + result.error + ": " + result.message);
        return result.value;
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
        CachedEvaluator instance = newEval();
        assertNumeric("0.3333333333333333", value(instance, "1 / 3"));
    }

    /**
     * Test of wrapping an evaluator with a custom number of decimals.
     */
    @Test
    public void testConstructorWithDecimals() throws Exception
    {
        CachedEvaluator instance = new CachedEvaluator(new ExpressionEvaluator(2), 16);
        assertNumeric("0.33", value(instance, "1 / 3"));
    }

    /**
     * Test that a negative number of decimals is rejected.
     */
    @Test
    public void testConstructorNegativeDecimals()
    {
        assertThrows(IllegalArgumentException.class, () -> new CachedEvaluator(new ExpressionEvaluator(-1), 16));
    }

    /**
     * Test that a non-positive capacity is rejected.
     */
    @Test
    public void testConstructorInvalidCapacity()
    {
        assertThrows(IllegalArgumentException.class, () -> new CachedEvaluator(new ExpressionEvaluator(), 0));
        assertThrows(IllegalArgumentException.class, () -> new CachedEvaluator(new ExpressionEvaluator(), -1));
        assertThrows(IllegalArgumentException.class, () -> new CachedEvaluator(0));
        assertThrows(IllegalArgumentException.class, () -> new CachedEvaluator(-1));
    }

    /**
     * Test that the wrapped evaluator cannot be null.
     */
    @Test
    public void testConstructorNullEvaluator()
    {
        assertThrows(NullPointerException.class, () -> new CachedEvaluator(null));
        assertThrows(NullPointerException.class, () -> new CachedEvaluator(null, 16));
    }

    /**
     * Test that MathEvaluator can be cached too.
     */
    @Test
    public void testMathEvaluatorCanBeCached()
    {
        CachedEvaluator instance = new CachedEvaluator(new MathEvaluator(16, false), 16);
        assertNumeric("6", value(instance, "2 * 3"));
        assertNumeric("6", value(instance, "2 * 3"));
    }

    /**
     * Test that an expression depending on a variable is recomputed after the
     * variable is assigned a new value.
     */
    @Test
    public void testDependencyInvalidation()
    {
        CachedEvaluator eval = new CachedEvaluator(new MathEvaluator(16, true), 16);

        assertNumeric("5", value(eval, "x = 5"));
        assertNumeric("10", value(eval, "x * 2"));
        assertNumeric("10", value(eval, "x * 2"));      // cached

        assertNumeric("7", value(eval, "x = 7"));       // x changes
        assertNumeric("14", value(eval, "x * 2"));      // recomputed
        assertNumeric("14", value(eval, "x * 2"));      // cached again
    }

    /**
     * Test that an assignment depending on another variable is recomputed
     * when that variable changes.
     */
    @Test
    public void testAssignmentDependencyInvalidation()
    {
        CachedEvaluator eval = new CachedEvaluator(new MathEvaluator(16, true), 16);

        assertNumeric("3", value(eval, "x = 3"));
        assertNumeric("4", value(eval, "y = x + 1"));
        assertNumeric("4", value(eval, "y = x + 1"));   // cached

        assertNumeric("10", value(eval, "x = 10"));     // x changes
        assertNumeric("11", value(eval, "y = x + 1"));  // recomputed
    }

    /**
     * Test that an assignment depending on itself is always recomputed.
     */
    @Test
    public void testSelfDependencyIsAlwaysRecomputed()
    {
        CachedEvaluator eval = new CachedEvaluator(new MathEvaluator(16, true), 16);

        assertNumeric("1", value(eval, "x = 1"));
        assertNumeric("2", value(eval, "x = x + 1"));
        assertNumeric("3", value(eval, "x = x + 1"));
        assertNumeric("4", value(eval, "x = x + 1"));
    }

    /**
     * Test that expressions without dependencies stay cached across variable
     * assignments.
     */
    @Test
    public void testConstantExpressionsStayCached() throws Exception
    {
        CALLS.set(0);
        MathEvaluator target = new MathEvaluator(16, true);
        target.addFunction("tick", CachedEvaluatorTest.class.getMethod("tick", new Class<?>[0]));
        CachedEvaluator eval = new CachedEvaluator(target, 16);

        assertNumeric("1", value(eval, "tick()"));
        assertNumeric("5", value(eval, "x = 5"));
        assertNumeric("1", value(eval, "tick()"));      // no dependencies, still cached

        assertEquals(1, CALLS.get());
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
        CachedEvaluator eval = newEvalWithTick();

        assertNumeric("1", value(eval, "tick()"));
        assertNumeric("1", value(eval, "tick()"));
        assertNumeric("1", value(eval, "tick()"));

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
        CachedEvaluator eval = newEvalWithTick();

        assertNumeric("1", value(eval, "tick()"));
        assertNumeric("3", value(eval, "tick() + 1"));
        assertNumeric("3", value(eval, "tick() + 1"));
        assertNumeric("3", value(eval, "tick() + 1"));

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
        ExpressionEvaluator target = new ExpressionEvaluator();
        target.addFunction("tick", CachedEvaluatorTest.class.getMethod("tick", new Class<?>[0]));
        CachedEvaluator eval = new CachedEvaluator(target, 2);

        assertNumeric("1", value(eval, "tick()"));
        assertNumeric("12", value(eval, "tick() + 10"));
        assertNumeric("103", value(eval, "tick() + 100"));

        assertNumeric("4", value(eval, "tick()"));

        assertEquals(4, CALLS.get());
    }

    /**
     * Test that arithmetic and functions behave as in ExpressionEvaluator.
     */
    @Test
    public void testEvaluationDelegatesToExpEvaluator() throws Exception
    {
        ExpressionEvaluator target = new ExpressionEvaluator();
        target.addFunction("sqrt", Math::sqrt);
        CachedEvaluator eval = new CachedEvaluator(target, 16);

        assertNumeric("3", value(eval, "sqrt(9)"));
        assertNumeric("3.3333333333333333", value(eval, "1 / 0.3"));
        assertEquals("abcd", value(eval, "\"ab\" + \"cd\""));
        assertTrue(value(eval, "2 + 2") instanceof BigInteger);
    }

    /**
     * Test that a cached result reflects the state at evaluation time and is
     * not refreshed while the variable binding changes.
     */
    @Test
    public void testCachedResultDoesNotFollowVariableChanges() throws Exception
    {
        ExpressionEvaluator target = new ExpressionEvaluator();
        target.addVariable("x", 1);
        CachedEvaluator eval = new CachedEvaluator(target, 16);

        assertNumeric("1", value(eval, "$x"));

        target.addVariable("x", 42);

        assertNumeric("1", value(eval, "$x"));
        assertNumeric("42", value(eval, "$x + 0"));
    }

    /**
     * Test that leading and trailing whitespace is ignored when computing the
     * cache key, so equivalent expressions share the same cached result.
     */
    @Test
    public void testLeadingAndTrailingWhitespaceIsCachedTogether() throws Exception
    {
        CALLS.set(0);
        CachedEvaluator eval = newEvalWithTick();

        assertNumeric("1", value(eval, "  tick() "));
        assertNumeric("1", value(eval, "tick()"));
        assertNumeric("1", value(eval, "  tick()  "));

        assertEquals(1, CALLS.get());

        assertNumeric("3", value(eval, " tick() + 1"));
        assertNumeric("3", value(eval, "tick() + 1 "));
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
        final CachedEvaluator eval = newEvalWithTick();

        final int threads = 8;
        final int iterations = 100;
        List<Thread> pool = new ArrayList<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
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
        CachedEvaluator eval = newEval();
        assertThrows(NullPointerException.class, () -> eval.eval(null));
    }

    /**
     * Test that a malformed expression is reported with an error result.
     */
    @Test
    public void testMalformedExpressionIsNotCached() throws Exception
    {
        CachedEvaluator eval = newEval();
        Evaluator.Result first = eval.eval("1 +");
        Evaluator.Result second = eval.eval("1 +");
        assertEquals(Evaluator.ErrorType.INCOMPLETE_EXPRESSION, first.error);
        assertEquals(Evaluator.ErrorType.INCOMPLETE_EXPRESSION, second.error);
        assertTrue(first.isError());
        assertTrue(second.isError());
    }
}
