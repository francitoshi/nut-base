/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

/**
 * Single-threaded async actor flavor: one permanent worker that stays
 * alive for the lifetime of the actor, polling the channel with a
 * {@link #PERMANENT_WAIT_MILLIS} timeout so it is not destroyed and
 * recreated per message.
 * <p>
 * Thread demand: 1.
 *
 * @param <M> the message type
 */
class SingleActor<M> extends AsyncActor<M>
{
    SingleActor(ActorHub hub, int queueSize, ActorHooks<M> hooks)
    {
        super(hub, 1, queueSize, hooks);
    }

    /**
     * The permanent worker loops forever, polling the channel with a
     * timeout. It only exits when the channel is closed.
     */
    @Override
    protected void permanentLoop()
    {
        try
        {
            drain(PERMANENT_WAIT_MILLIS);
        }
        finally
        {
            workerDone(true);
        }
    }
}
