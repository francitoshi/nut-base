/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Test-only Actor that records every message it receives, plus the
 * invocations of its {@code terminate()} and {@code exception(Exception)}
 * hooks, so unit tests can assert on what actually happened without
 * resorting to reflection or arbitrary sleeps. Not a test class itself;
 * used as a helper by several *Test classes in this package.
 */
class RecordingActor<M> extends Actor<M>
{
    final List<M> received = new CopyOnWriteArrayList<>();
    final AtomicBoolean terminated = new AtomicBoolean(false);
    final AtomicReference<Exception> lastException = new AtomicReference<>();
    private volatile Consumer<M> action;

    RecordingActor()
    {
        super();
    }

    RecordingActor(ActorHub actorHub)
    {
        super(actorHub);
    }

    RecordingActor(ActorHub actorHub, int threads, int queueSize)
    {
        super(actorHub, threads, queueSize);
    }

    /**
     * Installs extra behavior run right after a message is recorded,
     * e.g. to make a particular message throw and exercise the
     * exception-handling path.
     */
    RecordingActor<M> withAction(Consumer<M> action)
    {
        this.action = action;
        return this;
    }

    @Override
    protected void receive(M m)
    {
        received.add(m);
        Consumer<M> a = this.action;
        if (a != null)
        {
            a.accept(m);
        }
    }

    @Override
    protected void terminate()
    {
        terminated.set(true);
    }

    @Override
    protected void exception(Exception ex)
    {
        lastException.set(ex);
    }
}
