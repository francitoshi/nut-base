/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.francitoshi.nut.base.commons.concurrent.channel.ChannelTestSupport.*;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
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
            kind("closeableBuffered", () -> new CloseableBufferedChannel<String>(2)),
            kind("closeableUnbuffered", CloseableUnbufferedChannel::new),
            kind("closeableUnlimited", CloseableUnlimitedChannel::new),
            kind("closeableConflated", CloseableConflatedChannel::new));
    }

    private static <T> Arguments kind(String name, Supplier<Channel<T>> s)
    {
        return Arguments.of(name, s);
    }

    // ------------------------------------------------------------------
    // selectGet
    // ------------------------------------------------------------------

    @Test
    void selectGetRejectsAnEmptyReaderList()
    {
        assertThrows(IllegalArgumentException.class, () -> Channel.selectGet());
        assertThrows(IllegalArgumentException.class,
                () -> Channel.selectGet(0L, TimeUnit.NANOSECONDS));
    }

    @Test
    void selectGetRejectsNullReaders()
    {
        assertThrows(NullPointerException.class,
                () -> Channel.selectGet((ChannelReader<Integer>) null));
    }

    @Test
    void selectGetReturnsNullWhenNothingIsReadyAndTimeoutIsZero() throws Exception
    {
        Channel<Integer> a = Channel.buffered(1);
        Channel<Integer> b = Channel.buffered(1);
        assertNull(Channel.selectGet(0L, TimeUnit.NANOSECONDS, a, b));
    }

    @Test
    void selectGetReturnsTheValueOfTheFirstReadyReader() throws Exception
    {
        Channel<Integer> a = Channel.buffered(1);
        Channel<Integer> b = Channel.buffered(1);
        b.tryPut(7);
        assertEquals(7, Channel.selectGet(a, b).intValue());
        assertFalse(b.tryGet().isPresent(), "the select must consume the value");
    }

    @Test
    void selectGetPrefersTheFirstReaderWhenSeveralAreReady() throws Exception
    {
        Channel<Integer> a = Channel.buffered(1);
        Channel<Integer> b = Channel.buffered(1);
        a.tryPut(1);
        b.tryPut(2);

        assertEquals(1, Channel.selectGet(a, b).intValue());
        assertEquals(2, Channel.selectGet(a, b).intValue());
    }

    @Test
    void selectGetConsumesExactlyOneValue() throws Exception
    {
        Channel<Integer> channel = Channel.buffered(4);
        channel.tryPut(1);
        channel.tryPut(2);

        assertEquals(1, Channel.selectGet(channel).intValue());
        assertEquals(2, Channel.selectGet(channel).intValue());
        assertNull(Channel.selectGet(0L, TimeUnit.NANOSECONDS, channel));
    }

    @Test
    void selectGetBlocksUntilAValueIsPut() throws Exception
    {
        Channel<Integer> channel = Channel.buffered(1);
        Thread producer = startDaemon("select-producer", () -> channel.put(5));

        assertEquals(5, callWithin("selectGet", () -> Channel.selectGet(channel)).intValue());
        joinOrFail("select producer", producer);
    }

    @Test
    void selectGetTimesOutWhenNoValueArrives() throws Exception
    {
        Channel<Integer> channel = Channel.buffered(1);
        long start = System.nanoTime();
        assertNull(Channel.selectGet(50L, TimeUnit.MILLISECONDS, channel));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsedMs >= 25L, "should wait approximately the timeout, was " + elapsedMs + " ms");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kinds")
    void selectGetWorksOnEveryChannelKind(String name, Supplier<Channel<String>> factory) throws Exception
    {
        Channel<String> other = Channel.buffered(1);
        Channel<String> ch = factory.get();
        Thread producer = startDaemon("producer-" + name, () ->
        {
            try
            {
                Thread.sleep(50);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
            ch.put("hello-" + name);
        });

        assertEquals("hello-" + name, callWithin("selectGet-" + name,
                () -> Channel.selectGet(5, TimeUnit.SECONDS, other, ch)));
        joinOrFail("producer " + name, producer);
    }

    @Test
    void selectGetTakesAValueFromAReadyRendezvousProducer() throws Exception
    {
        Channel<Integer> channel = Channel.unbuffered();
        AtomicReference<Integer> delivered = new AtomicReference<>();
        Thread producer = startDaemon("rendezvous-producer",
                () -> delivered.set(channel.put(11, 2, TimeUnit.SECONDS) ? 11 : null));
        awaitBlocked(producer);

        assertEquals(11, callWithin("selectGet-rendezvous",
                () -> Channel.selectGet(channel)).intValue());
        joinOrFail("rendezvous producer", producer);
        assertEquals(11, delivered.get().intValue());
    }

    @Test
    void selectGetWorksOnDuplexChannel() throws Exception
    {
        Channel<String> in = Channel.buffered(2);
        Channel<String> out = Channel.buffered(2);
        DuplexChannel<String> duplex = Channel.duplex(in, out);
        Thread producer = startDaemon("duplex-producer", () ->
        {
            try
            {
                Thread.sleep(50);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
            in.put("via-duplex");
        });

        assertEquals("via-duplex", callWithin("selectGet-duplex",
                () -> Channel.selectGet(duplex)));
        joinOrFail("duplex producer", producer);
    }

    @Test
    void selectGetCanBeInterrupted() throws Exception
    {
        Channel<Integer> channel = Channel.buffered(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<Boolean> flagAfter = new AtomicReference<>();
        Thread selector = startDaemon("interrupted-select", () ->
        {
            try
            {
                Channel.selectGet(channel);
            }
            catch (Throwable ex)
            {
                error.set(ex);
            }
            flagAfter.set(Thread.currentThread().isInterrupted());
        });

        awaitBlocked(selector);
        selector.interrupt();
        joinOrFail("interrupted select", selector);

        assertInstanceOf(InterruptedException.class, error.get());
        assertFalse(flagAfter.get(), "the interruption must not be re-marked on the thread");
        assertFalse(channel.isInterrupted(), "the select is not a channel operation");
    }

    // ------------------------------------------------------------------
    // selectPut
    // ------------------------------------------------------------------

    @Test
    void selectPutRejectsAnEmptyWriterList()
    {
        assertThrows(IllegalArgumentException.class, () -> Channel.selectPut(1));
        assertThrows(IllegalArgumentException.class,
                () -> Channel.selectPut(0L, TimeUnit.NANOSECONDS, 1));
    }

    @Test
    void selectPutRejectsNullWriters()
    {
        assertThrows(NullPointerException.class,
                () -> Channel.selectPut(1, (ChannelWriter<Integer>) null));
    }

    @Test
    void selectPutReturnsNullWhenEveryWriterIsFullAndTimeoutIsZero() throws Exception
    {
        Channel<Integer> a = Channel.buffered(1);
        Channel<Integer> b = Channel.buffered(1);
        a.tryPut(1);
        b.tryPut(2);

        assertNull(Channel.selectPut(0L, TimeUnit.NANOSECONDS, 3, a, b));
    }

    @Test
    void selectPutUsesTheFirstWriterThatAcceptsTheValue() throws Exception
    {
        Channel<Integer> a = Channel.buffered(1);
        Channel<Integer> b = Channel.buffered(1);

        assertSame(a, Channel.selectPut(1, a, b));
        assertEquals(1, a.tryGet().get().intValue());
        assertFalse(b.tryGet().isPresent());
    }

    @Test
    void selectPutSkipsFullWritersAndUsesALaterOne() throws Exception
    {
        Channel<Integer> full = Channel.buffered(1);
        Channel<Integer> empty = Channel.buffered(1);
        full.tryPut(1);

        assertSame(empty, Channel.selectPut(2, full, empty));
        assertEquals(2, empty.tryGet().get().intValue());
    }

    @Test
    void selectPutTimesOutWhenNoWriterAcceptsTheValue() throws Exception
    {
        Channel<Integer> full = Channel.buffered(1);
        full.tryPut(1);

        long start = System.nanoTime();
        assertNull(Channel.selectPut(50L, TimeUnit.MILLISECONDS, 2, full));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsedMs >= 25L, "should wait approximately the timeout, was " + elapsedMs + " ms");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("putKinds")
    void selectPutSkipsFullWritersOfEveryKind(String name, Supplier<Channel<Integer>> factory) throws Exception
    {
        Channel<Integer> full = factory.get();
        full.tryPut(1);
        Channel<Integer> empty = Channel.buffered(1);

        assertSame(empty, Channel.selectPut(2, full, empty));
    }

    static Stream<Arguments> putKinds()
    {
        return Stream.of(
            kind("buffered", () -> Channel.<Integer>buffered(1)),
            kind("closeableBuffered", () -> new CloseableBufferedChannel<Integer>(1)));
    }

    @Test
    void selectPutBlocksUntilABufferedWriterHasSpace() throws Exception
    {
        Channel<Integer> channel = Channel.buffered(1);
        channel.tryPut(1);

        AtomicReference<ChannelWriter<Integer>> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread selector = startDaemon("select-put", () ->
        {
            try
            {
                result.set(Channel.selectPut(2, channel));
            }
            catch (Throwable ex)
            {
                error.set(ex);
            }
        });

        awaitBlocked(selector);
        assertEquals(1, channel.get().intValue());
        joinOrFail("select put", selector);

        assertNull(error.get());
        assertSame(channel, result.get());
        assertEquals(2, channel.get().intValue());
    }

    @Test
    void selectPutOnUnbufferedChannelTimesOutWithNoConsumer() throws Exception
    {
        Channel<Integer> channel = Channel.unbuffered();
        long start = System.nanoTime();
        assertNull(Channel.selectPut(100L, TimeUnit.MILLISECONDS, 2, channel));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsedMs >= 80L, "should wait approximately the timeout, was " + elapsedMs + " ms");
    }

    @Test
    void selectPutCompletesWithAWaitingRendezvousConsumer() throws Exception
    {
        Channel<Integer> channel = Channel.unbuffered();
        AtomicReference<Integer> received = new AtomicReference<>();
        Thread consumer = startDaemon("rendezvous-consumer",
                () -> received.set(channel.get()));
        awaitBlocked(consumer);

        assertSame(channel, Channel.selectPut(9, channel));
        joinOrFail("rendezvous consumer", consumer);
        assertEquals(9, received.get().intValue());
    }

    @Test
    void selectPutBroadcastsThroughFanoutChannel() throws Exception
    {
        Channel<Integer> first = Channel.buffered(1);
        Channel<Integer> second = Channel.buffered(1);
        FanoutChannel<Integer> fan = Channel.fanout(first, second);

        assertSame(fan, Channel.selectPut(5, fan));
        assertEquals(5, first.tryGet().get().intValue());
        assertEquals(5, second.tryGet().get().intValue());
    }

    @Test
    void selectPutCanBeInterrupted() throws Exception
    {
        Channel<Integer> full = Channel.buffered(1);
        full.tryPut(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread selector = startDaemon("interrupted-select-put", () ->
        {
            try
            {
                Channel.selectPut(2, full);
            }
            catch (Throwable ex)
            {
                error.set(ex);
            }
        });

        awaitBlocked(selector);
        selector.interrupt();
        joinOrFail("interrupted select put", selector);

        assertInstanceOf(InterruptedException.class, error.get());
    }

    // ------------------------------------------------------------------
    // Closed channels
    // ------------------------------------------------------------------

    @Test
    void selectGetReturnsNullWhenEveryReaderIsClosed() throws Exception
    {
        CloseableChannel<Integer> a = Channel.closeableBuffered(1);
        CloseableChannel<Integer> b = Channel.closeableUnlimited();
        a.close();
        b.close();

        assertNull(Channel.selectGet(a, b));
        assertNull(Channel.selectGet(0L, TimeUnit.NANOSECONDS, a, b));
    }

    @Test
    void selectGetDrainsClosedBufferedChannelsBeforeReturningNull() throws Exception
    {
        CloseableChannel<Integer> channel = Channel.closeableBuffered(2);
        channel.tryPut(1);
        channel.tryPut(2);
        channel.close();

        assertEquals(1, Channel.selectGet(channel).intValue());
        assertEquals(2, Channel.selectGet(channel).intValue());
        assertNull(Channel.selectGet(channel));
    }

    @Test
    void selectGetSkipsClosedReadersAndWaitsForAnOpenOne() throws Exception
    {
        CloseableChannel<Integer> closed = Channel.closeableBuffered(1);
        closed.close();
        Channel<Integer> open = Channel.buffered(1);
        open.tryPut(3);

        assertEquals(3, Channel.selectGet(closed, open).intValue());
    }

    @Test
    void selectGetReturnsNullWhenTheObservedChannelIsClosedWhileWaiting() throws Exception
    {
        CloseableChannel<Integer> channel = Channel.closeableBuffered(1);
        AtomicReference<Object> result = new AtomicReference<>("missing");
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread selector = startDaemon("select-get-close", () ->
        {
            try
            {
                result.set(Channel.selectGet(channel));
            }
            catch (Throwable ex)
            {
                error.set(ex);
            }
        });

        awaitBlocked(selector);
        channel.close();
        joinOrFail("select get close", selector);

        assertNull(error.get());
        assertNull(result.get());
    }

    @Test
    void selectGetUnblocksWhenAllObservedChannelsAreClosedByAnotherThread() throws Exception
    {
        CloseableChannel<Integer> first = Channel.closeableBuffered(1);
        CloseableChannel<Integer> second = Channel.closeableUnlimited();
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>("missing");
        Thread selector = startDaemon("select-all-closed", () ->
        {
            ready.countDown();
            try
            {
                result.set(Channel.selectGet(first, second));
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(ready.await(2, TimeUnit.SECONDS));
        awaitBlocked(selector);
        first.close();
        second.close();
        joinOrFail("select all closed", selector);

        assertNull(result.get());
    }

    @Test
    void selectPutReturnsNullWhenEveryWriterIsClosed() throws Exception
    {
        CloseableChannel<Integer> a = Channel.closeableBuffered(1);
        CloseableChannel<Integer> b = Channel.closeableUnbuffered();
        a.close();
        b.close();

        assertNull(Channel.selectPut(1, a, b));
        assertNull(Channel.selectPut(0L, TimeUnit.NANOSECONDS, 1, a, b));
    }

    @Test
    void selectPutSkipsClosedWritersAndUsesAnOpenOne() throws Exception
    {
        CloseableChannel<Integer> closed = Channel.closeableBuffered(1);
        closed.close();
        Channel<Integer> open = Channel.buffered(1);

        assertSame(open, Channel.selectPut(4, closed, open));
        assertEquals(4, open.tryGet().get().intValue());
    }

    @Test
    void selectPutReturnsNullWhenTheObservedChannelIsClosedWhileWaiting() throws Exception
    {
        CloseableChannel<Integer> channel = Channel.closeableBuffered(1);
        channel.tryPut(1);
        AtomicReference<ChannelWriter<Integer>> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread selector = startDaemon("select-put-close", () ->
        {
            try
            {
                result.set(Channel.selectPut(2, channel));
            }
            catch (Throwable ex)
            {
                error.set(ex);
            }
        });

        awaitBlocked(selector);
        channel.close();
        joinOrFail("select put close", selector);

        assertNull(error.get());
        assertNull(result.get());
    }

    // ------------------------------------------------------------------
    // operation listeners (the select primitive)
    // ------------------------------------------------------------------

    @Test
    void operationListenersReportGetPutAndClose() throws Exception
    {
        class Record
        {
            final StringBuilder log = new StringBuilder();
        }
        CloseableChannel<Integer> channel = Channel.closeableBuffered(1);
        StringBuilder log = new StringBuilder();
        ChannelObservable.OperationListener<Integer> listener =
                (ch, op, value) -> log.append(op).append(':').append(value).append(' ');
        channel.addOperationListener(listener);

        channel.tryPut(9);
        channel.tryGet();
        channel.close();

        assertEquals("PUT:9 GET:9 CLOSE:null ", log.toString());
        channel.removeOperationListener(listener);
        log.setLength(0);
        channel.put(1, 1, TimeUnit.MILLISECONDS);
        assertEquals("", log.toString());
    }
}
