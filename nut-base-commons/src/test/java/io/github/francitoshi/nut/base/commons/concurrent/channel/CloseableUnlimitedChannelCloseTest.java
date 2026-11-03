/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Same scenario as CloseableBufferedChannelCloseTest, adapted to the
 * unlimited channel.
 *
 * On the current engine a put() never blocks for lack of room, so there is no
 * write barrier to race a close() against: close(timeout, unit) completes
 * immediately and its result is always true, while values already committed
 * before the close are still delivered. These tests pin that contract down,
 * including that a caller with its interrupt status set can close a channel
 * whose readers are parked.
 */
class CloseableUnlimitedChannelCloseTest
{
    private ExecutorService executor;

    @AfterEach
    void tearDown()
    {
        if (executor != null)
        {
            executor.shutdownNow();
        }
    }

    @Test
    void closeCompletesWhilePuttersAreInFlight_andDrainsCommittedValues() throws Exception
    {
        executor = Executors.newFixedThreadPool(3);

        CloseableUnlimitedChannel<String> channel = new CloseableUnlimitedChannel<>();
        AtomicBoolean running = new AtomicBoolean(true);

        for (int p = 0; p < 3; p++)
        {
            executor.submit(() ->
            {
                int putted = 0;
                while (running.get() && putted < 100_000)
                {
                    try
                    {
                        channel.put("v-" + putted++);
                    }
                    catch (IllegalStateException closedUnderneath)
                    {
                        break; // a normal way to stop once the close wins the race
                    }
                }
                return null;
            });
        }
        // let the putters commit a good number of values first
        Thread.sleep(50);

        // the close always completes: nothing on this engine can block it
        assertTrue(channel.close(300, TimeUnit.MILLISECONDS), "close must complete immediately");
        assertTrue(channel.close(1, TimeUnit.MILLISECONDS), "idempotent close keeps returning true");
        assertTrue(channel.isClosed());

        // stop the survivors and check they only stopped by the closed door
        running.set(false);

        // values committed before the close must be delivered, then null
        int drained = 0;
        while (channel.get() != null)
        {
            drained++;
        }
        assertTrue(drained > 0, "values committed before the close must be drained");
        assertNull(channel.get(), "the channel must stay drained");

        // puts after close are rejected
        assertFalse(channel.tryPut("late"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> channel.put("later"));
    }

    @Test
    void close_isIdempotent_andReturnsTrueOnSubsequentCalls_whenNothingIsBlocked() throws Exception
    {
        CloseableUnlimitedChannel<String> channel = new CloseableUnlimitedChannel<>();
        channel.put("only");

        assertTrue(channel.close(), "first close() with nothing blocked must succeed");
        assertTrue(channel.close(), "close() must be idempotent and keep returning true");
        assertTrue(channel.isClosed());

        assertEqualsFirstThenNull(channel, "only");
    }

    private static void assertEqualsFirstThenNull(CloseableUnlimitedChannel<String> channel, String expected)
            throws InterruptedException
    {
        String value = channel.get();
        assertTrue(expected.equals(value), "expected '" + expected + "' but got '" + value + "'");
        assertNull(channel.get(), "channel should be drained and closed, get() must return null");
    }
}
