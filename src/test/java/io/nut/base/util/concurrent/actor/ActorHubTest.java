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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ActorHub}: the thread-pool lifecycle, the static
 * factories, {@code add}/{@code execute}, every Actor-factory method
 * ({@code pipe}, {@code actor}, {@code queue}, {@code list}, {@code set},
 * {@code filter}, {@code broadcast}, {@code batch}, {@code pipeline}),
 * and {@code async}/{@code lazy}.
 */
class ActorHubTest
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
    void coresConstantMatchesAvailableProcessors()
    {
        assertEquals(Runtime.getRuntime().availableProcessors(), ActorHub.CORES);
    }

    @Test
    void staticFactoryMethodsCreateUsableActorHubs() throws Exception
    {
        runsATaskOn(ActorHub.actorHub());
        runsATaskOn(ActorHub.actorHub(2));
        runsATaskOn(ActorHub.actorHub(2, 2, 1000));
        runsATaskOn(ActorHub.actorHub(2, 2, 1000, true));
    }

    private void runsATaskOn(ActorHub h) throws Exception
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
        actorHub.execute(latch::countDown);
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void executeRejectsNullTask()
    {
        assertThrows(NullPointerException.class, () -> actorHub.execute(null));
    }

    @Test
    void pipeFactoryCreatesAttachedTransformingStage() throws InterruptedException
    {
        List<String> sink = new CopyOnWriteArrayList<>();
        PipeActor<Integer,String> stage = actorHub.pipe(i -> "n" + i);
        stage.linkTo(actorHub.actor(sink::add));

        stage.accept(5);
        stage.waitForIdle().shutdown().awaitTermination(25);
        actorHub.close(true);

        assertEquals(Collections.singletonList("n5"), sink);
    }

    @Test
    void beeFactoryCreatesAttachedConsumerActor() throws InterruptedException
    {
        List<String> sink = new CopyOnWriteArrayList<>();
        Actor<String> b = actorHub.actor(sink::add);

        b.accept("hi");
        actorHub.close(true);

        assertEquals(Collections.singletonList("hi"), sink);
    }

    @Test
    void queueFactoryCreatesAttachedQueueActor() throws InterruptedException
    {
        BlockingQueue<Integer> q = new LinkedBlockingQueue<>();
        Actor<Integer> b = actorHub.queue(q);

        b.accept(1);
        b.accept(2);
        actorHub.close(true);

        assertEquals(2, q.size());
        assertEquals(Integer.valueOf(1), q.take());
    }

    @Test
    void listFactoryCreatesAttachedListActor() throws InterruptedException
    {
        List<String> list = new ArrayList<>();
        Actor<String> actor = actorHub.list(list);

        actor.accept("a");
        actor.accept("b");
        actorHub.close(true);

        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    void setFactoryCreatesAttachedSetActor()
    {
        Set<String> s = new HashSet<>();
        Actor<String> b = actorHub.set(s);

        b.accept("x");
        b.accept("x");
        b.accept("y");
        actorHub.close(true);

        assertEquals(new HashSet<>(Arrays.asList("x", "y")), new HashSet<>(s));
    }

    @Test
    void filterFactoryCreatesAttachedFilterActor()
    {
        List<Integer> sink = new CopyOnWriteArrayList<>();
        FilterActor<Integer> filter = actorHub.filter(i -> i > 0);
        filter.linkTo(actorHub.actor(sink::add));

        filter.accept(-1);
        filter.accept(2);
        filter.waitForIdle().shutdown(true).awaitTermination(1);
        actorHub.close(true);

        assertEquals(Collections.singletonList(2), sink);
    }

    @Test
    void broadcastFactoryCreatesAttachedFanOutActorWithGivenTargets()
    {
        List<String> a = new CopyOnWriteArrayList<>();
        List<String> b = new CopyOnWriteArrayList<>();
        FanOutActor<String> bc = actorHub.broadcast(actorHub.actor(a::add), actorHub.actor(b::add));

        bc.accept("m");

        bc.waitForIdle().shutdown().awaitTermination(25);
        actorHub.close(true);

        assertEquals(Collections.singletonList("m"), a);
        assertEquals(Collections.singletonList("m"), b);
    }

    @Test
    void batchFactoryCreatesAttachedBatchActor()
    {
        List<List<Integer>> sink = new CopyOnWriteArrayList<>();
        BatchActor<Integer> batch = actorHub.batch(2, 0L);
        batch.linkTo(actorHub.actor(sink::add));

        batch.accept(1);
        batch.accept(2);
        
        batch.waitForIdle().shutdown().awaitTermination(25);
        actorHub.close(true);

        assertEquals(Collections.singletonList(Arrays.asList(1, 2)), sink);
    }

    @Test
    void pipelineFactoryBuildsChainedHeadActor() throws InterruptedException
    {
        List<String> sink = new CopyOnWriteArrayList<>();
        Actor<Integer> head = actorHub.pipeline((Integer i) -> i + 1)
                                 .then(i -> "v" + i)
                                 .sink(sink::add);

        head.accept(4);
        
        Utils.parkMillis(25);
        actorHub.close(true);

        assertEquals(Collections.singletonList("v5"), sink);
    }

    @Test
    void shutdownStopsThePool() throws InterruptedException
    {
        assertFalse(actorHub.isShutdown());
        actorHub.shutdown();

        assertTrue(actorHub.isShutdown());
        assertTrue(actorHub.awaitTermination(2000));
        assertTrue(actorHub.isTerminated());
    }

    @Test
    void closeDrainsActorsThenStopsThePool() throws InterruptedException
    {
        RecordingActor<Integer> actor = new RecordingActor<>(actorHub);
        actor.accept(1);
        actor.accept(2);

        actorHub.close(true);
        actorHub.shutdown().awaitTermination(1);

        assertTrue(actor.isTerminated());
        assertEquals(2, actor.received.size());
        assertTrue(actorHub.isTerminated());
    }

    @Test
    void beesListTracksActiveNonSynchronousActorsAndRemovesOnShutdown()
    {
        RecordingActor<Integer> async1 = new RecordingActor<>(actorHub);
        RecordingActor<Integer> async2 = new RecordingActor<>(actorHub);
        RecordingActor<Integer> sync = new RecordingActor<>(actorHub, 0, 0);

        assertTrue(actorHub.actors().contains(async1));
        assertTrue(actorHub.actors().contains(async2));
        assertFalse(actorHub.actors().contains(sync));

        async1.shutdown().awaitTermination(50);

        assertFalse(actorHub.actors().contains(async1));
        assertTrue(actorHub.actors().contains(async2));

        async2.shutdown().awaitTermination(50);
        assertFalse(actorHub.actors().contains(async2));
    }

    @Test
    void poolSizeGetterAndSetterWork()
    {
        ActorHub h = ActorHub.actorHub(3);
        assertEquals(3, h.getCorePoolSize());
        assertEquals(3, h.getMaximumPoolSize());

        h.setPoolSize(5);

        assertEquals(5, h.getCorePoolSize());
        assertEquals(5, h.getMaximumPoolSize());

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
        ActorHub h = ActorHub.actorHub(2);
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
        Actor<String> sb = actorHub.set(set);

        sb.accept("hello");

        sb.waitForIdle();
        assertEquals(1, set.size());
        assertTrue(set.contains("hello"));
    }

    @Test
    void duplicatesAreRejectedByTheUnderlyingSet()
    {
        Set<String> set = ConcurrentHashMap.newKeySet();
        Actor<String> actor = actorHub.set(set);

        actor.accept("a");
        actor.accept("a");
        actor.accept("b");

        actor.waitForIdle();
        
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
    }

    @Test
    void iteratorTraversesAllElements() throws InterruptedException
    {
        Set<String> set = ConcurrentHashMap.newKeySet();
        Actor<String> actor = actorHub.set(set);

        actor.accept("a");
        actor.accept("b");
        actor.accept("c");

        actor.waitForIdle();
        
        Set<String> traversed = new HashSet<>();
        for (String s : set)
        {
            traversed.add(s);
        }
        
        assertEquals(set, traversed);
    }

    @Test
    void proxyActorHubCorrectlyDelegatesAllCalls() throws InterruptedException
    {
        ProxyActorHub proxy = new ProxyActorHub();
        proxy.setActorHub(actorHub);

        List<String> list = new ArrayList<>();
        Actor<String> beeList = proxy.list(list);
        beeList.accept("test-list");

        Set<String> set = new HashSet<>();
        Actor<String> beeSet = proxy.set(set);
        beeSet.accept("test-set");

        CountDownLatch latch = new CountDownLatch(1);
        proxy.execute(latch::countDown);
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        actorHub.close(true);

        assertEquals(Collections.singletonList("test-list"), list);
        assertEquals(Collections.singleton("test-set"), set);
    }

    @Test
    void proxyActorHubAccumulatesNoActorHubAndMigratesOnSetActorHub()
    {
        ProxyActorHub proxy = new ProxyActorHub();

        List<String> list1 = new ArrayList<>();
        Actor<String> bee1 = proxy.list(list1);
        List<String> list2 = new ArrayList<>();
        Actor<String> bee2 = proxy.list(list2);

        assertTrue(proxy.actors().contains(bee1));
        assertTrue(proxy.actors().contains(bee2));
        assertFalse(actorHub.actors().contains(bee1));
        assertFalse(actorHub.actors().contains(bee2));

        proxy.setActorHub(actorHub);

        assertTrue(actorHub.actors().contains(bee1));
        assertTrue(actorHub.actors().contains(bee2));
        assertTrue(proxy.actors().contains(bee1));
        assertTrue(proxy.actors().contains(bee2));

        actorHub.close(true);
    }
}
