/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

/**
 * Factory that selects the appropriate actor flavor based on the
 * requested thread count and hub configuration.
 * <p>
 * Selects {@link SynchronousActor} when {@code threads == 0}, the hub is
 * {@code null}, or the hub itself is synchronous; {@link SingleActor} when
 * {@code threads == 1}; {@link MultiActor} when {@code threads >= 2}.
 * Throws {@link IllegalArgumentException} for negative thread counts.
 */
final class ActorFlavors
{
    private ActorFlavors() {}

    /**
     * Creates the appropriate actor flavor.
     *
     * @param <M>      the message type
     * @param hub      the ActorHub, or {@code null} for synchronous
     * @param threads  the requested thread count
     * @param queueSize the channel queue capacity (0 = rendezvous)
     * @param hooks    the callbacks into the wrapper stage
     * @return a new actor of the selected flavor
     */
    static <M> Actor<M> create(ActorHub hub, int threads, int queueSize, ActorHooks<M> hooks)
    {
        if (threads < 0)
        {
            throw new IllegalArgumentException("threads must not be negative");
        }
        if (queueSize < 0)
        {
            throw new IllegalArgumentException("queueSize must not be negative");
        }
        if (threads == 0 || hub == null || hub.isSynchronous())
        {
            return new SynchronousActor<>(hub, hooks);
        }
        if (threads == 1)
        {
            return new SingleActor<>(hub, queueSize, hooks);
        }
        return new MultiActor<>(hub, threads, queueSize, hooks);
    }
}
