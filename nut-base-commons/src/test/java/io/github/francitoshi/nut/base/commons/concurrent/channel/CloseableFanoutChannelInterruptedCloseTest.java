/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static io.github.francitoshi.nut.base.commons.concurrent.channel.ChannelTestSupport.awaitBlocked;
import static io.github.francitoshi.nut.base.commons.concurrent.channel.ChannelTestSupport.joinOrFail;
import static io.github.francitoshi.nut.base.commons.concurrent.channel.ChannelTestSupport.startDaemon;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code CloseableFanoutChannel.close()} used to give up when its caller was
 * interrupted while waiting for the writers: the fanout stayed flagged as
 * closed, the targets were never closed, later {@code close()} calls returned
 * the cached {@code false} immediately, and the consumers of the targets
 * blocked forever. It also swallowed the interrupt status.
 */
class CloseableFanoutChannelInterruptedCloseTest
{
    @Test
    void interruptedCloseStillClosesTheTargetsAndKeepsTheInterruptStatus() throws Exception
    {
        CloseableBufferedChannel<String> t1 = new CloseableBufferedChannel<>(1);
        CloseableBufferedChannel<String> t2 = new CloseableBufferedChannel<>(1);
        CloseableFanoutChannel<String> fan = new CloseableFanoutChannel<>(t1, t2);

        t1.put("fill");                                   // t1 full: the next put blocks on it
        Thread writer = startDaemon("writer", () ->
        {
            try
            {
                fan.put("y");
            }
            catch (IllegalStateException ignored)
            {
                // the fanout may already be closed when the writer wakes up
            }
        });
        awaitBlocked(writer);                             // the writer is registered inside the fanout

        AtomicReference<String> consumed = new AtomicReference<>("<not returned>");
        Thread consumer = startDaemon("consumer-of-t2", () -> consumed.set(t2.get()));
        awaitBlocked(consumer);

        AtomicBoolean flagKept = new AtomicBoolean();
        Thread closer = startDaemon("closer", () ->
        {
            Thread.currentThread().interrupt();           // interrupted before/while waiting for the writer
            fan.close();
            flagKept.set(Thread.currentThread().isInterrupted());
        });
        joinOrFail("fan.close()", closer);

        assertTrue(fan.isClosed());
        assertTrue(t2.isClosed(), "target must be closed even if the closer was interrupted");
        joinOrFail("consumer of t2", consumer);
        assertNull(consumed.get(), "consumer of the target must be released");
        joinOrFail("writer", writer);
        assertTrue(flagKept.get(), "the closer's interrupt status must be preserved");
    }

    @Test
    void closeWithoutInterruptionClosesAllTargets()
    {
        CloseableBufferedChannel<String> t1 = new CloseableBufferedChannel<>(1);
        CloseableBufferedChannel<String> t2 = new CloseableBufferedChannel<>(1);
        CloseableFanoutChannel<String> fan = new CloseableFanoutChannel<>(t1, t2);

        assertTrue(fan.close());
        assertTrue(fan.isClosed());
        assertTrue(t1.isClosed());
        assertTrue(t2.isClosed());
        assertTrue(fan.close(), "idempotent: second close returns the same result");
    }

    @Test
    void closeOfAlreadyClosedFanoutDoesNotSwallowTheInterruptStatus() throws Exception
    {
        CloseableFanoutChannel<String> fan = new CloseableFanoutChannel<>(new CloseableBufferedChannel<String>(1));
        assertTrue(fan.close());

        AtomicBoolean flagKept = new AtomicBoolean();
        Thread closer = startDaemon("closer", () ->
        {
            Thread.currentThread().interrupt();
            fan.close();
            flagKept.set(Thread.currentThread().isInterrupted());
        });
        joinOrFail("fan.close()", closer);
        assertTrue(flagKept.get());
        assertTrue(fan.isClosed());
    }
}
