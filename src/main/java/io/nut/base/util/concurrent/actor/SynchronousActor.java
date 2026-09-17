/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

/**
 * Synchronous actor flavor: executes {@link #receive} directly in the
 * calling thread, with no channel or worker threads.
 * <p>
 * Selected when {@code threads == 0}, no hub is attached, or the hub
 * is synchronous.
 * <p>
 * Instantiate it as an anonymous or named subclass and implement
 * {@link #receive(Object)} (and optionally {@link #terminate()} and
 * {@link #exception(Exception)}):
 * <pre>{@code
 * Actor<String> a = new SynchronousActor<String>(hub)
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
public abstract class SynchronousActor<M> extends Actor<M>
{
    public SynchronousActor(ActorHub hub)
    {
        super(hub);
    }

    @Override
    public void accept(M message)
    {
        if (closed)
        {
            throw new IllegalStateException("closed");
        }
        countProcessed();
        try
        {
            receive(message);
        }
        catch (Exception ex)
        {
            handleException(ex);
        }
    }

    @Override
    public Actor<M> waitForIdle()
    {
        return this;
    }

    @Override
    public Actor<M> shutdown(boolean onlyWhenEmpty)
    {
        synchronized (lock)
        {
            if (!closed && !terminated)
            {
                closed = true;
                terminated = true;
                unregisterFromActorHub();
                try
                {
                    terminate();
                }
                catch (Exception ex)
                {
                    handleException(ex);
                }
                lock.notifyAll();
            }
        }
        return this;
    }

    private void unregisterFromActorHub()
    {
        actorHub.unregisterActor(this);
    }
}