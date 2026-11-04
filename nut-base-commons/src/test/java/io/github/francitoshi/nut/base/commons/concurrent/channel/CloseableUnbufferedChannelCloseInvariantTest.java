/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Invariants of closing a {@link CloseableUnbufferedChannel} under stress:
 * <ol>
 *   <li>{@code close()} never hangs while plain {@code put()} writers are
 *       blocked, no matter how the registration interleaves with the close
 *       snapshot;</li>
 *   <li>a blocked writer never delivers its value to a reader once the close
 *       has completed (i.e. nothing is pulled out of the rendezvous after the
 *       close returned);</li>
 *   <li>readers parked in {@code get()} are woken with {@code null}.</li>
 * </ol>
 * Each iteration parks real threads and closes the channel while they are
 * blocked, so a registration/close race has thousands of chances to surface.
 */
class CloseableUnbufferedChannelCloseInvariantTest
{
    private static final int ITERATIONS = 600;

    @Test
    void blockedWritersAlwaysAbortAndNeverDeliverAfterClose() throws Exception
    {
        for (int i = 0; i < ITERATIONS; i++)
        {
            CloseableUnbufferedChannel<String> ch = ChannelTestSupport.quiet(new CloseableUnbufferedChannel<>());
            List<String> postCloseDeliveries = new ArrayList<>();

            // two writers: a rendezvous can only complete between writer and
            // reader, so both stay genuinely blocked for the whole iteration.
            // The timed variant reports delivery with {@code true} and abort
            // (interrupt / close) with {@code false}, which is the distinction
            // the assertion below needs
            final int stripe = i;
            AtomicBoolean w1Delivered = new AtomicBoolean(false);
            AtomicBoolean w2Delivered = new AtomicBoolean(false);
            Thread w1 = ChannelTestSupport.startDaemon("put-w1-" + stripe, () ->
            {
                if (ch.put("w" + stripe + "-a", 5_000, TimeUnit.MILLISECONDS))
                {
                    w1Delivered.set(true);
                }
            });
            Thread w2 = ChannelTestSupport.startDaemon("put-w2-" + stripe, () ->
            {
                if (ch.put("w" + stripe + "-b", 5_000, TimeUnit.MILLISECONDS))
                {
                    w2Delivered.set(true);
                }
            });

            ChannelTestSupport.awaitBlocked(w1);
            ChannelTestSupport.awaitBlocked(w2);

            Thread closer = ChannelTestSupport.startDaemon("close-" + stripe, ch::close);
            Thread closer2 = ChannelTestSupport.startDaemon("close2-" + stripe, () ->
            {
                try
                {
                    assertTrue(ch.close(2, TimeUnit.SECONDS), "timed close must succeed");
                }
                catch (InterruptedException ex)
                {
                    fail("timed close interrupted: " + ex);
                }
            });

            ChannelTestSupport.joinOrFail("close", closer);
            ChannelTestSupport.joinOrFail("close2", closer2);

            // once close() completed, in-flight puts must have aborted without
            // delivering; a value pulled out after this point is a violation
            String v;
            while ((v = ch.get()) != null)
            {
                postCloseDeliveries.add(v);
            }
            assertTrue(postCloseDeliveries.isEmpty(),
                    "iteration " + stripe + ": delivered after close(): " + postCloseDeliveries);

            ChannelTestSupport.joinOrFail("put-w1", w1);
            ChannelTestSupport.joinOrFail("put-w2", w2);
            assertFalse(w1Delivered.get(), "iteration " + stripe + ": aborted put w1 must not deliver");
            assertFalse(w2Delivered.get(), "iteration " + stripe + ": aborted put w2 must not deliver");
        }
    }

    @Test
    void parkedReadersWakeWithNullAndTimedReadersDoNotBlock() throws Exception
    {
        for (int i = 0; i < ITERATIONS; i++)
        {
            CloseableUnbufferedChannel<String> ch = ChannelTestSupport.quiet(new CloseableUnbufferedChannel<>());
            final int stripe = i;

            // two mutually unaware readers: neither can rendezvous with the
            // other, so both park until close() pokes them
            Thread r1 = ChannelTestSupport.startDaemon("get-r1-" + stripe, () ->
            {
                assertNull(ch.get(), "reader must wake with null once closed and drained");
            });
            Thread r2 = ChannelTestSupport.startDaemon("get-r2-" + stripe, () ->
            {
                assertNull(ch.get(), "reader must wake with null once closed and drained");
            });

            ChannelTestSupport.awaitBlocked(r1);
            ChannelTestSupport.awaitBlocked(r2);

            Thread closer = ChannelTestSupport.startDaemon("close-" + stripe, ch::close);
            ChannelTestSupport.joinOrFail("close", closer);
            ChannelTestSupport.joinOrFail("get-r1", r1);
            ChannelTestSupport.joinOrFail("get-r2", r2);
        }
    }

    @Test
    void closeWhileTimedPutInFlightReturnsBoundedAndAborts() throws Exception
    {
        for (int i = 0; i < ITERATIONS; i++)
        {
            CloseableUnbufferedChannel<String> ch = ChannelTestSupport.quiet(new CloseableUnbufferedChannel<>());
            final int stripe = i;

            Thread putter = ChannelTestSupport.startDaemon("put-t-" + stripe, () ->
            {
                // keeps retrying with a fresh deadline: with/without the close
                // the last offers race the close snapshot and the POISON poke
                long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ChannelTestSupport.WAIT_MS);
                boolean last = true;
                while (System.nanoTime() < end)
                {
                    last = ch.put("v" + stripe, 1, TimeUnit.MILLISECONDS);
                    if (ch.isClosed())
                    {
                        break;
                    }
                }
                assertFalse(last, "timed put after close must not deliver");
            });

            ChannelTestSupport.awaitBlocked(putter);
            Thread closer = ChannelTestSupport.startDaemon("close-" + stripe, () ->
            {
                try
                {
                    assertTrue(ch.close(ChannelTestSupport.WAIT_MS, TimeUnit.MILLISECONDS),
                            "timed close of the in-flight put must succeed");
                }
                catch (InterruptedException ex)
                {
                    fail("timed close interrupted: " + ex);
                }
            });
            ChannelTestSupport.joinOrFail("close", closer);
            ChannelTestSupport.joinOrFail("put-t", putter);

            assertNull(ch.get(), "nothing may be deliverable once the close completed");
        }
    }
}
