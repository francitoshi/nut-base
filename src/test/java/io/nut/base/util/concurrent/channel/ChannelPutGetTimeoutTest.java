/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChannelPutGetTimeoutTest
{
    // ── UnbufferedChannel ────────────────────────────────────────────

    @Nested
    public class Unbuffered
    {
        @Test
        public void testGetZeroReturnsNullWhenEmpty() throws Exception
        {
            UnbufferedChannel<Integer> ch = new UnbufferedChannel<>();
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroReturnsValueWhenProducerBlocked() throws Exception
        {
            UnbufferedChannel<Integer> ch = new UnbufferedChannel<>();
            CountDownLatch producerReady = new CountDownLatch(1);
            Thread producer = new Thread(() ->
            {
                try
                {
                    producerReady.countDown();
                    ch.put(42);
                }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            producer.start();
            assertTrue(producerReady.await(5, TimeUnit.SECONDS));
            Thread.sleep(50);
            assertEquals(42, ch.get(0, TimeUnit.MILLISECONDS));
            producer.join(5000);
        }

        @Test
        public void testGetTimeoutReturnsNullWhenExpired() throws Exception
        {
            UnbufferedChannel<Integer> ch = new UnbufferedChannel<>();
            long start = System.nanoTime();
            assertNull(ch.get(100, TimeUnit.MILLISECONDS));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsed >= 80, "should wait at least ~100ms, was " + elapsed);
        }

        @Test
        public void testGetTimeoutReturnsValueWhenProducerBlocked() throws Exception
        {
            UnbufferedChannel<Integer> ch = new UnbufferedChannel<>();
            CountDownLatch producerReady = new CountDownLatch(1);
            Thread producer = new Thread(() ->
            {
                try
                {
                    producerReady.countDown();
                    ch.put(7);
                }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            producer.start();
            assertTrue(producerReady.await(5, TimeUnit.SECONDS));
            Thread.sleep(50);
            assertEquals(7, ch.get(5, TimeUnit.SECONDS));
            producer.join(5000);
        }

        @Test
        public void testPutZeroReturnsTrueWhenConsumerWaiting() throws Exception
        {
            UnbufferedChannel<Integer> ch = new UnbufferedChannel<>();
            CountDownLatch consumerReady = new CountDownLatch(1);
            Thread consumer = new Thread(() ->
            {
                try
                {
                    consumerReady.countDown();
                    ch.get();
                }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            consumer.start();
            assertTrue(consumerReady.await(5, TimeUnit.SECONDS));
            Thread.sleep(50);
            assertTrue(ch.put(1, 0, TimeUnit.MILLISECONDS));
            consumer.join(5000);
        }

        @Test
        public void testPutZeroReturnsFalseWhenNoConsumer() throws Exception
        {
            UnbufferedChannel<Integer> ch = new UnbufferedChannel<>();
            assertFalse(ch.put(1, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroNullThrows() throws Exception
        {
            UnbufferedChannel<Integer> ch = new UnbufferedChannel<>();
            assertThrows(NullPointerException.class, () -> ch.put(null, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutReturnsTrueWhenConsumerWaiting() throws Exception
        {
            UnbufferedChannel<Integer> ch = new UnbufferedChannel<>();
            CountDownLatch consumerReady = new CountDownLatch(1);
            Thread consumer = new Thread(() ->
            {
                try
                {
                    consumerReady.countDown();
                    ch.get();
                }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            consumer.start();
            assertTrue(consumerReady.await(5, TimeUnit.SECONDS));
            Thread.sleep(50);
            assertTrue(ch.put(9, 5, TimeUnit.SECONDS));
            consumer.join(5000);
        }

        @Test
        public void testPutTimeoutReturnsFalseWhenExpired() throws Exception
        {
            UnbufferedChannel<Integer> ch = new UnbufferedChannel<>();
            long start = System.nanoTime();
            assertFalse(ch.put(1, 100, TimeUnit.MILLISECONDS));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsed >= 80, "should wait at least ~100ms, was " + elapsed);
        }
    }

    // ── BufferedChannel ──────────────────────────────────────────────

    @Nested
    public class Buffered
    {
        @Test
        public void testGetZeroReturnsNullWhenEmpty() throws Exception
        {
            BufferedChannel<Integer> ch = new BufferedChannel<>(4);
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroReturnsValueWhenAvailable() throws InterruptedException
        {
            BufferedChannel<Integer> ch = new BufferedChannel<>(4);
            ch.put(10);
            assertEquals(10, ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetTimeoutReturnsNullWhenExpired() throws Exception
        {
            BufferedChannel<Integer> ch = new BufferedChannel<>(4);
            long start = System.nanoTime();
            assertNull(ch.get(100, TimeUnit.MILLISECONDS));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsed >= 80, "should wait at least ~100ms, was " + elapsed);
        }

        @Test
        public void testGetTimeoutReturnsValueBeforeTimeout() throws Exception
        {
            BufferedChannel<Integer> ch = new BufferedChannel<>(4);
            Thread producer = new Thread(() ->
            {
                try { Thread.sleep(50); ch.put(7); } catch (Exception e) { Thread.currentThread().interrupt(); }
            });
            producer.start();
            assertEquals(7, ch.get(5, TimeUnit.SECONDS));
            producer.join(5000);
        }

        @Test
        public void testGetFifoOrder() throws InterruptedException
        {
            BufferedChannel<Integer> ch = new BufferedChannel<>(8);
            ch.put(1);
            ch.put(2);
            ch.put(3);
            assertEquals(1, ch.get(0, TimeUnit.MILLISECONDS));
            assertEquals(2, ch.get(0, TimeUnit.MILLISECONDS));
            assertEquals(3, ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroReturnsTrueWhenRoom() throws Exception
        {
            BufferedChannel<Integer> ch = new BufferedChannel<>(4);
            assertTrue(ch.put(1, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroReturnsFalseWhenFull() throws Exception
        {
            BufferedChannel<Integer> ch = new BufferedChannel<>(1);
            assertTrue(ch.put(1, 0, TimeUnit.MILLISECONDS));
            assertFalse(ch.put(2, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroNullThrows() throws Exception
        {
            BufferedChannel<Integer> ch = new BufferedChannel<>(4);
            assertThrows(NullPointerException.class, () -> ch.put(null, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutReturnsTrueWhenRoom() throws Exception
        {
            BufferedChannel<Integer> ch = new BufferedChannel<>(4);
            assertTrue(ch.put(1, 100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutReturnsFalseWhenExpired() throws Exception
        {
            BufferedChannel<Integer> ch = new BufferedChannel<>(1);
            assertTrue(ch.put(1, 0, TimeUnit.MILLISECONDS));
            long start = System.nanoTime();
            assertFalse(ch.put(2, 100, TimeUnit.MILLISECONDS));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsed >= 80, "should wait at least ~100ms, was " + elapsed);
        }

        @Test
        public void testPutTimeoutReturnsTrueAfterDrain() throws Exception
        {
            BufferedChannel<Integer> ch = new BufferedChannel<>(1);
            assertTrue(ch.put(1, 0, TimeUnit.MILLISECONDS));
            Thread consumer = new Thread(() ->
            {
                try { Thread.sleep(50); ch.get(); } catch (Exception e) { Thread.currentThread().interrupt(); }
            });
            consumer.start();
            assertTrue(ch.put(2, 5, TimeUnit.SECONDS));
            consumer.join(5000);
        }
    }

    // ── UnlimitedChannel ─────────────────────────────────────────────

    @Nested
    public class Unlimited
    {
        @Test
        public void testGetZeroReturnsNullWhenEmpty() throws Exception
        {
            UnlimitedChannel<Integer> ch = new UnlimitedChannel<>();
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroReturnsValueWhenAvailable() throws InterruptedException
        {
            UnlimitedChannel<Integer> ch = new UnlimitedChannel<>();
            ch.put(55);
            assertEquals(55, ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetTimeoutReturnsNullWhenExpired() throws Exception
        {
            UnlimitedChannel<Integer> ch = new UnlimitedChannel<>();
            long start = System.nanoTime();
            assertNull(ch.get(100, TimeUnit.MILLISECONDS));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsed >= 80, "should wait at least ~100ms, was " + elapsed);
        }

        @Test
        public void testGetTimeoutReturnsValueBeforeTimeout() throws Exception
        {
            UnlimitedChannel<Integer> ch = new UnlimitedChannel<>();
            Thread producer = new Thread(() ->
            {
                try { Thread.sleep(50); ch.put(7); } catch (Exception e) { Thread.currentThread().interrupt(); }
            });
            producer.start();
            assertEquals(7, ch.get(5, TimeUnit.SECONDS));
            producer.join(5000);
        }

        @Test
        public void testPutZeroAlwaysReturnsTrue() throws InterruptedException
        {
            UnlimitedChannel<Integer> ch = new UnlimitedChannel<>();
            for (int i = 0; i < 10_000; i++)
            {
                assertTrue(ch.put(i, 0, TimeUnit.MILLISECONDS));
            }
            for (int i = 0; i < 10_000; i++)
            {
                assertEquals(i, ch.get());
            }
        }

        @Test
        public void testPutZeroNullThrows() throws Exception
        {
            UnlimitedChannel<Integer> ch = new UnlimitedChannel<>();
            assertThrows(NullPointerException.class, () -> ch.put(null, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutAlwaysReturnsTrue() throws InterruptedException
        {
            UnlimitedChannel<Integer> ch = new UnlimitedChannel<>();
            for (int i = 0; i < 10_000; i++)
            {
                assertTrue(ch.put(i, 100, TimeUnit.MILLISECONDS));
            }
            for (int i = 0; i < 10_000; i++)
            {
                assertEquals(i, ch.get());
            }
        }
    }

    // ── DuplexChannel ────────────────────────────────────────────────

    @Nested
    public class Duplex
    {
        @Test
        public void testGetZeroReturnsNullWhenEmpty() throws Exception
        {
            Channel<Integer> inChan = Channel.unbuffered();
            Channel<Integer> outChan = Channel.unbuffered();
            DuplexChannel<Integer> duplex = Channel.duplex(inChan, outChan);
            assertNull(duplex.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroDelegatesToIn() throws Exception
        {
            Channel<Integer> inChan = Channel.buffered(4);
            Channel<Integer> outChan = Channel.unbuffered();
            DuplexChannel<Integer> duplex = Channel.duplex(inChan, outChan);

            inChan.put(42);
            assertEquals(42, duplex.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetTimeoutDelegatesToIn() throws Exception
        {
            Channel<Integer> inChan = Channel.buffered(4);
            Channel<Integer> outChan = Channel.unbuffered();
            DuplexChannel<Integer> duplex = Channel.duplex(inChan, outChan);

            inChan.put(99);
            assertEquals(99, duplex.get(1, TimeUnit.SECONDS));
        }

        @Test
        public void testGetTimeoutReturnsNullWhenExpired() throws Exception
        {
            Channel<Integer> inChan = Channel.unbuffered();
            Channel<Integer> outChan = Channel.unbuffered();
            DuplexChannel<Integer> duplex = Channel.duplex(inChan, outChan);
            assertNull(duplex.get(100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroDelegatesToOut() throws Exception
        {
            Channel<Integer> inChan = Channel.unbuffered();
            Channel<Integer> outChan = Channel.buffered(4);
            DuplexChannel<Integer> duplex = Channel.duplex(inChan, outChan);

            assertTrue(duplex.put(1, 0, TimeUnit.MILLISECONDS));
            assertEquals(1, outChan.get());
        }

        @Test
        public void testPutZeroReturnsFalseWhenNotTaken() throws Exception
        {
            Channel<Integer> inChan = Channel.unbuffered();
            Channel<Integer> outChan = Channel.unbuffered();
            DuplexChannel<Integer> duplex = Channel.duplex(inChan, outChan);
            assertFalse(duplex.put(1, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutDelegatesToOut() throws Exception
        {
            Channel<Integer> inChan = Channel.unbuffered();
            Channel<Integer> outChan = Channel.buffered(1);
            DuplexChannel<Integer> duplex = Channel.duplex(inChan, outChan);

            assertTrue(duplex.put(1, 100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutReturnsFalseWhenExpired() throws Exception
        {
            Channel<Integer> inChan = Channel.unbuffered();
            Channel<Integer> outChan = Channel.buffered(1);
            DuplexChannel<Integer> duplex = Channel.duplex(inChan, outChan);

            assertTrue(duplex.put(1, 0, TimeUnit.MILLISECONDS));
            assertFalse(duplex.put(2, 100, TimeUnit.MILLISECONDS));
        }
    }

    // ── CloseableUnbufferedChannel ───────────────────────────────────

    @Nested
    public class CloseableUnbuffered
    {
        @Test
        public void testGetZeroReturnsNullWhenEmpty() throws Exception
        {
            CloseableUnbufferedChannel<Integer> ch = new CloseableUnbufferedChannel<>();
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroReturnsValueWhenProducerBlocked() throws Exception
        {
            CloseableUnbufferedChannel<Integer> ch = new CloseableUnbufferedChannel<>();
            CountDownLatch producerReady = new CountDownLatch(1);
            Thread producer = new Thread(() ->
            {
                try
                {
                    producerReady.countDown();
                    ch.put(42);
                }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            producer.start();
            assertTrue(producerReady.await(5, TimeUnit.SECONDS));
            Thread.sleep(50);
            assertEquals(42, ch.get(0, TimeUnit.MILLISECONDS));
            producer.join(5000);
        }

        @Test
        public void testGetZeroReturnsNullAfterClose() throws Exception
        {
            CloseableUnbufferedChannel<Integer> ch = new CloseableUnbufferedChannel<>();
            assertTrue(ch.close());
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetTimeoutReturnsNullAfterClose() throws Exception
        {
            CloseableUnbufferedChannel<Integer> ch = new CloseableUnbufferedChannel<>();
            assertTrue(ch.close());
            assertNull(ch.get(100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroReturnsFalseAfterClose() throws Exception
        {
            CloseableUnbufferedChannel<Integer> ch = new CloseableUnbufferedChannel<>();
            assertTrue(ch.close());
            assertFalse(ch.put(1, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutReturnsFalseAfterClose() throws Exception
        {
            CloseableUnbufferedChannel<Integer> ch = new CloseableUnbufferedChannel<>();
            assertTrue(ch.close());
            assertFalse(ch.put(1, 100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroNullThrows() throws Exception
        {
            CloseableUnbufferedChannel<Integer> ch = new CloseableUnbufferedChannel<>();
            assertThrows(NullPointerException.class, () -> ch.put(null, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetTimeoutReturnsNullWhenExpired() throws Exception
        {
            CloseableUnbufferedChannel<Integer> ch = new CloseableUnbufferedChannel<>();
            long start = System.nanoTime();
            assertNull(ch.get(100, TimeUnit.MILLISECONDS));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsed >= 80, "should wait at least ~100ms, was " + elapsed);
        }

        @Test
        public void testPutTimeoutReturnsFalseWhenExpired() throws Exception
        {
            CloseableUnbufferedChannel<Integer> ch = new CloseableUnbufferedChannel<>();
            long start = System.nanoTime();
            assertFalse(ch.put(1, 100, TimeUnit.MILLISECONDS));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsed >= 80, "should wait at least ~100ms, was " + elapsed);
        }
    }

    // ── CloseableBufferedChannel ─────────────────────────────────────

    @Nested
    public class CloseableBuffered
    {
        @Test
        public void testGetZeroReturnsNullWhenEmpty() throws Exception
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(4);
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroReturnsValueWhenAvailable() throws InterruptedException
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(4);
            ch.put(10);
            assertEquals(10, ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroReturnsNullAfterClose() throws InterruptedException
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(4);
            ch.put(1);
            assertEquals(1, ch.get(0, TimeUnit.MILLISECONDS));
            assertTrue(ch.close());
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetTimeoutReturnsNullAfterClose() throws Exception
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(4);
            assertTrue(ch.close());
            assertNull(ch.get(100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetFifoOrder() throws InterruptedException
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(8);
            ch.put(1);
            ch.put(2);
            ch.put(3);
            assertEquals(1, ch.get(0, TimeUnit.MILLISECONDS));
            assertEquals(2, ch.get(0, TimeUnit.MILLISECONDS));
            assertEquals(3, ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroReturnsTrueWhenRoom() throws Exception
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(4);
            assertTrue(ch.put(1, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroReturnsFalseWhenFull() throws Exception
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(1);
            assertTrue(ch.put(1, 0, TimeUnit.MILLISECONDS));
            assertFalse(ch.put(2, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroReturnsFalseAfterClose() throws Exception
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(4);
            assertTrue(ch.close());
            assertFalse(ch.put(1, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutReturnsFalseAfterClose() throws Exception
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(4);
            assertTrue(ch.close());
            assertFalse(ch.put(1, 100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroNullThrows() throws Exception
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(4);
            assertThrows(NullPointerException.class, () -> ch.put(null, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutReturnsTrueWhenRoom() throws Exception
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(4);
            assertTrue(ch.put(1, 100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutReturnsFalseWhenExpired() throws Exception
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(1);
            assertTrue(ch.put(1, 0, TimeUnit.MILLISECONDS));
            long start = System.nanoTime();
            assertFalse(ch.put(2, 100, TimeUnit.MILLISECONDS));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsed >= 80, "should wait at least ~100ms, was " + elapsed);
        }

        @Test
        public void testPutTimeoutReturnsTrueAfterDrain() throws Exception
        {
            CloseableBufferedChannel<Integer> ch = new CloseableBufferedChannel<>(1);
            assertTrue(ch.put(1, 0, TimeUnit.MILLISECONDS));
            Thread consumer = new Thread(() ->
            {
                try { Thread.sleep(50); ch.get(); } catch (Exception e) { Thread.currentThread().interrupt(); }
            });
            consumer.start();
            assertTrue(ch.put(2, 5, TimeUnit.SECONDS));
            consumer.join(5000);
        }
    }

    // ── CloseableUnlimitedChannel ────────────────────────────────────

    @Nested
    public class CloseableUnlimited
    {
        @Test
        public void testGetZeroReturnsNullWhenEmpty() throws Exception
        {
            CloseableUnlimitedChannel<Integer> ch = new CloseableUnlimitedChannel<>();
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroReturnsValueWhenAvailable() throws InterruptedException
        {
            CloseableUnlimitedChannel<Integer> ch = new CloseableUnlimitedChannel<>();
            ch.put(55);
            assertEquals(55, ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroReturnsNullAfterClose() throws InterruptedException
        {
            CloseableUnlimitedChannel<Integer> ch = new CloseableUnlimitedChannel<>();
            ch.put(1);
            assertEquals(1, ch.get(0, TimeUnit.MILLISECONDS));
            assertTrue(ch.close());
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetTimeoutReturnsNullAfterClose() throws Exception
        {
            CloseableUnlimitedChannel<Integer> ch = new CloseableUnlimitedChannel<>();
            assertTrue(ch.close());
            assertNull(ch.get(100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroAlwaysReturnsTrue() throws InterruptedException
        {
            CloseableUnlimitedChannel<Integer> ch = new CloseableUnlimitedChannel<>();
            for (int i = 0; i < 10_000; i++)
            {
                assertTrue(ch.put(i, 0, TimeUnit.MILLISECONDS));
            }
            for (int i = 0; i < 10_000; i++)
            {
                assertEquals(i, ch.get());
            }
        }

        @Test
        public void testPutZeroReturnsFalseAfterClose() throws Exception
        {
            CloseableUnlimitedChannel<Integer> ch = new CloseableUnlimitedChannel<>();
            assertTrue(ch.close());
            assertFalse(ch.put(1, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutReturnsFalseAfterClose() throws Exception
        {
            CloseableUnlimitedChannel<Integer> ch = new CloseableUnlimitedChannel<>();
            assertTrue(ch.close());
            assertFalse(ch.put(1, 100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutZeroNullThrows() throws Exception
        {
            CloseableUnlimitedChannel<Integer> ch = new CloseableUnlimitedChannel<>();
            assertThrows(NullPointerException.class, () -> ch.put(null, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutAlwaysReturnsTrue() throws InterruptedException
        {
            CloseableUnlimitedChannel<Integer> ch = new CloseableUnlimitedChannel<>();
            for (int i = 0; i < 10_000; i++)
            {
                assertTrue(ch.put(i, 100, TimeUnit.MILLISECONDS));
            }
            for (int i = 0; i < 10_000; i++)
            {
                assertEquals(i, ch.get());
            }
        }
    }
}
