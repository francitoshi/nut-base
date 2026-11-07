/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.profile;

import java.util.Arrays;

/**
 * A fixed-size logarithmic histogram of positive nanosecond durations for
 * approximate quantiles (percentiles) with constant memory, in the style of
 * HDR-Histogram but with only the JDK.
 * <p>
 * Each value falls in one bucket of a logarithmic ladder: octaves of the
 * form [2^k, 2^(k+1)), each split into four equal linear pieces from the
 * second octave on (the first two octaves, [1,2) and [2,4), are single
 * buckets). A duration is therefore placed with a relative error of at most
 * one bucket width, which is between 25% (high octaves) and 100% (tiny
 * values) of the value itself; quantiles interpolate linearly inside the
 * bucket under the assumption of a uniform spread within it, so the error
 * stays of that same order and never affects p0 or p100, which are exact.
 * <p>
 * Each bucket carries a weight, so a batch of N steps with their mean
 * duration can be recorded as one weighted entry
 * ({@code record(meanStepNanos, N)}). The counting is exact; only the
 * position of those batch means inside their bucket is an estimate.
 * <p>
 * Not thread-safe: the callers, like {@link Profiler.Task}, guard it with
 * their own lock.
 */
public final class LatencyHistogram
{
    /**
     * Linear pieces per octave; a power of two by construction.
     */
    static final int SUB_BUCKETS = 4;

    private static final int LOG_SUB = 2;

    /** Two single-octave buckets [1,2) and [2,4), then 62 octaves x 4. */
    static final int BUCKET_COUNT = 2 + 62 * SUB_BUCKETS;

    private final long[] counts = new long[BUCKET_COUNT];
    private long totalWeight;
    private long minNanos = Long.MAX_VALUE;
    private long maxNanos = -1;

    /**
     * Records a duration with the given weight.
     *
     * @param nanos the duration in nanoseconds, 0 or greater
     * @param weight how many events this duration represents, 0 or greater
     */
    public void record(long nanos, long weight)
    {
        if (nanos < 0 || weight < 0)
        {
            throw new IllegalArgumentException("negative nanos or weight: " + nanos + ", " + weight);
        }

        if (weight == 0)
        {
            return;
        }

        int bucket = bucketOf(nanos);
        counts[bucket] += weight;
        totalWeight += weight;

        if (nanos < minNanos)
        {
            minNanos = nanos;
        }
        if (nanos > maxNanos)
        {
            maxNanos = nanos;
        }
    }

    /**
     * Returns the smallest duration recorded, or 0 when nothing was recorded.
     *
     * @return the exact minimum in nanoseconds
     */
    public long minNanos()
    {
        return totalWeight == 0 ? 0 : minNanos;
    }

    /**
     * Returns the largest duration recorded, or 0 when nothing was recorded.
     *
     * @return the exact maximum in nanoseconds
     */
    public long maxNanos()
    {
        return totalWeight == 0 ? 0 : maxNanos;
    }

    /**
     * Returns the total weight recorded (the number of represented events).
     *
     * @return the total weight
     */
    public long totalWeight()
    {
        return totalWeight;
    }

    /**
     * Returns an estimate of the duration at the given quantile: 0 is the
     * exact minimum, 1 the exact maximum, 0.5 the median and 0.99
     * approximately the 99th percentile. An empty histogram quantizes to 0.
     *
     * @param q the quantile, clamped into [0, 1]
     * @return the estimated duration in nanoseconds
     */
    public long quantileNanos(double q)
    {
        if (totalWeight == 0)
        {
            return 0;
        }

        if (q <= 0)
        {
            return minNanos;
        }

        if (q >= 1)
        {
            return maxNanos;
        }

        long rank = (long) Math.ceil(q * totalWeight);

        if (rank < 1)
        {
            rank = 1;
        }
        if (rank > totalWeight)
        {
            rank = totalWeight;
        }

        long cumulative = 0;

        for (int bucket = 0; bucket < BUCKET_COUNT; bucket++)
        {
            long inBucket = counts[bucket];

            if (inBucket == 0)
            {
                continue;
            }

            if (cumulative + inBucket >= rank)
            {
                long lower = bucketLower(bucket);
                long width = bucketWidth(bucket);
                long offset = rank - cumulative;

                // midpoint interpolation: the best single-value guess under
                // the uniform-spread assumption inside the bucket; it never
                // reaches the bucket's upper bound
                double estimate = lower + ((double) (offset - 0.5) / inBucket) * width;

                // the edges of the distribution make the uniform assumption
                // overshoot: no recorded value can lie outside [min, max]
                if (estimate < minNanos)
                {
                    estimate = minNanos;
                }
                if (estimate > maxNanos)
                {
                    estimate = maxNanos;
                }

                return (long) estimate;
            }

            cumulative += inBucket;
        }

        return maxNanos; // unreachable: the rank is bounded by the total weight
    }

    /**
     * Adds every bucket, the weight and the min/max of the other histogram
     * to this one.
     *
     * @param other the histogram to merge in
     */
    public void merge(LatencyHistogram other)
    {
        for (int bucket = 0; bucket < BUCKET_COUNT; bucket++)
        {
            counts[bucket] += other.counts[bucket];
        }

        totalWeight += other.totalWeight;

        if (other.totalWeight > 0)
        {
            minNanos = Math.min(minNanos, other.minNanos);
            maxNanos = Math.max(maxNanos, other.maxNanos);
        }
    }

    /**
     * Returns a copy of the bucket counts, indexed as
     * {@link #bucketLower(int)} expects, for dumps and debugging.
     *
     * @return the bucket weights
     */
    public long[] snapshot()
    {
        return Arrays.copyOf(counts, counts.length);
    }

    /**
     * Returns the bucket index of a duration: 0 for values up to 1
     * nanosecond, one bucket for [1,2) and [2,4), then four linear pieces
     * per octave.
     *
     * @param nanos the duration, clamped at 0
     * @return the bucket index, 0..BUCKET_COUNT-1
     */
    static int bucketOf(long nanos)
    {
        long value = Math.max(0, nanos);

        if (value <= 1)
        {
            return 0;
        }

        int octave = 63 - Long.numberOfLeadingZeros(value);

        if (octave < 2)
        {
            return octave;
        }

        int piece = (int) ((value - (1L << octave)) >> (octave - LOG_SUB));

        if (piece < 0)
        {
            piece = 0;
        }
        if (piece > SUB_BUCKETS - 1)
        {
            piece = SUB_BUCKETS - 1;
        }

        return 2 + (octave - 2) * SUB_BUCKETS + piece;
    }

    /**
     * Returns the smallest duration that the bucket can hold.
     *
     * @param bucket the bucket index
     * @return the lower bound inclusive
     */
    static long bucketLower(int bucket)
    {
        if (bucket < 2)
        {
            return 1L << bucket;
        }

        int octave = 2 + (bucket - 2) / SUB_BUCKETS;
        int piece = (bucket - 2) % SUB_BUCKETS;

        return (1L << octave) + ((long) piece << (octave - LOG_SUB));
    }

    /**
     * Returns the width of the bucket, so its range is
     * [lower, lower + width).
     *
     * @param bucket the bucket index
     * @return the width in nanoseconds
     */
    static long bucketWidth(int bucket)
    {
        if (bucket < 2)
        {
            return 1L << bucket;
        }

        return 1L << ((2 + (bucket - 2) / SUB_BUCKETS) - LOG_SUB);
    }
}
