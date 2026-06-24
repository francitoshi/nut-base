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
 *   <li>{@link Hive#sub(String, Bee)} — subscriber registration</li>
 *   <li>{@link Hive#pub(String)} — publisher creation</li>
 *   <li>{@link Bee#sub(String)} — fluent self-registration</li>
 * </ul>
 *
 * <p>Tests run with a synchronous Bee (no Hive attached) where ordering and
 * determinism are needed, and with a Hive-attached Bee where async dispatch
 * is the focus.
 */
class PubTest
{
    /** Shared Hive instance; shut down after every test. */
    private Hive hive;

    @BeforeEach
    void setUp()
    {
        hive = new Hive();
    }

    @AfterEach
    void tearDown()
    {
        hive.close();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a synchronous (no-Hive) Bee that appends every received message
     * to {@code sink}.
     */
    private <T> Bee<T> syncBee(List<T> sink)
    {
        return new Bee<T>()   // no Hive → synchronous
        {
            @Override
            protected void receive(T m)
            {
                sink.add(m);
            }
        };
    }

    /**
     * Creates a Hive-attached Bee that appends every received message to
     * {@code sink} and counts down {@code latch} on each delivery.
     */
    private <T> Bee<T> asyncBee(Hive h, List<T> sink, CountDownLatch latch)
    {
        return new Bee<T>(h)
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
        Pub<String> pub = hive.pub("empty-topic");
        assertDoesNotThrow(() -> pub.accept("hello"));
    }

    /** A single subscriber receives the published message. */
    @Test
    void pub_singleSubscriber_receivesMessage()
    {
        List<String> sink = new ArrayList<>();
        hive.sub("t", syncBee(sink));

        hive.<String>pub("t").accept("ping");

        assertEquals(Collections.singletonList("ping"), sink);
    }

    /** All registered subscribers receive the same message, in order. */
    @Test
    void pub_multipleSubscribers_allReceiveInOrder()
    {
        List<String> sink1 = new ArrayList<>();
        List<String> sink2 = new ArrayList<>();
        List<String> sink3 = new ArrayList<>();

        hive.sub("t", syncBee(sink1));
        hive.sub("t", syncBee(sink2));
        hive.sub("t", syncBee(sink3));

        hive.<String>pub("t").accept("event");

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

        hive.sub("nums", syncBee(sink1));
        hive.sub("nums", syncBee(sink2));

        Pub<Integer> pub = hive.pub("nums");
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

        hive.sub("topicA", syncBee(sinkA));
        hive.sub("topicB", syncBee(sinkB));

        hive.<String>pub("topicA").accept("only-for-A");

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
        Pub<String> pub = hive.pub("live");

        List<String> sink = new ArrayList<>();
        hive.sub("live", syncBee(sink));   // registered after pub()

        pub.accept("late");

        assertEquals(Collections.singletonList("late"), sink);
    }

    /** Multiple pub() calls for the same topic return publishers over the same list. */
    @Test
    void pub_sameTopicTwoPubs_shareSubscriberList()
    {
        List<String> sink = new ArrayList<>();
        hive.sub("shared", syncBee(sink));

        Pub<String> pub1 = hive.pub("shared");
        Pub<String> pub2 = hive.pub("shared");

        pub1.accept("from-1");
        pub2.accept("from-2");

        List<String> expected = new ArrayList<>();
        expected.add("from-1");
        expected.add("from-2");

        assertEquals(expected, sink);
    }

    /** Registering the same Bee instance twice produces duplicate deliveries. */
    @Test
    void pub_sameBeeTwice_receivesTwice()
    {
        List<String> sink = new ArrayList<>();
        Bee<String> bee = syncBee(sink);

        hive.sub("dup", bee);
        hive.sub("dup", bee);

        hive.<String>pub("dup").accept("x");

        assertEquals(2, sink.size());
        assertEquals("x", sink.get(0));
        assertEquals("x", sink.get(1));
    }

    // =========================================================================
    // Hive.sub — argument validation
    // =========================================================================

    /** Hive.sub rejects a null topic. */
    @Test
    void hiveSub_nullTopic_throwsNPE()
    {
        assertThrows(NullPointerException.class, () -> hive.sub(null, syncBee(new ArrayList<>())));
    }

    /** Hive.sub rejects a null Bee. */
    @Test
    void hiveSub_nullBee_throwsNPE()
    {
        assertThrows(NullPointerException.class, () -> hive.sub("t", null));
    }

    // =========================================================================
    // Hive.pub — argument validation
    // =========================================================================

    /** Hive.pub rejects a null topic. */
    @Test
    void hivePub_nullTopic_throwsNPE()
    {
        assertThrows(NullPointerException.class, () -> hive.pub(null));
    }

    /** Hive.pub always returns a non-null Pub even for an unknown topic. */
    @Test
    void hivePub_unknownTopic_returnsNonNull()
    {
        assertNotNull(hive.pub("brand-new-topic"));
    }

    // =========================================================================
    // Bee.sub — fluent self-registration
    // =========================================================================

    /** Bee.sub returns the same Bee instance (fluent chaining). */
    @Test
    void beeSub_returnsSelf()
    {
        List<String> sink = new ArrayList<>();
        Bee<String> bee = hive.bee(sink::add);

        Bee<String> returned = bee.sub("fluent");

        assertSame(bee, returned);
    }

    /** A Bee subscribed via sub() receives messages published to that topic. */
    @Test
    void beeSub_receivesPublishedMessages()
    {
        List<String> sink = new ArrayList<>();
        Bee bee = hive.bee((Consumer<String>)sink::add).sub("greet");

        hive.<String>pub("greet").accept("hello");

        bee.waitForIdle();
        assertEquals(Collections.singletonList("hello"), sink);
    }

    /**
     * Bee.sub can be called multiple times for different topics; the Bee
     * receives messages from all of them.
     */
    @Test
    void beeSub_multipleTopics_receivesFromAll()
    {
        List<String> sink = new ArrayList<>();
        Bee<String> bee = hive.bee(sink::add);

        bee.sub("alpha").sub("beta");

        hive.<String>pub("alpha").accept("A");
        hive.<String>pub("beta").accept("B");

        List<String> expected = new ArrayList<>();
        expected.add("A");
        expected.add("B");

        bee.waitForIdle().shutdown(true);
        assertEquals(expected, sink);
    }

    /** Bee.sub throws IllegalStateException when no Hive is attached. */
    @Test
    void beeSub_noHive_throwsIllegalState()
    {
        Bee<String> detached = syncBee(new ArrayList<>());  // constructed without Hive

        assertThrows(IllegalStateException.class, () -> detached.sub("any"));
    }

    /**
     * Bee.sub works after a Hive is attached via setHive().
     */
    @Test
    void beeSub_afterSetHive_works()
    {
        List<String> sink = new ArrayList<>();
        Bee<String> bee = syncBee(sink);    // no Hive initially

        bee.setHive(hive);                  // attach Hive later
        bee.sub("late-attach");

        hive.<String>pub("late-attach").accept("ok");
        bee.waitForIdle();
       
        assertEquals(Collections.singletonList("ok"), sink);
    }

    // =========================================================================
    // Async delivery — Hive-attached Bees
    // =========================================================================

    /**
     * Messages published to a Hive-attached Bee are delivered asynchronously
     * and all arrive within a reasonable timeout.
     */
    @Test
    void pub_asyncBees_allMessagesDelivered() throws InterruptedException
    {
        int msgCount = 50;
        CountDownLatch latch1 = new CountDownLatch(msgCount);
        CountDownLatch latch2 = new CountDownLatch(msgCount);

        List<Integer> sink1 = new CopyOnWriteArrayList<>();
        List<Integer> sink2 = new CopyOnWriteArrayList<>();

        asyncBee(hive, sink1, latch1).sub("async");
        asyncBee(hive, sink2, latch2).sub("async");

        Pub<Integer> pub = hive.pub("async");
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

        Pub<Integer> pub = hive.pub("concurrent");

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
                hive.sub("concurrent", asyncBee(hive, sink, done));
            }
        });

        publisher.start();
        subscriber.start();
        publisher.join(5_000);
        subscriber.join(5_000);

        assertTrue(errors.isEmpty(), "Unexpected exception during concurrent pub/sub: " + errors);
    }
}
