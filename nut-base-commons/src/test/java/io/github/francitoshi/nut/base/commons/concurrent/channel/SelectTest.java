/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import io.github.francitoshi.nut.base.commons.concurrent.channel.SelectResult.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 30, unit = TimeUnit.SECONDS)
class SelectTest
{
    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    static Stream<Arguments> kinds()
    {
        return Stream.of(
            kind("buffered", () -> Channel.<String>buffered(2)),
            kind("unlimited", () -> Channel.<String>unlimited()),
            kind("conflated", () -> Channel.<String>conflated()),
            kind("unbuffered", () -> Channel.<String>unbuffered()),
            kind("closeableBuffered", () -> Channel.<String>closeableBuffered(2)),
            kind("closeableUnbuffered", () -> Channel.<String>closeableUnbuffered()),
            kind("closeableUnlimited", () -> Channel.<String>closeableUnlimited()),
            kind("closeableConflated", () -> Channel.<String>closeableConflated()));
    }

    private static Arguments kind(String name, Supplier<Channel<String>> s)
    {
        return Arguments.of(name, s);
    }

    private static Thread start(Runnable r)
    {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
        return t;
    }

    private static void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ex)
        {
            throw new AssertionError(ex);
        }
    }

    private static void awaitBlocked(Thread t)
    {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (t.getState() != Thread.State.WAITING && t.getState() != Thread.State.TIMED_WAITING)
        {
            assertTrue(System.nanoTime() < end, "thread never blocked");
            Thread.yield();
        }
    }

    @SafeVarargs
    private static <T> Channel<T>[] arr(Channel<T>... chs)
    {
        return chs;
    }

    // ------------------------------------------------------------------
    // basic behavior
    // ------------------------------------------------------------------

    @Test
    void returnsValueFromReadyChannel()
    {
        Channel<String> a = Channel.buffered(2);
        Channel<String> b = Channel.buffered(2);
        b.put("x");
        assertEquals("x", Select.select(a, b));
    }

    @Test
    void indexedReportsTheChannelPosition()
    {
        Channel<String> a = Channel.buffered(2);
        Channel<String> b = Channel.buffered(2);
        Channel<String> c = Channel.buffered(2);
        c.put("z");
        SelectResult<String> r = Select.selectIndexed(a, b, c);
        assertEquals(Status.VALUE, r.status());
        assertEquals(2, r.index());
        assertEquals("z", r.value());
        assertTrue(r.hasValue());
    }

    @Test
    void consumesExactlyOneValue()
    {
        Channel<String> a = Channel.buffered(4);
        a.put("1");
        a.put("2");
        assertEquals("1", Select.select(a));
        assertEquals("2", Select.select(a));
        assertNull(Select.trySelect(a));
    }

    @Test
    void blocksUntilAValueArrives()
    {
        Channel<String> a = Channel.buffered(2);
        Channel<String> b = Channel.buffered(2);
        start(() ->
        {
            sleep(100);
            b.put("late");
        });
        long t0 = System.nanoTime();
        SelectResult<String> r = Select.selectIndexed(a, b);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        assertEquals("late", r.value());
        assertEquals(1, r.index());
        assertTrue(elapsedMs >= 80, "returned too early: " + elapsedMs);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kinds")
    void worksWithEveryChannelKind(String name, Supplier<Channel<String>> factory)
    {
        Channel<String> other = Channel.buffered(1);
        Channel<String> ch = factory.get();
        start(() ->
        {
            sleep(50);
            ch.put("hello-" + name);
        });
        SelectResult<String> r = Select.selectIndexed(other, ch);
        assertEquals("hello-" + name, r.value());
        assertEquals(1, r.index());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kinds")
    void valueAlreadyInQueueIsFoundImmediately(String name, Supplier<Channel<String>> factory)
    {
        Channel<String> ch = factory.get();
        if (name.contains("nbuffered"))
        {
            start(() -> ch.put("v"));
        }
        else
        {
            ch.put("v");
        }
        assertEquals("v", Select.select(5, TimeUnit.SECONDS, ch));
    }

    @Test
    void worksOnDuplexChannel()
    {
        Channel<String> in = Channel.buffered(2);
        Channel<String> out = Channel.buffered(2);
        DuplexChannel<String> duplex = Channel.duplex(in, out);
        start(() ->
        {
            sleep(50);
            in.put("via-duplex");
        });
        assertEquals("via-duplex", Select.select(duplex));
        assertEquals(0, in.registeredSignals());
    }

    @Test
    void mixesDifferentChannelImplementations()
    {
        Channel<String> a = Channel.unbuffered();
        Channel<String> b = Channel.conflated();
        Channel<String> c = Channel.closeableUnlimited();
        start(() ->
        {
            sleep(50);
            c.put("from-c");
        });
        assertEquals("from-c", Select.select(a, b, c));
    }

    // ------------------------------------------------------------------
    // trySelect (default:)
    // ------------------------------------------------------------------

    @Test
    void trySelectDoesNotBlockWhenEmpty()
    {
        Channel<String> a = Channel.buffered(1);
        Channel<String> b = Channel.unbuffered();
        long t0 = System.nanoTime();
        SelectResult<String> r = Select.trySelectIndexed(a, b);
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0) < 500);
        assertEquals(Status.NOT_READY, r.status());
        assertEquals(-1, r.index());
        assertNull(r.value());
        assertNull(Select.trySelect(a, b));
    }

    @Test
    void trySelectReturnsReadyValue()
    {
        Channel<String> a = Channel.buffered(1);
        a.put("now");
        assertEquals("now", Select.trySelect(a));
    }

    @Test
    void trySelectOnClosedAndEmptyReportsClosed()
    {
        CloseableChannel<String> a = Channel.closeableBuffered(1);
        a.close();
        assertEquals(Status.CLOSED, Select.trySelectIndexed(a).status());
    }

    // ------------------------------------------------------------------
    // timeouts
    // ------------------------------------------------------------------

    @Test
    void timesOutWhenNothingArrives()
    {
        Channel<String> a = Channel.buffered(1);
        Channel<String> b = Channel.conflated();
        long t0 = System.nanoTime();
        SelectResult<String> r = Select.selectIndexed(150, TimeUnit.MILLISECONDS, a, b);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        assertEquals(Status.TIMEOUT, r.status());
        assertNull(r.value());
        assertTrue(elapsedMs >= 140, "too early: " + elapsedMs);
        assertTrue(elapsedMs < 2000, "too late: " + elapsedMs);
    }

    @Test
    void timeoutOnRendezvousChannelStillTimesOut()
    {
        Channel<String> a = Channel.unbuffered();
        assertEquals(Status.TIMEOUT, Select.selectIndexed(100, TimeUnit.MILLISECONDS, a).status());
    }

    @Test
    void zeroTimeoutScansOnceAndReturns()
    {
        Channel<String> a = Channel.buffered(1);
        assertEquals(Status.TIMEOUT, Select.selectIndexed(0, TimeUnit.SECONDS, a).status());
        a.put("v");
        assertEquals("v", Select.select(0, TimeUnit.SECONDS, a));
    }

    @Test
    void valueArrivingBeforeTimeoutIsReturned()
    {
        Channel<String> a = Channel.buffered(1);
        start(() ->
        {
            sleep(50);
            a.put("in-time");
        });
        assertEquals("in-time", Select.select(5, TimeUnit.SECONDS, a));
    }

    @Test
    void hugeTimeoutDoesNotOverflow()
    {
        Channel<String> a = Channel.buffered(1);
        start(() ->
        {
            sleep(50);
            a.put("ok");
        });
        assertEquals("ok", Select.select(Long.MAX_VALUE, TimeUnit.DAYS, a));
    }

    // ------------------------------------------------------------------
    // closed channels
    // ------------------------------------------------------------------

    @Test
    void allClosedReturnsClosed()
    {
        CloseableChannel<String> a = Channel.closeableBuffered(1);
        CloseableChannel<String> b = Channel.closeableUnlimited();
        CloseableChannel<String> c = Channel.closeableConflated();
        a.close();
        b.close();
        c.close();
        SelectResult<String> r = Select.selectIndexed(a, b, c);
        assertEquals(Status.CLOSED, r.status());
        assertNull(Select.select(a, b, c));
    }

    @Test
    void closedChannelIsDrainedBeforeBeingReportedClosed()
    {
        CloseableChannel<String> a = Channel.closeableBuffered(4);
        a.put("1");
        a.put("2");
        a.close();
        assertEquals("1", Select.select(a));
        assertEquals("2", Select.select(a));
        assertEquals(Status.CLOSED, Select.selectIndexed(a).status());
    }

    @Test
    void closedEmptyChannelDoesNotShadowOpenOnes()
    {
        CloseableChannel<String> closed = Channel.closeableBuffered(1);
        Channel<String> open = Channel.buffered(1);
        closed.close();
        start(() ->
        {
            sleep(100);
            open.put("still-here");
        });
        SelectResult<String> r = Select.selectIndexed(closed, open);
        assertEquals("still-here", r.value());
        assertEquals(1, r.index());
    }

    @Test
    void blockedSelectWakesUpWhenTheOnlyChannelCloses()
    {
        CloseableChannel<String> a = Channel.closeableBuffered(1);
        AtomicReference<SelectResult<String>> res = new AtomicReference<>();
        Thread t = start(() -> res.set(Select.selectIndexed(a)));
        awaitBlocked(t);
        a.close();
        join(t);
        assertEquals(Status.CLOSED, res.get().status());
    }

    @Test
    void blockedSelectWakesUpWhenAllChannelsClose()
    {
        CloseableChannel<String> a = Channel.closeableUnlimited();
        CloseableChannel<String> b = Channel.closeableConflated();
        AtomicReference<SelectResult<String>> res = new AtomicReference<>();
        Thread t = start(() -> res.set(Select.selectIndexed(a, b)));
        awaitBlocked(t);
        a.close();
        sleep(50);
        assertNull(res.get(), "one open channel left: select must keep waiting");
        b.close();
        join(t);
        assertEquals(Status.CLOSED, res.get().status());
    }

    @Test
    void blockedSelectOnRendezvousChannelWakesUpOnClose()
    {
        CloseableChannel<String> a = Channel.closeableUnbuffered();
        AtomicReference<SelectResult<String>> res = new AtomicReference<>();
        Thread t = start(() -> res.set(Select.selectIndexed(a)));
        awaitBlocked(t);
        a.close();
        join(t);
        assertEquals(Status.CLOSED, res.get().status());
    }

    private static void join(Thread t)
    {
        try
        {
            t.join(10_000);
        }
        catch (InterruptedException ex)
        {
            throw new AssertionError(ex);
        }
        assertFalse(t.isAlive(), "thread still running");
    }

    // ------------------------------------------------------------------
    // interruption
    // ------------------------------------------------------------------

    @Test
    void interruptedSelectReturnsInterruptedAndDoesNotCloseChannels()
    {
        CloseableChannel<String> a = Channel.closeableBuffered(1);
        Channel<String> b = Channel.buffered(1);
        AtomicReference<SelectResult<String>> res = new AtomicReference<>();
        AtomicReference<Boolean> flagAfter = new AtomicReference<>();
        Thread t = start(() ->
        {
            res.set(Select.selectIndexed(a, b));
            flagAfter.set(Thread.currentThread().isInterrupted());
        });
        awaitBlocked(t);
        t.interrupt();
        join(t);
        assertEquals(Status.INTERRUPTED, res.get().status());
        assertFalse(flagAfter.get(), "interruption must be consumed, as in channels");
        assertFalse(a.isClosed());
        assertFalse(a.isInterrupted());
        assertFalse(b.isInterrupted());
    }

    @Test
    void alreadyInterruptedThreadReturnsInterruptedImmediately()
    {
        Channel<String> a = Channel.buffered(1);
        a.put("v");
        Thread.currentThread().interrupt();
        try
        {
            assertEquals(Status.INTERRUPTED, Select.selectIndexed(a).status());
        }
        finally
        {
            Thread.interrupted();
        }
        assertEquals("v", Select.select(a), "value must not have been consumed");
    }

    // ------------------------------------------------------------------
    // registration lifecycle
    // ------------------------------------------------------------------

    @Test
    void signalsAreRegisteredWhileWaitingAndRemovedAfterwards()
    {
        Channel<String> a = Channel.buffered(1);
        CloseableChannel<String> b = Channel.closeableUnlimited();
        Thread t = start(() -> Select.select(a, b));
        awaitBlocked(t);
        assertEquals(1, a.registeredSignals());
        assertEquals(1, b.registeredSignals());
        a.put("go");
        join(t);
        assertEquals(0, a.registeredSignals());
        assertEquals(0, b.registeredSignals());
    }

    @Test
    void signalsAreRemovedAfterTimeoutAndTrySelect()
    {
        Channel<String> a = Channel.buffered(1);
        Select.select(20, TimeUnit.MILLISECONDS, a);
        Select.trySelect(a);
        assertEquals(0, a.registeredSignals());
    }

    // ------------------------------------------------------------------
    // fairness
    // ------------------------------------------------------------------

    @Test
    void doesNotStarveAnyChannel()
    {
        Channel<Integer> a = Channel.buffered(1);
        Channel<Integer> b = Channel.buffered(1);
        Channel<Integer> c = Channel.buffered(1);
        int[] hits = new int[3];
        final int rounds = 3000;
        for (int i = 0; i < rounds; i++)
        {
            a.put(0);
            b.put(1);
            c.put(2);
            SelectResult<Integer> r = Select.selectIndexed(a, b, c);
            hits[r.index()]++;
            // drain the losers so every round starts with all three ready
            Select.trySelect(a);
            Select.trySelect(b);
            Select.trySelect(c);
            Select.trySelect(a);
            Select.trySelect(b);
            Select.trySelect(c);
        }
        for (int h : hits)
        {
            assertTrue(h > rounds / 3 / 2, "unbalanced selection: " + java.util.Arrays.toString(hits));
        }
    }

    // ------------------------------------------------------------------
    // argument validation
    // ------------------------------------------------------------------

    @Test
    void rejectsInvalidArguments()
    {
        assertThrows(IllegalArgumentException.class, () -> Select.select(new Channel[0]));
        assertThrows(NullPointerException.class, () -> Select.select((Channel<String>[]) null));
        Channel<String> a = Channel.buffered(1);
        assertThrows(NullPointerException.class, () -> Select.select(a, null));
        assertThrows(NullPointerException.class, () -> Select.select(1, null, a));
    }

    // ------------------------------------------------------------------
    // concurrency: nothing lost, nothing duplicated, no lost wake-ups
    // ------------------------------------------------------------------

    static Stream<Arguments> stressKinds()
    {
        return Stream.of(
            kind("buffered", () -> Channel.<String>buffered(3)),
            kind("unlimited", () -> Channel.<String>unlimited()),
            kind("unbuffered", () -> Channel.<String>unbuffered()),
            kind("closeableBuffered", () -> Channel.<String>closeableBuffered(3)),
            kind("closeableUnbuffered", () -> Channel.<String>closeableUnbuffered()),
            kind("closeableUnlimited", () -> Channel.<String>closeableUnlimited()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stressKinds")
    void manyProducersManyConsumersLoseNothing(String name, Supplier<Channel<String>> factory) throws Exception
    {
        final int channelsCount = 4;
        final int perChannel = 400;
        final int consumers = 3;
        final int total = channelsCount * perChannel;

        List<Channel<String>> list = new ArrayList<>();
        for (int i = 0; i < channelsCount; i++)
        {
            list.add(factory.get());
        }
        @SuppressWarnings("unchecked")
        Channel<String>[] chs = list.toArray(new Channel[0]);

        Set<String> seen = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicates = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();

        List<Thread> threads = new ArrayList<>();
        for (int c = 0; c < channelsCount; c++)
        {
            final int ci = c;
            threads.add(start(() ->
            {
                for (int j = 0; j < perChannel; j++)
                {
                    chs[ci].put(ci + ":" + j);
                    if (j % 50 == 0)
                    {
                        sleep(1);
                    }
                }
            }));
        }
        for (int k = 0; k < consumers; k++)
        {
            threads.add(start(() ->
            {
                while (consumed.get() < total)
                {
                    String v = Select.select(50, TimeUnit.MILLISECONDS, chs);
                    if (v != null)
                    {
                        consumed.incrementAndGet();
                        if (!seen.add(v))
                        {
                            duplicates.incrementAndGet();
                        }
                    }
                }
            }));
        }
        for (Thread t : threads)
        {
            t.join(20_000);
            assertFalse(t.isAlive(), "stuck thread (lost wake-up?)");
        }
        assertEquals(0, duplicates.get());
        assertEquals(total, seen.size());
        for (Channel<String> ch : chs)
        {
            assertEquals(0, ch.registeredSignals());
        }
    }

    @Test
    void pingPongWithoutTimeoutNeverMissesAWakeUp() throws Exception
    {
        // Untimed select: a single lost wake-up would hang the test (-> @Timeout).
        Channel<Integer> a = Channel.unlimited();
        Channel<Integer> b = Channel.buffered(1);
        final int n = 5000;
        Thread producer = start(() ->
        {
            for (int i = 0; i < n; i++)
            {
                (i % 2 == 0 ? a : b).put(i);
                if (i % 7 == 0)
                {
                    Thread.yield();
                }
            }
        });
        long sum = 0;
        for (int i = 0; i < n; i++)
        {
            sum += Select.select(a, b);
        }
        producer.join(10_000);
        assertEquals((long) n * (n - 1) / 2, sum);
    }
}
