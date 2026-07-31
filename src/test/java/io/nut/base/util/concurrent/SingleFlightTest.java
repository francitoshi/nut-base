/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SingleFlight")
class SingleFlightTest
{
    @Test
    @DisplayName("coalesces duplicate concurrent executions to a single task run")
    void testBasicCoalescing() throws InterruptedException
    {
        SingleFlight<String, String> sf = new SingleFlight<>();
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch runLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(3);

        AtomicReference<String> res1 = new AtomicReference<>();
        AtomicReference<String> res2 = new AtomicReference<>();
        AtomicReference<String> res3 = new AtomicReference<>();

        Thread t1 = new Thread(() ->
        {
            try
            {
                Callable<String> task = () ->
                {
                    runs.incrementAndGet();
                    runLatch.countDown();
                    Thread.sleep(100);
                    return "result";
                };
                res1.set(sf.call("key", task));
            }
            catch (Exception e)
            {
                res1.set("error");
            }
            finally
            {
                finishLatch.countDown();
            }
        });

        Thread t2 = new Thread(() ->
        {
            try
            {
                runLatch.await();
                assertTrue(sf.isRunning("key"));
                assertEquals(1, sf.size());
                Supplier<String> supplier = () ->
                {
                    runs.incrementAndGet();
                    return "ignored";
                };
                res2.set(sf.call("key", supplier));
            }
            catch (Exception e)
            {
                res2.set("error");
            }
            finally
            {
                finishLatch.countDown();
            }
        });

        Thread t3 = new Thread(() ->
        {
            try
            {
                runLatch.await();
                Function<String, String> function = k ->
                {
                    runs.incrementAndGet();
                    return "ignored";
                };
                res3.set(sf.call("key", function));
            }
            catch (Exception e)
            {
                res3.set("error");
            }
            finally
            {
                finishLatch.countDown();
            }
        });

        t1.start();
        t2.start();
        t3.start();

        assertTrue(finishLatch.await(1, TimeUnit.SECONDS));

        assertEquals("result", res1.get());
        assertEquals("result", res2.get());
        assertEquals("result", res3.get());
        assertEquals(1, runs.get());
        assertFalse(sf.isRunning("key"));
        assertEquals(0, sf.size());
    }

    @Test
    @DisplayName("propagates exceptions to all waiting threads")
    void testExceptionPropagation() throws InterruptedException
    {
        SingleFlight<String, String> sf = new SingleFlight<>();
        CountDownLatch runLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        AtomicReference<Throwable> err1 = new AtomicReference<>();
        AtomicReference<Throwable> err2 = new AtomicReference<>();

        Thread t1 = new Thread(() -> 
        {
            try
            {
                Callable<String> task = () ->
                {
                    runLatch.countDown();
                    Thread.sleep(100);
                    throw new RuntimeException("expected failure");
                };
                sf.call("key", task);
            }
            catch (Throwable t)
            {
                err1.set(t);
            }
            finally
            {
                finishLatch.countDown();
            }
        });

        Thread t2 = new Thread(() ->
        {
            try
            {
                runLatch.await();
                Supplier<String> supplier = () -> "ignored";
                sf.call("key", supplier);
            }
            catch (Throwable t)
            {
                err2.set(t);
            }
            finally
            {
                finishLatch.countDown();
            }
        });

        t1.start();
        t2.start();

        assertTrue(finishLatch.await(10, TimeUnit.SECONDS));

        assertNotNull(err1.get());
        assertNotNull(err2.get());
        assertEquals("expected failure", err1.get().getMessage());
        assertEquals("expected failure", err2.get().getMessage());
    }
}
