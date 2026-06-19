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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FilterBee}: predicate-based message filtering,
 * CPS forwarding to the linked next stage, and the Hive factory methods.
 */
class FilterBeeTest
{
    private Hive hive;

    @BeforeEach
    void setUp()
    {
        hive = Hive.hive();
    }

    @AfterEach
    void tearDown()
    {
        hive.shutdown();
        try
        {
            hive.awaitTermination(2000);
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void directModeForwardsOnlyMessagesMatchingThePredicate()
    {
        RecordingBee<Integer> sink = new RecordingBee<>();
        FilterBee<Integer> filter = new FilterBee<>(i -> i > 0);
        filter.linkTo(sink);

        filter.send(-1);
        filter.send(2);
        filter.send(-3);
        filter.send(4);

        assertEquals(Arrays.asList(2, 4), sink.received);
    }

    @Test
    void discardedMessagesDoNotReachTheNextStage()
    {
        RecordingBee<String> sink = new RecordingBee<>();
        FilterBee<String> filter = new FilterBee<>(s -> s.length() > 2);
        filter.linkTo(sink);

        filter.send("a");
        filter.send("hello");
        filter.send("x");
        filter.send("ok");

        assertEquals(1, sink.received.size());
        assertTrue(sink.received.contains("hello"));
    }

    @Test
    void messageIsUnchangedWhenForwarded()
    {
        RecordingBee<Integer> sink = new RecordingBee<>();
        FilterBee<Integer> filter = new FilterBee<>(i -> true);
        filter.linkTo(sink);

        filter.send(42);

        assertEquals(Integer.valueOf(42), sink.received.get(0));
    }

    @Test
    void linkToReturnsTheSameNextInstanceForChaining()
    {
        FilterBee<Integer> f1 = new FilterBee<>(i -> i > 0);
        FilterBee<Integer> f2 = new FilterBee<>(i -> i < 100);

        FilterBee<Integer> returned = f1.linkTo(f2);

        assertSame(f2, returned);
    }

    @Test
    void filtersCanBeChainedInSequence()
    {
        RecordingBee<Integer> sink = new RecordingBee<>();
        FilterBee<Integer> positive = new FilterBee<>(i -> i > 0);
        FilterBee<Integer> small = new FilterBee<>(i -> i < 100);

        positive.linkTo(small).linkTo(sink);

        positive.send(-5);
        positive.send(1);
        positive.send(200);
        positive.send(50);

        assertEquals(2, sink.received.size());
        assertTrue(sink.received.contains(1));
        assertTrue(sink.received.contains(50));
    }

    @Test
    void filterWithPipeFormALogicalChain() throws InterruptedException
    {
        java.util.List<String> result = new CopyOnWriteArrayList<>();
        FilterBee<Integer> filter = hive.filter(i -> i % 2 == 0);
        PipeBee<Integer,String> pipe = hive.pipe(i -> "even=" + i);
        pipe.linkTo(hive.bee(result::add));

        filter.linkTo(pipe);

        filter.send(1);
        filter.send(2);
        filter.send(3);
        filter.send(4);
        
        filter.waitForIdle();
                
        Hive.shutdownAndAwaitTermination(true, true, filter, pipe);

        assertEquals(2, result.size());
        assertTrue(result.contains("even=2"));
        assertTrue(result.contains("even=4"));
    }

    @Test
    void messageIsProcessedEvenWhenNoNextStageIsLinked()
    {
        RecordingBee<Integer> meter = new RecordingBee<Integer>()
        {
            @Override
            protected void receive(Integer m)
            {
                super.receive(m);
            }
        };

        FilterBee<Integer> filter = new FilterBee<>(i -> true);
        filter.send(1);
        // No error, predicate is evaluated even without a next stage
    }

    @Test
    void constructorRejectsNullPredicate()
    {
        assertThrows(NullPointerException.class, () -> new FilterBee<>(null));
        assertThrows(NullPointerException.class, () -> new FilterBee<>(hive, null));
        assertThrows(NullPointerException.class, () -> new FilterBee<>(2, hive, null));
    }

    @Test
    void linkToRejectsNull()
    {
        FilterBee<Integer> f = new FilterBee<>(i -> true);
        assertThrows(NullPointerException.class, () -> f.linkTo(null));
    }

    @Test
    void hiveFilterFactoryCreatesBoundFilterBee() throws InterruptedException
    {
        List<Integer> result = new CopyOnWriteArrayList<>();
        FilterBee<Integer> filter = hive.filter(i -> i > 10);
        filter.linkTo(hive.bee(result::add));

        filter.send(5);
        filter.send(15);

        filter.waitForIdle().shutdown().awaitTermination(Integer.MAX_VALUE);
        
        Hive.shutdownAndAwaitTermination(true, true, filter);

        assertEquals(1, result.size());
        assertTrue(result.contains(15));
    }

    @Test
    void hiveFilterFactoryWithThreadsParameter() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();
        FilterBee<String> filter = hive.filter(2, s -> s.length() > 3);
        filter.linkTo(hive.bee(result::add));

        filter.send("hi");
        filter.send("hello");

        filter.waitForIdle().shutdown().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, filter);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("hello"));
    }

    @Test
    void hiveFilterFactoryWithQueueSizeParameter() throws InterruptedException
    {
        java.util.List<Integer> result = new CopyOnWriteArrayList<>();
        FilterBee<Integer> filter = hive.filter(20, 10, i -> i > 0);
        filter.linkTo(hive.bee(result::add));

        filter.send(-1);
        filter.send(1);

        filter.waitForIdle().shutdown(true).awaitTermination(1);
        Hive.shutdownAndAwaitTermination(true, true, filter);

        assertEquals(1, result.size());
    }

    @Test
    void predicateThatAlwaysReturnsFalseDropsAllMessages()
    {
        RecordingBee<String> sink = new RecordingBee<>();
        FilterBee<String> filter = new FilterBee<>(s -> false);
        filter.linkTo(sink);

        filter.send("a");
        filter.send("b");
        filter.send("c");

        assertTrue(sink.received.isEmpty());
    }

    @Test
    void predicateThatAlwaysReturnsTrueForwardsAll()
    {
        RecordingBee<String> sink = new RecordingBee<>();
        FilterBee<String> filter = new FilterBee<>(s -> true);
        filter.linkTo(sink);

        filter.send("a");
        filter.send("b");
        filter.send("c");

        assertEquals(3, sink.received.size());
    }
}
