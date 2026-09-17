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
 * <p>
 * Instantiate it as an anonymous or named subclass and implement
 * {@link #receive(Object)} (and optionally {@link #terminate()} and
 * {@link #exception(Exception)}), e.g.:
 * <pre>{@code
 * Actor<String> a = new MultiActor<String>(hub, 4, 0)
 * {
 *     @Override
 *     protected void receive(String m)
 *     {
 *         ...
 *     }
 * };
 * }</pre>
 *
 * @param <M> the message type
 */
public abstract class MultiActor<M> extends AsyncActor<M>
{
    public MultiActor(ActorHub hub, int threads, int queueSize)
    {
        super(hub, threads, queueSize);
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