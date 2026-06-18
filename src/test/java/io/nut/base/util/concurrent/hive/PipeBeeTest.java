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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PipeBee}: CPS transform-and-forward semantics,
 * fluent chaining via {@link PipeBee#linkTo}, and exception capture.
 */
class PipeBeeTest
{
    private Hive hive;

    @BeforeEach
    void setUp()
    {
        hive = Hive.hive(2);
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
    void directModeTransformsAndForwardsSynchronously()
    {
        PipeBee<Integer,String> pipe = new PipeBee<>(i -> "n" + i);
        RecordingBee<String> sink = new RecordingBee<>();
        pipe.linkTo(sink);

        assertTrue(pipe.send(7));

        assertEquals(Arrays.asList("n7"), sink.received);
    }

    @Test
    void hiveBackedChainOfMultipleStagesProducesFinalResult() throws InterruptedException
    {
        PipeBee<Integer,Integer> doubler = hive.pipe(i -> i * 2);
        PipeBee<Integer,String> stringify = hive.pipe(i -> "v=" + i);
        RecordingBee<String> sink = new RecordingBee<>(hive);

        doubler.linkTo(stringify).linkTo(sink);

        doubler.send(10);
        doubler.send(20);

        doubler.waitForIdle().shutdown(true).awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, doubler, stringify, sink);

        assertEquals(2, sink.received.size());
        assertTrue(sink.received.contains("v=20"));
        assertTrue(sink.received.contains("v=40"));
    }

    @Test
    void linkToReturnsTheSameNextInstanceForFluentChaining()
    {
        PipeBee<Integer,Integer> a = new PipeBee<>(i -> i);
        PipeBee<Integer,Integer> b = new PipeBee<>(i -> i);

        PipeBee<Integer,Integer> returned = a.linkTo(b);

        assertSame(b, returned);
    }

    @Test
    void linkToRejectsNull()
    {
        PipeBee<Integer,Integer> a = new PipeBee<>(i -> i);
        assertThrows(NullPointerException.class, () -> a.linkTo(null));
    }

    @Test
    void messageIsTransformedEvenWhenNoNextStageIsLinked()
    {
        AtomicInteger calls = new AtomicInteger(0);
        PipeBee<Integer,Integer> pipe = new PipeBee<>(i ->
        {
            calls.incrementAndGet();
            return i;
        });

        assertTrue(pipe.send(1));
        assertEquals(1, calls.get());
    }

    @Test
    void constructorsRejectNullFunction()
    {
        assertThrows(NullPointerException.class, () -> new PipeBee<Object,Object>((Function<Object,Object>) null));
        assertThrows(NullPointerException.class, () -> new PipeBee<Object,Object>(hive, null));
        assertThrows(NullPointerException.class, () -> new PipeBee<Object,Object>(2, (Function<Object,Object>) null));
    }

    @Test
    void exceptionInFunctionIsCapturedByTheExceptionHook()
    {
        RuntimeException boom = new RuntimeException("boom");
        PipeBee<Integer,Integer> pipe = new PipeBee<>(i ->
        {
            throw boom;
        });
        pipe.dryLogger();

        assertFalse(pipe.send(1));
        assertSame(boom, pipe.getException());
    }
}
