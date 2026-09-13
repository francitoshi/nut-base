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
 * An {@link Actor} stage that forwards every produced value to a single
 * downstream stage linked with {@link #linkTo}. This is the shared base of the
 * <em>Continuation-Passing Style</em> (CPS) stages {@link PipeActor},
 * {@link FilterActor}, and {@link BatchActor}: each of them defines only its
 * {@link Actor#receive(Object)} transformation and hands the produced value to
 * {@link #forward(Object)}, which delivers it to the currently linked {@code next}
 * stage (or silently discards it when none is linked).
 * <p>
 * The receive type {@code I} and the forwarded type {@code F} are independent so
 * that a stage may change the message type (e.g. {@link BatchActor} receives
 * {@code T} and forwards {@code List<T>}).
 *
 * @param <I> the type of messages this stage receives
 * @param <F> the type of messages this stage produces and forwards
 */
public abstract class LinkableActor<I,F> extends Actor<I>
{
    /**
     * Delegates to {@link Actor#Actor(ActorHub, int, int)}.
     *
     * @param actorHub  the ActorHub thread pool, or {@code null} for synchronous mode
     * @param threads   the maximum number of concurrent worker threads
     * @param queueSize the internal queue capacity (0 = default)
     */
    protected LinkableActor(ActorHub actorHub, int threads, int queueSize)
    {
        super(actorHub, threads, queueSize);
    }

    /**
     * The next stage in the chain that will receive the forwarded values.
     * Declared {@code volatile} so that a call to {@link #linkTo} from one
     * thread is immediately visible to worker threads invoking
     * {@link #forward(Object)}.
     */
    protected volatile Consumer<F> next;

    /**
     * Links this stage to the next stage of the chain (the continuation).
     * The returned value is {@code next} itself, so multiple {@code linkTo}
     * calls can be chained without intermediate variables:
     * <pre>{@code
     * pipeA.linkTo(pipeB).linkTo(pipeC).linkTo(sink);
     * }</pre>
     *
     * @param <S>  the concrete type of the next stage (must extend
     *             {@link Consumer}{@code <F>})
     * @param next the stage that will receive the forwarded values; must not be
     *             {@code null}
     * @return {@code next}, typed as {@code S}, enabling fluent chaining
     */
    public <S extends Consumer<F>> S linkTo(S next)
    {
        this.next = Objects.requireNonNull(next, "next must not be null");
        return next;
    }

    /**
     * Delivers {@code value} to the linked {@code next} stage. If no stage is
     * linked, the value is silently discarded.
     *
     * @param value the value to forward; never {@code null}
     */
    protected void forward(F value)
    {
        Consumer<F> n = this.next;
        if (n != null)
        {
            n.accept(value);
        }
    }

    @Override
    public Collection<Consumer<?>> getLinkedTargets()
    {
        return next != null ? Collections.singletonList(next) : Collections.emptyList();
    }
}