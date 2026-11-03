/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import java.util.concurrent.CopyOnWriteArrayList;
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
            Channel.selectPut(1, java.util.concurrent.TimeUnit.MILLISECONDS, "v", fan);
        }

        fan.put("flush");
        assertEquals("flush", dest.get());

        java.lang.reflect.Field field = null;
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
