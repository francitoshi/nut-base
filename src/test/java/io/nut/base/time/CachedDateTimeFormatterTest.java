/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.time;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachedDateTimeFormatterTest
{

    private static final ZoneId UTC = ZoneOffset.UTC;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String expected(long epochMilli, ZoneId zoneId, DateTimeFormatter fmt)
    {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), zoneId).format(fmt);
    }

    @Test
    void formatsBasicTimestampCorrectly()
    {
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(UTC, FMT);

        long ms = 1_700_000_000_000L; // arbitrary fixed instant
        assertEquals(expected(ms, UTC, FMT), formatter.format(ms));
    }

    @Test
    void returnsSameStringForDifferentMillisWithinSameSecond()
    {
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(UTC, FMT);

        long base = 1_700_000_000_000L;
        String first = formatter.format(base);
        String second = formatter.format(base + 500); // same second, different ms

        assertEquals(first, second);
    }

    @Test
    void recomputesWhenSecondChanges()
    {
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(UTC, FMT);

        long base = 1_700_000_000_000L;
        String first = formatter.format(base);
        String next = formatter.format(base + 1000); // next second

        assertEquals(expected(base, UTC, FMT), first);
        assertEquals(expected(base + 1000, UTC, FMT), next);
        assertTrue(!first.equals(next) || !expected(base, UTC, FMT).equals(expected(base + 1000, UTC, FMT)));
    }

    @Test
    void handlesSecondBoundaryPrecisely()
    {
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(UTC, FMT);

        long secondStart = 1_700_000_001_000L;

        // last ms of previous second
        assertEquals(expected(secondStart - 1, UTC, FMT), formatter.format(secondStart - 1));
        // first ms of new second
        assertEquals(expected(secondStart, UTC, FMT), formatter.format(secondStart));
    }

    @Test
    void worksAcrossMultipleDistinctSeconds()
    {
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(UTC, FMT);

        for (long s = 0; s < 5; s++)
        {
            long ms = 1_700_000_000_000L + s * 1000;
            assertEquals(expected(ms, UTC, FMT), formatter.format(ms));
        }
    }

    @Test
    void respectsGivenZoneId()
    {
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(tokyo, FMT);

        long ms = 1_700_000_000_000L;
        assertEquals(expected(ms, tokyo, FMT), formatter.format(ms));
    }

    @Test
    void respectsGivenFormatterPattern()
    {
        DateTimeFormatter isoLike = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(UTC, isoLike);

        long ms = 1_700_000_000_000L;
        assertEquals(expected(ms, UTC, isoLike), formatter.format(ms));
    }

    @Test
    void handlesEpochZero()
    {
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(UTC, FMT);

        assertEquals(expected(0L, UTC, FMT), formatter.format(0L));
    }

    @Test
    void constructorRejectsNullZoneId()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new CachedDateTimeFormatter(null, FMT));
    }

    @Test
    void constructorRejectsNullFormatter()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new CachedDateTimeFormatter(UTC, null));
    }

    @Test
    void formattedResultIsNeverNull()
    {
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(UTC, FMT);
        assertNotNull(formatter.format(1_700_000_000_000L));
    }

    // Concurrency: many threads hammering the same second must all get
    // the correct, identical result with no exceptions.
    @Test
    void isThreadSafeUnderConcurrentAccessSameSecond() throws InterruptedException
    {
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(UTC, FMT);
        long base = 1_700_000_000_000L;
        String expected = expected(base, UTC, FMT);

        int threadCount = 16;
        int callsPerThread = 10_000;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger mismatches = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++)
        {
            int threadIndex = t;
            pool.submit(() ->
            {
                try
                {
                    startLatch.await();
                    for (int i = 0; i < callsPerThread; i++)
                    {
                        long ms = base + ((threadIndex + i) % 1000); // stays in same second
                        String result = formatter.format(ms);
                        if (!expected.equals(result))
                        {
                            mismatches.incrementAndGet();
                        }
                    }
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                finally
                {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(0, mismatches.get());
    }

    // Concurrency across a rolling window of seconds, to stress cache invalidation.
    @Test
    void isThreadSafeUnderConcurrentAccessAcrossSeconds() throws InterruptedException
    {
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(UTC, FMT);
        long base = 1_700_000_000_000L;

        int threadCount = 8;
        int secondsSpan = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger mismatches = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++)
        {
            pool.submit(() ->
            {
                try
                {
                    for (int s = 0; s < secondsSpan; s++)
                    {
                        for (int ms = 0; ms < 1000; ms += 100)
                        {
                            long timestamp = base + s * 1000L + ms;
                            String result = formatter.format(timestamp);
                            if (!expected(timestamp, UTC, FMT).equals(result))
                            {
                                mismatches.incrementAndGet();
                            }
                        }
                    }
                }
                finally
                {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(0, mismatches.get());
    }

    @Test
    void producesConsistentDayOfWeekFormatting()
    {
        DateTimeFormatter dowFmt = DateTimeFormatter.ofPattern("EEEE");
        CachedDateTimeFormatter formatter = new CachedDateTimeFormatter(UTC, dowFmt);

        long ms = 1_700_000_000_000L; // known instant, Tuesday in UTC
        DayOfWeek dow = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), UTC).getDayOfWeek();

        assertEquals(dow.toString().substring(0, 1) + dow.toString().substring(1).toLowerCase(),
                formatter.format(ms));
    }

    @Test
    void throwsOnInvalidPatternAtConstructionTime()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new CachedDateTimeFormatter(UTC, DateTimeFormatter.ofPattern("not a valid pattern }}}")));
    }
}
