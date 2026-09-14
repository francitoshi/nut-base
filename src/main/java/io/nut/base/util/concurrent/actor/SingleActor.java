/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

/**
 * Single-threaded async actor flavor: uses a single worker that drains the
 * channel with a {@link #PERMANENT_WAIT_MILLIS} idle window, yielding its
 * thread back to the pool once the window expires. Re-submitted on the next
 * {@link #accept} if a new message arrives.
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
     * Drains the channel once with a {@link #PERMANENT_WAIT_MILLIS} idle
     * window, then yields the thread back to the pool so it can be
     * reused by other work. Re-started on the next {@link #accept}.
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
