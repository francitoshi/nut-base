/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Timeout;

/**
 * Unit tests for {@link Actor}: direct (actorHub-less) message delivery,
 * actorHub-backed asynchronous delivery, the shutdown/termination lifecycle,
 * and the exception hook.
 */
class ActorTest
{
    private ActorHub actorHub;

    @BeforeEach
    void setUp()
    {
        actorHub = ActorHub.actorHub(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException
    {
        actorHub.shutdown();
        actorHub.awaitTermination(2000);
    }

    @Test
    void directSendInvokesReceiveSynchronously()
    {
        RecordingActor<String> actor = new RecordingActor<>();

        actor.accept("hello");

        assertEquals(Collections.singletonList("hello"), actor.received);
    }

    @Test
    void getExceptionIsNullInitially()
    {
        RecordingActor<String> actor = new RecordingActor<>();
        assertNull(actor.getException());
    }

    @Test
    void sendAfterShutdownReturnsFalseAndIsNotReceived()
    {
        RecordingActor<String> actor = new RecordingActor<>();
        actor.shutdown();
        actor.dryLogger();
        assertTrue(actor.isShutdown());
        actor.accept("too late");
        assertTrue(actor.received.isEmpty());
    }

    @Test
    void shutdownWithoutActorHubTerminatesImmediatelyWhenEmpty()
    {
        RecordingActor<String> actor = new RecordingActor<>();

        assertFalse(actor.isTerminated());
        actor.shutdown();

        assertTrue(actor.isShutdown());
        assertTrue(actor.isTerminated());
        assertTrue(actor.terminated.get());
    }

    @Test
    void awaitTerminationTimesOutWhenNeverShutdown()
    {
        RecordingActor<String> actor = new RecordingActor<>();
        assertFalse(actor.awaitTermination(50));
    }

    @Test
    void awaitTerminationReturnsTrueAfterShutdown()
    {
        RecordingActor<String> actor = new RecordingActor<>();
        actor.shutdown();
        assertTrue(actor.awaitTermination(50));
    }

    @Test
    void exceptionHookAndGetExceptionCaptureFailureInDirectMode()
    {
        RuntimeException boom = new RuntimeException("boom");
        RecordingActor<String> actor = new RecordingActor<>();
        actor.dryLogger(); // avoid noisy SEVERE log output for this expected failure
        actor.withAction(m ->
        {
            throw boom;
        });

        actor.accept("x");
        assertSame(boom, actor.getException());
        assertSame(boom, actor.lastException.get());
    }

    @Test
    void hubBackedSendIsProcessedAsynchronouslyAndDeterministicallyDrained()
    {
        RecordingActor<Integer> actor = new RecordingActor<>(actorHub);
        for (int i = 0; i < 20; i++)
        {
            actor.accept(i);
        }

        actor.shutdown().awaitTermination(1);

        assertTrue(actor.isTerminated());
        assertTrue(actor.terminated.get());
        assertEquals(20, actor.received.size());
        for (int i = 0; i < 20; i++)
        {
            assertTrue(actor.received.contains(i));
        }
    }

    @Test
    void shutdownOnlyWhenEmptyWaitsForPendingMessagesBeforeTerminating()
    {
        RecordingActor<Integer> actor = new RecordingActor<>(actorHub, 1, 2);
        for (int i = 0; i < 50; i++)
        {
            actor.accept(i);
        }

        actor.shutdown(true);

        assertTrue(actor.awaitTermination(2000));
        assertEquals(50, actor.received.size());
    }

    @Test
    void zeroThreadsWithActorHubProcessesSynchronously()
    {
        RecordingActor<String> actor = new RecordingActor<>(actorHub, 0, 0);

        actor.accept("hello");
        actor.accept("world");

        // Zero threads means synchronous mode: messages are received immediately
        // in the calling thread, even though an ActorHub is attached.
        assertEquals(Arrays.asList("hello", "world"), actor.received);
        assertEquals(0, actor.getPendingCount());
    }

    @Test
    void synchronousActorHubProcessesSynchronouslyEvenWithPositiveThreads()
    {
        // A ActorHub sized 0x0 runs synchronously; an Actor attached to it must too,
        // regardless of its own (positive) thread count.
        ActorHub syncActorHub = ActorHub.actorHub(0);
        try
        {
            assertTrue(syncActorHub.isSynchronous());

            RecordingActor<String> actor = new RecordingActor<>(syncActorHub, 4, 0);
            actor.accept("hello");
            actor.accept("world");

            assertEquals(Arrays.asList("hello", "world"), actor.received);
            assertEquals(0, actor.getPendingCount());
        }
        finally
        {
            syncActorHub.shutdown();
        }
    }

    @Test
    void invalidConstructorArgumentsThrow()
    {
        assertThrows(IllegalArgumentException.class, () -> new RecordingActor<String>(actorHub, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RecordingActor<String>(actorHub, 1, -1));
    }

    @Test
    void closeHandlesMultipleActors()
    {
        RecordingActor<Integer> b1 = new RecordingActor<>(actorHub);
        RecordingActor<Integer> b2 = new RecordingActor<>(actorHub);
        b1.accept(1);
        b2.accept(2);

        actorHub.close(true);

        assertTrue(b1.isTerminated());
        assertTrue(b2.isTerminated());
        assertEquals(Collections.singletonList(1), b1.received);
        assertEquals(Collections.singletonList(2), b2.received);
    }

    @Test
    void dryLoggerReturnsSameInstance()
    {
        RecordingActor<String> actor = new RecordingActor<>();
        assertSame(actor, actor.dryLogger());
    }

    @Test
    void testExceptionInWorkerDoesNotStopDraining() throws InterruptedException
    {
        RecordingActor<String> actor = new RecordingActor<>(actorHub);
        actor.dryLogger();
        actor.withAction(m -> 
        {
            if ("fail".equals(m)) 
            {
                throw new RuntimeException("fail");
            }
        });
        actor.accept("first");
        actor.accept("fail");
        actor.accept("last");

        actor.shutdown();
        assertTrue(actor.awaitTermination(2000));
        assertEquals(3, actor.received.size());
        assertTrue(actor.received.contains("first"));
        assertTrue(actor.received.contains("fail"));
        assertTrue(actor.received.contains("last"));
    }

    @Test
    @Timeout(240)
//    @Disabled
    void simpleCascadeActors() throws InterruptedException
    {
        System.out.println("actorCascadeForwarding(20,1,1)");
        actorsCascadeForwarding(20,20,20);
        System.out.println("-----------------");
    }
    
    
    @Test
    @Timeout(240)
    @Disabled
    void twentyActorsCascadeForwardingCountsProcessedAndElapsed() throws InterruptedException
    {
        System.out.println("actorsCascadeForwarding(20,1,1)");
        actorsCascadeForwarding(20,1,1);
        System.out.println("-----------------");
                
        int actors = 20;
        
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
                long count = actorsCascadeForwarding(actors, queueSize, msgCount);
                long t1 = System.nanoTime();
                long ms = TimeUnit.NANOSECONDS.toMillis(t1-t0);
                System.out.printf("%d ms %d msg\n", ms, count);
            }
        }
        
    }
    static long actorsCascadeForwarding(int actorsCount, int queueSize, long maxSends) throws InterruptedException
    {
        System.out.println("actorsCascadeForwarding("+actorsCount+","+queueSize+","+maxSends+")");

        AtomicLong processed = new AtomicLong();
        // A zero-capacity task queue: a LinkedBlockingQueue would let worker tasks
        // park behind forwarders that are blocked on a full downstream channel,
        // and a parked worker cannot drain that channel, deadlocking the cascade
        // once every pool thread is held by a blocked forwarder. This is the same
        // configuration the ActorPool's default constructor uses to avoid that
        // deadlock (see ActorPool#ActorPool(int)).
        ActorHub bigActorHub = ActorHub.actorHub(100, 0, 10000, false);
        try
        {
            Actor<Long>[] actors = new Actor[actorsCount];
            for (int i = 0; i < actorsCount; i++)
            {
                final int index = i;
                actors[index] = new Actor<Long>(bigActorHub, index + 1, queueSize)
                {
                    @Override
                    protected void receive(Long m)
                    {
                        processed.incrementAndGet();
                        // every actor forwards the message to the next two, except the last ones
                        if (index + 1 < actors.length)
                        {
                            actors[index + 1].accept(m);
                        }
                        if (index + 2 < actors.length)
                        {
                            actors[index + 2].accept(m);
                        }
                    }
                }.dryLogger();
            }

            long start = System.nanoTime();
            long sent = 0;
            while (sent < maxSends)
            {
                actors[0].accept(sent++);
                if(sent%10==0)
                {
                    System.out.println("sent="+sent+" processed="+processed.get());
                }
            }

            bigActorHub.close(true);

            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            // Every actor forwards each received message to the next two actors, so a
            // single root message reaches fanOut(actorsCount) receivers. The count
            // must be exact: any message dropped by a shutdown racing an in-flight
            // forward would fall short of this total.
            long expected = fanOut(actorsCount) * maxSends;
            if (processed.get() != expected)
            {
                System.out.println("MISMATCH processed=" + processed.get() + " expected=" + expected);
            }
            assertEquals(expected, processed.get());
        }
        finally
        {
            bigActorHub.shutdown();
            bigActorHub.awaitTermination(2000);
        }
        return processed.get();
    }

    /**
     * Number of {@code receive()} invocations a single forwarded message
     * reaches in a chain of {@code n} actors where every actor forwards to the
     * next two (the last two actors only count their own receive). Computed
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
