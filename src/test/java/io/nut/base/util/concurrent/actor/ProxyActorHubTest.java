/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ProxyActorHub}.
 */
class ProxyActorHubTest
{
    private ActorHub actorHub;

    @BeforeEach
    void setUp()
    {
        actorHub = ActorHub.hub(4);
    }

    @AfterEach
    void tearDown()
    {
        actorHub.shutdown();
        actorHub.awaitTermination(2000);
    }

    /**
     * A composable unit that owns a {@link SingleActor}, a {@link MultiActor}
     * and the {@link ProxyActorHub} they are created against. The members are
     * initialized in the field declarations, before the constructor runs, so
     * the actors start as pending in the proxy and migrate to the real
     * ActorHub assigned by the constructor.
     */
    static class Proxyble
    {
        final List<String> multiReceived = new CopyOnWriteArrayList<>();

        final ProxyActorHub proxyActorHub = new ProxyActorHub();

        final SingleActor<String> singleActor = new SingleActor<String>(proxyActorHub, 0)
        {
            @Override
            protected void receive(String m)
            {
                multiActor.accept(m);
            }
        };

        final MultiActor<String> multiActor = new MultiActor<String>(proxyActorHub, 2, 0)
        {
            @Override
            protected void receive(String m)
            {
                multiReceived.add(m);
            }
        };

        Proxyble(ActorHub actorHub)
        {
            proxyActorHub.setActorHub(actorHub);
        }
    }

    @Test
    void proxybleActorsMigrateToTheSharedHubAndProcessOnlyTheirOwnMessages()
    {
        Proxyble p1 = new Proxyble(actorHub);
        Proxyble p2 = new Proxyble(actorHub);

        // all four pending actors migrated into the very same real hub
        assertEquals(4, actorHub.actors().size());
        assertTrue(actorHub.actors().contains(p1.singleActor));
        assertTrue(actorHub.actors().contains(p1.multiActor));
        assertTrue(actorHub.actors().contains(p2.singleActor));
        assertTrue(actorHub.actors().contains(p2.multiActor));

        // each proxy now reports the assigned hub's actors
        assertEquals(actorHub.actors(), p1.proxyActorHub.actors());
        assertEquals(actorHub.actors(), p2.proxyActorHub.actors());

        p1.singleActor.accept("p1-single-1");
        p1.singleActor.accept("p1-single-2");
        p1.singleActor.accept("p1-single-3");

        p1.multiActor.accept("p1-multi-1");
        p1.multiActor.accept("p1-multi-2");
        p1.multiActor.accept("p1-multi-3");

        p2.singleActor.accept("p2-single-1");
        p2.singleActor.accept("p2-single-2");

        p2.multiActor.accept("p2-multi-1");
        p2.multiActor.accept("p2-multi-2");

        actorHub.close(true);

        Collections.sort(p1.multiReceived);
        assertEquals(
                Arrays.asList("p1-multi-1", "p1-multi-2", "p1-multi-3",
                        "p1-single-1", "p1-single-2", "p1-single-3"),
                p1.multiReceived);
        Collections.sort(p2.multiReceived);
        assertEquals(
                Arrays.asList("p2-multi-1", "p2-multi-2",
                        "p2-single-1", "p2-single-2"),
                p2.multiReceived);
    }
}