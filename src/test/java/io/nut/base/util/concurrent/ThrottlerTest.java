/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Throttler")
class ThrottlerTest
{
    @Test
    @DisplayName("first execution is permitted immediately and subsequent call is throttled")
    void testBasicThrottling() throws InterruptedException
    {
        Throttler throttler = Throttler.of(100, TimeUnit.MILLISECONDS);
        AtomicInteger runs = new AtomicInteger();

        assertTrue(throttler.isReady());
        assertEquals(0L, throttler.getRemainingDelay(TimeUnit.MILLISECONDS));

        // First submit should succeed
        assertTrue(throttler.submit(runs::incrementAndGet));
        assertEquals(1, runs.get());
        assertFalse(throttler.isReady());
        assertTrue(throttler.getRemainingDelay(TimeUnit.MILLISECONDS) > 0);

        // Second submit should be throttled
        assertFalse(throttler.submit(runs::incrementAndGet));
        assertEquals(1, runs.get());

        // Wait for interval to elapse
        Thread.sleep(120);

        assertTrue(throttler.isReady());
        assertEquals(0L, throttler.getRemainingDelay(TimeUnit.MILLISECONDS));

        // Third submit should now succeed
        assertTrue(throttler.submit(runs::incrementAndGet));
        assertEquals(2, runs.get());
    }

    @Test
    @DisplayName("reset makes the throttler ready immediately")
    void testReset()
    {
        Throttler throttler = Throttler.of(1, TimeUnit.HOURS);
        AtomicInteger runs = new AtomicInteger();

        assertTrue(throttler.submit(runs::incrementAndGet));
        assertFalse(throttler.isReady());

        throttler.reset();

        assertTrue(throttler.isReady());
        assertTrue(throttler.submit(runs::incrementAndGet));
        assertEquals(2, runs.get());
    }

    @Test
    @DisplayName("executes asynchronously when an executor is provided")
    void testExecutor() throws InterruptedException
    {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger runs = new AtomicInteger();
        Executor directExecutor = Runnable::run;

        Throttler throttler = Throttler.builder()
                .interval(100, TimeUnit.MILLISECONDS)
                .executor(directExecutor)
                .build();

        assertTrue(throttler.submit(() -> 
        {
            runs.incrementAndGet();
            latch.countDown();
        }));

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        assertEquals(1, runs.get());
    }

    @Test
    @DisplayName("validates builder arguments strictly")
    void testBuilderValidation()
    {
        assertThrows(IllegalArgumentException.class, () -> Throttler.builder().interval(0, TimeUnit.SECONDS));
        assertThrows(NullPointerException.class, () -> Throttler.builder().interval(10, null));
        assertThrows(NullPointerException.class, () -> Throttler.builder().executor(null));

        assertThrows(IllegalStateException.class, () -> Throttler.builder().build());
    }

    @Test
    @DisplayName("returns configured interval correctly")
    void testGetInterval()
    {
        Throttler throttler = Throttler.of(5, TimeUnit.SECONDS);
        assertEquals(5, throttler.getInterval(TimeUnit.SECONDS));
        assertEquals(5000, throttler.getInterval(TimeUnit.MILLISECONDS));
    }
}
