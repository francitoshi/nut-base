/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

/**
 * Single-threaded async actor flavor: uses one permanent worker that parks on
 * the channel and drains every message it receives, staying ready until the
 * Actor is shut down.
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
     * The permanent worker drains the channel, blocking between messages so a
     * live reader is always parked for (possibly rendezvous) channels.
     */
    @Override
    protected void permanentLoop()
    {
        try
        {
            drainWhileRegistered();
        }
        finally
        {
            workerDone(true);
        }
    }
}
