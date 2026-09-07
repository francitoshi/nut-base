/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A topic entry of the {@link ActorHub} Pub/Sub registry, keyed by tag.
 * <p>
 * A {@code PubSub} holds the ordered list of subscribers to notify and counts
 * how many {@link Publisher}s currently reference it. It lives in the registry
 * map as long as it is still relevant: {@link #removeSubscriber(Consumer)} and
 * {@link #releasePublisher()} atomically remove the entry from the registry
 * once it holds neither subscribers nor publishers, so abandoned topics do not
 * accumulate.
 *
 * @param <T> the payload type.
 */
final class PubSub<T>
{
    private final ConcurrentHashMap<String, PubSub<?>> registry;
    private final String tag;
    private final List<Consumer<?>> subscribers = new ArrayList<>();
    private final Object lock = new Object();
    private int publisherCount;

    /**
     * Creates a new Pub/Sub entry.
     *
     * @param registry the registry map this entry lives in, used to remove the
     *                 entry once it becomes empty
     * @param tag      the topic tag this entry is registered under
     */
    PubSub(ConcurrentHashMap<String, PubSub<?>> registry, String tag)
    {
        this.registry = registry;
        this.tag = tag;
    }

    /**
     * Records a new {@link Publisher} referencing this topic. Called while the
     * map entry is being created or reused, so it is serialized against
     * concurrent removal.
     */
    void acquire()
    {
        synchronized (lock)
        {
            publisherCount++;
        }
    }

    /**
     * Appends a subscriber to the delivery list.
     *
     * @param subscriber the subscriber (a consumer or an Actor) to add
     */
    void addSubscriber(Consumer<?> subscriber)
    {
        synchronized (lock)
        {
            subscribers.add(subscriber);
        }
    }

    /**
     * Removes a subscriber from the delivery list. When this entry no longer
     * holds subscribers nor publishers, it is removed from the registry.
     *
     * @param subscriber the subscriber to remove
     */
    void removeSubscriber(Consumer<?> subscriber)
    {
        registry.compute(tag, (key, current) ->
        {
            if (current != this)
            {
                return current;
            }
            synchronized (lock)
            {
                subscribers.remove(subscriber);
                if (publisherCount == 0 && subscribers.isEmpty())
                {
                    return null;
                }
            }
            return current;
        });
    }

    /**
     * Releases a {@link Publisher} reference. When this entry no longer holds
     * subscribers nor publishers, it is removed from the registry.
     */
    void releasePublisher()
    {
        registry.compute(tag, (key, current) ->
        {
            if (current != this)
            {
                return current;
            }
            synchronized (lock)
            {
                publisherCount--;
                if (publisherCount == 0 && subscribers.isEmpty())
                {
                    return null;
                }
            }
            return current;
        });
    }

    /**
     * Delivers {@code event} to every currently registered subscriber, in
     * registration order. A snapshot of the subscriber list is taken so that
     * concurrent subscription changes never cause a
     * {@link java.util.ConcurrentModificationException}.
     *
     * @param event the event to deliver
     */
    @SuppressWarnings("unchecked")
    void publish(T event)
    {
        Object[] snapshot;
        synchronized (lock)
        {
            snapshot = subscribers.toArray();
        }
        for (Object subscriber : snapshot)
        {
            ((Consumer<T>) subscriber).accept(event);
        }
    }
}