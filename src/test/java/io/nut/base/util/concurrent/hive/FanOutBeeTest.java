/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

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
 * Unit tests for {@link FanOutBee}: fan-out delivery to multiple
 * targets, dynamic target management via {@link FanOutBee#addTarget}
 * and {@link FanOutBee#removeTarget}, and thread-safe target mutation.
 */
class FanOutBeeTest
{
    private Hive hive;

    @BeforeEach
    void setUp()
    {
        hive = Hive.hive();
    }

    @AfterEach
    void tearDown() throws InterruptedException
    {
        hive.shutdown();
        hive.awaitTermination(2000);
    }

    @Test
    void directModeBroadcastsEachMessageToAllTargets()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        RecordingBee<String> t3 = new RecordingBee<>();
        FanOutBee<String> bc = new FanOutBee<>(t1, t2, t3);

        bc.accept("msg");

        assertEquals(Arrays.asList("msg"), t1.received);
        assertEquals(Arrays.asList("msg"), t2.received);
        assertEquals(Arrays.asList("msg"), t3.received);
    }

    @Test
    void multipleMessagesAreRepeatedToEachTarget()
    {
        RecordingBee<Integer> t1 = new RecordingBee<>();
        RecordingBee<Integer> t2 = new RecordingBee<>();
        FanOutBee<Integer> bc = new FanOutBee<>(t1, t2);

        bc.accept(1);
        bc.accept(2);
        bc.accept(3);

        assertEquals(Arrays.asList(1, 2, 3), t1.received);
        assertEquals(Arrays.asList(1, 2, 3), t2.received);
    }

    @Test
    void addTargetEnablesNewTargetReceivingFutureMessages()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        FanOutBee<String> bc = new FanOutBee<>(t1);

        bc.accept("before");

        bc.addTarget(t2);
        bc.accept("after");

        assertEquals(Arrays.asList("before", "after"), t1.received);
        assertEquals(Arrays.asList("after"), t2.received);
    }

    @Test
    void addTargetReturnsTheFanOutBeeForFluentChaining()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        FanOutBee<String> bc = new FanOutBee<>();

        FanOutBee<String> returned = bc.addTarget(t1);

        assertSame(bc, returned);
        returned.addTarget(t2);

        bc.accept("chained");

        assertEquals(1, t1.received.size());
        assertEquals(1, t2.received.size());
    }

    @Test
    void addTargetRejectsNull()
    {
        FanOutBee<String> bc = new FanOutBee<>();
        assertThrows(NullPointerException.class, () -> bc.addTarget(null));
    }

    @Test
    void removeTargetStopsTheTargetReceivingFutureMessages()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        FanOutBee<String> bc = new FanOutBee<>(t1, t2);

        bc.accept("before");
        assertTrue(bc.removeTarget(t2));
        bc.accept("after");

        assertEquals(Arrays.asList("before", "after"), t1.received);
        assertEquals(Arrays.asList("before"), t2.received);
    }

    @Test
    void removeTargetReturnsFalseIfTargetWasNotPresent()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        FanOutBee<String> bc = new FanOutBee<>(t1);

        assertFalse(bc.removeTarget(t2));
    }

    @Test
    void getTargetsReturnsAnUnmodifiableSnapshot()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        FanOutBee<String> bc = new FanOutBee<>(t1, t2);

        List<Consumer<String>> targets = bc.getTargets();

        assertEquals(2, targets.size());
        assertThrows(UnsupportedOperationException.class, () -> targets.add(new RecordingBee<>()));
    }

    @Test
    void emptyFanOutBeeWithNoTargetsStillAcceptsMessages()
    {
        FanOutBee<String> bc = new FanOutBee<>();
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

            FanOutBee<Integer> bc = hive.broadcast(th, hive.bee(th, a::add), hive.bee(th, b::add), hive.bee(th, c::add));

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
        RecordingBee<String> t1 = new RecordingBee<>();
        assertThrows(NullPointerException.class, () -> new FanOutBee<>(t1, null));
    }

    @Test
    void hiveFactoryWithTargets()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        FanOutBee<String> bc = hive.broadcast(t1, t2);

        bc.accept("msg");
        Utils.parkMillis(100);
        
        assertEquals(1, t1.received.size());
        assertEquals(1, t2.received.size());
    }

    @Test
    void hiveFactoryWithThreadsParameter()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        FanOutBee<String> bc = hive.broadcast(2, t1);

        bc.accept("msg");
        Utils.parkMillis(100);

        assertEquals(1, t1.received.size());
    }

    @Test
    void hiveFactoryWithQueueSizeAndThreadsParameter()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        FanOutBee<String> bc = hive.broadcast(2, 10, t1);

        bc.accept("msg");
        Utils.parkMillis(100);

        assertEquals(1, t1.received.size());
    }

    @Test
    void broadcastCanFanOutToMultiplePipelineChains() throws InterruptedException
    {
        List<String> chain1Result = new CopyOnWriteArrayList<>();
        List<String> chain2Result = new CopyOnWriteArrayList<>();

        PipeBee<Integer,String> p1 = hive.pipe(i -> "chain1=" + i);
        PipeBee<Integer,String> p2 = hive.pipe(i -> "chain2=" + i);
        p1.linkTo(hive.bee(chain1Result::add));
        p2.linkTo(hive.bee(chain2Result::add));

        FanOutBee<Integer> bc = hive.broadcast(p1, p2);

        bc.accept(5);
        bc.accept(10);
        
        Utils.parkMillis(100);

        assertEquals(2, chain1Result.size());
        assertEquals(2, chain2Result.size());
        assertTrue(chain1Result.contains("chain1=5"));
        assertTrue(chain2Result.contains("chain2=10"));
    }
}
