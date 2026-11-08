/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.profile;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exact-value tests of the bucket map and quantile tests against uniform
 * distributions, whose intra-bucket cdf is linear, so the interpolation is
 * expected near-exact within the documented bucket error.
 */
public class LatencyHistogramTest
{
    @Test
    public void bucketOfFitsTheLadder()
    {
        assertEquals(0, LatencyHistogram.bucketOf(0));
        assertEquals(0, LatencyHistogram.bucketOf(1));
        assertEquals(1, LatencyHistogram.bucketOf(2));
        assertEquals(1, LatencyHistogram.bucketOf(3));
        assertEquals(2, LatencyHistogram.bucketOf(4));
        assertEquals(5, LatencyHistogram.bucketOf(7));  // octave 2 piece 3... see bounds
        assertEquals(6, LatencyHistogram.bucketOf(8));  // octave 3 piece 0
    }

    @Test
    public void boundsAreContiguous()
    {
        long expected = 1;

        for (int bucket = 0; bucket < LatencyHistogram.BUCKET_COUNT; bucket++)
        {
            assertEquals(expected, LatencyHistogram.bucketLower(bucket), "lower of bucket " + bucket);
            expected += LatencyHistogram.bucketWidth(bucket);
        }

        assertEquals(1L << 61, LatencyHistogram.bucketWidth(LatencyHistogram.BUCKET_COUNT - 1));
    }

    @Test
    public void everyValueFallsInsideItsOwnBucket()
    {
        for (int octave = 0; octave < 61; octave++)
        {
            long base = octave == 0 ? 1 : 1L << octave;   // [1,2) for the first octave
            long upper = base * 2;                        // exclusive

            long[] probes = {base, base + 1, base + (upper - base) / 2, upper - 1};

            for (long value : probes)
            {
                int bucket = LatencyHistogram.bucketOf(value);
                long lower = LatencyHistogram.bucketLower(bucket);
                long width = LatencyHistogram.bucketWidth(bucket);

                assertTrue(value >= lower && value < lower + width,
                    "value " + value + " out of bucket " + bucket
                        + " [" + lower + "," + (lower + width) + ")");
            }
        }
    }

    @Test
    public void uniformDistributionQuantilesAreNearExact()
    {
        LatencyHistogram histogram = new LatencyHistogram();

        long n = 100_000;
        for (long v = 1; v <= n; v++)
        {
            histogram.record(v, 1);
        }

        assertEquals(n, histogram.totalWeight());
        assertEquals(1, histogram.minNanos());
        assertEquals(n, histogram.maxNanos());

        // for the uniform 1..n distribution the value of rank r is r itself;
        // the interpolation is near exact where buckets are well filled and
        // weaker at the tail, where the last bucket is half empty (max 2%)
        assertEquals(50_000, histogram.quantileNanos(0.50), 500);
        assertEquals(75_000, histogram.quantileNanos(0.75), 750);
        assertEquals(90_000, histogram.quantileNanos(0.90), 900);
        assertEquals(99_000, histogram.quantileNanos(0.99), 2_000);
        assertEquals(99_900, histogram.quantileNanos(0.999), 2_000);
    }

    @Test
    public void monotonicQuantiles()
    {
        LatencyHistogram histogram = new LatencyHistogram();

        Random random = new Random(42);
        for (int i = 0; i < 10_000; i++)
        {
            histogram.record(1 + (long) (random.nextDouble() * random.nextDouble() * 1_000_000), 1);
        }

        long previous = 0;
        for (double q = 0; q <= 1.0; q += 0.01)
        {
            long value = histogram.quantileNanos(q);
            assertTrue(value >= previous, "quantile " + q + " went down: " + previous + " -> " + value);
            previous = value;
        }
    }

    @Test
    public void singleValueExactAtTheEnds()
    {
        LatencyHistogram histogram = new LatencyHistogram();
        histogram.record(1_000_000, 1);

        assertEquals(1, histogram.totalWeight());
        assertEquals(1_000_000, histogram.quantileNanos(0));
        assertEquals(1_000_000, histogram.quantileNanos(1));

        long middle = histogram.quantileNanos(0.5);
        long lower = LatencyHistogram.bucketLower(LatencyHistogram.bucketOf(1_000_000));
        long upper = lower + LatencyHistogram.bucketWidth(LatencyHistogram.bucketOf(1_000_000));

        assertTrue(middle >= lower && middle <= upper, "the estimate must stay in the bucket: " + middle);
    }

    @Test
    public void weightedBatchBeatsWeights()
    {
        LatencyHistogram histogram = new LatencyHistogram();
        histogram.record(1000, 9);      // nine events of 1000ns in one bucket
        histogram.record(100_000, 1);   // one event, for the upper tail

        assertEquals(10, histogram.totalWeight());

        // nine tenths of the weight sit in the 1000ns bucket: the 90th
        // percentile must still be an estimate inside that bucket
        long p90 = histogram.quantileNanos(0.9);
        int bucket = LatencyHistogram.bucketOf(1000);
        assertTrue(p90 >= LatencyHistogram.bucketLower(bucket) && p90 <= LatencyHistogram.bucketLower(bucket) + LatencyHistogram.bucketWidth(bucket),
            "p90 estimate outside the 1000ns bucket: " + p90);

        // the top event falls in the 100000ns bucket, above the 1000ns one
        long p99 = histogram.quantileNanos(0.99);
        assertTrue(p99 > 1000, "p99 must be above the 1000ns bucket: " + p99);
        assertTrue(p99 <= histogram.maxNanos());
    }

    @Test
    public void zeroAndEmptyBehave()
    {
        LatencyHistogram empty = new LatencyHistogram();

        assertEquals(0, empty.quantileNanos(0.5));
        assertEquals(0, empty.minNanos());
        assertEquals(0, empty.maxNanos());

        LatencyHistogram histogram = new LatencyHistogram();
        histogram.record(0, 1);
        histogram.record(0, 0); // zero weight: no-op

        assertEquals(1, histogram.totalWeight());
        assertEquals(0, histogram.minNanos());
        // the estimate is clamped into [min,max] = [0,0]
        assertEquals(0, histogram.quantileNanos(0.5));
    }

    @Test
    public void mergeAddsEverything()
    {
        LatencyHistogram a = new LatencyHistogram();
        LatencyHistogram b = new LatencyHistogram();

        for (long v = 1; v <= 1000; v++)
        {
            a.record(v, 1);
        }
        for (long v = 1001; v <= 2000; v++)
        {
            b.record(v, 1);
        }

        a.merge(b);

        assertEquals(2000, a.totalWeight());
        assertEquals(1, a.minNanos());
        assertEquals(2000, a.maxNanos());
        assertEquals(1000, a.quantileNanos(0.5), 1000 * 1.5);
        assertEquals(2000, a.quantileNanos(1));
    }

    @Test
    public void mergeIntoEmptyKeepsMin()
    {
        LatencyHistogram empty = new LatencyHistogram();
        LatencyHistogram source = new LatencyHistogram();
        source.record(5000, 3);

        empty.merge(source);

        assertEquals(3, empty.totalWeight());
        assertEquals(5000, empty.minNanos());
        assertEquals(5000, empty.maxNanos());
    }

    @Test
    public void rejectsNegativeInputs()
    {
        LatencyHistogram histogram = new LatencyHistogram();
        assertThrows(IllegalArgumentException.class, () -> histogram.record(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> histogram.record(10, -1));
    }

    @Test
    public void snapshotIsACopy()
    {
        LatencyHistogram histogram = new LatencyHistogram();
        histogram.record(8, 2);

        int bucket8 = LatencyHistogram.bucketOf(8);
        long[] snapshot = histogram.snapshot();
        assertEquals(2, snapshot[bucket8]);

        snapshot[bucket8] = 0;
        assertEquals(2, histogram.snapshot()[bucket8], "the snapshot is a copy, not a view");
    }
}
