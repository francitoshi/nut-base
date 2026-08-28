/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ChannelConflatedTest
{
    // ── ConflatedChannel ─────────────────────────────────────────────

    @Nested
    public class Conflated
    {
        @Test
        public void testPutGet() throws Exception
        {
            ConflatedChannel<String> ch = new ConflatedChannel<>();
            ch.put("a");
            assertEquals("a", ch.get());
        }

        @Test
        public void testPutOverwritesUnreadValue() throws Exception
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            ch.put(1);
            ch.put(2);
            ch.put(3);
            assertEquals(3, ch.get());
        }

        @Test
        public void testGetBlocksUntilValue() throws Exception
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            AtomicReference<Integer> result = new AtomicReference<>();
            CountDownLatch started = new CountDownLatch(1);

            Thread consumer = new Thread(() ->
            {
                started.countDown();
                result.set(ch.get());
            });
            consumer.start();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            Thread.sleep(100);
            assertNull(result.get());

            ch.put(42);
            consumer.join(5000);
            assertEquals(Integer.valueOf(42), result.get());
        }

        @Test
        public void testGetZeroReturnsNullWhenEmpty() throws InterruptedException
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroReturnsValueWhenAvailable() throws InterruptedException
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            ch.put(10);
            assertEquals(10, ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroConsumesValue() throws InterruptedException
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            ch.put(10);
            assertEquals(10, ch.get(0, TimeUnit.MILLISECONDS));
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetTimeoutReturnsNullWhenExpired() throws Exception
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            long start = System.nanoTime();
            assertNull(ch.get(100, TimeUnit.MILLISECONDS));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsed >= 80, "should wait at least ~100ms, was " + elapsed);
        }

        @Test
        public void testGetTimeoutReturnsValueBeforeTimeout() throws Exception
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            Thread producer = new Thread(() ->
            {
                try { Thread.sleep(50); ch.put(7); } catch (Exception e) { Thread.currentThread().interrupt(); }
            });
            producer.start();
            assertEquals(7, ch.get(5, TimeUnit.SECONDS));
            producer.join(5000);
        }

        @Test
        public void testPutNeverBlocks() throws Exception
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            long start = System.nanoTime();
            for (int i = 0; i < 100_000; i++)
            {
                ch.put(i);
            }
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsed < 1000, "100k puts should be near-instant, was " + elapsed + "ms");
            assertEquals(99_999, ch.get());
        }

        @Test
        public void testPutNullThrows()
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            assertThrows(NullPointerException.class, () -> ch.put(null));
        }

        @Test
        public void testPutZeroNeverBlocks() throws InterruptedException
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            assertTrue(ch.put(1, 0, TimeUnit.MILLISECONDS));
            assertTrue(ch.put(2, 0, TimeUnit.MILLISECONDS));
            assertTrue(ch.put(3, 0, TimeUnit.MILLISECONDS));
            assertEquals(3, ch.get());
        }

        @Test
        public void testPutGetCycle() throws InterruptedException
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            for (int i = 0; i < 1000; i++)
            {
                ch.put(i);
                assertEquals(i, ch.get());
            }
        }

        @Test
        public void testConcurrentPutOverwrite() throws Exception
        {
            ConflatedChannel<Integer> ch = new ConflatedChannel<>();
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);
            AtomicReference<Throwable> error = new AtomicReference<>();

            for (int p = 0; p < 2; p++)
            {
                final int base = p * 100_000;
                Thread producer = new Thread(() ->
                {
                    try
                    {
                        start.await();
                        for (int i = 0; i < 100_000; i++)
                        {
                            ch.put(base + i);
                        }
                    }
                    catch (Throwable t) { error.compareAndSet(null, t); }
                    finally { done.countDown(); }
                });
                producer.start();
            }

            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));
            assertNull(error.get());

            Integer last = ch.get();
            assertNotNull(last);
            assertTrue(last >= 0 && last < 200_000, "unexpected value: " + last);
        }

        @Test
        public void testFactoryMethod() throws InterruptedException
        {
            Channel<String> ch = Channel.conflated();
            ch.put("hello");
            assertEquals("hello", ch.get());
        }
    }

    // ── CloseableConflatedChannel ────────────────────────────────────

    @Nested
    public class CloseableConflated
    {
        @Test
        public void testPutGet() throws Exception
        {
            CloseableConflatedChannel<String> ch = new CloseableConflatedChannel<>();
            ch.put("a");
            assertEquals("a", ch.get());
        }

        @Test
        public void testPutOverwritesUnreadValue() throws Exception
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            ch.put(1);
            ch.put(2);
            ch.put(3);
            assertEquals(3, ch.get());
        }

        @Test
        public void testGetBlocksUntilValue() throws Exception
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            AtomicReference<Integer> result = new AtomicReference<>();
            CountDownLatch started = new CountDownLatch(1);

            Thread consumer = new Thread(() ->
            {
                started.countDown();
                result.set(ch.get());
            });
            consumer.start();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            Thread.sleep(100);
            assertNull(result.get());

            ch.put(42);
            consumer.join(5000);
            assertEquals(Integer.valueOf(42), result.get());
        }

        @Test
        public void testGetZeroReturnsNullWhenEmpty() throws InterruptedException
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetZeroReturnsValueWhenAvailable() throws InterruptedException
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            ch.put(10);
            assertEquals(10, ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetTimeoutReturnsNullWhenExpired() throws Exception
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            long start = System.nanoTime();
            assertNull(ch.get(100, TimeUnit.MILLISECONDS));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(elapsed >= 80, "should wait at least ~100ms, was " + elapsed);
        }

        @Test
        public void testGetTimeoutReturnsValueBeforeTimeout() throws Exception
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            Thread producer = new Thread(() ->
            {
                try { Thread.sleep(50); ch.put(7); } catch (Exception e) { Thread.currentThread().interrupt(); }
            });
            producer.start();
            assertEquals(7, ch.get(5, TimeUnit.SECONDS));
            producer.join(5000);
        }

        @Test
        public void testPutNullThrows()
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertThrows(NullPointerException.class, () -> ch.put(null));
        }

        @Test
        public void testPutZeroNeverBlocks() throws InterruptedException
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertTrue(ch.put(1, 0, TimeUnit.MILLISECONDS));
            assertTrue(ch.put(2, 0, TimeUnit.MILLISECONDS));
            assertTrue(ch.put(3, 0, TimeUnit.MILLISECONDS));
            assertEquals(3, ch.get());
        }

        // ── Close semantics ──────────────────────────────────────────

        @Test
        public void testInitialStateNotClosed()
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertFalse(ch.isClosed());
        }

        @Test
        public void testClose()
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertTrue(ch.close());
            assertTrue(ch.isClosed());
        }

        @Test
        public void testDoubleCloseReturnsTrue()
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertTrue(ch.close());
            assertTrue(ch.close());
        }

        @Test
        public void testPutAfterCloseThrows()
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertTrue(ch.close());
            assertThrows(IllegalStateException.class, () -> ch.put(1));
        }

        @Test
        public void testPutZeroAfterCloseReturnsFalse() throws InterruptedException
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertTrue(ch.close());
            assertFalse(ch.put(1, 0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testPutTimeoutAfterCloseReturnsFalse() throws Exception
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertTrue(ch.close());
            assertFalse(ch.put(1, 100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetAfterCloseReturnsRemainingValue() throws InterruptedException
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            ch.put(42);
            assertTrue(ch.close());
            assertEquals(42, ch.get());
        }

        @Test
        public void testGetAfterCloseReturnsNullWhenEmpty() throws InterruptedException
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertTrue(ch.close());
            assertNull(ch.get());
        }

        @Test
        public void testGetZeroAfterCloseReturnsNullWhenEmpty() throws InterruptedException
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertTrue(ch.close());
            assertNull(ch.get(0, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testGetTimeoutAfterCloseReturnsNull() throws Exception
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            assertTrue(ch.close());
            assertNull(ch.get(100, TimeUnit.MILLISECONDS));
        }

        @Test
        public void testCloseUnblocksBlockedGet() throws Exception
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            CountDownLatch started = new CountDownLatch(1);
            AtomicReference<Integer> result = new AtomicReference<>();

            Thread consumer = new Thread(() ->
            {
                started.countDown();
                result.set(ch.get());
            });
            consumer.start();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            Thread.sleep(100);
            assertNull(result.get());

            assertTrue(ch.close());
            consumer.join(5000);
            assertNull(result.get());
        }

        @Test
        public void testCloseUnblocksBlockedGetTimeout() throws Exception
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            CountDownLatch started = new CountDownLatch(1);
            AtomicReference<Integer> result = new AtomicReference<>();

            Thread consumer = new Thread(() ->
            {
                started.countDown();
                result.set(ch.get(5, TimeUnit.SECONDS));
            });
            consumer.start();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            Thread.sleep(100);
            assertNull(result.get());

            assertTrue(ch.close());
            consumer.join(5000);
            assertNull(result.get());
        }

        @Test
        public void testJoinBlocksUntilClose() throws Exception
        {
            CloseableConflatedChannel<Integer> ch = new CloseableConflatedChannel<>();
            CountDownLatch started = new CountDownLatch(1);
            AtomicBoolean joined = new AtomicBoolean(false);

            Thread waiter = new Thread(() ->
            {
                try
                {
                    started.countDown();
                    ch.join();
                    joined.set(true);
                }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            waiter.start();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            Thread.sleep(100);
            assertFalse(joined.get());

            assertTrue(ch.close());
            waiter.join(5000);
            assertTrue(joined.get());
        }

        @Test
        public void testFactoryMethod() throws InterruptedException
        {
            CloseableChannel<String> ch = Channel.closeableConflated();
            ch.put("hello");
            assertEquals("hello", ch.get());
            assertTrue(ch.close());
        }
    }
}
