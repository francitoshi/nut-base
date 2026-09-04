/*
 * Copyright (c) 2024-2026 francitoshi@gmail.com
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
package io.nut.base.util.concurrent.actor;

import java.util.List;
import java.util.function.Consumer;

/**
 * A publisher for a single topic in the ActorHub Pub/Sub system.
 * <p>
 * A {@code Pub<T>} is a {@link Consumer}{@code <T>} obtained from
 * {@link ActorHub#pub(String)}. Calling {@link #accept(Object)} fan-outs the
 * message to every {@link Actor} that was registered for this topic via
 * {@link Actor#sub(String)} or {@link ActorHub#sub(String, Actor)}, in registration
 * order. Each subscriber receives the message through its own
 * {@link Actor#accept(Object)}, so dispatch is fully asynchronous when the
 * Actors are attached to an ActorHub.
 * <p>
 * {@code Pub} instances are lightweight wrappers around the live subscriber
 * list held by the {@link ActorHub}; there is no need to re-obtain them after
 * new subscribers join — they will automatically be included in the next
 * {@link #accept} call.
 * <p>
 * <strong>Thread safety</strong>: {@link #accept} iterates over a snapshot
 * copy of the subscriber list so that concurrent subscriptions never cause
 * a {@link java.util.ConcurrentModificationException}.
 *
 * @param <T> the message type published to subscribers
 */
public final class Pub<T> implements Consumer<T>
{
    /**
     * Live reference to the subscriber list managed by {@link ActorHub}.
     * Reads take a snapshot before iterating to remain safe under concurrent
     * modifications.
     */
    private final List<Consumer<T>> subscribers;

    /**
     * Package-private constructor called by {@link ActorHub#pub(String)}.
     *
     * @param subscribers the live subscriber list for the topic; must not be
     *                    {@code null}
     */
    Pub(List<Consumer<T>> subscribers)
    {
        this.subscribers = subscribers;
    }

    /**
     * Publishes {@code message} to all current subscribers.
     * <p>
     * A snapshot of the subscriber list is taken at the start of the call so
     * that Actors registered concurrently during dispatch are not included in
     * this round (consistent fan-out semantics). Each subscriber's
     * {@link Consumer#accept accept()} is called in registration order.
     *
     * @param message the message to deliver; may be {@code null} if the
     *                subscriber Actors accept {@code null} messages
     */
    @Override
    @SuppressWarnings("unchecked")
    public void accept(T message)
    {
        // Snapshot to avoid ConcurrentModificationException and ensure a
        // consistent view of subscribers for this publish round.
        Object[] snapshot;
        synchronized (subscribers)
        {
            snapshot = subscribers.toArray();
        }
        for (Object subscriber : snapshot)
        {
            ((Consumer<T>) subscriber).accept(message);
        }
    }
}
