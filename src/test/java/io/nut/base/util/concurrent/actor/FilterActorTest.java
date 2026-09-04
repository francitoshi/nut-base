/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

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
 * Unit tests for {@link FilterActor}: predicate-based message filtering,
 * CPS forwarding to the linked next stage, and the ActorHub factory methods.
 */
class FilterActorTest
{
    private ActorHub actorHub;

    @BeforeEach
    void setUp()
    {
        actorHub = ActorHub.actorHub();
    }

    @AfterEach
    void tearDown()
    {
        actorHub.shutdown();
        try
        {
            actorHub.awaitTermination(2000);
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void directModeForwardsOnlyMessagesMatchingThePredicate()
    {
        RecordingActor<Integer> sink = new RecordingActor<>();
        FilterActor<Integer> filter = new FilterActor<>(i -> i > 0);
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
        RecordingActor<String> sink = new RecordingActor<>();
        FilterActor<String> filter = new FilterActor<>(s -> s.length() > 2);
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
        RecordingActor<Integer> sink = new RecordingActor<>();
        FilterActor<Integer> filter = new FilterActor<>(i -> true);
        filter.linkTo(sink);

        filter.accept(42);

        assertEquals(Integer.valueOf(42), sink.received.get(0));
    }

    @Test
    void linkToReturnsTheSameNextInstanceForChaining()
    {
        FilterActor<Integer> f1 = new FilterActor<>(i -> i > 0);
        FilterActor<Integer> f2 = new FilterActor<>(i -> i < 100);

        FilterActor<Integer> returned = f1.linkTo(f2);

        assertSame(f2, returned);
    }

    @Test
    void filtersCanBeChainedInSequence()
    {
        RecordingActor<Integer> sink = new RecordingActor<>();
        FilterActor<Integer> positive = new FilterActor<>(i -> i > 0);
        FilterActor<Integer> small = new FilterActor<>(i -> i < 100);

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
        FilterActor<Integer> filter = actorHub.filter(i -> i % 2 == 0);
        PipeActor<Integer,String> pipe = actorHub.pipe(i -> "even=" + i);
        pipe.linkTo(actorHub.actor(result::add));

        filter.linkTo(pipe);

        filter.accept(1);
        filter.accept(2);
        filter.accept(3);
        filter.accept(4);
        
        filter.waitForIdle();
                
        actorHub.close(true);

        assertEquals(2, result.size());
        assertTrue(result.contains("even=2"));
        assertTrue(result.contains("even=4"));
    }

    @Test
    void messageIsProcessedEvenWhenNoNextStageIsLinked()
    {
        RecordingActor<Integer> meter = new RecordingActor<Integer>()
        {
            @Override
            protected void receive(Integer m)
            {
                super.receive(m);
            }
        };

        FilterActor<Integer> filter = new FilterActor<>(i -> true);
        filter.accept(1);
        // No error, predicate is evaluated even without a next stage
    }

    @Test
    void constructorRejectsNullPredicate()
    {
        assertThrows(NullPointerException.class, () -> new FilterActor<>(null));
        assertThrows(NullPointerException.class, () -> new FilterActor<>(actorHub, null));
        assertThrows(NullPointerException.class, () -> new FilterActor<>(actorHub, 2, 2, null));
    }

    @Test
    void linkToRejectsNull()
    {
        FilterActor<Integer> f = new FilterActor<>(i -> true);
        assertThrows(NullPointerException.class, () -> f.linkTo(null));
    }

    @Test
    void hubFilterFactoryCreatesBoundFilterActor() throws InterruptedException
    {
        List<Integer> result = new CopyOnWriteArrayList<>();
        FilterActor<Integer> filter = actorHub.filter(i -> i > 10);
        filter.linkTo(actorHub.actor(result::add));

        filter.accept(5);
        filter.accept(15);

        filter.waitForIdle().shutdown().awaitTermination(Integer.MAX_VALUE);
        
        actorHub.close(true);

        assertEquals(1, result.size());
        assertTrue(result.contains(15));
    }

    @Test
    void hubFilterFactoryWithThreadsParameter() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();
        FilterActor<String> filter = actorHub.filter(2, s -> s.length() > 3);
        filter.linkTo(actorHub.actor(result::add));

        filter.accept("hi");
        filter.accept("hello");

        filter.waitForIdle().shutdown().awaitTermination(25);
        actorHub.close(true);
        
        assertEquals(1, result.size());
        assertTrue(result.contains("hello"));
    }

    @Test
    void hubFilterFactoryWithQueueSizeParameter() throws InterruptedException
    {
        java.util.List<Integer> result = new CopyOnWriteArrayList<>();
        FilterActor<Integer> filter = actorHub.filter(20, 10, i -> i > 0);
        filter.linkTo(actorHub.actor(result::add));

        filter.accept(-1);
        filter.accept(1);

        filter.waitForIdle().shutdown(true).awaitTermination(1);
        actorHub.close(true);

        assertEquals(1, result.size());
    }

    @Test
    void predicateThatAlwaysReturnsFalseDropsAllMessages()
    {
        RecordingActor<String> sink = new RecordingActor<>();
        FilterActor<String> filter = new FilterActor<>(s -> false);
        filter.linkTo(sink);

        filter.accept("a");
        filter.accept("b");
        filter.accept("c");

        assertTrue(sink.received.isEmpty());
    }

    @Test
    void predicateThatAlwaysReturnsTrueForwardsAll()
    {
        RecordingActor<String> sink = new RecordingActor<>();
        FilterActor<String> filter = new FilterActor<>(s -> true);
        filter.linkTo(sink);

        filter.accept("a");
        filter.accept("b");
        filter.accept("c");

        assertEquals(3, sink.received.size());
    }
}
