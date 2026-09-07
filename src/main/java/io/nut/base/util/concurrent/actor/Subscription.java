/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.function.Consumer;

/**
 * An active subscription returned by {@link ActorHub#sub}. Closing it
 * unregisters the consumer; closing an already closed subscription has no
 * effect.
 *
 * @param <T> the payload type.
 * @author franci
 * @since 1.8
 */
public class Subscription<T> implements AutoCloseable
{
    private final String tag;
    private final Consumer<T> consumer;
    private final Runnable onClose;
    private volatile boolean closed;

    /**
     * Creates a new subscription.
     *
     * @param tag      the topic tag this subscription is registered for.
     * @param consumer the consumer that receives the events.
     * @param onClose  action executed once when {@link #close()} is called;
     *                 typically removes this subscription from a registry.
     */
    Subscription(String tag, Consumer<T> consumer, Runnable onClose)
    {
        this.tag = tag;
        this.consumer = consumer;
        this.onClose = onClose;
    }

    /**
     * Unregisters this subscription, if still active. If the subscription
     * wrapped the consumer in an internally-created {@link Actor} (async
     * hubs), that Actor is shut down too. Idempotent.
     */
    @Override
    public void close()
    {
        if (!closed)
        {
            closed = true;
            onClose.run();
        }
    }

    /**
     * Returns whether this subscription has been closed.
     *
     * @return true if closed, false otherwise.
     */
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * Returns the tag this subscription registered for.
     *
     * @return the tag.
     */
    public String getTag()
    {
        return tag;
    }

    /**
     * Returns the consumer that receives the events.
     *
     * @return the consumer.
     */
    public Consumer<T> getConsumer()
    {
        return consumer;
    }

    @Override
    public String toString()
    {
        return "Subscription[tag=" + tag + ", closed=" + closed + "]";
    }
}
