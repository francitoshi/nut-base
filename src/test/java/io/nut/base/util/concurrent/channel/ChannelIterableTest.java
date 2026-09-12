/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import io.nut.base.util.As;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
            new CloseableUnbufferedChannel<Integer>(),
            new CloseableBufferedChannel<Integer>(4),
            new CloseableUnlimitedChannel<Integer>(),
            new CloseableConflatedChannel<Integer>()
        };

        for (Object channel : channels)
        {
            assertTrue(channel instanceof Iterable, channel.getClass().getName() + " must be Iterable");
        }
    }

    @Test
    public void testDuplexChannel_iterator_readsFromInputReader() throws Exception
    {
        Channel<Integer> x = Channel.unbuffered();
        Channel<Integer> y = Channel.unbuffered();
        DuplexChannel<Integer> duplex = Channel.duplex(y, x);
        DuplexChannel<Integer> peer = Channel.duplex(x, y);
        Thread producer = new Thread(() ->
        {
            for (int i = 0; i < 3; i++)
            {
                peer.put(i);
            }
        });
        producer.start();

        Iterator<Integer> it = duplex.iterator();
        for (int i = 0; i < 3; i++)
        {
            assertTrue(it.hasNext());
            assertEquals(i, it.next());
        }
        producer.join(5000);
        assertFalse(producer.isAlive());
    }

    @Test
    public void testNonCloseableIterator_hasNextAlwaysTrue()
    {
        Channel<Integer>[] channels = new Channel[]
        {
            Channel.unbuffered(),
            Channel.buffered(4),
            Channel.unlimited(),
            Channel.conflated()
        };

        for (Channel<Integer> channel : channels)
        {
            Iterator<Integer> it = channel.iterator();
            assertTrue(it.hasNext());
            assertTrue(it.hasNext());
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
    public void testNonCloseableIterator_interruptedNext_returnsNull() throws Exception
    {
        Channel<Integer> channel = Channel.unbuffered();
        Iterator<Integer> it = channel.iterator();
        assertTrue(it.hasNext());

        AtomicReference<Object> result = new AtomicReference<>(MISSING);
        Thread consumer = new Thread(() -> result.set(it.next()));
        consumer.start();
        Thread.sleep(200);
        consumer.interrupt();
        consumer.join(5000);

        assertTrue(channel.isInterrupted());
        assertNull(result.get(), "next() hooked to get() must return null after interrupt");
        assertFalse(consumer.isAlive());
    }

    // -------------------------------------------------------------------
    // Closeable channels
    // -------------------------------------------------------------------

    @Test
    public void testCloseableBufferedIterator_terminatesWhenClosed()
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(2);
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
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(1);
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
        CloseableChannel<Integer> channel = Channel.closeableUnbuffered();
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
    public void testCloseableIterator_interruptedGet_terminatesAndCloses() throws Exception
    {
        CloseableChannel<Integer> channel = Channel.closeableUnbuffered();
        Iterator<Integer> it = channel.iterator();
        AtomicInteger count = new AtomicInteger();

        Thread consumer = new Thread(() ->
        {
            while (it.hasNext())
            {
                count.incrementAndGet();
            }
        });
        consumer.start();
        Thread.sleep(200);
        consumer.interrupt();
        consumer.join(5000);

        assertFalse(consumer.isAlive());
        assertEquals(0, count.get(), "iteration must abort on the first interrupted get()");
        // The interrupted get() aborted returning null and, being closeable,
        // requested close() on the channel.
        assertTrue(channel.isInterrupted());
        assertTrue(channel.isClosed());
    }

@Test
    public void testCloseableIterator_forEach_drainsAll()
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(2);
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
    public void testStreamSupport_onCloseableChannel_terminatesWhenClosed()
    {
        CloseableBufferedChannel<Integer> channel = new CloseableBufferedChannel<>(2);
        channel.put(1);
        channel.put(2);
        channel.close();

        List<Integer> received = StreamSupport.stream(channel.spliterator(), false).collect(Collectors.toList());
        assertEquals(As.list(1, 2), received);
    }

    @Test
    public void testStreamSupport_onNonCloseableChannel_canBeLimited()
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