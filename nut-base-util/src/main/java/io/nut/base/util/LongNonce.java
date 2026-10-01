/*
 * LongNonce.java
 *
 * Copyright (c) 2021-2026 francitoshi@gmail.com
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
package io.nut.base.util;

import io.nut.base.time.JavaTime;

/**
 * Abstract base class for generating monotonically increasing {@code long} nonce values.
 *
 * <p>A <em>nonce</em> (number used once) is a value that is guaranteed never to repeat
 * within a given instance. All implementations are thread-safe: concurrent calls to
 * {@link #get()} will always return distinct, strictly increasing values.</p>
 *
 * <p>Three built-in strategies are provided:</p>
 * <ul>
 *   <li><b>Sequential</b> – simple counter incremented by one on every call.</li>
 *   <li><b>CurrentMillis</b> – tracks wall-clock milliseconds, but never goes backwards;
 *       if the clock has not advanced it returns the previous value plus one.</li>
 *   <li><b>EpochSecond</b> – same behaviour as {@code CurrentMillis} but at
 *       second granularity.</li>
 * </ul>
 *
 * <p>All factory methods guarantee that successive calls to {@link #get()} on the
 * returned instance are strictly monotonically increasing.</p>
 *
 * @see #getSequentialInstance()
 * @see #getCurrentMillisInstance()
 * @see #getEpochSecondInstance()
 */
public abstract class LongNonce
{
    /**
     * Creates a sequential nonce starting at the given value.
     * The first call to {@link #get()} will return {@code start + 1}.
     *
     * @param start the initial seed value (exclusive)
     * @return a new {@link LongNonce} that increments by one on every call
     */
    public static LongNonce getSequentialInstance(long start)
    {
        return new LongNonce.Sequential(start);
    }

    /**
     * Creates a sequential nonce starting at {@code 0}.
     * The first call to {@link #get()} will return {@code 1}.
     *
     * @return a new {@link LongNonce} that increments by one on every call
     */
    public static LongNonce getSequentialInstance()
    {
        return new LongNonce.Sequential(0L);
    }

    /**
     * Creates a millisecond-based nonce seeded with the given value.
     * Each call to {@link #get()} returns the greater of
     * {@code (lastValue + 1)} and {@link System#currentTimeMillis()},
     * ensuring values never decrease even if the system clock is unchanged.
     *
     * @param start the initial seed value; should normally be a recent
     *              millisecond epoch value
     * @return a new {@link LongNonce} driven by the system's millisecond clock
     */
    public static LongNonce getCurrentMillisInstance(long start)
    {
        return new LongNonce.CurrentMillis(start);
    }

    /**
     * Creates a millisecond-based nonce seeded with the current system time.
     * Equivalent to {@code getCurrentMillisInstance(System.currentTimeMillis())}.
     *
     * @return a new {@link LongNonce} driven by the system's millisecond clock,
     *         seeded at the moment of creation
     */
    public static LongNonce getCurrentMillisInstance()
    {
        return new LongNonce.CurrentMillis(System.currentTimeMillis());
    }

    /**
     * Creates a second-granularity nonce seeded with the given epoch-second value.
     * Each call to {@link #get()} returns the greater of
     * {@code (lastValue + 1)} and {@link JavaTime#epochSecond()},
     * ensuring values never decrease even within the same wall-clock second.
     *
     * @param start the initial seed value; should normally be a recent
     *              Unix epoch-second value
     * @return a new {@link LongNonce} driven by the system's second clock
     */
    public static LongNonce getEpochSecondInstance(long start)
    {
        return new LongNonce.EpochSecond(start);
    }

    /**
     * Creates a second-granularity nonce seeded with the current epoch second.
     * Equivalent to {@code getEpochSecondInstance(JavaTime.epochSecond())}.
     *
     * @return a new {@link LongNonce} driven by the system's second clock,
     *         seeded at the moment of creation
     */
    public static LongNonce getEpochSecondInstance()
    {
        return new LongNonce.EpochSecond(JavaTime.epochSecond());
    }

    /** Lock object used to synchronise all access to {@link #value}. */
    protected final Object lock = new Object();

    /**
     * The most recently returned (or initial seed) nonce value.
     * Access must always be performed while holding {@link #lock}.
     */
    protected volatile long value;

    /**
     * Constructs a {@code LongNonce} with the given seed value.
     *
     * @param value the initial seed; the first {@link #get()} call will return
     *              a value strictly greater than this
     */
    protected LongNonce(long value)
    {
        this.value = value;
    }

    /**
     * Returns the most recently issued nonce value without advancing the counter.
     *
     * <p>This method is thread-safe. The returned value reflects the last value
     * produced by {@link #get()}, or the seed value if {@link #get()} has not
     * yet been called.</p>
     *
     * @return the current (last issued) nonce value
     */
    public final long peek()
    {
        synchronized(lock)
        {
            return this.value;
        }
    }

    /**
     * Returns the next nonce value, guaranteed to be strictly greater than all
     * previously returned values.
     *
     * <p>Implementations must be thread-safe.</p>
     *
     * @return the next unique, monotonically increasing nonce value
     */
    public abstract long get();

    /**
     * A simple counter-based nonce that increments by exactly one on every call.
     */
    private static class Sequential extends LongNonce
    {
        /**
         * Constructs a sequential nonce with the given seed.
         *
         * @param value the initial seed value
         */
        public Sequential(long value)
        {
            super(value);
        }

        /**
         * Returns the next sequential value ({@code lastValue + 1}).
         *
         * @return the next strictly increasing nonce value
         */
        @Override
        public long get()
        {
            synchronized(this.lock)
            {
                return ++this.value;
            }
        }
    }

    /**
     * A millisecond-clock-based nonce. Returns {@link System#currentTimeMillis()}
     * if it has advanced beyond the last issued value, otherwise returns
     * {@code lastValue + 1}, ensuring strict monotonic increase even under
     * high call rates or clock resolution limits.
     */
    private static class CurrentMillis extends LongNonce
    {
        /**
         * Constructs a millisecond-based nonce with the given seed.
         *
         * @param value the initial seed value, typically a recent millisecond
         *              epoch timestamp
         */
        public CurrentMillis(long value)
        {
            super(value);
        }

        /**
         * Returns the greater of {@code (lastValue + 1)} and the current
         * system time in milliseconds, then stores and returns that value.
         *
         * @return the next strictly increasing millisecond-aligned nonce value
         */
        @Override
        public long get()
        {
            synchronized(this.lock)
            {
                return this.value = Math.max(this.value + 1, System.currentTimeMillis());
            }
        }
    }

    /**
     * A second-granularity nonce backed by {@link JavaTime#epochSecond()}.
     * Returns the current epoch second if it has advanced beyond the last issued
     * value, otherwise returns {@code lastValue + 1}, ensuring strict monotonic
     * increase even when multiple nonces are requested within the same second.
     */
    private static class EpochSecond extends LongNonce
    {
        /**
         * Constructs an epoch-second nonce with the given seed.
         *
         * @param value the initial seed value, typically a recent Unix
         *              epoch-second timestamp
         */
        public EpochSecond(long value)
        {
            super(value);
        }

        /**
         * Returns the greater of {@code (lastValue + 1)} and the current
         * epoch second, then stores and returns that value.
         *
         * @return the next strictly increasing epoch-second-aligned nonce value
         */
        @Override
        public long get()
        {
            synchronized(this.lock)
            {
                return this.value = Math.max(this.value + 1, JavaTime.epochSecond());
            }
        }
    }

}
