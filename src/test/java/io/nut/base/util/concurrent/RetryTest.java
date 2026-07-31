/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Retry")
class RetryTest
{
    @Test
    @DisplayName("defaults() uses sensible defaults")
    void testDefaults() throws Exception
    {
        Retry retry = Retry.defaults();
        AtomicInteger attempts = new AtomicInteger();
        String result = retry.call(() -> {
            attempts.incrementAndGet();
            return "success";
        });
        assertEquals("success", result);
        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("retries up to max attempts and throws the last exception on failure")
    void testMaxAttemptsReached()
    {
        Retry retry = Retry.builder()
                .maxAttempts(3)
                .backoff(Retry.Backoffs.none())
                .build();

        AtomicInteger attempts = new AtomicInteger();
        IOException finalException = assertThrows(IOException.class, () -> retry.call(() ->
        {
            attempts.incrementAndGet();
            throw new IOException("Attempt " + attempts.get());
        }));

        assertEquals("Attempt 3", finalException.getMessage());
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("succeeds if a subsequent attempt is successful")
    void testSucceedsEventually() throws Exception
    {
        Retry retry = Retry.builder()
                .maxAttempts(3)
                .backoff(Retry.Backoffs.none())
                .build();

        AtomicInteger attempts = new AtomicInteger();
        String result = retry.call(() ->
        {
            int current = attempts.incrementAndGet();
            if (current < 3)
            {
                throw new IOException("Fail");
            }
            return "ok";
        });

        assertEquals("ok", result);
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("does not retry on non-matching exception classes")
    void testNonMatchingExceptionClass()
    {
        Retry retry = Retry.builder()
                .maxAttempts(3)
                .retryOn(IOException.class)
                .backoff(Retry.Backoffs.none())
                .build();

        AtomicInteger attempts = new AtomicInteger();
        assertThrows(RuntimeException.class, () -> retry.call(() ->
        {
            attempts.incrementAndGet();
            throw new RuntimeException("immediate");
        }));

        assertEquals(1, attempts.get());
    }

    @Test
    @DisplayName("retries on matching exception classes and predicates")
    void testMatchingExceptionPredicate()
    {
        Retry retry = Retry.builder()
                .maxAttempts(3)
                .retryIf(ex -> "retryable".equals(ex.getMessage()))
                .backoff(Retry.Backoffs.none())
                .build();

        AtomicInteger attempts = new AtomicInteger();
        assertThrows(RuntimeException.class, () -> retry.call(() ->
        {
            int current = attempts.incrementAndGet();
            if (current == 1)
            {
                throw new RuntimeException("retryable");
            }
            else
            {
                throw new RuntimeException("immediate");
            }
        }));

        assertEquals(2, attempts.get());
    }

    @Test
    @DisplayName("retries based on result predicate")
    void testRetryIfResult() throws Exception
    {
        Retry retry = Retry.builder()
                .maxAttempts(3)
                .retryIfResult(res -> "retry".equals(res))
                .backoff(Retry.Backoffs.none())
                .build();

        AtomicInteger attempts = new AtomicInteger();
        String result = retry.call(() ->
        {
            int current = attempts.incrementAndGet();
            if (current < 3)
            {
                return "retry";
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(3, attempts.get());
    }

    @Test
    @DisplayName("notifies listeners on failure")
    void testListeners()
    {
        List<String> logs = new ArrayList<>();
        Retry retry = Retry.builder()
                .maxAttempts(3)
                .backoff(Retry.Backoffs.fixed(10))
                .listener((attempt, error, delay) -> logs.add(attempt + ":" + (error != null ? error.getMessage() : "null") + ":" + delay))
                .build();

        AtomicInteger attempts = new AtomicInteger();
        assertThrows(IOException.class, () -> retry.call(() ->
        {
            int current = attempts.incrementAndGet();
            throw new IOException("err" + current);
        }));

        assertEquals(2, logs.size());
        assertEquals("1:err1:10", logs.get(0));
        assertEquals("2:err2:10", logs.get(1));
    }

    @Test
    @DisplayName("uses custom sleeper")
    void testCustomSleeper() throws Exception
    {
        List<Long> sleepTimes = new ArrayList<>();
        Retry retry = Retry.builder()
                .maxAttempts(3)
                .backoff(Retry.Backoffs.fixed(50))
                .sleeper(sleepTimes::add)
                .build();

        AtomicInteger attempts = new AtomicInteger();
        retry.call(() ->
        {
            int current = attempts.incrementAndGet();
            if (current < 3)
            {
                throw new IOException();
            }
            return "ok";
        });

        assertEquals(2, sleepTimes.size());
        assertEquals(50L, sleepTimes.get(0));
        assertEquals(50L, sleepTimes.get(1));
    }

    @Test
    @DisplayName("validates arguments strictly")
    void testArgumentValidation()
    {
        assertThrows(IllegalArgumentException.class, () -> Retry.builder().maxAttempts(0));
        assertThrows(NullPointerException.class, () -> Retry.builder().backoff(null));
        assertThrows(NullPointerException.class, () -> Retry.builder().retryOn(null));
        assertThrows(NullPointerException.class, () -> Retry.builder().retryIf(null));
        assertThrows(NullPointerException.class, () -> Retry.builder().retryIfResult(null));
        assertThrows(NullPointerException.class, () -> Retry.builder().listener(null));
        assertThrows(NullPointerException.class, () -> Retry.builder().sleeper(null));

        assertThrows(IllegalArgumentException.class, () -> Retry.Backoffs.fixed(-1));
        assertThrows(IllegalArgumentException.class, () -> Retry.Backoffs.exponential(0));
        assertThrows(IllegalArgumentException.class, () -> Retry.Backoffs.exponential(10, 0.9, 100));
        assertThrows(IllegalArgumentException.class, () -> Retry.Backoffs.exponential(10, 2.0, 5));
        assertThrows(IllegalArgumentException.class, () -> Retry.Backoffs.jitter(Retry.Backoffs.none(), -0.1));
        assertThrows(IllegalArgumentException.class, () -> Retry.Backoffs.jitter(Retry.Backoffs.none(), 1.1));
    }

    @Test
    @DisplayName("exponential backoff calculates correct delays")
    void testExponentialBackoff()
    {
        Retry.Backoff exp = Retry.Backoffs.exponential(100, 2.0, 1000);
        assertEquals(100L, exp.delayMillis(1));
        assertEquals(200L, exp.delayMillis(2));
        assertEquals(400L, exp.delayMillis(3));
        assertEquals(800L, exp.delayMillis(4));
        assertEquals(1000L, exp.delayMillis(5));
        assertEquals(1000L, exp.delayMillis(10));
    }

    @Test
    @DisplayName("jittered backoff remains within bounds")
    void testJitterBounds()
    {
        Retry.Backoff base = Retry.Backoffs.fixed(1000);
        Retry.Backoff jittered = Retry.Backoffs.jitter(base, 0.2); // [800, 1200]

        for (int i = 0; i < 100; i++)
        {
            long delay = jittered.delayMillis(1);
            assertTrue(delay >= 800L, "delay was " + delay);
            assertTrue(delay <= 1200L, "delay was " + delay);
        }
    }
}
