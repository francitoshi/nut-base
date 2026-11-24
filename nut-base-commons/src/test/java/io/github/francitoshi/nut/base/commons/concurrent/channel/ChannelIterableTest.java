/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import io.github.francitoshi.nut.base.commons.lang.As;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChannelIterableTest
{
    private static final Object MISSING = new Object();

    // -------------------------------------------------------------------
    // Non-closeable channels
    // -------------------------------------------------------------------

    @Test
    public void testAllChannelTypes_implementIterable()
    {
        Object[] channels =
        {
            new UnbufferedChannel<Integer>(),
            new BufferedChannel<Integer>(4),
            new UnlimitedChannel<Integer>(),
            new ConflatedChannel<Integer>(),
            new UnbufferedChannel<Integer>(),
            new BufferedChannel<Integer>(4),
            new UnlimitedChannel<Integer>(),
            new ConflatedChannel<Integer>()
        };

        for (Object channel : channels)
        {
            assertTrue(channel instanceof Iterable, channel.getClass().getName() + " must be Iterable");
        }
    }

    @Test
    public void testIteratorCall_createsIndependentIterator() throws Exception
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(2);
        channel.put(1);
        channel.put(2);

        Iterator<Integer> it1 = channel.iterator();
        Iterator<Integer> it2 = channel.iterator();
        assertNotSame(it1, it2, "each iterator() call must create a new iterator");

        // Each iterator consumes from the shared channel but has its own state.
        assertTrue(it1.hasNext());
        assertEquals(1, it1.next());
        assertTrue(it2.hasNext());
        assertEquals(2, it2.next());
    }

    @Test
    public void testUnbufferedIterator_nextReadingFromGet_inFifoOrder() throws Exception
    {
        Channel<Integer> channel = Channel.unbuffered();
        final int n = 100;
        Thread producer = new Thread(() ->
        {
            for (int i = 0; i < n; i++)
            {
                channel.put(i);
            }
        });
        producer.start();

        Iterator<Integer> it = channel.iterator();
        for (int i = 0; i < n; i++)
        {
            assertTrue(it.hasNext());
            assertEquals(i, it.next());
        }
        producer.join(5000);
        assertFalse(producer.isAlive());
    }

    @Test
    public void testBufferedIterator_nextReadingFromGet_inFifoOrder() throws Exception
    {
        Channel<Integer> channel = Channel.buffered(1);
        final int n = 100;
        Thread producer = new Thread(() ->
        {
            for (int i = 0; i < n; i++)
            {
                channel.put(i);
            }
        });
        producer.start();

        Iterator<Integer> it = channel.iterator();
        for (int i = 0; i < n; i++)
        {
            assertTrue(it.hasNext());
            assertEquals(i, it.next());
        }
        producer.join(5000);
        assertFalse(producer.isAlive());
    }

    @Test
    public void testUnlimitedIterator_nextReadingFromGet_inFifoOrder()
    {
        Channel<Integer> channel = Channel.unlimited();
        final int n = 100;
        for (int i = 0; i < n; i++)
        {
            channel.put(i);
        }

        Iterator<Integer> it = channel.iterator();
        for (int i = 0; i < n; i++)
        {
            assertTrue(it.hasNext());
            assertEquals(i, it.next());
        }
    }

    @Test
    public void testIterator_interruptedHasNext_returnsTrueAndNextYieldsNull() throws Exception
    {
        Channel<Integer> channel = ChannelTestSupport.quiet(Channel.unbuffered());
        Iterator<Integer> it = channel.iterator();

        AtomicReference<Object> result = new AtomicReference<>(MISSING);
        Thread consumer = new Thread(() ->
        {
            // hasNext() blocks in get(): the interruption aborts the read,
            // the iterator keeps going (the channel stays open), the null
            // turn is cached and next() returns it
            if (it.hasNext())
            {
                result.set(it.next());
            }
        });
        consumer.start();
        Thread.sleep(200);
        consumer.interrupt();
        consumer.join(5000);

        assertTrue(channel.isInterrupted());
        assertFalse(channel.isClosed(), "an interrupt must not close the channel");
        assertNull(result.get(), "the interrupted turn must yield a null value");
        assertFalse(consumer.isAlive());
    }

    // -------------------------------------------------------------------
    // Terminating iteration
    // -------------------------------------------------------------------

    @Test
    public void testCloseableBufferedIterator_terminatesWhenClosed()
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(2);
        channel.put(10);
        channel.put(20);
        assertTrue(channel.close());

        Iterator<Integer> it = channel.iterator();
        List<Integer> received = new ArrayList<>();
        while (it.hasNext())
        {
            received.add(it.next());
        }
        assertEquals(As.list(10, 20), received);
    }

    @Test
    public void testCloseableIterator_nextReturnsTheValueSavedByHasNext() throws Exception
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(1);
        channel.put(1);

        Iterator<Integer> it = channel.iterator();
        assertTrue(it.hasNext());
        // hasNext captured 1 and emptied the buffer; the producer now fills
        // and closes the channel before next() is called.
        Thread producer = new Thread(() ->
        {
            channel.put(2);
            channel.close();
        });
        producer.start();
        producer.join(5000);

        // next() must return the value saved by hasNext() (1), not re-read a
        // new value from the channel (which would yield 2).
        assertEquals(1, it.next(), "next() must return the value saved by hasNext()");
        assertTrue(it.hasNext());
        assertEquals(2, it.next());
        // The channel is closed and drained: get() yields null.
        assertTrue(channel.isClosed());
        assertFalse(it.hasNext());
    }

    @Test
    public void testCloseableUnbufferedIterator_terminatesAfterLastValue() throws Exception
    {
        Channel<Integer> channel = Channel.unbuffered();
        CountDownLatch consumed = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread producer = new Thread(() ->
        {
            try
            {
                channel.put(7);
                consumed.await(5, TimeUnit.SECONDS);
                channel.close();
            }
            catch (Throwable t)
            {
                error.set(t);
            }
        });
        producer.start();

        Iterator<Integer> it = channel.iterator();
        assertTrue(it.hasNext());
        assertEquals(7, it.next());

        consumed.countDown();
        producer.join(5000);
        assertNull(error.get());
        assertTrue(channel.isClosed());
        assertFalse(it.hasNext(), "iterator must terminate when close() yields null");
    }

    @Test
    public void testCloseableIterator_interruptedGet_yieldsOneTurnAndKeepsGoingUntilClosed() throws Exception
    {
        Channel<Integer> channel = ChannelTestSupport.quiet(Channel.unbuffered());
        Iterator<Integer> it = channel.iterator();
        AtomicInteger turns = new AtomicInteger();
        AtomicInteger values = new AtomicInteger();

        Thread consumer = new Thread(() ->
        {
            while (it.hasNext())
            {
                // the iterator contract: hasNext() followed by next() — a
                // bare hasNext() loop would spin on the cached null turn
                Integer value = it.next();
                turns.incrementAndGet();
                if (value != null)
                {
                    values.incrementAndGet();
                }
            }
        });
        consumer.start();
        Thread.sleep(200);
        consumer.interrupt();

        // the interrupted get yields a null turn and the channel stays open
        Thread.sleep(200);
        assertFalse(channel.isClosed(), "a reader interrupt must not close the channel");
        assertTrue(channel.isInterrupted());
        channel.close();
        consumer.join(5000);

        assertFalse(consumer.isAlive(), "the iteration must end once the channel is closed");
        assertEquals(1, turns.get(), "one null turn for the interrupted get");
        assertEquals(0, values.get(), "no real value was read");
    }

@Test
    public void testCloseableIterator_forEach_drainsAll()
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(2);
        channel.put(1);
        channel.put(2);
        channel.close();

        List<Integer> received = new ArrayList<>();
        for (Integer value : channel)
        {
            received.add(value);
        }
        assertEquals(As.list(1, 2), received);
    }

    @Test
    public void testStreamSupport_onChannel_terminatesWhenClosed()
    {
        BufferedChannel<Integer> channel = new BufferedChannel<>(2);
        channel.put(1);
        channel.put(2);
        channel.close();

        List<Integer> received = StreamSupport.stream(channel.spliterator(), false).collect(Collectors.toList());
        assertEquals(As.list(1, 2), received);
    }

    @Test
    public void testStreamSupport_onNonChannel_canBeLimited()
    {
        Channel<Integer> channel = Channel.unlimited();
        for (int i = 0; i < 3; i++)
        {
            channel.put(i);
        }

        List<Integer> received = StreamSupport.stream(channel.spliterator(), false).limit(3).collect(Collectors.toList());
        assertEquals(As.list(0, 1, 2), received);
    }
}