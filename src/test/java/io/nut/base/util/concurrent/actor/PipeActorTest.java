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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PipeActor}: CPS transform-and-forward semantics,
 * fluent chaining via {@link PipeActor#linkTo}, and exception capture.
 */
class PipeActorTest
{
    private ActorHub actorHub;

    @BeforeEach
    void setUp()
    {
        actorHub = ActorHub.actorHub(2);
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
    void directModeTransformsAndForwardsSynchronously()
    {
        PipeActor<Integer,String> pipe = new PipeActor<>(i -> "n" + i);
        RecordingActor<String> sink = new RecordingActor<>();
        pipe.linkTo(sink);

        pipe.accept(7);

        assertEquals(Arrays.asList("n7"), sink.received);
    }

    @Test
    void hubBackedChainOfMultipleStagesProducesFinalResult() throws InterruptedException
    {
        PipeActor<Integer,Integer> doubler = actorHub.pipe(i -> i * 2);
        PipeActor<Integer,String> stringify = actorHub.pipe(i -> "v=" + i);
        RecordingActor<String> sink = new RecordingActor<>(actorHub);

        doubler.linkTo(stringify).linkTo(sink);

        doubler.accept(10);
        doubler.accept(20);

        doubler.waitForIdle();
        stringify.waitForIdle();
        sink.waitForIdle();
        actorHub.close(true);

        assertEquals(2, sink.received.size());
        assertTrue(sink.received.contains("v=20"));
        assertTrue(sink.received.contains("v=40"));
    }

    @Test
    void linkToReturnsTheSameNextInstanceForFluentChaining()
    {
        PipeActor<Integer,Integer> a = new PipeActor<>(i -> i);
        PipeActor<Integer,Integer> b = new PipeActor<>(i -> i);

        PipeActor<Integer,Integer> returned = a.linkTo(b);

        assertSame(b, returned);
    }

    @Test
    void linkToRejectsNull()
    {
        PipeActor<Integer,Integer> a = new PipeActor<>(i -> i);
        assertThrows(NullPointerException.class, () -> a.linkTo(null));
    }

    @Test
    void messageIsTransformedEvenWhenNoNextStageIsLinked()
    {
        AtomicInteger calls = new AtomicInteger(0);
        PipeActor<Integer,Integer> pipe = new PipeActor<>(i ->
        {
            calls.incrementAndGet();
            return i;
        });

        pipe.accept(1);
        assertEquals(1, calls.get());
    }

    @Test
    void constructorsRejectNullFunction()
    {
        assertThrows(NullPointerException.class, () -> new PipeActor<Object,Object>((Function<Object,Object>) null));
        assertThrows(NullPointerException.class, () -> new PipeActor<Object,Object>(actorHub, null));
        assertThrows(NullPointerException.class, () -> new PipeActor<Object,Object>(2, 2, (Function<Object,Object>) null));
    }

    @Test
    void exceptionInFunctionIsCapturedByTheExceptionHook()
    {
        RuntimeException boom = new RuntimeException("boom");
        PipeActor<Integer,Integer> pipe = new PipeActor<>(i ->
        {
            throw boom;
        });
        pipe.dryLogger();

        pipe.accept(1);
        assertSame(boom, pipe.getException());
    }
}
