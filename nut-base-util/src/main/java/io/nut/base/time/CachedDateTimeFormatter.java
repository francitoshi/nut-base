/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * High-performance, thread-safe formatter that converts an epoch-millisecond
 * timestamp into a formatted date/time String, caching the last computed
 * result and only recomputing when the timestamp falls into a different
 * second.
 *
 * <p>Intended for extremely hot call sites (e.g. logging frameworks called
 * millions of times per minute), where most calls happen within the same
 * wall-clock second and therefore produce an identical formatted String.
 *
 * <p><b>Thread-safety:</b> This class is lock-free. It uses a single
 * {@code volatile} reference to an immutable {@link CacheEntry}. Concurrent
 * threads may occasionally race and recompute the same value redundantly,
 * but this is harmless because formatting is a pure function of the second
 * value: no lock is required to guarantee correctness, only visibility,
 * which {@code volatile} provides.
 *
 * <p><b>Important:</b> the cache key is the epoch second (ms / 1000), so
 * this class is only correct if the supplied {@link DateTimeFormatter} does
 * NOT render sub-second precision (milliseconds/nanoseconds). If your
 * pattern includes {@code SSS} or nanos, do not use this cache, since two
 * different milliseconds within the same second would incorrectly return
 * the same cached String.
 *
 * <p>Instances are meant to be created once (e.g. as a {@code static final}
 * field) and reused for the lifetime of the application.
 */
public final class CachedDateTimeFormatter
{

    private final ZoneId zoneId;
    private final DateTimeFormatter formatter;

    // Sentinel initial value guarantees the very first call always misses
    // the cache and computes a real result.
    private static final long NO_SECOND_CACHED = Long.MIN_VALUE;

    // Single volatile reference to an immutable snapshot (second -> formatted string).
    // Reading/writing a reference is atomic in the JMM, so no extra locking is needed.
    private volatile CacheEntry cache = new CacheEntry(NO_SECOND_CACHED, null);

    public CachedDateTimeFormatter(ZoneId zoneId, DateTimeFormatter formatter)
    {
        if (zoneId == null || formatter == null)
        {
            throw new IllegalArgumentException("zoneId and formatter must not be null");
        }
        this.zoneId = zoneId;
        this.formatter = formatter;
    }

    /**
     * Converts the given epoch-millisecond timestamp to a formatted String,
     * reusing the previous result if {@code epochMilli} falls within the
     * same second as the last call.
     *
     * @param epochMilli milliseconds since the epoch (1970-01-01T00:00:00Z)
     * @return the formatted date/time as a String
     */
    public String format(long epochMilli) 
    {
        // Math.floorDiv handles negative epoch millis correctly (pre-1970),
        // unlike plain integer division which truncates toward zero.
        final long second = Math.floorDiv(epochMilli, 1000L);

        // Single volatile read: cheap, and gives us a consistent snapshot
        // (entry.second and entry.formatted always correspond to each other
        // since CacheEntry is immutable).
        final CacheEntry entry = cache;

        if (entry.second == second) 
        {
            return entry.formatted;
        }

        // Cache miss: recompute. Multiple threads might do this
        // simultaneously for the same second — that's fine, the result
        // is identical regardless of which thread "wins".
        final String formatted = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), zoneId).format(formatter);

        // Publish the new snapshot. A plain volatile write is enough:
        // we don't need compare-and-swap because a "lost update" here
        // just means a redundant recomputation happens on the next call,
        // never an incorrect result.
        cache = new CacheEntry(second, formatted);

        return formatted;
    }

    /**
     * Immutable cache snapshot: an epoch-second value paired with its
     * already-formatted String representation.
     */
    private static final class CacheEntry 
    {
        private final long second;
        private final String formatted;

        private CacheEntry(long second, String formatted)
        {
            this.second = second;
            this.formatted = formatted;
        }
    }
}