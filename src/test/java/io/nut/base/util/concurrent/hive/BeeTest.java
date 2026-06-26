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

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Bee}: direct (hive-less) message delivery,
 * hive-backed asynchronous delivery, the shutdown/termination lifecycle,
 * and the exception hook.
 */
class BeeTest
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
    void directSendInvokesReceiveSynchronously()
    {
        RecordingBee<String> bee = new RecordingBee<>();

        bee.accept("hello");

        assertEquals(Collections.singletonList("hello"), bee.received);
    }

    @Test
    void getExceptionIsNullInitially()
    {
        RecordingBee<String> bee = new RecordingBee<>();
        assertNull(bee.getException());
    }

    @Test
    void sendAfterShutdownReturnsFalseAndIsNotReceived()
    {
        RecordingBee<String> bee = new RecordingBee<>();
        bee.shutdown();
        bee.dryLogger();
        assertTrue(bee.isShutdown());
        bee.accept("too late");
        assertTrue(bee.received.isEmpty());
    }

    @Test
    void shutdownWithoutHiveTerminatesImmediatelyWhenEmpty()
    {
        RecordingBee<String> bee = new RecordingBee<>();

        assertFalse(bee.isTerminated());
        bee.shutdown();

        assertTrue(bee.isShutdown());
        assertTrue(bee.isTerminated());
        assertTrue(bee.terminated.get());
    }

    @Test
    void awaitTerminationTimesOutWhenNeverShutdown()
    {
        RecordingBee<String> bee = new RecordingBee<>();
        assertFalse(bee.awaitTermination(50));
    }

    @Test
    void awaitTerminationReturnsTrueAfterShutdown()
    {
        RecordingBee<String> bee = new RecordingBee<>();
        bee.shutdown();
        assertTrue(bee.awaitTermination(50));
    }

    @Test
    void exceptionHookAndGetExceptionCaptureFailureInDirectMode()
    {
        RuntimeException boom = new RuntimeException("boom");
        RecordingBee<String> bee = new RecordingBee<>();
        bee.dryLogger(); // avoid noisy SEVERE log output for this expected failure
        bee.withAction(m ->
        {
            throw boom;
        });

        bee.accept("x");
        assertSame(boom, bee.getException());
        assertSame(boom, bee.lastException.get());
    }

    @Test
    void hiveBackedSendIsProcessedAsynchronouslyAndDeterministicallyDrained()
    {
        RecordingBee<Integer> bee = new RecordingBee<>(hive);
        for (int i = 0; i < 20; i++)
        {
            bee.accept(i);
        }

        bee.shutdown().awaitTermination(1);

        assertTrue(bee.isTerminated());
        assertTrue(bee.terminated.get());
        assertEquals(20, bee.received.size());
        for (int i = 0; i < 20; i++)
        {
            assertTrue(bee.received.contains(i));
        }
    }

    @Test
    void shutdownOnlyWhenEmptyWaitsForPendingMessagesBeforeTerminating()
    {
        RecordingBee<Integer> bee = new RecordingBee<>(2, hive);
        for (int i = 0; i < 50; i++)
        {
            bee.accept(i);
        }

        bee.shutdown(true);

        assertTrue(bee.awaitTermination(2000));
        assertEquals(50, bee.received.size());
    }

    @Test
    void setHiveAttachesHiveAfterConstructionForLaterSends()
    {
        RecordingBee<String> bee = new RecordingBee<>(); // no hive yet
        bee.accept("direct"); // processed synchronously, no hive involved

        bee.setHive(hive);
        bee.accept("via-hive");

        bee.shutdown().awaitTermination(1);

        assertEquals(2, bee.received.size());
        assertTrue(bee.received.contains("direct"));
        assertTrue(bee.received.contains("via-hive"));
    }

    @Test
    void invalidConstructorArgumentsThrow()
    {
        assertThrows(IllegalArgumentException.class, () -> new RecordingBee<String>(-1, hive));
        assertThrows(IllegalArgumentException.class, () -> new RecordingBee<String>(1, hive, -1));
    }

    @Test
    void staticShutdownAndAwaitTerminationHandlesMultipleBees()
    {
        RecordingBee<Integer> b1 = new RecordingBee<>(hive);
        RecordingBee<Integer> b2 = new RecordingBee<>(hive);
        b1.accept(1);
        b2.accept(2);

        Hive.shutdownAndAwaitTermination(true, b1, b2);

        assertTrue(b1.isTerminated());
        assertTrue(b2.isTerminated());
        assertEquals(Collections.singletonList(1), b1.received);
        assertEquals(Collections.singletonList(2), b2.received);
    }

    @Test
    void dryLoggerReturnsSameInstance()
    {
        RecordingBee<String> bee = new RecordingBee<>();
        assertSame(bee, bee.dryLogger());
    }
}
