/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

/**
 * Multi-threaded async actor flavor: one permanent worker plus temporary
 * "rush" workers that are started on demand (up to the configured thread
 * count) and return to the pool once the channel is drained.
 * <p>
 * Thread demand: {@code threads}.
 *
 * @param <M> the message type
 */
class MultiActor<M> extends AsyncActor<M>
{
    MultiActor(ActorHub hub, int threads, int queueSize, ActorHooks<M> hooks)
    {
        super(hub, threads, queueSize, hooks);
    }

    /**
     * The permanent worker drains the channel, blocking between messages; the
     * temporary "rush" workers (see the class javadoc) are the ones that start
     * and stop on demand.
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
