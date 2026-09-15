/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

/**
 * Single-threaded async actor flavor: uses a single worker that drains the
 * channel with a hub-configured idle window, yielding its thread back to
 * the pool once the window expires. Re-started on the next {@link #accept}.
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
     * The permanent worker drains with a timeout, then yields its thread
     * back to the pool (restarted on the next {@link #accept}).
     */
    @Override
    protected void permanentLoop()
    {
        try
        {
            drain(this.actorHub.getPermanentWaitMillis());
        }
        finally
        {
            workerDone(true);
        }
    }
}
