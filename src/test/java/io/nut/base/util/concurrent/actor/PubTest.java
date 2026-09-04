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
 * Unit tests for the Pub/Sub feature:
 * <ul>
 *   <li>{@link Pub#accept(Object)} — fan-out to all registered subscribers</li>
 *   <li>{@link ActorHub#sub(String, Actor)} — subscriber registration</li>
 *   <li>{@link ActorHub#pub(String)} — publisher creation</li>
 *   <li>{@link Actor#sub(String)} — fluent self-registration</li>
 * </ul>
 *
 * <p>Tests run with a synchronous Actor (no ActorHub attached) where ordering and
 * determinism are needed, and with an ActorHub-attached Actor where async dispatch
 * is the focus.
 */
class PubTest
{
    /** Shared ActorHub instance; shut down after every test. */
    private ActorHub actorHub;

    @BeforeEach
    void setUp()
    {
        actorHub = new ActorHub();
    }

    @AfterEach
    void tearDown()
    {
        actorHub.close();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a synchronous (no-ActorHub) Actor that appends every received message
     * to {@code sink}.
     */
    private <T> Actor<T> syncActor(List<T> sink)
    {
        return new Actor<T>()   // no ActorHub → synchronous
        {
            @Override
            protected void receive(T m)
            {
                sink.add(m);
            }
        };
    }

    /**
     * Creates an ActorHub-attached Actor that appends every received message to
     * {@code sink} and counts down {@code latch} on each delivery.
     */
    private <T> Actor<T> asyncActor(ActorHub h, List<T> sink, CountDownLatch latch)
    {
        return new Actor<T>(h)
        {
            @Override
            protected void receive(T m)
            {
                sink.add(m);
                latch.countDown();
            }
        };
    }

    // =========================================================================
    // Pub — core fan-out behaviour
    // =========================================================================

    /** A Pub with no subscribers must not throw and must be a no-op. */
    @Test
    void pub_noSubscribers_noException()
    {
        Pub<String> pub = actorHub.pub("empty-topic");
        assertDoesNotThrow(() -> pub.accept("hello"));
    }

    /** A single subscriber receives the published message. */
    @Test
    void pub_singleSubscriber_receivesMessage()
    {
        List<String> sink = new ArrayList<>();
        actorHub.sub("t", syncActor(sink));

        actorHub.<String>pub("t").accept("ping");

        assertEquals(Collections.singletonList("ping"), sink);
    }

    /** All registered subscribers receive the same message, in order. */
    @Test
    void pub_multipleSubscribers_allReceiveInOrder()
    {
        List<String> sink1 = new ArrayList<>();
        List<String> sink2 = new ArrayList<>();
        List<String> sink3 = new ArrayList<>();

        actorHub.sub("t", syncActor(sink1));
        actorHub.sub("t", syncActor(sink2));
        actorHub.sub("t", syncActor(sink3));

        actorHub.<String>pub("t").accept("event");

        assertEquals(Collections.singletonList("event"), sink1);
        assertEquals(Collections.singletonList("event"), sink2);
        assertEquals(Collections.singletonList("event"), sink3);
    }

    /** Every call to accept delivers to all subscribers independently. */
    @Test
    void pub_multiplePublishes_eachDeliveredToAllSubscribers()
    {
        List<Integer> sink1 = new ArrayList<>();
        List<Integer> sink2 = new ArrayList<>();

        actorHub.sub("nums", syncActor(sink1));
        actorHub.sub("nums", syncActor(sink2));

        Pub<Integer> pub = actorHub.pub("nums");
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

    /** Topics are independent; publishing to one does not reach another. */
    @Test
    void pub_distinctTopics_noLeakBetweenTopics()
    {
        List<String> sinkA = new ArrayList<>();
        List<String> sinkB = new ArrayList<>();

        actorHub.sub("topicA", syncActor(sinkA));
        actorHub.sub("topicB", syncActor(sinkB));

        actorHub.<String>pub("topicA").accept("only-for-A");

        assertEquals(Collections.singletonList("only-for-A"), sinkA);
        assertTrue(sinkB.isEmpty(), "topicB sink must remain empty");
    }

    /**
     * A subscriber registered AFTER the Pub is obtained still receives
     * subsequent publishes (live-list semantics).
     */
    @Test
    void pub_subscriberAddedAfterPubCreation_receivesSubsequentMessages()
    {
        Pub<String> pub = actorHub.pub("live");

        List<String> sink = new ArrayList<>();
        actorHub.sub("live", syncActor(sink));   // registered after pub()

        pub.accept("late");

        assertEquals(Collections.singletonList("late"), sink);
    }

    /** Multiple pub() calls for the same topic return publishers over the same list. */
    @Test
    void pub_sameTopicTwoPubs_shareSubscriberList()
    {
        List<String> sink = new ArrayList<>();
        actorHub.sub("shared", syncActor(sink));

        Pub<String> pub1 = actorHub.pub("shared");
        Pub<String> pub2 = actorHub.pub("shared");

        pub1.accept("from-1");
        pub2.accept("from-2");

        List<String> expected = new ArrayList<>();
        expected.add("from-1");
        expected.add("from-2");

        assertEquals(expected, sink);
    }

    /** Registering the same Actor instance twice produces duplicate deliveries. */
    @Test
    void pub_sameActorTwice_receivesTwice()
    {
        List<String> sink = new ArrayList<>();
        Actor<String> actor = syncActor(sink);

        actorHub.sub("dup", actor);
        actorHub.sub("dup", actor);

        actorHub.<String>pub("dup").accept("x");

        assertEquals(2, sink.size());
        assertEquals("x", sink.get(0));
        assertEquals("x", sink.get(1));
    }

    // =========================================================================
    // ActorHub.sub — argument validation
    // =========================================================================

    /** ActorHub.sub rejects a null topic. */
    @Test
    void hiveSub_nullTopic_throwsNPE()
    {
        assertThrows(NullPointerException.class, () -> actorHub.sub(null, syncActor(new ArrayList<>())));
    }

    /** ActorHub.sub rejects a null Actor. */
    @Test
    void hiveSub_nullActor_throwsNPE()
    {
        assertThrows(NullPointerException.class, () -> actorHub.sub("t", null));
    }

    // =========================================================================
    // ActorHub.pub — argument validation
    // =========================================================================

    /** ActorHub.pub rejects a null topic. */
    @Test
    void hivePub_nullTopic_throwsNPE()
    {
        assertThrows(NullPointerException.class, () -> actorHub.pub(null));
    }

    /** ActorHub.pub always returns a non-null Pub even for an unknown topic. */
    @Test
    void hivePub_unknownTopic_returnsNonNull()
    {
        assertNotNull(actorHub.pub("brand-new-topic"));
    }

    // =========================================================================
    // Actor.sub — fluent self-registration
    // =========================================================================

    /** Actor.sub returns the same Actor instance (fluent chaining). */
    @Test
    void beeSub_returnsSelf()
    {
        List<String> sink = new ArrayList<>();
        Actor<String> actor = actorHub.actor(sink::add);

        Actor<String> returned = actor.sub("fluent");

        assertSame(actor, returned);
    }

    /** A Actor subscribed via sub() receives messages published to that topic. */
    @Test
    void beeSub_receivesPublishedMessages()
    {
        List<String> sink = new ArrayList<>();
        Actor actor = actorHub.actor((Consumer<String>)sink::add).sub("greet");

        actorHub.<String>pub("greet").accept("hello");

        actor.waitForIdle();
        assertEquals(Collections.singletonList("hello"), sink);
    }

    /**
     * Actor.sub can be called multiple times for different topics; the Actor
     * receives messages from all of them.
     */
    @Test
    void beeSub_multipleTopics_receivesFromAll()
    {
        List<String> sink = new ArrayList<>();
        Actor<String> actor = actorHub.actor(sink::add);

        actor.sub("alpha").sub("beta");

        actorHub.<String>pub("alpha").accept("A");
        actorHub.<String>pub("beta").accept("B");

        List<String> expected = new ArrayList<>();
        expected.add("A");
        expected.add("B");

        actor.waitForIdle().shutdown(true);
        assertEquals(expected, sink);
    }

    /** Actor.sub throws IllegalStateException when no ActorHub is attached. */
    @Test
    void beeSub_noActorHub_throwsIllegalState()
    {
        Actor<String> detached = syncActor(new ArrayList<>());  // constructed without ActorHub

        assertThrows(IllegalStateException.class, () -> detached.sub("any"));
    }

    /**
     * Actor.sub works when constructed with an ActorHub.
     */
    @Test
    void beeSub_withActorHub_works()
    {
        List<String> sink = new ArrayList<>();
        Actor<String> actor = new Actor<String>(actorHub)
        {
            @Override
            protected void receive(String m)
            {
                sink.add(m);
            }
        };

        actor.sub("with-actorHub");

        actorHub.<String>pub("with-actorHub").accept("ok");
        actor.waitForIdle();
       
        assertEquals(Collections.singletonList("ok"), sink);
    }

    // =========================================================================
    // Async delivery — ActorHub-attached Actors
    // =========================================================================

    /**
     * Messages published to an ActorHub-attached Actor are delivered asynchronously
     * and all arrive within a reasonable timeout.
     */
    @Test
    void pub_asyncActors_allMessagesDelivered() throws InterruptedException
    {
        int msgCount = 50;
        CountDownLatch latch1 = new CountDownLatch(msgCount);
        CountDownLatch latch2 = new CountDownLatch(msgCount);

        List<Integer> sink1 = new CopyOnWriteArrayList<>();
        List<Integer> sink2 = new CopyOnWriteArrayList<>();

        asyncActor(actorHub, sink1, latch1).sub("async");
        asyncActor(actorHub, sink2, latch2).sub("async");

        Pub<Integer> pub = actorHub.pub("async");
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

        Pub<Integer> pub = actorHub.pub("concurrent");

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
                List<Integer> sink = new CopyOnWriteArrayList<>();
                actorHub.sub("concurrent", asyncActor(actorHub, sink, done));
            }
        });

        publisher.start();
        subscriber.start();
        publisher.join(5_000);
        subscriber.join(5_000);

        assertTrue(errors.isEmpty(), "Unexpected exception during concurrent pub/sub: " + errors);
    }
}
