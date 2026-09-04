/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import io.nut.base.util.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FanOutActor}: fan-out delivery to multiple
 * targets, dynamic target management via {@link FanOutActor#addTarget}
 * and {@link FanOutActor#removeTarget}, and thread-safe target mutation.
 */
class FanOutActorTest
{
    private ActorHub actorHub;

    @BeforeEach
    void setUp()
    {
        actorHub = ActorHub.actorHub();
    }

    @AfterEach
    void tearDown() throws InterruptedException
    {
        actorHub.shutdown();
        actorHub.awaitTermination(2000);
    }

    @Test
    void directModeBroadcastsEachMessageToAllTargets()
    {
        RecordingActor<String> t1 = new RecordingActor<>();
        RecordingActor<String> t2 = new RecordingActor<>();
        RecordingActor<String> t3 = new RecordingActor<>();
        FanOutActor<String> bc = new FanOutActor<>(t1, t2, t3);

        bc.accept("msg");

        assertEquals(Arrays.asList("msg"), t1.received);
        assertEquals(Arrays.asList("msg"), t2.received);
        assertEquals(Arrays.asList("msg"), t3.received);
    }

    @Test
    void multipleMessagesAreRepeatedToEachTarget()
    {
        RecordingActor<Integer> t1 = new RecordingActor<>();
        RecordingActor<Integer> t2 = new RecordingActor<>();
        FanOutActor<Integer> bc = new FanOutActor<>(t1, t2);

        bc.accept(1);
        bc.accept(2);
        bc.accept(3);

        assertEquals(Arrays.asList(1, 2, 3), t1.received);
        assertEquals(Arrays.asList(1, 2, 3), t2.received);
    }

    @Test
    void addTargetEnablesNewTargetReceivingFutureMessages()
    {
        RecordingActor<String> t1 = new RecordingActor<>();
        RecordingActor<String> t2 = new RecordingActor<>();
        FanOutActor<String> bc = new FanOutActor<>(t1);

        bc.accept("before");

        bc.addTarget(t2);
        bc.accept("after");

        assertEquals(Arrays.asList("before", "after"), t1.received);
        assertEquals(Arrays.asList("after"), t2.received);
    }

    @Test
    void addTargetReturnsTheFanOutActorForFluentChaining()
    {
        RecordingActor<String> t1 = new RecordingActor<>();
        RecordingActor<String> t2 = new RecordingActor<>();
        FanOutActor<String> bc = new FanOutActor<>();

        FanOutActor<String> returned = bc.addTarget(t1);

        assertSame(bc, returned);
        returned.addTarget(t2);

        bc.accept("chained");

        assertEquals(1, t1.received.size());
        assertEquals(1, t2.received.size());
    }

    @Test
    void addTargetRejectsNull()
    {
        FanOutActor<String> bc = new FanOutActor<>();
        assertThrows(NullPointerException.class, () -> bc.addTarget(null));
    }

    @Test
    void removeTargetStopsTheTargetReceivingFutureMessages()
    {
        RecordingActor<String> t1 = new RecordingActor<>();
        RecordingActor<String> t2 = new RecordingActor<>();
        FanOutActor<String> bc = new FanOutActor<>(t1, t2);

        bc.accept("before");
        assertTrue(bc.removeTarget(t2));
        bc.accept("after");

        assertEquals(Arrays.asList("before", "after"), t1.received);
        assertEquals(Arrays.asList("before"), t2.received);
    }

    @Test
    void removeTargetReturnsFalseIfTargetWasNotPresent()
    {
        RecordingActor<String> t1 = new RecordingActor<>();
        RecordingActor<String> t2 = new RecordingActor<>();
        FanOutActor<String> bc = new FanOutActor<>(t1);

        assertFalse(bc.removeTarget(t2));
    }

    @Test
    void getTargetsReturnsAnUnmodifiableSnapshot()
    {
        RecordingActor<String> t1 = new RecordingActor<>();
        RecordingActor<String> t2 = new RecordingActor<>();
        FanOutActor<String> bc = new FanOutActor<>(t1, t2);

        List<Consumer<String>> targets = bc.getTargets();

        assertEquals(2, targets.size());
        assertThrows(UnsupportedOperationException.class, () -> targets.add(new RecordingActor<>()));
    }

    @Test
    void emptyFanOutActorWithNoTargetsStillAcceptsMessages()
    {
        FanOutActor<String> bc = new FanOutActor<>();
        bc.accept("msg");
        // Message is silently dropped, no targets present
    }

    @Test
    void hiveBackedBroadcastDeliversConcurrently() throws InterruptedException
    {
        for(int th=0;th<2;th++)
        {
            List<Integer> a = new CopyOnWriteArrayList<>();
            List<Integer> b = new CopyOnWriteArrayList<>();
            List<Integer> c = new CopyOnWriteArrayList<>();

            FanOutActor<Integer> bc = actorHub.broadcast(th, actorHub.actor(th, a::add), actorHub.actor(th, b::add), actorHub.actor(th, c::add));

            for (int i = 0; i < 10; i++)
            {
                bc.accept(i);
            }

            bc.waitForIdle().shutdown(true).awaitTermination(100);

            assertEquals(10, a.size());
            assertEquals(10, b.size());
            assertEquals(10, c.size());
        }
    }

    @Test
    void constructorRejectsNullTargetVarargs()
    {
        RecordingActor<String> t1 = new RecordingActor<>();
        assertThrows(NullPointerException.class, () -> new FanOutActor<>(t1, null));
    }

    @Test
    void hiveFactoryWithTargets()
    {
        RecordingActor<String> t1 = new RecordingActor<>();
        RecordingActor<String> t2 = new RecordingActor<>();
        FanOutActor<String> bc = actorHub.broadcast(t1, t2);

        bc.accept("msg");
        Utils.parkMillis(100);
        
        assertEquals(1, t1.received.size());
        assertEquals(1, t2.received.size());
    }

    @Test
    void hiveFactoryWithThreadsParameter()
    {
        RecordingActor<String> t1 = new RecordingActor<>();
        FanOutActor<String> bc = actorHub.broadcast(2, t1);

        bc.accept("msg");
        Utils.parkMillis(100);

        assertEquals(1, t1.received.size());
    }

    @Test
    void hiveFactoryWithQueueSizeAndThreadsParameter()
    {
        RecordingActor<String> t1 = new RecordingActor<>();
        FanOutActor<String> bc = actorHub.broadcast(2, 10, t1);

        bc.accept("msg");
        Utils.parkMillis(100);

        assertEquals(1, t1.received.size());
    }

    @Test
    void broadcastCanFanOutToMultiplePipelineChains() throws InterruptedException
    {
        List<String> chain1Result = new CopyOnWriteArrayList<>();
        List<String> chain2Result = new CopyOnWriteArrayList<>();

        PipeActor<Integer,String> p1 = actorHub.pipe(i -> "chain1=" + i);
        PipeActor<Integer,String> p2 = actorHub.pipe(i -> "chain2=" + i);
        p1.linkTo(actorHub.actor(chain1Result::add));
        p2.linkTo(actorHub.actor(chain2Result::add));

        FanOutActor<Integer> bc = actorHub.broadcast(p1, p2);

        bc.accept(5);
        bc.accept(10);
        
        Utils.parkMillis(100);

        assertEquals(2, chain1Result.size());
        assertEquals(2, chain2Result.size());
        assertTrue(chain1Result.contains("chain1=5"));
        assertTrue(chain2Result.contains("chain2=10"));
    }
}
