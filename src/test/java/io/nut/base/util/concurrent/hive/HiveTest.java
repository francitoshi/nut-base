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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Hive}: the thread-pool lifecycle, the static
 * factories, {@code add}/{@code execute}, every Bee-factory method
 * ({@code pipe}, {@code bee}, {@code queue}, {@code list}, {@code set},
 * {@code filter}, {@code broadcast}, {@code batch}, {@code pipeline}),
 * and {@code async}/{@code lazy}.
 */
class HiveTest
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
    void coresConstantMatchesAvailableProcessors()
    {
        assertEquals(Runtime.getRuntime().availableProcessors(), Hive.CORES);
    }

    @Test
    void staticFactoryMethodsCreateUsableHives() throws Exception
    {
        runsATaskOn(Hive.hive());
        runsATaskOn(Hive.hive(2));
        runsATaskOn(Hive.hive(2, 2, 2, 1000));
        runsATaskOn(Hive.hive(2, 2, 2, 1000, true));
    }

    private void runsATaskOn(Hive h) throws Exception
    {
        CountDownLatch latch = new CountDownLatch(1);
        h.execute(latch::countDown);
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        h.waitForIdle().shutdown().awaitTermination(1000);
    }

    @Test
    void executeRunsTaskOnThePool() throws InterruptedException
    {
        CountDownLatch latch = new CountDownLatch(1);
        hive.execute(latch::countDown);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void executeRejectsNullTask()
    {
        assertThrows(NullPointerException.class, () -> hive.execute(null));
    }

    @Test
    void addAttachesHiveToMultipleBeesAtOnceAndReturnsThis() throws InterruptedException
    {
        RecordingBee<Integer> b1 = new RecordingBee<>();
        RecordingBee<Integer> b2 = new RecordingBee<>();

        Hive returned = hive.add(b1, b2);
        assertSame(hive, returned);

        b1.accept(1);
        b2.accept(2);
        Hive.shutdownAndAwaitTermination(true, b1, b2);

        assertEquals(Collections.singletonList(1), b1.received);
        assertEquals(Collections.singletonList(2), b2.received);
    }

    @Test
    void addRejectsNullArray()
    {
        assertThrows(NullPointerException.class, () -> hive.add((Bee<?>[]) null));
    }

    @Test
    void pipeFactoryCreatesAttachedTransformingStage() throws InterruptedException
    {
        List<String> sink = new CopyOnWriteArrayList<>();
        PipeBee<Integer,String> stage = hive.pipe(i -> "n" + i);
        stage.linkTo(hive.bee(sink::add));

        stage.accept(5);
        stage.waitForIdle().shutdown().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, stage);

        assertEquals(Collections.singletonList("n5"), sink);
    }

    @Test
    void beeFactoryCreatesAttachedConsumerBee() throws InterruptedException
    {
        List<String> sink = new CopyOnWriteArrayList<>();
        Bee<String> b = hive.bee(sink::add);

        b.accept("hi");
        Hive.shutdownAndAwaitTermination(true, b);

        assertEquals(Collections.singletonList("hi"), sink);
    }

    @Test
    void queueFactoryCreatesAttachedQueueBee() throws InterruptedException
    {
        BlockingQueue<Integer> q = new LinkedBlockingQueue<>();
        Bee<Integer> b = hive.queue(q);

        b.accept(1);
        b.accept(2);
        Hive.shutdownAndAwaitTermination(true, b);

        assertEquals(2, q.size());
        assertEquals(Integer.valueOf(1), q.take());
    }

    @Test
    void listFactoryCreatesAttachedListBee() throws InterruptedException
    {
        List<String> list = new ArrayList<>();
        Bee<String> bee = hive.list(list);

        bee.accept("a");
        bee.accept("b");
        Hive.shutdownAndAwaitTermination(true, bee);

        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    void setFactoryCreatesAttachedSetBee()
    {
        Set<String> s = new HashSet<>();
        Bee<String> b = hive.set(s);

        b.accept("x");
        b.accept("x");
        b.accept("y");
        Hive.shutdownAndAwaitTermination(true, b);

        assertEquals(new HashSet<>(Arrays.asList("x", "y")), new HashSet<>(s));
    }

    @Test
    void filterFactoryCreatesAttachedFilterBee()
    {
        List<Integer> sink = new CopyOnWriteArrayList<>();
        FilterBee<Integer> filter = hive.filter(i -> i > 0);
        filter.linkTo(hive.bee(sink::add));

        filter.accept(-1);
        filter.accept(2);
        filter.waitForIdle().shutdown(true).awaitTermination(1);
        Hive.shutdownAndAwaitTermination(true, filter);

        assertEquals(Collections.singletonList(2), sink);
    }

    @Test
    void broadcastFactoryCreatesAttachedFanOutBeeWithGivenTargets()
    {
        List<String> a = new CopyOnWriteArrayList<>();
        List<String> b = new CopyOnWriteArrayList<>();
        FanOutBee<String> bc = hive.broadcast(hive.bee(a::add), hive.bee(b::add));

        bc.accept("m");

        bc.waitForIdle().shutdown().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, bc);

        assertEquals(Collections.singletonList("m"), a);
        assertEquals(Collections.singletonList("m"), b);
    }

    @Test
    void batchFactoryCreatesAttachedBatchBee()
    {
        List<List<Integer>> sink = new CopyOnWriteArrayList<>();
        BatchBee<Integer> batch = hive.batch(2, 0L);
        batch.linkTo(hive.bee(sink::add));

        batch.accept(1);
        batch.accept(2);
        
        batch.waitForIdle().shutdown().awaitTermination(25);
        Hive.shutdownAndAwaitTermination(true, batch);

        assertEquals(Collections.singletonList(Arrays.asList(1, 2)), sink);
    }

    @Test
    void pipelineFactoryBuildsChainedHeadBee() throws InterruptedException
    {
        List<String> sink = new CopyOnWriteArrayList<>();
        Bee<Integer> head = hive.pipeline((Integer i) -> i + 1)
                                 .then(i -> "v" + i)
                                 .sink(sink::add);

        head.accept(4);
        
        Utils.parkMillis(25);
        Hive.shutdownAndAwaitTermination(true, head);

        assertEquals(Collections.singletonList("v5"), sink);
    }

    @Test
    void shutdownAndAwaitTerminationStopsThePool() throws InterruptedException
    {
        assertFalse(hive.isShutdown());
        hive.shutdown();

        assertTrue(hive.isShutdown());
        assertTrue(hive.awaitTermination(2000));
        assertTrue(hive.isTerminated());
    }

    @Test
    void instanceShutdownAndAwaitTerminationDrainsBeesThenStopsThePool() throws InterruptedException
    {
        RecordingBee<Integer> bee = new RecordingBee<>(hive);
        bee.accept(1);
        bee.accept(2);

        Hive.shutdownAndAwaitTermination(true, bee);
        hive.shutdown().awaitTermination(1);

        assertTrue(bee.isTerminated());
        assertEquals(2, bee.received.size());
        assertTrue(hive.isTerminated());
    }

    @Test
    void corePoolSizeGetterAndSetterWork()
    {
        Hive h = Hive.hive(3);
        assertEquals(3, h.getCorePoolSize());
        assertEquals(3, h.getMaximumPoolSize());

        h.setMaximumPoolSize(10);
        h.setCorePoolSize(5);

        assertEquals(5, h.getCorePoolSize());
        assertEquals(10, h.getMaximumPoolSize());

        h.shutdown();
        try
        {
            h.awaitTermination(1000);
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void closeShutsDownAndAwaitsTermination() throws Exception
    {
        Hive h = Hive.hive(2);
        CountDownLatch latch = new CountDownLatch(1);
        h.execute(latch::countDown);
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        h.close();

        assertTrue(h.isTerminated());
    }
    
    @Test
    void directSendAddsMessageToTheSet()
    {
        Set<String> set = new HashSet<>();
        Bee<String> sb = hive.set(set);

        sb.accept("hello");

        sb.waitForIdle();
        assertEquals(1, set.size());
        assertTrue(set.contains("hello"));
    }

    @Test
    void duplicatesAreRejectedByTheUnderlyingSet()
    {
        Set<String> set = ConcurrentHashMap.newKeySet();
        Bee<String> bee = hive.set(set);

        bee.accept("a");
        bee.accept("a");
        bee.accept("b");

        bee.waitForIdle();
        
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
    }

    @Test
    void iteratorTraversesAllElements() throws InterruptedException
    {
        Set<String> set = ConcurrentHashMap.newKeySet();
        Bee<String> bee = hive.set(set);

        bee.accept("a");
        bee.accept("b");
        bee.accept("c");

        bee.waitForIdle();
        
        Set<String> traversed = new HashSet<>();
        for (String s : set)
        {
            traversed.add(s);
        }
        
        assertEquals(set, traversed);
    }

    @Test
    void proxyHiveCorrectlyDelegatesAllCalls() throws InterruptedException
    {
        ProxyHive proxy = new ProxyHive();
        proxy.setHive(hive);

        List<String> list = new ArrayList<>();
        Bee<String> beeList = proxy.list(list);
        beeList.accept("test-list");
        Hive.shutdownAndAwaitTermination(true, beeList);
        assertEquals(Collections.singletonList("test-list"), list);

        Set<String> set = new HashSet<>();
        Bee<String> beeSet = proxy.set(set);
        beeSet.accept("test-set");
        Hive.shutdownAndAwaitTermination(true, beeSet);
        assertEquals(Collections.singleton("test-set"), set);

        CountDownLatch latch = new CountDownLatch(1);
        proxy.execute(latch::countDown);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}
