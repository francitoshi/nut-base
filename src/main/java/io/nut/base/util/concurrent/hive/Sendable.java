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

import java.util.function.Consumer;

/**
 * Represents a stage in a Hive pipeline that can accept messages of type
 * {@code M}. This is the common interface shared by {@link Bee}, {@link PipeBee},
 * {@link FilterBee}, {@link BatchBee}, {@link QueueBee}, and any other object
 * that can participate as a downstream sink in a chain.
 * <p>
 * The {@link #send(Object)} method is the single point of entry: callers
 * push messages in, and the implementing class decides what to do with them
 * (transform, filter, accumulate, store, etc.).
 * <p>
 * The {@link #asConsumer()} default method bridges this interface to the
 * standard {@link Consumer} functional interface, so a {@code Sendable} can
 * be passed wherever a {@code Consumer<M>} is expected.
 *
 * @param <M> the type of messages this stage accepts
 */
public interface Sendable<M>
{
    /**
     * Sends a message to this stage for processing.
     * <p>
     * Implementations may process the message synchronously (when no
     * {@link Hive} is attached) or enqueue it for asynchronous processing
     * on the Hive's thread pool. The caller is not blocked waiting for
     * the message to be fully processed.
     *
     * @param message the message to deliver; must not be {@code null} for
     *                most implementations
     * @return {@code true} if the message was accepted, {@code false} if it
     *         was rejected (e.g. because the stage has been shut down or an
     *         error occurred while enqueuing)
     */
    boolean send(M message);

    /**
     * Returns a {@link Consumer}{@code <M>} view of this {@code Sendable},
     * allowing it to be used wherever the standard functional interface is
     * expected (e.g. {@code Stream.forEach}, method references, etc.).
     * <p>
     * The returned consumer simply delegates to {@link #send(Object)}, so
     * the return value of {@code send} is silently discarded.
     *
     * @return a {@code Consumer<M>} that forwards each accepted value to
     *         {@link #send(Object)}
     */
    default Consumer<M> asConsumer()
    {
        return (msg) -> send(msg);
    }
}
