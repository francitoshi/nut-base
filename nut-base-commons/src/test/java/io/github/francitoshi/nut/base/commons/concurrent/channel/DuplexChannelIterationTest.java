/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static io.github.francitoshi.nut.base.commons.concurrent.channel.ChannelTestSupport.callWithin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A for-each over a {@link DuplexChannel} whose input is closeable used to
 * never end: the inherited iterator always answered {@code hasNext() == true}
 * and, once the input was closed, {@code get()} returned {@code null} at once,
 * so the loop spun at 100% CPU producing {@code null}s.
 */
class DuplexChannelIterationTest
{
    /** Reads at most {@code limit + 1} elements so that a regression fails fast instead of looping forever. */
    private static List<String> collect(Iterable<String> source, int limit)
    {
        List<String> out = new ArrayList<>();
        for (String s : source)
        {
            out.add(s);
            if (out.size() > limit)
            {
                break;
            }
        }
        return out;
    }

    @Test
    void iterationEndsAfterDrainingAClosedCloseableInput() throws Exception
    {
        CloseableBufferedChannel<String> in = new CloseableBufferedChannel<>(4);
        CloseableBufferedChannel<String> out = new CloseableBufferedChannel<>(4);
        DuplexChannel<String> duplex = Channel.duplex(in, out);
        in.put("a");
        in.put("b");
        in.close();

        List<String> seen = callWithin("duplex iteration", () -> collect(duplex, 10));
        assertEquals(Arrays.asList("a", "b"), seen);
    }

    @Test
    void iterationOverAClosedEmptyInputYieldsNothing() throws Exception
    {
        CloseableBufferedChannel<String> in = new CloseableBufferedChannel<>(1);
        DuplexChannel<String> duplex = Channel.duplex(in, new CloseableBufferedChannel<String>(1));
        in.close();

        List<String> seen = callWithin("duplex iteration", () -> collect(duplex, 10));
        assertTrue(seen.isEmpty(), "expected no elements but got " + seen.size() + " (nulls?)");
    }

    @Test
    void iterationEndsWhenAProducerClosesTheInputLater() throws Exception
    {
        CloseableBufferedChannel<String> in = new CloseableBufferedChannel<>(1);
        DuplexChannel<String> duplex = Channel.duplex(in, new CloseableBufferedChannel<String>(1));
        Thread producer = new Thread(() ->
        {
            in.put("x");
            in.put("y");
            in.close();
        });
        producer.setDaemon(true);
        producer.start();

        List<String> seen = callWithin("duplex iteration", () -> collect(duplex, 10));
        assertEquals(Arrays.asList("x", "y"), seen);
    }

    @Test
    void duplexOverTwoCloseableChannelsEndsWhenTheReadSideIsClosed() throws Exception
    {
        CloseableBufferedChannel<String> a = new CloseableBufferedChannel<>(2);
        CloseableBufferedChannel<String> b = new CloseableBufferedChannel<>(2);
        DuplexChannel<String> left = new DuplexChannel<>(a, b);   // same wiring as duplexPair: reads a, writes b
        a.put("one");
        a.close();

        List<String> seen = callWithin("duplex iteration", () -> collect(left, 10));
        assertEquals(Arrays.asList("one"), seen);
    }

    @Test
    void spliteratorAndStreamsAlsoTerminate() throws Exception
    {
        CloseableBufferedChannel<String> in = new CloseableBufferedChannel<>(4);
        DuplexChannel<String> duplex = Channel.duplex(in, new CloseableBufferedChannel<String>(1));
        in.put("s");
        in.close();

        List<String> seen = callWithin("duplex stream", () ->
        {
            List<String> l = new ArrayList<>();
            java.util.stream.StreamSupport.stream(duplex.spliterator(), false).limit(10).forEach(l::add);
            return l;
        });
        assertEquals(Arrays.asList("s"), seen);
    }

    @Test
    void nonCloseableInputKeepsTheOriginalNeverEndingIteration() throws Exception
    {
        BufferedChannel<String> in = new BufferedChannel<>(2);
        DuplexChannel<String> duplex = Channel.duplex(in, new BufferedChannel<String>(2));
        in.put("v");

        Iterator<String> it = duplex.iterator();
        assertTrue(it.hasNext(), "without close semantics hasNext() is always true and does not block");
        assertEquals("v", it.next());
        assertTrue(it.hasNext());
        assertEquals(null, in.get(10, TimeUnit.MILLISECONDS));
    }
}
