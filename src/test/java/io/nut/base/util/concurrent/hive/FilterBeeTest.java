/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

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

        filter.accept(-1);
        filter.accept(2);
        filter.accept(-3);
        filter.accept(4);

        assertEquals(Arrays.asList(2, 4), sink.received);
    }

    @Test
    void discardedMessagesDoNotReachTheNextStage()
    {
        RecordingBee<String> sink = new RecordingBee<>();
        FilterBee<String> filter = new FilterBee<>(s -> s.length() > 2);
        filter.linkTo(sink);

        filter.accept("a");
        filter.accept("hello");
        filter.accept("x");
        filter.accept("ok");

        assertEquals(1, sink.received.size());
        assertTrue(sink.received.contains("hello"));
    }

    @Test
    void messageIsUnchangedWhenForwarded()
    {
        RecordingBee<Integer> sink = new RecordingBee<>();
        FilterBee<Integer> filter = new FilterBee<>(i -> true);
        filter.linkTo(sink);

        filter.accept(42);

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

        positive.accept(-5);
        positive.accept(1);
        positive.accept(200);
        positive.accept(50);

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

        filter.accept(1);
        filter.accept(2);
        filter.accept(3);
        filter.accept(4);
        
        filter.waitForIdle();
                
        hive.close(true);

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
        filter.accept(1);
        // No error, predicate is evaluated even without a next stage
    }

    @Test
    void constructorRejectsNullPredicate()
    {
        assertThrows(NullPointerException.class, () -> new FilterBee<>(null));
        assertThrows(NullPointerException.class, () -> new FilterBee<>(hive, null));
        assertThrows(NullPointerException.class, () -> new FilterBee<>(hive, 2, 2, null));
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

        filter.accept(5);
        filter.accept(15);

        filter.waitForIdle().shutdown().awaitTermination(Integer.MAX_VALUE);
        
        hive.close(true);

        assertEquals(1, result.size());
        assertTrue(result.contains(15));
    }

    @Test
    void hiveFilterFactoryWithThreadsParameter() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();
        FilterBee<String> filter = hive.filter(2, s -> s.length() > 3);
        filter.linkTo(hive.bee(result::add));

        filter.accept("hi");
        filter.accept("hello");

        filter.waitForIdle().shutdown().awaitTermination(25);
        hive.close(true);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("hello"));
    }

    @Test
    void hiveFilterFactoryWithQueueSizeParameter() throws InterruptedException
    {
        java.util.List<Integer> result = new CopyOnWriteArrayList<>();
        FilterBee<Integer> filter = hive.filter(20, 10, i -> i > 0);
        filter.linkTo(hive.bee(result::add));

        filter.accept(-1);
        filter.accept(1);

        filter.waitForIdle().shutdown(true).awaitTermination(1);
        hive.close(true);

        assertEquals(1, result.size());
    }

    @Test
    void predicateThatAlwaysReturnsFalseDropsAllMessages()
    {
        RecordingBee<String> sink = new RecordingBee<>();
        FilterBee<String> filter = new FilterBee<>(s -> false);
        filter.linkTo(sink);

        filter.accept("a");
        filter.accept("b");
        filter.accept("c");

        assertTrue(sink.received.isEmpty());
    }

    @Test
    void predicateThatAlwaysReturnsTrueForwardsAll()
    {
        RecordingBee<String> sink = new RecordingBee<>();
        FilterBee<String> filter = new FilterBee<>(s -> true);
        filter.linkTo(sink);

        filter.accept("a");
        filter.accept("b");
        filter.accept("c");

        assertEquals(3, sink.received.size());
    }
}
