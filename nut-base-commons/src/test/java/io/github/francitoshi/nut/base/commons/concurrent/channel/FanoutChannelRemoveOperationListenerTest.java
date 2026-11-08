/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.lang.reflect.Field;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression guard for the fan-out listener removal: once a listener is
 * removed from the fan-out, its forwarded copy must also be removed from every
 * target, or targets leak listeners and keep notifying the dead copy on every
 * operation (and {@code selectGet}/{@code selectPut} leaks one {@code Signal}
 * per invocation).
 */
class FanoutChannelRemoveOperationListenerTest
{
    @Test
    void removedListenerIsNoLongerNotified()
    {
        BufferedChannel<String> dest = new BufferedChannel<>(4);
        FanoutChannel<String> fan = new FanoutChannel<>(dest);

        AtomicInteger notified = new AtomicInteger();
        ChannelObservable.OperationListener<String> listener =
                (channel, operation, value) -> notified.incrementAndGet();

        fan.addOperationListener(listener);
        fan.removeOperationListener(listener);

        fan.put("x");
        dest.get();

        assertEquals(0, notified.get(),
                "listener removed from the fan-out must not be notified through the targets");
    }

    @Test
    void removeAfterSelectDoesNotAccumulateListenersInTargets() throws Exception
    {
        BufferedChannel<String> dest = new BufferedChannel<>(8);
        FanoutChannel<String> fan = new FanoutChannel<>(dest);

        // each select cycle installs one listener (wrapped in a Forwarder per
        // target) and removes it again; nothing may be left attached to the
        // targets but the target's own machinery (one listener expected:
        // `dest` is also observed... it registers none, so expect zero)
        for (int i = 0; i < 10; i++)
        {
            Channel.selectPut(1, TimeUnit.MILLISECONDS, "v", fan);
        }

        // the accepted selects leave up to the buffer capacity of "v" pending
        // (FIFO); drain them so the flush round-trip below starts on an empty
        // channel instead of parking forever on a full one
        while (dest.tryGet().isPresent())
        {
            // drained
        }

        fan.put("flush");
        assertEquals("flush", dest.get());

        Field field = null;
        try
        {
            field = Channel.class.getDeclaredField("operationListeners");
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        field.setAccessible(true);
        Object value;
        try
        {
            value = field.get(dest);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        assertEquals(0, ((java.util.List<?>) value).size(),
                "selects must leave no leaked listeners behind in the fan-out targets");
    }

    @Test
    void multipleTargetsAllDetached()
    {
        BufferedChannel<String> dest1 = new BufferedChannel<>(4);
        BufferedChannel<String> dest2 = new BufferedChannel<>(4);
        FanoutChannel<String> fan = new FanoutChannel<>(dest1, dest2);

        CopyOnWriteArrayList<String> calls = new CopyOnWriteArrayList<>();
        ChannelObservable.OperationListener<String> listener =
                (channel, operation, value) -> calls.add(channel + "/" + operation);

        fan.addOperationListener(listener);
        fan.removeOperationListener(listener);

        fan.put("x");
        dest1.get();
        dest2.get();

        assertTrue(calls.isEmpty(),
                "removed listener must be detached from every target, got " + calls);
    }
}
