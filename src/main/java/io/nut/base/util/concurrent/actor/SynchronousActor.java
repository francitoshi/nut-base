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
 *
 * @param <M> the message type
 */
class SynchronousActor<M> extends Actor<M>
{
    private final ActorHooks<M> hooks;
    private long sequenceCounter;

    SynchronousActor(ActorHub hub, ActorHooks<M> hooks)
    {
        super(hub);
        this.hooks = hooks;
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
            hooks.receive(message, ++sequenceCounter);
        }
        catch (Exception ex)
        {
            handleException(ex);
        }
    }

    @Override
    protected void receive(M m)
    {
        // Not called — hooks.receive is used instead.
    }

    @Override
    protected void exception(Exception ex)
    {
        hooks.exception(ex);
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
                    hooks.terminate();
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
