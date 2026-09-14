/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.atomic;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;

/**
 * Two 32-bit signed counters packed into a single {@code long} and updated
 * atomically through one {@link java.util.concurrent.atomic.AtomicLong}.
 * <p>
 * Packing both counters into the same word makes any transformation that
 * touches both counters a single compare-and-set, so observers never see a
 * torn state where, say, the first counter has already been decremented but
 * the second not yet incremented. It also keeps both counters on a single
 * cache line, which reduces the invalidation traffic of highly contended
 * counters such as a producer-consumer {@code pending}/{@code processing}
 * pair.
 * <p>
 * The first counter lives in the 32 high bits, the second in the 32 low bits.
 * Values occupy the full signed {@code int} range; as with the
 * {@code java.util.concurrent.atomic} family, overflow is not detected. The
 * dedicated methods ({@link #incrementFirst()}, {@link #decrementSecond()},
 * ...) cover the common single-counter updates, and
 * {@link #addFirstToSecond(int)} / {@link #update(IntBinaryOperator, IntBinaryOperator, BiFunction)}
 * cover transformations that must move units between the two counters in a
 * single atomic step.
 *
 * @author franci
 */
public final class AtomicCounterPair
{
    private static final long MASK = 0xFFFFFFFFL;

    private final AtomicLong value;

    /**
     * Creates a pair with both counters at {@code 0}.
     */
    public AtomicCounterPair()
    {
        this.value = new AtomicLong();
    }

    /**
     * Creates a pair with the given initial values.
     *
     * @param first  the initial value of the first (high-bit) counter
     * @param second the initial value of the second (low-bit) counter
     */
    public AtomicCounterPair(int first, int second)
    {
        this.value = new AtomicLong(pack(first, second));
    }

    // -----------------------------------------------------------------
    // Packing
    // -----------------------------------------------------------------

    private static long pack(int first, int second)
    {
        return ((long) first << 32) | (MASK & second);
    }

    private static int firstOf(long word)
    {
        return (int) (word >>> 32);
    }

    private static int secondOf(long word)
    {
        return (int) word;
    }

    // -----------------------------------------------------------------
    // Reading
    // -----------------------------------------------------------------

    /**
     * Returns the raw packed word: the 32 high bits are the first counter,
     * the 32 low bits the second. Reading a single {@code long} is atomic, so
     * the two counters are always observed in a consistent pairwise state.
     *
     * @return the packed state
     */
    public long get()
    {
        return value.get();
    }

    /**
     * Returns the current value of the first (high-bit) counter.
     *
     * @return the first counter
     */
    public int getFirst()
    {
        return firstOf(value.get());
    }

    /**
     * Returns the current value of the second (low-bit) counter.
     *
     * @return the second counter
     */
    public int getSecond()
    {
        return secondOf(value.get());
    }

    /**
     * Returns {@code true} if both counters are exactly {@code 0}. Equivalent
     * to {@code getFirst() == 0 && getSecond() == 0}, but performed as a
     * single atomic read.
     *
     * @return {@code true} if both counters are zero
     */
    public boolean isZero()
    {
        return value.get() == 0L;
    }

    /**
     * Sets both counters to the given values. This is a plain write, not a
     * compound transformation: a concurrent update can be overwritten.
     *
     * @param first  the new first counter
     * @param second the new second counter
     */
    public void set(int first, int second)
    {
        value.set(pack(first, second));
    }

    @Override
    public String toString()
    {
        return getFirst() + "/" + getSecond();
    }

    // -----------------------------------------------------------------
    // Single-counter updates (return the new value of the touched counter)
    // -----------------------------------------------------------------

    /**
     * Atomically increments the first counter.
     *
     * @return the new value of the first counter
     */
    public int incrementFirst()
    {
        return update((f, s) -> f + 1, (f, s) -> s, (nf, ns) -> nf);
    }

    /**
     * Atomically decrements the first counter.
     *
     * @return the new value of the first counter
     */
    public int decrementFirst()
    {
        return update((f, s) -> f - 1, (f, s) -> s, (nf, ns) -> nf);
    }

    /**
     * Atomically increments the second counter.
     *
     * @return the new value of the second counter
     */
    public int incrementSecond()
    {
        return update((f, s) -> f, (f, s) -> s + 1, (nf, ns) -> ns);
    }

    /**
     * Atomically decrements the second counter.
     *
     * @return the new value of the second counter
     */
    public int decrementSecond()
    {
        return update((f, s) -> f, (f, s) -> s - 1, (nf, ns) -> ns);
    }

    // -----------------------------------------------------------------
    // Compound updates (both counters in a single compare-and-set)
    // -----------------------------------------------------------------

    /**
     * Atomically moves {@code delta} units from the first counter to the
     * second: {@code first -= delta} and {@code second += delta} happen in a
     * single atomic step. Use a negative {@code delta} to move the other way.
     *
     * @param delta the number of units to move from first to second
     */
    public void addFirstToSecond(int delta)
    {
        update((f, s) -> f - delta, (f, s) -> s + delta, (nf, ns) -> null);
    }

    /**
     * Atomically decrements the second counter and returns {@code true} if, as
     * a result of this operation, <em>both</em> counters are zero. This is the
     * exact primitive a producer/consumer actor needs to wake
     * {@code waitForIdle()} waiters as soon as the last in-flight message
     * finishes: the check observes the post-decrement state in the same atomic
     * step as the decrement itself.
     *
     * @return {@code true} if both counters are zero after the decrement
     */
    public boolean decrementSecondAndAllZero()
    {
        return update((f, s) -> f, (f, s) -> s - 1, (nf, ns) -> nf == 0 && ns == 0);
    }

    /**
     * Atomically decrements the first counter and returns {@code true} if, as
     * a result of this operation, <em>both</em> counters are zero.
     *
     * @return {@code true} if both counters are zero after the decrement
     */
    public boolean decrementFirstAndAllZero()
    {
        return update((f, s) -> f - 1, (f, s) -> s, (nf, ns) -> nf == 0 && ns == 0);
    }

    // -----------------------------------------------------------------
    // Generic primitive
    // -----------------------------------------------------------------

    /**
     * Applies an arbitrary transformation to both counters as a single atomic
     * compare-and-set and returns an observation of the new state.
     * <p>
     * The transformation is retried on conflict: {@code nextFirst} and
     * {@code nextSecond} receive the current values and produce the desired
     * successors, then {@code observer} computes the result from the successor
     * values. All three functions must therefore be pure (side-effect free and
     * deterministic), as they may be invoked more than once.
     * <p>
     * Implementations that hot-path this primitive should hoist the lambdas
     * into constants so no allocation happens per call.
     *
     * @param <R>          the observation type
     * @param nextFirst    maps the current pair to the new first counter
     * @param nextSecond   maps the current pair to the new second counter
     * @param observer     maps the new pair to the returned observation; may be
     *                     {@code null} when no observation is needed
     * @return the result of {@code observer}, or {@code null} when
     *         {@code observer} is {@code null}
     */
    public <R> R update(IntBinaryOperator nextFirst, IntBinaryOperator nextSecond, BiFunction<Integer,Integer,R> observer)
    {
        long word;
        int first;
        int second;
        int nextFirstValue;
        int nextSecondValue;
        R result = null;
        while (true)
        {
            word = value.get();
            first = firstOf(word);
            second = secondOf(word);
            nextFirstValue = nextFirst.applyAsInt(first, second);
            nextSecondValue = nextSecond.applyAsInt(first, second);
            if (observer != null)
            {
                result = observer.apply(nextFirstValue, nextSecondValue);
            }
            if (value.compareAndSet(word, pack(nextFirstValue, nextSecondValue)))
            {
                return result;
            }
        }
    }
}