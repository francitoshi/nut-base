/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

/**
 * Callback hooks used by the actor flavor implementations
 * ({@link SynchronousActor}, {@link SingleActor}, {@link MultiActor}) to
 * delegate message processing, termination, and exception handling back to
 * the wrapper stage that owns the flavor.
 *
 * @param <M> the message type
 */
interface ActorHooks<M>
{
    /**
     * Called for each message delivered by the engine.
     *
     * @param m the message
     */
    void receive(M m);

    /**
     * Called once after the channel is closed and drained.
     */
    void terminate();

    /**
     * Called when an unhandled exception escapes from receive.
     *
     * @param ex the exception
     */
    void exception(Exception ex);
}
