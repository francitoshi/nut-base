/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Wrapper stage that holds an inner flavor actor and forwards every
 * produced value to a single downstream stage linked with {@link #linkTo}.
 * This is the shared base of {@link PipeActor}, {@link FilterActor}, and
 * {@link BatchActor}.
 * <p>
 * Subclasses pass {@code null} for {@code inner} to the constructor and
 * set the field in their own constructor body after creating the hooks.
 *
 * @param <I> the type of messages this stage receives
 * @param <F> the type of messages this stage produces and forwards
 */
public abstract class LinkableActor<I,F> implements Consumer<I>, Linkable
{
    protected Actor<I> inner;

    protected volatile Consumer<F> next;

    private volatile Exception ex;

    protected LinkableActor(Actor<I> inner)
    {
        this.inner = inner;
    }

    @Override
    public void accept(I message)
    {
        inner.accept(message);
    }

    public <S extends Consumer<F>> S linkTo(S next)
    {
        this.next = Objects.requireNonNull(next, "next must not be null");
        return next;
    }

    protected void forward(F value)
    {
        Consumer<F> n = this.next;
        if (n != null)
        {
            n.accept(value);
        }
    }

    // -----------------------------------------------------------------
    // Linkable
    // -----------------------------------------------------------------

    @Override
    public LinkableActor<I,F> waitForIdle()
    {
        inner.waitForIdle();
        return this;
    }

    @Override
    public LinkableActor<I,F> shutdown()
    {
        inner.shutdown();
        return this;
    }

    @Override
    public LinkableActor<I,F> shutdown(boolean onlyWhenEmpty)
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
        return inner.isTerminated();
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
        Exception own = this.ex;
        return own != null ? own : inner.getException();
    }

    protected void handleException(Exception ex)
    {
        this.ex = ex;
    }

    public LinkableActor<I,F> dryLogger()
    {
        inner.dryLogger();
        return this;
    }

    public int getPendingCount()
    {
        return inner.getPendingCount();
    }

    public ActorHub getActorHub()
    {
        return inner.getActorHub();
    }

    @Override
    public Collection<Consumer<?>> getLinkedTargets()
    {
        return next != null ? Collections.singletonList(next) : Collections.emptyList();
    }
}
