/*
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent.hive;

import io.nut.base.util.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BroadcastBee}: fan-out delivery to multiple
 * targets, dynamic target management via {@link BroadcastBee#addTarget}
 * and {@link BroadcastBee#removeTarget}, and thread-safe target mutation.
 */
class BroadcastBeeTest
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
        BroadcastBee<String> bc = new BroadcastBee<>(t1, t2, t3);

        bc.send("msg");

        assertEquals(Arrays.asList("msg"), t1.received);
        assertEquals(Arrays.asList("msg"), t2.received);
        assertEquals(Arrays.asList("msg"), t3.received);
    }

    @Test
    void multipleMessagesAreRepeatedToEachTarget()
    {
        RecordingBee<Integer> t1 = new RecordingBee<>();
        RecordingBee<Integer> t2 = new RecordingBee<>();
        BroadcastBee<Integer> bc = new BroadcastBee<>(t1, t2);

        bc.send(1);
        bc.send(2);
        bc.send(3);

        assertEquals(Arrays.asList(1, 2, 3), t1.received);
        assertEquals(Arrays.asList(1, 2, 3), t2.received);
    }

    @Test
    void addTargetEnablesNewTargetReceivingFutureMessages()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        BroadcastBee<String> bc = new BroadcastBee<>(t1);

        bc.send("before");

        bc.addTarget(t2);
        bc.send("after");

        assertEquals(Arrays.asList("before", "after"), t1.received);
        assertEquals(Arrays.asList("after"), t2.received);
    }

    @Test
    void addTargetReturnsTheBroadcastBeeForFluentChaining()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        BroadcastBee<String> bc = new BroadcastBee<>();

        BroadcastBee<String> returned = bc.addTarget(t1);

        assertSame(bc, returned);
        returned.addTarget(t2);

        bc.send("chained");

        assertEquals(1, t1.received.size());
        assertEquals(1, t2.received.size());
    }

    @Test
    void addTargetRejectsNull()
    {
        BroadcastBee<String> bc = new BroadcastBee<>();
        assertThrows(NullPointerException.class, () -> bc.addTarget(null));
    }

    @Test
    void removeTargetStopsTheTargetReceivingFutureMessages()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        BroadcastBee<String> bc = new BroadcastBee<>(t1, t2);

        bc.send("before");
        assertTrue(bc.removeTarget(t2));
        bc.send("after");

        assertEquals(Arrays.asList("before", "after"), t1.received);
        assertEquals(Arrays.asList("before"), t2.received);
    }

    @Test
    void removeTargetReturnsFalseIfTargetWasNotPresent()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        BroadcastBee<String> bc = new BroadcastBee<>(t1);

        assertFalse(bc.removeTarget(t2));
    }

    @Test
    void getTargetsReturnsAnUnmodifiableSnapshot()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        BroadcastBee<String> bc = new BroadcastBee<>(t1, t2);

        java.util.List<Sendable<String>> targets = bc.getTargets();

        assertEquals(2, targets.size());
        assertThrows(UnsupportedOperationException.class, () -> targets.add(new RecordingBee<>()));
    }

    @Test
    void emptyBroadcastBeeWithNoTargetsStillAcceptsMessages()
    {
        BroadcastBee<String> bc = new BroadcastBee<>();
        assertTrue(bc.send("msg"));
        // Message is silently dropped, no targets present
    }

    @Test
    void hiveBackedBroadcastDeliversConcurrently() throws InterruptedException
    {
        List<Integer> a = new CopyOnWriteArrayList<>();
        List<Integer> b = new CopyOnWriteArrayList<>();
        List<Integer> c = new CopyOnWriteArrayList<>();

        BroadcastBee<Integer> bc = hive.broadcast(hive.bee(a::add), hive.bee(b::add), hive.bee(c::add));

        for (int i = 0; i < 10; i++)
        {
            bc.send(i);
        }
        
        bc.shutdown(true).awaitTermination(100);

        assertEquals(10, a.size());
        assertEquals(10, b.size());
        assertEquals(10, c.size());
    }

    @Test
    void constructorRejectsNullTargetVarargs()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        assertThrows(NullPointerException.class, () -> new BroadcastBee<>(t1, null));
    }

    @Test
    void hiveFactoryWithTargets()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        RecordingBee<String> t2 = new RecordingBee<>();
        BroadcastBee<String> bc = hive.broadcast(t1, t2);

        bc.send("msg");
        Utils.parkMillis(100);
        
        assertEquals(1, t1.received.size());
        assertEquals(1, t2.received.size());
    }

    @Test
    void hiveFactoryWithThreadsParameter()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        BroadcastBee<String> bc = hive.broadcast(2, t1);

        bc.send("msg");
        Utils.parkMillis(100);

        assertEquals(1, t1.received.size());
    }

    @Test
    void hiveFactoryWithQueueSizeAndThreadsParameter()
    {
        RecordingBee<String> t1 = new RecordingBee<>();
        BroadcastBee<String> bc = hive.broadcast(2, 10, t1);

        bc.send("msg");
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

        BroadcastBee<Integer> bc = hive.broadcast(p1, p2);

        bc.send(5);
        bc.send(10);
        
        Utils.parkMillis(100);

        assertEquals(2, chain1Result.size());
        assertEquals(2, chain2Result.size());
        assertTrue(chain1Result.contains("chain1=5"));
        assertTrue(chain2Result.contains("chain2=10"));
    }
}
