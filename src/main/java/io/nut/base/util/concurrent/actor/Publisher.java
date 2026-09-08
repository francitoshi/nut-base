/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.function.Consumer;

/**
 * A publisher handle returned by {@link ActorHub#pub(String)}. It is a
 * lightweight wrapper around the backing {@code PubSub} entry for its tag:
 * {@link #accept(Object)} delivers to the entry's current subscribers, and
 * {@link #close()} releases this publisher's reference on the entry, which is
 * removed from the registry once no publisher references it and no subscriber
 * remains.
 * <p>
 * Implements {@link Consumer} so that a {@code Publisher} can be passed
 * anywhere a consumer is expected, hiding the pub/sub detail from the caller.
 *
 * @param <T> the payload type.
 * @author franci
 * @since 1.8
 */
public class Publisher<T> implements Consumer<T>, AutoCloseable
{
    private final String tag;
    private final PubSub<T> pubSub;
    private volatile boolean closed;

    /**
     * Creates a new publisher.
     *
     * @param tag   the topic tag this publisher publishes under.
     * @param pubSub the backing Pub/Sub entry that holds the subscribers.
     */
    Publisher(String tag, PubSub<T> pubSub)
    {
        this.tag = tag;
        this.pubSub = pubSub;
    }

    /**
     * Delivers an event to all current subscribers of the publisher's tag.
     *
     * @param event the event to deliver.
     * @throws IllegalStateException if this publisher is closed.
     */
    @Override
    public void accept(T event)
    {
        if (closed)
        {
            throw new IllegalStateException("Publisher for tag '" + tag + "' is closed");
        }
        pubSub.publish(event);
    }

    /**
     * Closes this publisher: any further {@link #accept(Object)} throws an
     * {@link IllegalStateException}, and this publisher's reference on the
     * backing {@code PubSub} entry is released. Idempotent.
     */
    @Override
    public void close()
    {
        if (!closed)
        {
            closed = true;
            pubSub.releasePublisher();
        }
    }

    /**
     * Returns whether this publisher has been closed.
     *
     * @return true if closed, false otherwise.
     */
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * Returns the tag this publisher publishes under.
     *
     * @return the tag.
     */
    public String getTag()
    {
        return tag;
    }

    @Override
    public String toString()
    {
        return "Publisher[tag=" + tag + ", closed=" + closed + "]";
    }
}
