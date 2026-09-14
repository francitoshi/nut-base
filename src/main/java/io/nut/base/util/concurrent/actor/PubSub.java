/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

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
 * <p>
 * The subscriber list is held as a {@code volatile} copy-on-write array:
 * subscribing and unsubscribing replace the array under the lock, while
 * {@link #publish(Object)} reads it lock-free. A publish therefore observes a
 * consistent snapshot of the subscribers present at the moment the reference
 * was read; subscribers added or removed concurrently do not affect the
 * delivery in progress and can never cause a
 * {@link java.util.ConcurrentModificationException}. Unsubscription matches by
 * identity ({@code ==}), which is what the framework always relies on.
 *
 * @param <T> the payload type.
 */
final class PubSub<T>
{
    private static final Consumer<?>[] EMPTY = new Consumer<?>[0];

    private final ConcurrentHashMap<String, PubSub<?>> registry;
    private final String tag;
    private final Object lock = new Object();

    /** Subscribers in registration order; replaced copy-on-write. */
    private volatile Consumer<?>[] subscribers = EMPTY;
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
            Consumer<?>[] current = subscribers;
            int n = current.length;
            Consumer<?>[] next = new Consumer<?>[n + 1];
            System.arraycopy(current, 0, next, 0, n);
            next[n] = subscriber;
            subscribers = next;
        }
    }

    /**
     * Removes a subscriber (matched by identity) from the delivery list. When
     * this entry no longer holds subscribers nor publishers, it is removed
     * from the registry.
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
                removeIfPresent(subscriber);
                if (publisherCount == 0 && subscribers.length == 0)
                {
                    return null;
                }
            }
            return current;
        });
    }

    /**
     * Removes {@code subscriber} from {@link #subscribers} by identity,
     * replacing the array when present. Returns the removed index, or
     * {@code -1} if the subscriber was not registered. Must be called under
     * {@link #lock}.
     */
    private int removeIfPresent(Consumer<?> subscriber)
    {
        Consumer<?>[] current = subscribers;
        int index = -1;
        for (int i = 0; i < current.length; i++)
        {
            if (current[i] == subscriber)
            {
                index = i;
                break;
            }
        }
        if (index < 0)
        {
            return -1;
        }
        Consumer<?>[] next = new Consumer<?>[current.length - 1];
        System.arraycopy(current, 0, next, 0, index);
        System.arraycopy(current, index + 1, next, index, current.length - index - 1);
        subscribers = next;
        return index;
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
                if (publisherCount == 0 && subscribers.length == 0)
                {
                    return null;
                }
            }
            return current;
        });
    }

    /**
     * Delivers {@code event} to every currently registered subscriber, in
     * registration order. Lock-free: reads the volatile subscriber array once
     * and iterates over that immutable snapshot, so concurrent subscription
     * changes never affect a delivery in progress.
     *
     * @param event the event to deliver
     */
    @SuppressWarnings("unchecked")
    void publish(T event)
    {
        for (Consumer<?> subscriber : subscribers)
        {
            ((Consumer<T>) subscriber).accept(event);
        }
    }
}