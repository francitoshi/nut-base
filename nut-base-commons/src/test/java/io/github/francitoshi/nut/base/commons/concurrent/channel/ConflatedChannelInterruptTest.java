/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static io.github.francitoshi.nut.base.commons.concurrent.channel.ChannelTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class ConflatedChannelInterruptTest
{
    @Test
    void aPreInterruptedGetAbortsEvenWhenAValueIsReady()
    {
        ConflatedChannel<Integer> channel = new ConflatedChannel<>();
        List<InterruptedException> received = new CopyOnWriteArrayList<>();
        channel.setInterruptedExceptionListener(received::add);
        channel.put(7);

        Thread.currentThread().interrupt();
        try
        {
            assertNull(channel.get(), "an interrupted get() must never deliver a value");
            assertFalse(Thread.currentThread().isInterrupted(), "the status is consumed");
        }
        finally
        {
            Thread.interrupted();
        }
        assertEquals(1, received.size(), "the listener saw the interruption");
        assertTrue(channel.isInterrupted(), "the channel keeps its own record");

        // the slot was not touched: a normal get() still reads it
        assertEquals(7, channel.get().intValue());
    }

    @Test
    void aPreInterruptedTimedGetAbortsEvenWhenAValueIsReady()
    {
        ConflatedChannel<Integer> channel = new ConflatedChannel<>();
        channel.put(7);

        Thread.currentThread().interrupt();
        try
        {
            assertNull(channel.get(1, TimeUnit.SECONDS));
        }
        finally
        {
            Thread.interrupted();
        }
        assertTrue(channel.isInterrupted());

        assertEquals(7, channel.get().intValue(), "the slot survives the aborted get");
    }

    @Test
    void aPreInterruptedPutDeliversNothing()
    {
        ConflatedChannel<Integer> channel = new ConflatedChannel<>();
        List<InterruptedException> received = new CopyOnWriteArrayList<>();
        channel.setInterruptedExceptionListener(received::add);

        Thread.currentThread().interrupt();
        try
        {
            channel.put(7);
        }
        finally
        {
            Thread.interrupted();
        }

        assertTrue(received.size() == 1, "the listener saw the interruption");
        assertEquals(java.util.Optional.empty(), channel.tryGet(), "nothing was delivered");
        assertTrue(channel.isInterrupted());
        assertFalse(Thread.currentThread().isInterrupted(), "the status was consumed");
    }

    @Test
    void anInterruptionWhileParkedStillAbortsTheGet() throws Exception
    {
        ConflatedChannel<Integer> channel = new ConflatedChannel<>();

        Thread getter = startDaemon("conflated-get", channel::get);
        awaitBlocked(getter);
        getter.interrupt();
        joinOrFail("conflated get", getter);

        assertFalse(getter.isInterrupted(), "the default listener leaves the thread un-marked");
        assertTrue(channel.isInterrupted());
    }
}
