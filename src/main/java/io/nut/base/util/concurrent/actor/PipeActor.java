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
 * <p>
 * {@code PipeActor} follows the <em>Continuation-Passing Style</em> (CPS)
 * pattern: {@link #receive(Object)} never returns the transformed value to its
 * caller. Instead, it applies the configured {@link Function}{@code <T,R>} and
 * immediately calls {@link java.util.function.Consumer#accept accept()} on the
 * linked next stage. The next stage can be another {@code PipeActor<R,S>}
 * (which keeps transforming), or any other {@link java.util.function.Consumer}
 * {@code <R>} (such as a plain {@code Actor<R>} that consumes the value).
 * <p>
 * Stages are wired together with {@link #linkTo}, which returns the next stage
 * so calls can be chained:
 * <pre>{@code
 * PipeActor<Integer, String> fmt  = actorHub.pipe(i -> "item " + i);
 * Actor<String>              sink = actorHub.actor(System.out::println);
 * fmt.linkTo(sink);
 * fmt.accept(42);  // prints "item 42"
 * }</pre>
 * For long chains it is more convenient to use {@link ActorHub#pipeline}, which
 * wires stages automatically via {@link PipelineActor#then}.
 * <p>
 * If the next stage has not been set when {@link #receive(Object)} is called,
 * the transformed value is silently discarded.
 *
 * @param <T> the type of messages this stage receives
 * @param <R> the type of messages this stage produces and forwards
 */
public class PipeActor<T,R> extends LinkableActor<T,R>
{
    private final Function<T,R> function;

    /**
     * Full constructor.
     *
     * @param threads   the maximum number of concurrent worker threads
     * @param actorHub      the ActorHub thread pool, or {@code null} for synchronous mode
     * @param queueSize the internal queue capacity (0 = default)
     * @param function  the transformation applied to each message; must not be
     *                  {@code null}
     */
    public PipeActor(ActorHub actorHub, int threads, int queueSize, Function<T,R> function)
    {
        super(actorHub, threads, queueSize);
        this.function = Objects.requireNonNull(function, "function must not be null");
    }

    /**
     * Constructs a PipeActor attached to the given ActorHub with the default thread
     * count and queue size.
     *
     * @param actorHub     the ActorHub thread pool, or {@code null} for synchronous mode
     * @param function the transformation applied to each message; must not be
     *                 {@code null}
     */
    public PipeActor(ActorHub actorHub, Function<T,R> function)
    {
        this(actorHub, 1, 0, function);
    }

    /**
     * Constructs a standalone PipeActor with the given thread count but no ActorHub.
     * A ActorHub is attached at construction time and cannot be changed during the lifecycle of the instance.
     *
     * @param threads  the maximum number of concurrent worker threads
     * @param function the transformation applied to each message; must not be
     *                 {@code null}
     */
    public PipeActor(int threads, int queueSize, Function<T,R> function)
    {
        this(null, threads, queueSize, function);
    }

    /**
     * Constructs a standalone PipeActor with the default thread count and no
     * ActorHub. A ActorHub is attached at construction time and cannot be changed during the lifecycle of the instance.
     *
     * @param function the transformation applied to each message; must not be
     *                 {@code null}
     */
    public PipeActor(Function<T,R> function)
    {
        this(null, 1, 0, function);
    }

    /**
     * Applies the configured function to the received message and forwards the
     * result to the linked next stage. If no stage is linked, the result is
     * silently discarded.
     *
     * @param m the message to transform
     */
    @Override
    protected void receive(T m)
    {
        forward(function.apply(m));
    }

    /**
     * {@inheritDoc}
     * Overridden to return the more specific {@code PipeActor<T,R>} type for
     * fluent chaining.
     */
    @Override
    public PipeActor<T,R> shutdown()
    {
        return (PipeActor<T,R>) super.shutdown();
    }

    /**
     * {@inheritDoc}
     * Overridden to return the more specific {@code PipeActor<T,R>} type for
     * fluent chaining.
     */
    @Override
    public PipeActor<T,R> shutdown(boolean onlyWhenEmpty)
    {
        return (PipeActor<T,R>) super.shutdown(onlyWhenEmpty);
    }
}