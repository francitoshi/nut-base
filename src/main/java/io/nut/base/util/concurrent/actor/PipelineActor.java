/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import io.nut.base.math.Nums;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A fluent, type-safe builder for linear chains of {@link PipeActor} stages all
 * attached to the same {@link ActorHub}, created via {@link ActorHub#pipeline}.
 *
 * @param <T> the type of message accepted by the first stage of the chain
 * @param <R> the type currently produced by the last stage added so far
 */
public final class PipelineActor<T,R> implements Consumer<T>
{
    private final ActorHub actorHub;
    private final PipeActor<T,?> head;
    private final PipeActor<?,R> tail;
    private final List<Linkable> owned;

    PipelineActor(ActorHub actorHub, PipeActor<T,?> head, PipeActor<?,R> tail)
    {
        this.actorHub = actorHub;
        this.head = head;
        this.tail = tail;
        this.owned = new ArrayList<>();
        this.owned.add(head);
    }

    private PipelineActor(ActorHub actorHub, PipeActor<T,?> head, PipeActor<?,R> tail, List<Linkable> owned)
    {
        this.actorHub = actorHub;
        this.head = head;
        this.tail = tail;
        this.owned = owned;
    }

    public <S> PipelineActor<T,S> then(Function<R,S> function)
    {
        PipeActor<R,S> next = actorHub.pipe(function);
        tail.linkTo(next);
        owned.add(next);
        return new PipelineActor<>(actorHub, head, next, owned);
    }

    public <S> PipelineActor<T,S> then(int threads, Function<R,S> function)
    {
        PipeActor<R,S> next = actorHub.pipe(threads, function);
        tail.linkTo(next);
        owned.add(next);
        return new PipelineActor<>(actorHub, head, next, owned);
    }

    public <S> PipelineActor<T,S> then(int threads, int queueSize, Function<R,S> function)
    {
        PipeActor<R,S> next = actorHub.pipe(threads, queueSize, function);
        tail.linkTo(next);
        owned.add(next);
        return new PipelineActor<>(actorHub, head, next, owned);
    }

    /**
     * Closes the chain with a terminal {@link Consumer}{@code <R>} and returns
     * the head wrapper of the fully-wired chain.
     */
    public PipeActor<T,?> sink(Consumer<R> consumer)
    {
        Actor<R> terminal = actorHub.actor(consumer);
        tail.linkTo(terminal);
        return head;
    }

    /**
     * Closes the chain by linking it to an already-built
     * {@link Consumer}{@code <R>} and returns the head wrapper.
     */
    public PipeActor<T,?> to(Consumer<R> next)
    {
        tail.linkTo(Objects.requireNonNull(next, "next must not be null"));
        return head;
    }

    /**
     * Returns the head {@link PipeActor}{@code <T,?>} of the chain built so far.
     */
    public PipeActor<T,?> head()
    {
        return head;
    }

    @Override
    public void accept(T message)
    {
        head.accept(message);
    }

    public PipelineActor<T,R> shutdown()
    {
        return shutdown(false);
    }

    public PipelineActor<T,R> shutdown(boolean onlyWhenEmpty)
    {
        for (Linkable stage : owned)
        {
            stage.shutdown(onlyWhenEmpty);
        }
        return this;
    }

    public boolean awaitTermination(int millis)
    {
        long nanos = Nums.saturatedAdd(System.nanoTime(), TimeUnit.MILLISECONDS.toNanos(millis));
        boolean ok = true;
        for (Linkable stage : owned)
        {
            ok = stage.awaitTerminationUntilNanos(nanos) && ok;
        }
        return ok;
    }
}