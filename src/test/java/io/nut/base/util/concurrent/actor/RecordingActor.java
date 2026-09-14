/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Test-only wrapper that records every message it receives, plus the
 * invocations of its {@code terminate()} and {@code exception(Exception)}
 * hooks, so unit tests can assert on what actually happened without
 * resorting to reflection or arbitrary sleeps. Not a test class itself;
 * used as a helper by several *Test classes in this package.
 */
class RecordingActor<M> implements Consumer<M>, Linkable
{
    final List<M> received = new CopyOnWriteArrayList<>();
    final AtomicBoolean terminated = new AtomicBoolean(false);
    final AtomicReference<Exception> lastException = new AtomicReference<>();
    private volatile Consumer<M> action;
    private volatile boolean allowLogger = true;
    private volatile Exception ex;

    private final Actor<M> inner;

    RecordingActor()
    {
        this.inner = ActorFlavors.create(null, 0, 0, hooks());
    }

    RecordingActor(ActorHub actorHub)
    {
        this.inner = ActorFlavors.create(actorHub, 1, ActorPool.CORES, hooks());
    }

    RecordingActor(ActorHub actorHub, int threads, int queueSize)
    {
        this.inner = ActorFlavors.create(actorHub, threads, queueSize, hooks());
    }

    private ActorHooks<M> hooks()
    {
        return new ActorHooks<M>()
        {
            @Override
            public void receive(M m, long seq)
            {
                RecordingActor.this.receive(m);
            }

            @Override
            public void terminate()
            {
                RecordingActor.this.onTerminate();
            }

            @Override
            public void exception(Exception ex)
            {
                RecordingActor.this.onException(ex);
            }
        };
    }

    /**
     * Returns the inner flavor actor.
     */
    Actor<M> inner()
    {
        return inner;
    }

    RecordingActor<M> withAction(Consumer<M> action)
    {
        this.action = action;
        return this;
    }

    protected void receive(M m)
    {
        received.add(m);
        Consumer<M> a = this.action;
        if (a != null)
        {
            a.accept(m);
        }
    }

    private void onTerminate()
    {
        terminated.set(true);
    }

    private void onException(Exception ex)
    {
        lastException.set(ex);
        this.ex = ex;
    }

    // -----------------------------------------------------------------
    // Consumer
    // -----------------------------------------------------------------

    @Override
    public void accept(M message)
    {
        inner.accept(message);
    }

    // -----------------------------------------------------------------
    // Linkable
    // -----------------------------------------------------------------

    @Override
    public RecordingActor<M> waitForIdle()
    {
        inner.waitForIdle();
        return this;
    }

    @Override
    public RecordingActor<M> shutdown()
    {
        inner.shutdown();
        return this;
    }

    @Override
    public RecordingActor<M> shutdown(boolean onlyWhenEmpty)
    {
        inner.shutdown(onlyWhenEmpty);
        return this;
    }

    @Override
    public boolean isShutdown()
    {
        return inner.isShutdown();
    }

    @Override
    public boolean isTerminated()
    {
        return terminated.get();
    }

    @Override
    public boolean isIdle()
    {
        return inner.isIdle();
    }

    @Override
    public ActorLifecycle awaitTermination()
    {
        inner.awaitTermination();
        return this;
    }

    @Override
    public void close()
    {
        inner.close();
    }

    public boolean awaitTermination(int millis)
    {
        return inner.awaitTermination(millis);
    }

    @Override
    public boolean awaitTerminationUntilNanos(long untilNanos)
    {
        return inner.awaitTerminationUntilNanos(untilNanos);
    }

    public Exception getException()
    {
        return ex;
    }

    public RecordingActor<M> dryLogger()
    {
        this.allowLogger = false;
        return this;
    }

    public int getPendingCount()
    {
        return inner.getPendingCount();
    }

    @Override
    public Collection<Consumer<?>> getLinkedTargets()
    {
        return Collections.emptyList();
    }
}
