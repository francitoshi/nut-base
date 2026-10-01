/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

import io.nut.base.cache.Cache;
import io.nut.base.cache.CacheFactory;
import io.nut.base.cache.CacheType;
import java.util.Objects;

/**
 * An {@link ExpEval} that caches the results of evaluated expressions in an
 * ARC {@link Cache}. If the same expression is evaluated again, the cached
 * result is returned without re-parsing or re-evaluating it. The cache is
 * thread-safe: concurrent evaluations of the same expression are computed
 * only once.
 *
 * @author franci
 */
public class CachedExpEval extends ExpEval
{
    private final Cache<String, Object> cache;

    /**
     * Constructs a {@code CachedExpEval} using {@link #DEFAULT_DECIMALS}
     * decimals and the given cache capacity.
     *
     * @param capacity the number of expressions kept in the cache
     * @throws IllegalArgumentException if {@code capacity} is invalid
     */
    public CachedExpEval(int capacity)
    {
        this(capacity, DEFAULT_DECIMALS);
    }

    /**
     * Constructs a {@code CachedExpEval} rounding the decimal results of the
     * arithmetic operations to the given number of decimals and keeping the
     * given number of results in the cache.
     *
     * @param capacity the number of expressions kept in the cache
     * @param decimals the number of decimals for the results; must not be negative
     * @throws IllegalArgumentException if {@code decimals} is negative
     */
    public CachedExpEval(int capacity, int decimals)
    {
        super(decimals);
        this.cache = CacheFactory.<String, Object>getInstance(CacheType.ARC, capacity).synchronizedCache();
    }

    /**
     * Evaluates the given expression returning the cached result when
     * available, otherwise evaluating it and storing it in the cache.
     *
     * @param expression the expression to evaluate
     * @return the result of the expression
     * @throws NullPointerException     if {@code expression} is {@code null}
     * @throws IllegalArgumentException if the expression is malformed, references an
     *                                  unknown variable or function, or a function argument
     *                                  cannot be converted
     * @throws ArithmeticException      on division by zero or an out-of-range exponent
     */
    @Override
    public Object eval(String expression)
    {
        Objects.requireNonNull(expression, "expression");
        String normalized = expression.trim();
        return cache.get(normalized, key -> CachedExpEval.super.eval(key));
    }
}