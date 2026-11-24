/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.math;

import io.github.francitoshi.nut.base.commons.cache.Cache;
import io.github.francitoshi.nut.base.commons.cache.CacheFactory;
import io.github.francitoshi.nut.base.commons.cache.CacheType;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@link Evaluator} decorator that caches the results of the wrapped
 * evaluator in an ARC {@link Cache}. If the same expression is evaluated
 * again, the cached result is returned without re-evaluating it. The cache is
 * thread-safe: concurrent evaluations of the same expression are computed
 * only once.
 *
 * <p>Any {@link Evaluator} can be wrapped, e.g.
 * {@code new CachedEvaluator(new MathEvaluator(16, true), 128)} or
 * {@code new CachedEvaluator(new ExpressionEvaluator(), 128)}. Cached results are
 * validated against the versions of the variables they depend on: every time
 * the wrapped evaluator returns a result that modifies a variable, a
 * sequential version number is stored for that variable, so a cached result
 * whose dependencies have changed since it was computed is discarded and
 * recomputed. Evaluators that report no dependencies, such as
 * {@link ExpressionEvaluator}, are always served from the cache.</p>
 *
 * <p>Note that the wrapped evaluator must expose its variable usage through
 * the {@link Evaluator.Result#modifiedVariable} and {@link Evaluator.Result#dependencies}
 * fields of its results for the invalidation to work; an evaluator that
 * changes its internal state without reporting it would produce stale cached
 * results.</p>
 *
 * @author franci
 */
public class CachedEvaluator implements Evaluator
{
    /** The default cache capacity. */
    public static final int DEFAULT_CAPACITY = 16;

    private final Evaluator evaluator;
    private final Cache<String, Entry> cache;
    private final Map<String, Long> versions = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    private static final class Entry
    {
        final Result result;
        /** The version counter value at the time the result was computed. */
        final long snapshot;

        Entry(Result result, long snapshot)
        {
            this.result = result;
            this.snapshot = snapshot;
        }
    }

    /**
     * Constructs a {@code CachedEvaluator} wrapping the given evaluator
     * with the default cache capacity.
     *
     * @param evaluator the evaluator whose results are cached
     * @throws NullPointerException if {@code evaluator} is {@code null}
     */
    public CachedEvaluator(Evaluator evaluator)
    {
        this(evaluator, DEFAULT_CAPACITY);
    }

    /**
     * Constructs a {@code CachedEvaluator} wrapping a new
     * {@link ExpressionEvaluator} with the given cache capacity.
     *
     * @param capacity the number of expressions kept in the cache
     * @throws IllegalArgumentException if {@code capacity} is invalid
     */
    public CachedEvaluator(int capacity)
    {
        this(new ExpressionEvaluator(), capacity);
    }

    /**
     * Constructs a {@code CachedEvaluator} wrapping the given evaluator
     * and keeping the given number of results in the cache.
     *
     * @param evaluator the evaluator whose results are cached
     * @param capacity  the number of expressions kept in the cache
     * @throws NullPointerException     if {@code evaluator} is {@code null}
     * @throws IllegalArgumentException if {@code capacity} is invalid
     */
    public CachedEvaluator(Evaluator evaluator, int capacity)
    {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.cache = CacheFactory.<String, Entry>getInstance(CacheType.ARC, capacity).synchronizedCache();
    }

    /**
     * Evaluates the given expression returning the cached result of the
     * wrapped evaluator when available and still valid with respect to the
     * current variable versions, otherwise delegating the evaluation, storing
     * the result in the cache and recording the versions of the variables it
     * modifies.
     *
     * @param expression the expression to evaluate
     * @return the result of the expression
     * @throws NullPointerException if {@code expression} is {@code null}
     */
    @Override
    public Result eval(String expression)
    {
        Objects.requireNonNull(expression, "expression");
        String normalized = expression.trim();
        AtomicReference<Entry> computed = new AtomicReference<>();
        Entry entry = cache.get(normalized, key ->
        {
            Entry fresh = compute(key);
            computed.set(fresh);
            return fresh;
        });
        if (computed.get() != entry && !isValid(entry))
        {
            // the entry came from the cache and is stale: recompute it
            entry = compute(normalized);
            cache.put(normalized, entry);
        }
        Result result = entry.result;
        if (result.modifiedVariable != null)
        {
            // every assignment bumps the version of the modified variable
            versions.put(result.modifiedVariable, counter.incrementAndGet());
        }
        return result;
    }

    private Entry compute(String expression)
    {
        Result result = evaluator.eval(expression);
        String modified = result.modifiedVariable;
        long snapshot = modified != null ? counter.incrementAndGet() : counter.get();
        if (modified != null)
        {
            versions.put(modified, snapshot);
        }
        return new Entry(result, snapshot);
    }

    /**
     * Returns whether the cached result is still valid: the expression must
     * not depend on the variable it modifies, and every variable it depends
     * on must have been assigned before the result was computed.
     */
    private boolean isValid(Entry entry)
    {
        Set<String> dependencies = entry.result.dependencies;
        if (dependencies == null || dependencies.isEmpty())
        {
            return true;
        }
        String modified = entry.result.modifiedVariable;
        if (modified != null)
        {
            if (dependencies.contains(modified))
            {
                // an assignment that depends on itself must be recomputed
                return false;
            }
            for (String dependency : dependencies)
            {
                if (versions.getOrDefault(dependency, 0L) >= entry.snapshot)
                {
                    return false;
                }
            }
            return true;
        }
        for (String dependency : dependencies)
        {
            if (versions.getOrDefault(dependency, 0L) > entry.snapshot)
            {
                return false;
            }
        }
        return true;
    }
}
