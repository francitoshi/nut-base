/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static io.github.francitoshi.nut.base.commons.concurrent.channel.ChannelTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class ChannelInterruptedListenerTest
{
    // ------------------------------------------------------------------
    // default listener
    // ------------------------------------------------------------------

    @Test
    void theDefaultListenerLeavesTheThreadUnmarkedButMarksTheChannel()
    {
        Channel<Integer> channel = Channel.buffered(1);

        // the default listener IS the logging one, and the log entry is part
        // of its contract: capture it instead of letting it spill to the
        // console, staying silent while the default listener keeps being the
        // one under test
        Logger logger = Logger.getLogger(Channel.class.getName());
        List<LogRecord> records = new CopyOnWriteArrayList<>();
        Handler capture = new Handler()
        {
            @Override
            public void publish(LogRecord record)
            {
                if (record.getLevel().intValue() >= Level.WARNING.intValue())
                {
                    records.add(record);
                }
            }
            @Override
            public void flush()
            {
            }
            @Override
            public void close()
            {
            }
        };

        boolean useParentHandlers = logger.getUseParentHandlers();
        logger.setUseParentHandlers(false);
        logger.addHandler(capture);
        try
        {
            Thread.currentThread().interrupt();
            assertNull(channel.get(), "the pre-interrupted blocking get aborts");
            assertFalse(Thread.currentThread().isInterrupted(), "the default listener must not re-mark the thread");
            assertTrue(channel.isInterrupted(), "the channel keeps its own record");
        }
        finally
        {
            logger.removeHandler(capture);
            logger.setUseParentHandlers(useParentHandlers);
            Thread.interrupted();
        }

        assertTrue(records.stream()
                        .anyMatch(r -> r.getThrown() instanceof InterruptedException),
                "the default listener must log the interruption");
    }

    @Test
    void theListenerIsTheOnlyDecisionPointForTheThread()
    {
        Channel<Integer> channel = Channel.buffered(1);
        channel.setInterruptedExceptionListener(ex -> { });
        Thread.currentThread().interrupt();
        try
        {
            assertNull(channel.get());
            assertFalse(Thread.currentThread().isInterrupted(),
                    "a no-op listener must leave the thread un-marked");
            assertTrue(channel.isInterrupted(), "marking the channel is internal to the family");
        }
        finally
        {
            Thread.interrupted();
        }
    }

    // ------------------------------------------------------------------
    // custom listener
    // ------------------------------------------------------------------

    @Test
    void aCustomListenerReceivesTheException() throws Exception
    {
        Channel<Integer> channel = Channel.buffered(1);
        List<InterruptedException> received = new CopyOnWriteArrayList<>();
        channel.setInterruptedExceptionListener(received::add);

        Thread waiter = startDaemon("blocking-get", channel::get);
        awaitBlocked(waiter);
        waiter.interrupt();
        joinOrFail("blocking get", waiter);

        assertFalse(waiter.isInterrupted(), "with the default listener the thread is left un-marked");
        assertEquals(1, received.size());
        assertTrue(channel.isInterrupted());
    }

    @Test
    void aCustomListenerCanRestoreTheThreadFlag() throws Exception
    {
        Channel<Integer> channel = Channel.buffered(1);
        AtomicBoolean flagAfter = new AtomicBoolean();
        AtomicReference<Throwable> error = new AtomicReference<>();
        channel.setInterruptedExceptionListener(ex -> Thread.currentThread().interrupt());

        Thread waiter = startDaemon("restoring-get", () ->
        {
            try
            {
                channel.get();
            }
            catch (Throwable ex)
            {
                error.set(ex);
            }
            flagAfter.set(Thread.currentThread().isInterrupted());
        });
        awaitBlocked(waiter);
        waiter.interrupt();
        joinOrFail("restoring get", waiter);

        assertNull(error.get(), "the operation still aborts, it does not throw");
        assertTrue(flagAfter.get(), "the listener decided to re-mark the thread");
        assertTrue(channel.isInterrupted());
    }

    @Test
    void settersValidateAndGetterReturnsTheInstalledListener()
    {
        Channel<Integer> channel = Channel.buffered(1);
        assertSame(Channel.DEFAULT_INTERRUPTED_EXCEPTION_LISTENER,
                channel.getInterruptedExceptionListener());
        assertThrows(NullPointerException.class,
                () -> channel.setInterruptedExceptionListener(null));

        Channel.InterruptedExceptionListener custom = ex -> { };
        channel.setInterruptedExceptionListener(custom);
        assertSame(custom, channel.getInterruptedExceptionListener());
    }

    @Test
    void theListenerReceivesEveryInterruptionOfTheChannel() throws Exception
    {
        Channel<Integer> channel = Channel.buffered(1);
        List<InterruptedException> received = new CopyOnWriteArrayList<>();
        channel.setInterruptedExceptionListener(received::add);

        // interruption of a blocking get
        Thread getter = startDaemon("int-get", channel::get);
        awaitBlocked(getter);
        getter.interrupt();
        joinOrFail("interrupted get", getter);

        // interruption of a blocking put
        assertTrue(channel.tryPut(1));
        Thread putter = startDaemon("int-put", () -> channel.put(2));
        awaitBlocked(putter);
        putter.interrupt();
        joinOrFail("interrupted put", putter);

        assertEquals(2, received.size(), "both kinds of interruption reach the listener");
        assertTrue(channel.isInterrupted());
    }

    // ------------------------------------------------------------------
    // duplex
    // ------------------------------------------------------------------

    @Test
    void aDuplexChannelPropagatesTheListenerToItsDelegates() throws Exception
    {
        Channel<Integer> in = Channel.buffered(1);
        Channel<Integer> out = Channel.buffered(1);
        DuplexChannel<Integer> duplex = Channel.duplex(in, out);

        List<InterruptedException> received = new CopyOnWriteArrayList<>();
        duplex.setInterruptedExceptionListener(received::add);

        Thread waiter = startDaemon("duplex-get", duplex::get);
        awaitBlocked(waiter);
        waiter.interrupt();
        joinOrFail("duplex get", waiter);

        assertEquals(1, received.size(), "the delegate in processed the interruption");
        assertTrue(in.isInterrupted());
        assertFalse(out.isInterrupted());
        assertEquals(in.getInterruptedExceptionListener(), out.getInterruptedExceptionListener());
    }
}
