/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.actor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression guard for the idempotency of {@link Publisher#close()} and
 * {@link Subscription#close()}: both must release their registry reference
 * exactly once, even when several threads close concurrently, so the backing
 * {@code PubSub} entry's publisher count can never go negative (which would
 * keep a fully abandoned entry pinned in the registry forever).
 * <p>
 * Note on scope: the check-then-act window this guards against is a few
 * instructions wide and did not manifest in tens of thousands of racing
 * close() attempts during development; the tests below therefore document the
 * contract and stress the happy path. The real-world guarantee is structural:
 * the release happens under a {@code closed.compareAndSet(false, true)}, which
 * makes a double release impossible rather than merely unlikely.
 */
class PublisherSubscriptionCloseRaceTest
{
    private ActorHub hub;
    private ExecutorService executor;

    @BeforeEach
    void freshHub()
    {
        // synchronous hub: sub(tag, consumer) registers the consumer directly,
        // so the observation is free of extra actor bookkeeping
        this.hub = new ActorHub(0, 0, 0);
        this.executor = Executors.newFixedThreadPool(16);
    }

    @AfterEach
    void closeHub()
    {
        executor.shutdownNow();
        hub.shutdown();
    }

    /**
     * The registry count is not part of the public API, but the correctness of
     * close() is exactly what it must be validated against: racing closers
     * release the reference once, the count lands on zero, and the empty entry
     * leaves the registry.
     */
    private static int publisherCount(ActorHub hub, String tag) throws Exception
    {
        java.lang.reflect.Field registryField = ActorHub.class.getDeclaredField("pubSubRegistry");
        registryField.setAccessible(true);
        ConcurrentHashMap<String, ?> registry = (ConcurrentHashMap<String, ?>) registryField.get(hub);
        Object entry = registry.get(tag);
        if (entry == null)
        {
            return 0;
        }
        java.lang.reflect.Field countField = PubSub.class.getDeclaredField("publisherCount");
        countField.setAccessible(true);
        return countField.getInt(entry);
    }

    private static boolean registryContains(ActorHub hub, String tag) throws Exception
    {
        java.lang.reflect.Field registryField = ActorHub.class.getDeclaredField("pubSubRegistry");
        registryField.setAccessible(true);
        ConcurrentHashMap<String, ?> registry = (ConcurrentHashMap<String, ?>) registryField.get(hub);
        return registry.containsKey(tag);
    }

    @Test
    void racingPublisherCloseReleasesExactlyOnce() throws Exception
    {
        // the double-release is only observable while a subscriber remains
        // (the first release would otherwise already remove the entry), so the
        // entry's cleanup after the subscriber leaves is the symptom
        int threads = 16;

        for (int trial = 0; trial < 200; trial++)
        {
            String tag = "t" + trial;
            AtomicInteger received = new AtomicInteger();
            Subscription<Object> sub = hub.sub(tag, event -> received.incrementAndGet());
            Publisher<Object> publisher = hub.pub(tag);

            CyclicBarrier barrier = new CyclicBarrier(threads);
            List<Future<Void>> closers = new ArrayList<>();
            for (int t = 0; t < threads; t++)
            {
                final Publisher<Object> p = publisher;
                closers.add(executor.submit(() ->
                {
                    barrier.await();
                    p.close();
                    return null;
                }));
            }
            for (Future<Void> closer : closers)
            {
                closer.get(5, java.util.concurrent.TimeUnit.SECONDS);
            }

            assertTrue(publisher.isClosed(), "trial " + trial + ": publisher must end closed");
            assertEquals(0, publisherCount(hub, tag),
                    "trial " + trial + ": racing closes released the reference more than once");

            sub.close();
            assertFalse(registryContains(hub, tag),
                    "trial " + trial + ": closed and empty entry must leave the registry");

            // the tag is reusable afterwards and starts at a clean state
            assertEquals(0, publisherCount(hub, tag));
        }
    }

    @Test
    void doubleSubscriptionCloseRemovesSubscriberOnce() throws Exception
    {
        String tag = "sub-race";
        AtomicInteger received = new AtomicInteger(0);

        Subscription<Object> sub = hub.sub(tag, event -> received.incrementAndGet());
        Publisher<Object> publisher = hub.pub(tag);

        // close down the race window with many racing closers
        int threads = 16;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        List<Future<Void>> closers = new ArrayList<>();
        for (int t = 0; t < threads; t++)
        {
            final Subscription<Object> s = sub;
            closers.add(executor.submit(() ->
            {
                barrier.await();
                s.close();
                return null;
            }));
        }
        for (Future<Void> closer : closers)
        {
            closer.get(5, java.util.concurrent.TimeUnit.SECONDS);
        }

        assertTrue(sub.isClosed());
        assertTrue(registryContains(hub, tag));

        publisher.close();
        assertFalse(registryContains(hub, tag), "empty entry must leave the registry");
    }
}
