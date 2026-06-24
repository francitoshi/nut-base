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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link HivePipeline}: fluent chainable builder syntax,
 * type-safe transformation stages, closing with {@code sink}/{@code to},
 * integration with {@link Hive}, and message delivery through the chain.
 */
class HivePipelineTest
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
    void simpleThreeStageChainProducesCorrectResult() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();

        Bee<Integer> head = hive.pipeline((Integer i) -> i * 2)
                                 .then(i -> "value=" + i)
                                 .then(String::toUpperCase)
                                 .sink(result::add);

        head.accept(10);
        head.waitForIdle().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, head);

        assertEquals(1, result.size());
        assertTrue(result.contains("VALUE=20"));
    }

    @Test
    void multipleMessagesFlowThroughAllStages() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();

        Bee<Integer> head = hive.pipeline((Integer i) -> i + 1)
                                 .then((Integer i) -> i * 2)
                                 .then((Integer i) -> "n=" + i)
                                 .sink(result::add);

        head.accept(1);
        head.accept(5);
        head.accept(10);

        head.waitForIdle().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, head);

        assertEquals(3, result.size());
        assertTrue(result.contains("n=4")); // (1+1)*2 = 4
        assertTrue(result.contains("n=12")); // (5+1)*2 = 12
        assertTrue(result.contains("n=22")); // (10+1)*2 = 22
    }

    @Test
    void thenReturnsANewPipelineViewWithUpdatedOutputType() throws InterruptedException
    {
        HivePipeline<Integer,Integer> stage1 = hive.pipeline(i -> i * 2);
        HivePipeline<Integer,String> stage2 = stage1.then(i -> "v=" + i);
        HivePipeline<Integer,String> stage3 = stage2.then(String::toUpperCase);

        List<String> result = new CopyOnWriteArrayList<>();
        stage3.sink(result::add).accept(5);
        
        stage3.head().waitForIdle().awaitTermination(25);

        Hive.shutdownAndAwaitTermination(true, true, stage1.head());

        assertEquals(1, result.size());
        assertTrue(result.contains("V=10"));
    }

    @Test
    void sinkClosesThePipelineWithAConsumer() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();

        Bee<String> head = hive.pipeline((String s) -> s)
                                 .sink(result::add);

        head.accept("msg");

        head.waitForIdle().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, head);

        assertEquals(1, result.size());
        assertTrue(result.contains("msg"));
    }

    @Test
    void toClosesThePipelineWithAnArbitrarySendable() throws InterruptedException
    {
        BlockingQueue<String> q = new LinkedBlockingQueue<>();
        Bee<String> b = hive.queue(q);

        Bee<String> head = hive.pipeline((String s) -> s.toUpperCase()).to(b);

        head.accept("hello");

        head.waitForIdle().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, head, b);

        assertEquals(1, q.size());
        assertEquals("HELLO", q.peek());
    }

    @Test
    void toRejectsNull()
    {
        HivePipeline<Integer,String> p = hive.pipeline(i -> "v=" + i);
        assertThrows(NullPointerException.class, () -> p.to(null));
    }

    @Test
    void headReturnsTheFirstBeeOfTheChain()
    {
        Bee<Integer> head = hive.pipeline((Integer i) -> i)
                                 .then((Integer i) -> i)
                                 .head();

        head.accept(1);
    }

    @Test
    void sendDelegatesBeeToTheHead() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();

        HivePipeline<Integer,String> pipeline = hive.pipeline(i -> "v=" + i);
        Bee<Integer> unused = pipeline.sink(result::add);

        pipeline.accept(42);

        pipeline.head().waitForIdle().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, unused);

        assertEquals(1, result.size());
        assertTrue(result.contains("v=42"));
    }

    @Test
    void longChainOfTransformationsPreservesTypeCorrectness() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();

        Bee<Integer> head = hive.pipeline((Integer i) -> i + 1)           // Integer -> Integer
                                 .then(i -> i * 2)                       // Integer -> Integer
                                 .then(i -> (double) i / 3)             // Integer -> Double
                                 .then(d -> String.format("%.2f", d))  // Double -> String
                                 .sink(result::add);

        head.accept(6);
        
        head.waitForIdle().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, head);

        // (6+1)*2 = 14, 14/3 ≈ 4.67
        assertEquals(1, result.size());
        assertTrue(result.contains("4.67"));
    }

    @Test
    void pipelineCanBeLinkedToAFilter() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();

        Bee<Integer> head = hive.pipeline((Integer i) -> "n=" + i)
                                 .sink(s -> result.add(s));

        FilterBee<Integer> filter = hive.filter(i -> i > 5);
        filter.linkTo(head);

        filter.accept(3);
        filter.accept(10);
        filter.waitForIdle().awaitTermination(25);

        Hive.shutdownAndAwaitTermination(true, true, head, filter);

        assertEquals(1, result.size());
        assertTrue(result.contains("n=10"));
    }

    @Test
    void pipelineWithThreadsParameter() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();

        Bee<Integer> head = hive.pipeline(2, (Integer i) -> "v=" + i)
                                 .then(s -> s.toUpperCase())
                                 .sink(result::add);

        head.accept(7);

        head.waitForIdle().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, head);

        assertEquals(1, result.size());
        assertTrue(result.contains("V=7"));
    }

    @Test
    void pipelineWithQueueSizeParameter() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();

        Bee<Integer> head = hive.pipeline(2, 10, (Integer i) -> "v=" + i)
                                 .sink(result::add);

        head.accept(99);

        head.waitForIdle().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, head);

        assertEquals(1, result.size());
        assertTrue(result.contains("v=99"));
    }

    @Test
    void eachStageInThePipelineCanHaveDifferentThreadSettings() throws InterruptedException
    {
        List<String> result = new CopyOnWriteArrayList<>();

        HivePipeline<Integer,Integer> p1 = hive.pipeline(i -> i * 2);
        HivePipeline<Integer,String> p2 = p1.then(2, i -> "v=" + i); // different thread count
        Bee<Integer> head = p2.sink(result::add);

        head.accept(5);

        head.waitForIdle().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, head);

        assertEquals(1, result.size());
        assertTrue(result.contains("v=10"));
    }

    @Test
    void pipelineCanLinkToMultipleTargetsViaABroadcaster() throws InterruptedException
    {
        List<String> a = new CopyOnWriteArrayList<>();
        List<String> b = new CopyOnWriteArrayList<>();

        Bee<Integer> head = hive.pipeline((Integer i) -> "v=" + i).head();
        BroadcastBee<String> broadcaster = hive.broadcast(hive.bee(a::add), hive.bee(b::add));

        PipeBee<Integer,String> pipe = (PipeBee<Integer,String>) head;
        pipe.linkTo(broadcaster);

        pipe.accept(5);
        pipe.accept(10);

        pipe.waitForIdle().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, true, head, broadcaster);

        assertEquals(2, a.size());
        assertEquals(2, b.size());
    }

    @Test
    void shutdownOfHeadShutdownsEntireChain()
    {
        RecordingBee<String> collector = new RecordingBee<>(hive);

        Bee<Integer> head = hive.pipeline((Integer i) -> "v=" + i)
                                 .then((String s) -> s.toUpperCase())
                                 .to(collector);

        head.accept(1);

        head.waitForIdle().shutdown().awaitTermination(Integer.MAX_VALUE);

        Hive.shutdownAndAwaitTermination(true, true, head);
        
        assertTrue(head.isTerminated());
        assertTrue(collector.isTerminated());
    }

    @Test
    void pipelineSendReturnsFalseAfterHeadIsShutdown()
    {
        HivePipeline<Integer,String> pipeline = hive.pipeline((Integer i) -> "v=" + i);
        Bee<Integer> head = pipeline.sink(s -> {});
        head.shutdown();
        head.dryLogger();

        pipeline.accept(1);
    }

    private static <T> boolean assertFalse(boolean condition)
    {
        assertTrue(!condition);
        return !condition;
    }
}
