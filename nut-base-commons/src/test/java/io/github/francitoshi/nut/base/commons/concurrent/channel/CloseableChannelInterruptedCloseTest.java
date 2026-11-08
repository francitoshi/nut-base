/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static io.github.francitoshi.nut.base.commons.concurrent.channel.ChannelTestSupport.awaitBlocked;
import static io.github.francitoshi.nut.base.commons.concurrent.channel.ChannelTestSupport.joinOrFail;
import static io.github.francitoshi.nut.base.commons.concurrent.channel.ChannelTestSupport.startDaemon;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Closing a channel from a thread whose interrupt status is set (typical of
 * {@code ExecutorService.shutdownNow()} or {@code Future.cancel(true)}) used
 * to leave the channel flagged as closed with its blocked readers parked
 * forever: the {@code POISON} that wakes them was sent with an interruptible
 * call that threw immediately.
 */
class CloseableChannelInterruptedCloseTest
{
    private static final int READERS = 3;

    private static void assertReadersAreWokenByInterruptedCloser(CloseableChannel<String> ch) throws Exception
    {
        AtomicReference<String>[] values = newReferences(READERS);
        Thread[] readers = new Thread[READERS];
        for (int i = 0; i < READERS; i++)
        {
            final int n = i;
            values[n].set("<not returned>");
            readers[n] = startDaemon("reader-" + n, () -> values[n].set(ch.get()));
        }
        for (Thread reader : readers)
        {
            awaitBlocked(reader);
        }

        AtomicBoolean flagKept = new AtomicBoolean();
        Thread closer = startDaemon("closer", () ->
        {
            Thread.currentThread().interrupt();      // interrupt status already set
            ch.close();
            flagKept.set(Thread.currentThread().isInterrupted());
        });
        joinOrFail("close()", closer);

        assertTrue(ch.isClosed(), "channel must be closed");
        for (int i = 0; i < READERS; i++)
        {
            joinOrFail("reader-" + i, readers[i]);
            assertNull(values[i].get(), "reader-" + i + " must be woken with null");
        }
        assertTrue(flagKept.get(), "the closer's interrupt status must be preserved");
    }

    @SuppressWarnings("unchecked")
    private static AtomicReference<String>[] newReferences(int n)
    {
        AtomicReference<String>[] refs = new AtomicReference[n];
        for (int i = 0; i < n; i++)
        {
            refs[i] = new AtomicReference<>();
        }
        return refs;
    }

    @Test
    void bufferedChannelReadersAreWokenWhenClosedByInterruptedThread() throws Exception
    {
        assertReadersAreWokenByInterruptedCloser(new CloseableBufferedChannel<>(4));
    }

    @Test
    void unbufferedChannelReadersAreWokenWhenClosedByInterruptedThread() throws Exception
    {
        assertReadersAreWokenByInterruptedCloser(new CloseableUnbufferedChannel<>());
    }

    @Test
    void unlimitedChannelReadersAreWokenWhenClosedByInterruptedThread() throws Exception
    {
        assertReadersAreWokenByInterruptedCloser(new CloseableUnlimitedChannel<>());
    }

    @Test
    void timedCloseFromInterruptedThreadAlsoWakesReaders() throws Exception
    {
        for (CloseableChannel<String> ch : Arrays.<CloseableChannel<String>>asList(new CloseableBufferedChannel<String>(4), new CloseableUnlimitedChannel<String>()))
        {
            AtomicReference<String> value = new AtomicReference<>("<not returned>");
            Thread reader = startDaemon("reader", () -> value.set(ch.get()));
            awaitBlocked(reader);

            Thread closer = startDaemon("closer", () ->
            {
                Thread.currentThread().interrupt();
                try
                {
                    if (ch instanceof CloseableBufferedChannel)
                    {
                        ((CloseableBufferedChannel<String>) ch).close(1, TimeUnit.SECONDS);
                    }
                    else
                    {
                        ((CloseableUnlimitedChannel<String>) ch).close(1, TimeUnit.SECONDS);
                    }
                }
                catch (InterruptedException expectedOrNot)
                {
                    // acceptable: the interrupt may surface here, what matters is the readers
                }
            });
            joinOrFail("close(timeout)", closer);
            joinOrFail("reader of " + ch.getClass().getSimpleName(), reader);
            assertNull(value.get());
            assertTrue(ch.isClosed());
        }
    }

    @Test
    void closeStillDrainsBufferedValuesBeforeSignallingTheEnd() throws Exception
    {
        CloseableBufferedChannel<String> ch = new CloseableBufferedChannel<>(4);
        ch.put("a");
        ch.put("b");
        Thread closer = startDaemon("closer", () ->
        {
            Thread.currentThread().interrupt();
            ch.close();
        });
        joinOrFail("close()", closer);
        assertEquals("a", ch.get(1, TimeUnit.SECONDS));
        assertEquals("b", ch.get(1, TimeUnit.SECONDS));
        assertNull(ch.get(1, TimeUnit.SECONDS));
    }
}
