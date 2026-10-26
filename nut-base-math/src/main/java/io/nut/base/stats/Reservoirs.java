/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/**
 * Factory methods for {@link Reservoir} implementations.
 * <p>
 * All the returned reservoirs are thread-safe. Methods that take a
 * {@link Random} allow reproducible sampling in tests; the others use
 * {@link ThreadLocalRandom}.
 *
 * @author franci
 */
public final class Reservoirs
{
    private Reservoirs()
    {
    }

    /**
     * Returns a uniform reservoir: after {@code n} offered values, each one has
     * the same probability {@code k/n} of being in the sample.
     * <p>
     * It uses {@link #uniformL(int) Algorithm L}, which is the fastest option
     * when the stream is much longer than {@code k}.
     *
     * @param <E> the type of sampled values
     * @param k the capacity; must be positive
     * @return a new uniform reservoir
     * @throws IllegalArgumentException if {@code k} is not positive
     */
    public static <E> Reservoir<E> uniform(int k)
    {
        return uniformL(k);
    }

    /**
     * Returns a uniform reservoir using Algorithm R (Vitter).
     * <p>
     * It draws one random number for every offered value after the first
     * {@code k}: O(n) random draws. Simple and predictable.
     *
     * @param <E> the type of sampled values
     * @param k the capacity; must be positive
     * @return a new uniform reservoir
     * @throws IllegalArgumentException if {@code k} is not positive
     */
    public static <E> Reservoir<E> uniformR(int k)
    {
        return new UniformR<>(checkCapacity(k), null);
    }

    /**
     * Same as {@link #uniformR(int)} but using the given random source.
     *
     * @param <E> the type of sampled values
     * @param k the capacity; must be positive
     * @param random the random source
     * @return a new uniform reservoir
     * @throws IllegalArgumentException if {@code k} is not positive
     * @throws NullPointerException if {@code random} is {@code null}
     */
    public static <E> Reservoir<E> uniformR(int k, Random random)
    {
        return new UniformR<>(checkCapacity(k), Objects.requireNonNull(random, "random"));
    }

    /**
     * Returns a uniform reservoir using Algorithm L (Li, 1994).
     * <p>
     * Instead of drawing a random number per value, it computes how many values
     * to skip before the next replacement, so it needs only
     * O(k (1 + log(n/k))) random draws. The distribution of the sample is the
     * same as with {@link #uniformR(int)}.
     *
     * @param <E> the type of sampled values
     * @param k the capacity; must be positive
     * @return a new uniform reservoir
     * @throws IllegalArgumentException if {@code k} is not positive
     */
    public static <E> Reservoir<E> uniformL(int k)
    {
        return new UniformL<>(checkCapacity(k), null);
    }

    /**
     * Same as {@link #uniformL(int)} but using the given random source.
     *
     * @param <E> the type of sampled values
     * @param k the capacity; must be positive
     * @param random the random source
     * @return a new uniform reservoir
     * @throws IllegalArgumentException if {@code k} is not positive
     * @throws NullPointerException if {@code random} is {@code null}
     */
    public static <E> Reservoir<E> uniformL(int k, Random random)
    {
        return new UniformL<>(checkCapacity(k), Objects.requireNonNull(random, "random"));
    }

    /**
     * Returns a reservoir biased towards recent values, using forward decay
     * (Cormode et al.), the same approach as Dropwizard Metrics.
     * <p>
     * Each value gets the priority {@code exp(alpha * t) / u}, where {@code t}
     * is the elapsed time in seconds and {@code u} a uniform random number in
     * (0,1). The {@code k} values with the highest priority are kept, so newer
     * values are more likely to be retained. Larger {@code alpha} forgets the
     * past faster; for instance {@code 0.015} with {@code k = 1028} favors
     * roughly the last five minutes.
     *
     * @param <E> the type of sampled values
     * @param k the capacity; must be positive
     * @param alpha the decay rate per second; must be positive and finite
     * @return a new decaying reservoir
     * @throws IllegalArgumentException if {@code k} or {@code alpha} are invalid
     */
    public static <E> Reservoir<E> decaying(int k, double alpha)
    {
        return decaying(k, alpha, null, System::nanoTime);
    }

    /**
     * Same as {@link #decaying(int, double)} but using the given random source.
     *
     * @param <E> the type of sampled values
     * @param k the capacity; must be positive
     * @param alpha the decay rate per second; must be positive and finite
     * @param random the random source
     * @return a new decaying reservoir
     * @throws IllegalArgumentException if {@code k} or {@code alpha} are invalid
     * @throws NullPointerException if {@code random} is {@code null}
     */
    public static <E> Reservoir<E> decaying(int k, double alpha, Random random)
    {
        return decaying(k, alpha, Objects.requireNonNull(random, "random"), System::nanoTime);
    }

    /**
     * Same as {@link #decaying(int, double)} but using the given random source
     * and time source, which makes the behavior fully deterministic in tests.
     *
     * @param <E> the type of sampled values
     * @param k the capacity; must be positive
     * @param alpha the decay rate per second; must be positive and finite
     * @param random the random source, or {@code null} to use
     * {@link ThreadLocalRandom}
     * @param nanoClock a monotonic time source in nanoseconds, like
     * {@link System#nanoTime()}
     * @return a new decaying reservoir
     * @throws IllegalArgumentException if {@code k} or {@code alpha} are invalid
     * @throws NullPointerException if {@code nanoClock} is {@code null}
     */
    public static <E> Reservoir<E> decaying(int k, double alpha, Random random, LongSupplier nanoClock)
    {
        checkCapacity(k);
        if (!(alpha > 0.0) || Double.isInfinite(alpha))
        {
            throw new IllegalArgumentException("alpha must be positive and finite: " + alpha);
        }
        return new Decaying<>(k, alpha, random, Objects.requireNonNull(nanoClock, "nanoClock"));
    }

    // ------------------------------------------------------------------ helpers

    private static int checkCapacity(int k)
    {
        if (k <= 0)
        {
            throw new IllegalArgumentException("k must be positive: " + k);
        }
        return k;
    }

    private static Random rnd(Random random)
    {
        return random != null ? random : ThreadLocalRandom.current();
    }

    /** Uniform double in the open interval (0,1). */
    private static double nextOpenDouble(Random r)
    {
        double u;
        do
        {
            u = r.nextDouble();
        }
        while (u == 0.0);
        return u;
    }

    /** Uniform long in [0, bound); {@link Random} has no such method in Java 8. */
    private static long nextLong(Random r, long bound)
    {
        long m = bound - 1;
        if ((bound & m) == 0L)
        {
            return r.nextLong() & m;
        }
        long u = r.nextLong() >>> 1;
        long v;
        while (u + m - (v = u % bound) < 0L)
        {
            u = r.nextLong() >>> 1;
        }
        return v;
    }

    // ----------------------------------------------------------- implementations

    /**
     * Shared storage for the uniform algorithms. Subclasses decide what to do
     * with each value once the reservoir is full.
     */
    private abstract static class ArrayReservoir<E> implements Reservoir<E>
    {
        final Object[] items;
        final Random random;
        long seen;

        ArrayReservoir(int k, Random random)
        {
            this.items = new Object[k];
            this.random = random;
        }

        /** Called once, right after the reservoir has been filled. */
        abstract void filled();

        /** Called for every value after the reservoir is full; {@code seen} already counts it. */
        abstract void offer(E item);

        /** Called by {@link #clear()} to reset the algorithm state. */
        abstract void reset();

        @Override
        public final synchronized void add(E item)
        {
            Objects.requireNonNull(item, "item");
            long n = seen++;
            if (n < items.length)
            {
                items[(int) n] = item;
                if (n + 1 == items.length)
                {
                    filled();
                }
            }
            else
            {
                offer(item);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public final synchronized List<E> snapshot()
        {
            int n = (int) Math.min(seen, items.length);
            List<E> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
            {
                list.add((E) items[i]);
            }
            return list;
        }

        @Override
        public final synchronized int size()
        {
            return (int) Math.min(seen, items.length);
        }

        @Override
        public final int capacity()
        {
            return items.length;
        }

        @Override
        public final synchronized long seen()
        {
            return seen;
        }

        @Override
        public final synchronized void clear()
        {
            Arrays.fill(items, null);
            seen = 0;
            reset();
        }
    }

    /** Algorithm R: one random draw per value. */
    private static final class UniformR<E> extends ArrayReservoir<E>
    {
        UniformR(int k, Random random)
        {
            super(k, random);
        }

        @Override
        void filled()
        {
        }

        @Override
        void offer(E item)
        {
            long j = nextLong(rnd(random), seen);   // seen = i, 1-based position of item
            if (j < items.length)
            {
                items[(int) j] = item;
            }
        }

        @Override
        void reset()
        {
        }
    }

    /** Algorithm L: computes how many values to skip between replacements. */
    private static final class UniformL<E> extends ArrayReservoir<E>
    {
        private double w;      // running value of the algorithm's W
        private long next;     // 1-based position of the next value to be stored

        UniformL(int k, Random random)
        {
            super(k, random);
        }

        @Override
        void filled()
        {
            Random r = rnd(random);
            w = Math.exp(Math.log(nextOpenDouble(r)) / items.length);
            next = items.length;
            advance(r);
        }

        @Override
        void offer(E item)
        {
            if (seen != next)
            {
                return;
            }
            Random r = rnd(random);
            items[r.nextInt(items.length)] = item;
            w *= Math.exp(Math.log(nextOpenDouble(r)) / items.length);
            advance(r);
        }

        /** Moves {@code next} forward by floor(log(u)/log(1-W)) + 1, saturating at Long.MAX_VALUE. */
        private void advance(Random r)
        {
            double skip = Math.floor(Math.log(nextOpenDouble(r)) / Math.log1p(-w));
            if (skip != skip || skip >= (double) (Long.MAX_VALUE - next - 1))
            {
                next = Long.MAX_VALUE;
            }
            else
            {
                next += (long) Math.max(skip, 0.0) + 1L;
            }
        }

        @Override
        void reset()
        {
            w = 0.0;
            next = 0L;
        }
    }

    /** Forward-decay reservoir: keeps the k values with the highest priority. */
    private static final class Decaying<E> implements Reservoir<E>
    {
        /** Rescale before exp(alpha * t) gets close to overflowing (~1e308). */
        private static final double MAX_EXPONENT = 500.0;

        private final int k;
        private final double alpha;
        private final Random random;
        private final LongSupplier clock;
        private final PriorityQueue<Entry<E>> heap;   // min-heap: head has the lowest priority
        private long startNanos;
        private long seen;

        Decaying(int k, double alpha, Random random, LongSupplier clock)
        {
            this.k = k;
            this.alpha = alpha;
            this.random = random;
            this.clock = clock;
            this.heap = new PriorityQueue<>(k);
            this.startNanos = clock.getAsLong();
        }

        @Override
        public synchronized void add(E item)
        {
            Objects.requireNonNull(item, "item");
            long now = clock.getAsLong();
            double elapsed = (now - startNanos) / 1e9;
            if (alpha * elapsed > MAX_EXPONENT)
            {
                rescale(elapsed);
                startNanos = now;
                elapsed = 0.0;
            }
            double priority = Math.exp(alpha * elapsed) / nextOpenDouble(rnd(random));
            seen++;
            if (heap.size() < k)
            {
                heap.add(new Entry<>(item, priority));
            }
            else if (priority > heap.peek().priority)
            {
                heap.poll();
                heap.add(new Entry<>(item, priority));
            }
        }

        /** Multiplying every priority by the same positive factor keeps the heap order valid. */
        private void rescale(double elapsed)
        {
            double factor = Math.exp(-alpha * elapsed);
            for (Entry<E> e : heap)
            {
                e.priority *= factor;
            }
        }

        @Override
        public synchronized List<E> snapshot()
        {
            List<E> list = new ArrayList<>(heap.size());
            for (Entry<E> e : heap)
            {
                list.add(e.item);
            }
            return list;
        }

        @Override
        public synchronized int size()
        {
            return heap.size();
        }

        @Override
        public int capacity()
        {
            return k;
        }

        @Override
        public synchronized long seen()
        {
            return seen;
        }

        @Override
        public synchronized void clear()
        {
            heap.clear();
            seen = 0;
            startNanos = clock.getAsLong();
        }

        private static final class Entry<E> implements Comparable<Entry<E>>
        {
            final E item;
            double priority;

            Entry(E item, double priority)
            {
                this.item = item;
                this.priority = priority;
            }

            @Override
            public int compareTo(Entry<E> o)
            {
                return Double.compare(priority, o.priority);
            }
        }
    }
}
