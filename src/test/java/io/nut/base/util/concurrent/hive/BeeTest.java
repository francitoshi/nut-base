/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Timeout;

/**
 * Unit tests for {@link Bee}: direct (hive-less) message delivery,
 * hive-backed asynchronous delivery, the shutdown/termination lifecycle,
 * and the exception hook.
 */
class BeeTest
{
    private Hive hive;

    @BeforeEach
    void setUp()
    {
        hive = Hive.hive(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException
    {
        hive.shutdown();
        hive.awaitTermination(2000);
    }

    @Test
    void directSendInvokesReceiveSynchronously()
    {
        RecordingBee<String> bee = new RecordingBee<>();

        bee.accept("hello");

        assertEquals(Collections.singletonList("hello"), bee.received);
    }

    @Test
    void getExceptionIsNullInitially()
    {
        RecordingBee<String> bee = new RecordingBee<>();
        assertNull(bee.getException());
    }

    @Test
    void sendAfterShutdownReturnsFalseAndIsNotReceived()
    {
        RecordingBee<String> bee = new RecordingBee<>();
        bee.shutdown();
        bee.dryLogger();
        assertTrue(bee.isShutdown());
        bee.accept("too late");
        assertTrue(bee.received.isEmpty());
    }

    @Test
    void shutdownWithoutHiveTerminatesImmediatelyWhenEmpty()
    {
        RecordingBee<String> bee = new RecordingBee<>();

        assertFalse(bee.isTerminated());
        bee.shutdown();

        assertTrue(bee.isShutdown());
        assertTrue(bee.isTerminated());
        assertTrue(bee.terminated.get());
    }

    @Test
    void awaitTerminationTimesOutWhenNeverShutdown()
    {
        RecordingBee<String> bee = new RecordingBee<>();
        assertFalse(bee.awaitTermination(50));
    }

    @Test
    void awaitTerminationReturnsTrueAfterShutdown()
    {
        RecordingBee<String> bee = new RecordingBee<>();
        bee.shutdown();
        assertTrue(bee.awaitTermination(50));
    }

    @Test
    void exceptionHookAndGetExceptionCaptureFailureInDirectMode()
    {
        RuntimeException boom = new RuntimeException("boom");
        RecordingBee<String> bee = new RecordingBee<>();
        bee.dryLogger(); // avoid noisy SEVERE log output for this expected failure
        bee.withAction(m ->
        {
            throw boom;
        });

        bee.accept("x");
        assertSame(boom, bee.getException());
        assertSame(boom, bee.lastException.get());
    }

    @Test
    void hiveBackedSendIsProcessedAsynchronouslyAndDeterministicallyDrained()
    {
        RecordingBee<Integer> bee = new RecordingBee<>(hive);
        for (int i = 0; i < 20; i++)
        {
            bee.accept(i);
        }

        bee.shutdown().awaitTermination(1);

        assertTrue(bee.isTerminated());
        assertTrue(bee.terminated.get());
        assertEquals(20, bee.received.size());
        for (int i = 0; i < 20; i++)
        {
            assertTrue(bee.received.contains(i));
        }
    }

    @Test
    void shutdownOnlyWhenEmptyWaitsForPendingMessagesBeforeTerminating()
    {
        RecordingBee<Integer> bee = new RecordingBee<>(hive, 1, 2);
        for (int i = 0; i < 50; i++)
        {
            bee.accept(i);
        }

        bee.shutdown(true);

        assertTrue(bee.awaitTermination(2000));
        assertEquals(50, bee.received.size());
    }

    @Test
    void setHiveAttachesHiveAfterConstructionForLaterSends()
    {
        RecordingBee<String> bee = new RecordingBee<>(); // no hive yet
        bee.accept("direct"); // processed synchronously, no hive involved

        bee.setHive(hive);
        bee.accept("via-hive");

        bee.shutdown().awaitTermination(1);

        assertEquals(2, bee.received.size());
        assertTrue(bee.received.contains("direct"));
        assertTrue(bee.received.contains("via-hive"));
    }

    @Test
    void zeroThreadsWithHiveProcessesSynchronously()
    {
        RecordingBee<String> bee = new RecordingBee<>(hive, 0, 0);

        bee.accept("hello");
        bee.accept("world");

        // Zero threads means synchronous mode: messages are received immediately
        // in the calling thread, even though a Hive is attached.
        assertEquals(Arrays.asList("hello", "world"), bee.received);
        assertEquals(0, bee.getPendingCount());
    }

    @Test
    void invalidConstructorArgumentsThrow()
    {
        assertThrows(IllegalArgumentException.class, () -> new RecordingBee<String>(hive, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RecordingBee<String>(hive, 1, -1));
    }

    @Test
    void staticShutdownAndAwaitTerminationHandlesMultipleBees()
    {
        RecordingBee<Integer> b1 = new RecordingBee<>(hive);
        RecordingBee<Integer> b2 = new RecordingBee<>(hive);
        b1.accept(1);
        b2.accept(2);

        Hive.shutdownAndAwaitTermination(true, b1, b2);

        assertTrue(b1.isTerminated());
        assertTrue(b2.isTerminated());
        assertEquals(Collections.singletonList(1), b1.received);
        assertEquals(Collections.singletonList(2), b2.received);
    }

    @Test
    void dryLoggerReturnsSameInstance()
    {
        RecordingBee<String> bee = new RecordingBee<>();
        assertSame(bee, bee.dryLogger());
    }

    @Test
    void testExceptionInWorkerDoesNotStopDraining() throws InterruptedException
    {
        RecordingBee<String> bee = new RecordingBee<>(hive);
        bee.dryLogger();
        bee.withAction(m -> 
        {
            if ("fail".equals(m)) 
            {
                throw new RuntimeException("fail");
            }
        });
        bee.accept("first");
        bee.accept("fail");
        bee.accept("last");

        bee.shutdown();
        assertTrue(bee.awaitTermination(2000));
        assertEquals(3, bee.received.size());
        assertTrue(bee.received.contains("first"));
        assertTrue(bee.received.contains("fail"));
        assertTrue(bee.received.contains("last"));
    }

    @Test
    @Timeout(240)
//    @Disabled
    void simpleCascadeBees() throws InterruptedException
    {
        System.out.println("beesCascadeForwarding(20,1,1)");
        beesCascadeForwarding(20,20,20);
        System.out.println("-----------------");
    }
    
    
    @Test
    @Timeout(240)
    @Disabled
    void twentyBeesCascadeForwardingCountsProcessedAndElapsed() throws InterruptedException
    {
        System.out.println("beesCascadeForwarding(20,1,1)");
        beesCascadeForwarding(20,1,1);
        System.out.println("-----------------");
                
        int bees = 20;
        
        // Sweep message counts (powers of two) and queue sizes, verifying the
        // exact fan-out total on every iteration. Note the shift must not be
        // wedged into the expression (1 << (8+255) wraps to a 7-bit shift and
        // yields an unreachable ~290M-receive workload): the sweep is bounded
        // by Character.MAX_VALUE as written.
        for(int msgCount=1;msgCount<Character.MAX_VALUE;msgCount <<= 8)
        {
            for(int queueSize=1;queueSize<Byte.MAX_VALUE;queueSize*=4)
            {
                long t0 = System.nanoTime();
                long count = beesCascadeForwarding(bees, queueSize, msgCount);
                long t1 = System.nanoTime();
                long ms = TimeUnit.NANOSECONDS.toMillis(t1-t0);
                System.out.printf("%d ms %d msg\n", ms, count);
            }
        }
        
    }
    static long beesCascadeForwarding(int beesCount, int queueSize, long maxSends) throws InterruptedException
    {
        System.out.println("beesCascadeForwarding("+beesCount+","+queueSize+","+maxSends+")");

        AtomicLong processed = new AtomicLong();
        // A zero-capacity task queue: a LinkedBlockingQueue would let worker tasks
        // park behind forwarders that are blocked on a full downstream channel,
        // and a parked worker cannot drain that channel, deadlocking the cascade
        // once every pool thread is held by a blocked forwarder. This is the same
        // configuration the Queen's default constructor uses to avoid that
        // deadlock (see Queen#Queen(int)).
        Hive bigHive = Hive.hive(100, 100, 0, 10000, false);
        try
        {
            Bee<Long>[] bees = new Bee[beesCount];
            for (int i = 0; i < beesCount; i++)
            {
                final int index = i;
                bees[index] = new Bee<Long>(bigHive, index + 1, queueSize)
                {
                    @Override
                    protected void receive(Long m)
                    {
                        processed.incrementAndGet();
                        // every bee forwards the message to the next two, except the last ones
                        if (index + 1 < bees.length)
                        {
                            bees[index + 1].accept(m);
                        }
                        if (index + 2 < bees.length)
                        {
                            bees[index + 2].accept(m);
                        }
                    }
                }.dryLogger();
            }

            long start = System.nanoTime();
            long sent = 0;
            while (sent < maxSends)
            {
                bees[0].accept(sent++);
                if(sent%10==0)
                {
                    System.out.println("sent="+sent+" processed="+processed.get());
                }
            }

            Consumer<?>[] stages = bees;
            Hive.shutdownAndAwaitTermination(true, stages);

            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            // Every bee forwards each received message to the next two bees, so a
            // single root message reaches fanOut(beesCount) receivers. The count
            // must be exact: any message dropped by a shutdown racing an in-flight
            // forward would fall short of this total.
            long expected = fanOut(beesCount) * maxSends;
            if (processed.get() != expected)
            {
                System.out.println("MISMATCH processed=" + processed.get() + " expected=" + expected);
            }
            assertEquals(expected, processed.get());
        }
        finally
        {
            bigHive.shutdown();
            bigHive.awaitTermination(2000);
        }
        return processed.get();
    }

    /**
     * Number of {@code receive()} invocations a single forwarded message
     * reaches in a chain of {@code n} bees where every bee forwards to the
     * next two (the last two bees only count their own receive). Computed
     * with {@code t[n-1]=1, t[n-2]=2, t[i]=1+t[i+1]+t[i+2]}.
     */
    static long fanOut(int n)
    {
        long[] total = new long[n + 2];
        total[n - 1] = 1;
        total[n - 2] = 2;
        for (int i = n - 3; i >= 0; i--)
        {
            total[i] = 1 + total[i + 1] + total[i + 2];
        }
        return total[0];
    }
}
