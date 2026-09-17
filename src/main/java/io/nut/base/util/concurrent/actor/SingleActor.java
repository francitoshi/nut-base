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
 * <p>
 * Instantiate it as an anonymous or named subclass and implement
 * {@link #receive(Object)} (and optionally {@link #terminate()} and
 * {@link #exception(Exception)}), e.g.:
 * <pre>{@code
 * Actor<String> a = new SingleActor<String>(hub, 0)
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
public abstract class SingleActor<M> extends AsyncActor<M>
{
    public SingleActor(ActorHub hub, int queueSize)
    {
        super(hub, 1, queueSize);
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