/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Debouncer")
class DebouncerTest
{
    @Test
    @DisplayName("standard debounce (trailing only) executes last task after delay")
    void testStandardDebounce() throws InterruptedException
    {
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);

        try (Debouncer debouncer = Debouncer.builder()
                .delay(50, TimeUnit.MILLISECONDS)
                .listener(new Debouncer.Listener()
                {
                    @Override
                    public void onScheduled() {}

                    @Override
                    public void onCancelled() {}

                    @Override
                    public void onExecuted(Throwable failure)
                    {
                        latch.countDown();
                    }
                })
                .build())
        {
            debouncer.submit(() -> runs.incrementAndGet());
            debouncer.submit(() -> runs.incrementAndGet());
            debouncer.submit(() -> runs.set(42)); // Last one should run

            assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
            assertEquals(42, runs.get());
        }
    }

    @Test
    @DisplayName("cancel prevents task execution")
    void testCancel() throws InterruptedException
    {
        AtomicInteger runs = new AtomicInteger();
        try (Debouncer debouncer = Debouncer.of(50, TimeUnit.MILLISECONDS))
        {
            debouncer.submit(() -> runs.incrementAndGet());
            assertTrue(debouncer.isPending());
            assertTrue(debouncer.cancel());
            assertFalse(debouncer.isPending());

            Thread.sleep(100);
            assertEquals(0, runs.get());
        }
    }

    @Test
    @DisplayName("flush executes pending task immediately")
    void testFlush() throws InterruptedException
    {
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);

        try (Debouncer debouncer = Debouncer.builder()
                .delay(1000, TimeUnit.MILLISECONDS)
                .listener(new Debouncer.Listener()
                {
                    @Override
                    public void onScheduled() {}

                    @Override
                    public void onCancelled() {}

                    @Override
                    public void onExecuted(Throwable failure)
                    {
                        latch.countDown();
                    }
                })
                .build())
        {
            debouncer.submit(() -> runs.set(10));
            assertTrue(debouncer.isPending());
            assertTrue(debouncer.flush());
            assertFalse(debouncer.isPending());

            assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
            assertEquals(10, runs.get());
        }
    }

    @Test
    @DisplayName("leading execution runs immediately")
    void testLeadingExecution() throws InterruptedException
    {
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);

        try (Debouncer debouncer = Debouncer.builder()
                .delay(100, TimeUnit.MILLISECONDS)
                .leading(true)
                .trailing(false)
                .listener(new Debouncer.Listener()
                {
                    @Override
                    public void onScheduled() {}

                    @Override
                    public void onCancelled() {}

                    @Override
                    public void onExecuted(Throwable failure)
                    {
                        latch.countDown();
                    }
                })
                .build())
        {
            debouncer.submit(() -> runs.incrementAndGet());
            // Wait for the asynchronous immediate execution to complete
            assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
            assertEquals(1, runs.get());

            // Subsequent call during cooldown is ignored
            debouncer.submit(() -> runs.incrementAndGet());
            Thread.sleep(150);

            assertEquals(1, runs.get());
        }
    }

    @Test
    @DisplayName("leading and trailing execution combination works")
    void testLeadingAndTrailing() throws InterruptedException
    {
        AtomicInteger runs = new AtomicInteger();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(2);

        try (Debouncer debouncer = Debouncer.builder()
                .delay(50, TimeUnit.MILLISECONDS)
                .leading(true)
                .trailing(true)
                .listener(new Debouncer.Listener()
                {
                    @Override
                    public void onScheduled() {}

                    @Override
                    public void onCancelled() {}

                    @Override
                    public void onExecuted(Throwable failure)
                    {
                        failures.add(failure);
                        latch.countDown();
                    }
                })
                .build())
        {
            debouncer.submit(() -> runs.addAndGet(1)); // runs immediately (1)
            debouncer.submit(() -> runs.addAndGet(10)); // trailing (scheduled)
            debouncer.submit(() -> runs.addAndGet(100)); // trailing (scheduled, overrides 10)

            assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
            assertEquals(101, runs.get());
        }
    }

    @Test
    @DisplayName("validates builder arguments strictly")
    void testBuilderValidation()
    {
        assertThrows(IllegalArgumentException.class, () -> Debouncer.builder().delay(0, TimeUnit.SECONDS));
        assertThrows(NullPointerException.class, () -> Debouncer.builder().delay(10, null));
        assertThrows(NullPointerException.class, () -> Debouncer.builder().scheduler(null));
        assertThrows(NullPointerException.class, () -> Debouncer.builder().executor(null));
        assertThrows(NullPointerException.class, () -> Debouncer.builder().listener(null));

        assertThrows(IllegalStateException.class, () -> Debouncer.builder().build());
        assertThrows(IllegalArgumentException.class, () -> Debouncer.builder().delay(10, TimeUnit.MILLISECONDS).leading(false).trailing(false).build());
    }

    @Test
    @DisplayName("listener notifies cancellation correctly")
    void testListenerCancellation()
    {
        AtomicInteger cancellations = new AtomicInteger();
        try (Debouncer debouncer = Debouncer.builder()
                .delay(50, TimeUnit.MILLISECONDS)
                .listener(new Debouncer.Listener()
                {
                    @Override
                    public void onScheduled() {}

                    @Override
                    public void onCancelled()
                    {
                        cancellations.incrementAndGet();
                    }

                    @Override
                    public void onExecuted(Throwable failure) {}
                })
                .build())
        {
            debouncer.submit(() -> {});
            debouncer.submit(() -> {}); // Should cancel the first one
            assertEquals(1, cancellations.get());
            debouncer.cancel(); // Should cancel the second one
            assertEquals(2, cancellations.get());
        }
    }
}
