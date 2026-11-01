/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;

/**
 * Helpers so that a regression shows up as a test <em>failure</em> instead of
 * a hung build: every potentially blocking call runs in a daemon thread and the
 * test fails if it does not finish in {@link #WAIT_MS}.
 */
final class ChannelTestSupport
{
    static final long WAIT_MS = 3000;

    private ChannelTestSupport()
    {
    }

    /** Runs {@code call} in a daemon thread and fails the test if it does not return in time. */
    static <T> T callWithin(String what, Callable<T> call) throws Exception
    {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread t = new Thread(() ->
        {
            try
            {
                result.set(call.call());
            }
            catch (Throwable ex)
            {
                error.set(ex);
            }
        }, "test-" + what);
        t.setDaemon(true);
        t.start();
        t.join(WAIT_MS);
        if (t.isAlive())
        {
            Assertions.fail(what + " is still blocked after " + WAIT_MS + " ms");
        }
        if (error.get() instanceof Exception)
        {
            throw (Exception) error.get();
        }
        if (error.get() != null)
        {
            throw (Error) error.get();
        }
        return result.get();
    }

    static Thread startDaemon(String name, Runnable body)
    {
        Thread t = new Thread(body, name);
        t.setDaemon(true);
        t.start();
        return t;
    }

    /** Waits until {@code t} is parked (blocked in a queue, lock or monitor wait). */
    static void awaitBlocked(Thread t) throws InterruptedException
    {
        long end = System.nanoTime() + WAIT_MS * 1_000_000L;
        while (true)
        {
            Thread.State s = t.getState();
            if (s == Thread.State.WAITING || s == Thread.State.TIMED_WAITING)
            {
                return;
            }
            if (!t.isAlive() || System.nanoTime() > end)
            {
                Assertions.fail("thread " + t.getName() + " did not block, state=" + s);
            }
            Thread.sleep(2);
        }
    }

    /** Joins {@code t} and fails the test if it is still alive after {@link #WAIT_MS}. */
    static void joinOrFail(String what, Thread t) throws InterruptedException
    {
        t.join(WAIT_MS);
        if (t.isAlive())
        {
            Assertions.fail(what + " is still blocked after " + WAIT_MS + " ms");
        }
    }
}
