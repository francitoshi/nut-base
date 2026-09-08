/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the unified Pub/Sub feature of {@link ActorHub}:
 * <ul>
 *   <li>{@link ActorHub#sub(String, Consumer)} — subscriber registration, returning
 *       a {@link Subscription} and wrapping the consumer in an Actor unless the hub
 *       is synchronous</li>
 *   <li>{@link ActorHub#pub(String)} — publisher creation, returning a
 *       {@link Publisher}</li>
 *   <li>{@link Actor#sub(String)} — fluent self-registration</li>
 * </ul>
 *
 * <p>Deterministic ordering tests run against a synchronous ActorHub (no actors,
 * no pool); async dispatch is tested with a pool-backed ActorHub.
 */
class PubSubTest
{
    /** Pool-backed ActorHub for async dispatch tests. */
    private ActorHub asyncHub;

    /** Synchronous ActorHub (no pool) for deterministic tests. */
    private ActorHub syncHub;

    @BeforeEach
    void setUp()
    {
        asyncHub = new ActorHub();
        syncHub = new ActorHub(0, 0, 0, false, false);
    }

    @AfterEach
    void tearDown()
    {
        asyncHub.close();
        syncHub.close();
    }

    // -------------------------------------------------------------------------
    // Pub/Sub — core fan-out behaviour (synchronous hub)
    // -------------------------------------------------------------------------

    /** A Publisher with no subscribers must not throw and must be a no-op. */
    @Test
    void pub_noSubscribers_noException()
    {
        Publisher<String> pub = syncHub.pub("empty-topic");
        assertDoesNotThrow(() -> pub.accept("hello"));
    }

    /** A single subscriber receives the published message. */
    @Test
    void pub_singleSubscriber_receivesMessage()
    {
        List<String> sink = new ArrayList<>();
        syncHub.<String>sub("t", sink::add);

        syncHub.<String>pub("t").accept("ping");

        assertEquals(Collections.singletonList("ping"), sink);
    }

    /** All registered subscribers receive the same message, in order. */
    @Test
    void pub_multipleSubscribers_allReceiveInOrder()
    {
        List<String> sink1 = new ArrayList<>();
        List<String> sink2 = new ArrayList<>();
        List<String> sink3 = new ArrayList<>();

        syncHub.<String>sub("t", sink1::add);
        syncHub.<String>sub("t", sink2::add);
        syncHub.<String>sub("t", sink3::add);

        syncHub.<String>pub("t").accept("event");

        assertEquals(Collections.singletonList("event"), sink1);
        assertEquals(Collections.singletonList("event"), sink2);
        assertEquals(Collections.singletonList("event"), sink3);
    }

    /** Every publish delivers to all subscribers independently. */
    @Test
    void pub_multiplePublishes_eachDeliveredToAllSubscribers()
    {
        List<Integer> sink1 = new ArrayList<>();
        List<Integer> sink2 = new ArrayList<>();

        syncHub.<Integer>sub("nums", sink1::add);
        syncHub.<Integer>sub("nums", sink2::add);

        Publisher<Integer> pub = syncHub.pub("nums");
        pub.accept(1);
        pub.accept(2);
        pub.accept(3);

        List<Integer> expected = new ArrayList<>();
        expected.add(1);
        expected.add(2);
        expected.add(3);

        assertEquals(expected, sink1);
        assertEquals(expected, sink2);
    }

    /** Tags are independent; publishing to one does not reach another. */
    @Test
    void pub_distinctTopics_noLeakBetweenTopics()
    {
        List<String> sinkA = new ArrayList<>();
        List<String> sinkB = new ArrayList<>();

        syncHub.<String>sub("topicA", sinkA::add);
        syncHub.<String>sub("topicB", sinkB::add);

        syncHub.<String>pub("topicA").accept("only-for-A");

        assertEquals(Collections.singletonList("only-for-A"), sinkA);
        assertTrue(sinkB.isEmpty(), "topicB sink must remain empty");
    }

    /**
     * A subscriber registered AFTER the Publisher is obtained still receives
     * subsequent publishes (live-list semantics).
     */
    @Test
    void pub_subscriberAddedAfterPubCreation_receivesSubsequentMessages()
    {
        Publisher<String> pub = syncHub.pub("live");

        List<String> sink = new ArrayList<>();
        syncHub.<String>sub("live", sink::add);   // registered after pub()

        pub.accept("late");

        assertEquals(Collections.singletonList("late"), sink);
    }

    /** Multiple pub() calls for the same tag return publishers over the same list. */
    @Test
    void pub_sameTopicTwoPubs_shareSubscriberList()
    {
        List<String> sink = new ArrayList<>();
        syncHub.<String>sub("shared", sink::add);

        Publisher<String> pub1 = syncHub.pub("shared");
        Publisher<String> pub2 = syncHub.pub("shared");

        pub1.accept("from-1");
        pub2.accept("from-2");

        List<String> expected = new ArrayList<>();
        expected.add("from-1");
        expected.add("from-2");

        assertEquals(expected, sink);
    }

    /** Registering the same consumer twice produces duplicate deliveries. */
    @Test
    void pub_sameConsumerTwice_receivesTwice()
    {
        List<String> sink = new ArrayList<>();
        Consumer<String> consumer = sink::add;

        syncHub.<String>sub("dup", consumer);
        syncHub.<String>sub("dup", consumer);

        syncHub.<String>pub("dup").accept("x");

        assertEquals(2, sink.size());
        assertEquals("x", sink.get(0));
        assertEquals("x", sink.get(1));
    }

    /** Closing the Subscription removes the subscriber. */
    @Test
    void pub_subscriptionClose_stopsDelivery()
    {
        List<String> sink = new ArrayList<>();
        Subscription<String> subscription = syncHub.<String>sub("t", sink::add);

        syncHub.<String>pub("t").accept("a");
        subscription.close();
        assertTrue(subscription.isClosed());
        syncHub.<String>pub("t").accept("b");

        assertEquals(Collections.singletonList("a"), sink);
    }

    /** Closing a Publisher prevents further publishes. */
    @Test
    void pub_publisherClose_throwsOnPublish()
    {
        List<String> sink = new ArrayList<>();
        syncHub.<String>sub("t", sink::add);
        Publisher<String> p = syncHub.pub("t");
        p.accept("a");
        p.close();
        assertTrue(p.isClosed());
        assertThrows(IllegalStateException.class, () -> p.accept("b"));
        assertEquals(Collections.singletonList("a"), sink);
    }

    // -------------------------------------------------------------------------
    // pub(String, Consumer) and pub(String) — argument validation
    // -------------------------------------------------------------------------

    /** pub(tag, consumer) rejects a null tag. */
    @Test
    void hubPubNullTag_throwsNPE()
    {
        assertThrows(NullPointerException.class, () -> syncHub.sub(null, e -> {}));
    }

    /** pub(tag, consumer) rejects a null consumer. */
    @Test
    void hubPubNullConsumer_throwsNPE()
    {
        assertThrows(NullPointerException.class, () -> syncHub.sub("t", null));
    }

    /** pub(tag) rejects a null tag. */
    @Test
    void hubPubNullTopic_throwsNPE()
    {
        assertThrows(NullPointerException.class, () -> syncHub.pub(null));
    }

    /** pub(tag) always returns a non-null Publisher even for an unknown tag. */
    @Test
    void hubPub_unknownTopic_returnsNonNull()
    {
        assertNotNull(syncHub.pub("brand-new-topic"));
    }

    // -------------------------------------------------------------------------
    // Actor.sub — fluent self-registration
    // -------------------------------------------------------------------------

    /** Actor.sub returns the same Actor instance (fluent chaining). */
    @Test
    void actorSub_returnsSelf()
    {
        List<String> sink = new ArrayList<>();
        Actor<String> actor = syncHub.actor(sink::add);

        Actor<String> returned = actor.sub("fluent");

        assertSame(actor, returned);
    }

    /** An Actor subscribed via sub() receives messages published to that tag. */
    @Test
    void actorSub_receivesPublishedMessages()
    {
        List<String> sink = new ArrayList<>();
        Actor actor = syncHub.actor((Consumer<String>) sink::add).sub("greet");

        syncHub.<String>pub("greet").accept("hello");

        assertEquals(Collections.singletonList("hello"), sink);
    }

    /**
     * Actor.sub can be called multiple times for different tags; the Actor
     * receives messages from all of them.
     */
    @Test
    void actorSub_multipleTopics_receivesFromAll()
    {
        List<String> sink = new ArrayList<>();
        Actor<String> actor = syncHub.actor(sink::add);

        actor.sub("alpha").sub("beta");

        syncHub.<String>pub("alpha").accept("A");
        syncHub.<String>pub("beta").accept("B");

        List<String> expected = new ArrayList<>();
        expected.add("A");
        expected.add("B");

        assertEquals(expected, sink);
    }

    /** Actor.sub throws IllegalStateException when no ActorHub is attached. */
    @Test
    void actorSub_noActorHub_throwsIllegalState()
    {
        Actor<String> detached = new Actor<String>()   // constructed without ActorHub
        {
            @Override
            protected void receive(String m)
            {
            }
        };

        assertThrows(IllegalStateException.class, () -> detached.sub("any"));
    }

    // -------------------------------------------------------------------------
    // Async delivery — pool-backed ActorHub
    // -------------------------------------------------------------------------

    /**
     * Messages published to consumers registered on a pool-backed ActorHub are
     * delivered asynchronously (wrapped in Actors) and all arrive within a
     * reasonable timeout.
     */
    @Test
    void pub_asyncActors_allMessagesDelivered() throws InterruptedException
    {
        int msgCount = 50;
        CountDownLatch latch1 = new CountDownLatch(msgCount);
        CountDownLatch latch2 = new CountDownLatch(msgCount);

        List<Integer> sink1 = new CopyOnWriteArrayList<>();
        List<Integer> sink2 = new CopyOnWriteArrayList<>();

        asyncHub.sub("async", (Consumer<Integer>) i ->
        {
            sink1.add(i);
            latch1.countDown();
        });
        asyncHub.sub("async", (Consumer<Integer>) i ->
        {
            sink2.add(i);
            latch2.countDown();
        });

        Publisher<Integer> pub = asyncHub.pub("async");
        for (int i = 0; i < msgCount; i++)
        {
            pub.accept(i);
        }

        assertTrue(latch1.await(5, TimeUnit.SECONDS), "subscriber-1 did not receive all messages");
        assertTrue(latch2.await(5, TimeUnit.SECONDS), "subscriber-2 did not receive all messages");

        assertEquals(msgCount, sink1.size());
        assertEquals(msgCount, sink2.size());
    }

    /**
     * A subscriber added while publishing is in progress still receives
     * messages published after it was registered (live-list concurrency safety).
     */
    @Test
    void pub_concurrentSubAndPub_noExceptions() throws InterruptedException
    {
        int rounds = 200;
        CountDownLatch done = new CountDownLatch(rounds);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        Publisher<Integer> pub = asyncHub.pub("concurrent");

        Thread publisher = new Thread(() ->
        {
            for (int i = 0; i < rounds; i++)
            {
                try
                {
                    pub.accept(i);
                }
                catch (Throwable t)
                {
                    errors.add(t);
                }
            }
        });

        Thread subscriber = new Thread(() ->
        {
            for (int i = 0; i < rounds; i++)
            {
                asyncHub.sub("concurrent", (Consumer<Integer>) x -> done.countDown());
            }
        });

        publisher.start();
        subscriber.start();
        publisher.join(5_000);
        subscriber.join(5_000);

        assertTrue(errors.isEmpty(), "Unexpected exception during concurrent pub/sub: " + errors);
    }
}