/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.actor;

import java.util.Objects;
import java.util.function.Function;

/**
 * An intermediate pipeline stage that transforms messages of type {@code T}
 * into messages of type {@code R} and forwards them to the next stage.
 *
 * @param <T> the type of messages this stage receives
 * @param <R> the type of messages this stage produces and forwards
 */
public class PipeActor<T,R> extends LinkableActor<T,R>
{
    private final Function<T,R> function;

    public PipeActor(ActorHub actorHub, int threads, int queueSize, Function<T,R> function)
    {
        super(null);
        this.function = Objects.requireNonNull(function, "function must not be null");
        this.inner = Actor.create(actorHub, threads, queueSize,
                m -> PipeActor.this.forward(function.apply(m)), null, ex -> PipeActor.this.handleException(ex));
    }

    public PipeActor(ActorHub actorHub, Function<T,R> function)
    {
        this(actorHub, 1, ActorPool.CORES, function);
    }

    public PipeActor(int threads, int queueSize, Function<T,R> function)
    {
        this(null, threads, queueSize, function);
    }

    public PipeActor(Function<T,R> function)
    {
        this(null, 1, ActorPool.CORES, function);
    }

}
