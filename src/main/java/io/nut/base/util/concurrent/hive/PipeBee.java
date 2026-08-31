/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.concurrent.hive;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An intermediate pipeline stage that transforms messages of type {@code T}
 * into messages of type {@code R} and forwards them to the next stage.
 * <p>
 * {@code PipeBee} follows the <em>Continuation-Passing Style</em> (CPS)
 * pattern: {@link #receive(Object)} never returns the transformed value to its
 * caller. Instead, it applies the configured {@link Function}{@code <T,R>} and
 * immediately calls {@link Consumer#accept accept()} on the linked {@code next}
 * stage. The next stage can be another {@code PipeBee<R,S>} (which keeps
 * transforming), or any other {@link Consumer}{@code <R>} (such as a plain
 * {@code Bee<R>} that consumes the value).
 * <p>
 * Stages are wired together with {@link #linkTo}, which returns the next stage
 * so calls can be chained:
 * <pre>{@code
 * PipeBee<Integer, String> fmt  = hive.pipe(i -> "item " + i);
 * Bee<String>              sink = hive.bee(System.out::println);
 * fmt.linkTo(sink);
 * fmt.accept(42);  // prints "item 42"
 * }</pre>
 * For long chains it is more convenient to use {@link Hive#pipeline}, which
 * wires stages automatically via {@link HivePipeline#then}.
 * <p>
 * If the {@code next} stage has not been set when {@link #receive(Object)} is
 * called, the transformed value is silently discarded.
 *
 * @param <T> the type of messages this stage receives
 * @param <R> the type of messages this stage produces and forwards
 */
public class PipeBee<T,R> extends Bee<T>
{
    private final Function<T,R> function;

    /**
     * The next stage in the chain that will receive transformed messages.
     * Declared {@code volatile} so that a call to {@link #linkTo} from one
     * thread is immediately visible to worker threads invoking
     * {@link #receive(Object)}.
     */
    protected volatile Consumer<R> next;

    /**
     * Full constructor.
     *
     * @param threads   the maximum number of concurrent worker threads
     * @param hive      the Hive thread pool, or {@code null} for synchronous mode
     * @param queueSize the internal queue capacity (0 = default)
     * @param function  the transformation applied to each message; must not be
     *                  {@code null}
     */
    public PipeBee(Hive hive, int threads, int queueSize, Function<T,R> function)
    {
        super(hive, threads, queueSize);
        this.function = Objects.requireNonNull(function, "function must not be null");
    }

    /**
     * Constructs a PipeBee attached to the given Hive with the default thread
     * count and queue size.
     *
     * @param hive     the Hive thread pool, or {@code null} for synchronous mode
     * @param function the transformation applied to each message; must not be
     *                 {@code null}
     */
    public PipeBee(Hive hive, Function<T,R> function)
    {
        this(hive, 1, 0, function);
    }

    /**
     * Constructs a standalone PipeBee with the given thread count but no Hive.
     * A Hive is attached at construction time and cannot be changed during the lifecycle of the instance.
     *
     * @param threads  the maximum number of concurrent worker threads
     * @param function the transformation applied to each message; must not be
     *                 {@code null}
     */
    public PipeBee(int threads, int queueSize, Function<T,R> function)
    {
        this(null, threads, queueSize, function);
    }

    /**
     * Constructs a standalone PipeBee with the default thread count and no
     * Hive. A Hive is attached at construction time and cannot be changed during the lifecycle of the instance.
     *
     * @param function the transformation applied to each message; must not be
     *                 {@code null}
     */
    public PipeBee(Function<T,R> function)
    {
        this(null, 1, 0, function);
    }

    /**
     * Links this stage to the next stage of the chain (the continuation).
     * The returned value is {@code next} itself, so multiple {@code linkTo}
     * calls can be chained without intermediate variables:
     * <pre>{@code
     * pipeA.linkTo(pipeB).linkTo(pipeC).linkTo(sink);
     * }</pre>
     *
     * @param <S>  the concrete type of the next stage (must extend
     *             {@link Consumer}{@code <R>})
     * @param next the stage that will receive the transformed values; must not
     *             be {@code null}
     * @return {@code next}, typed as {@code S}, enabling fluent chaining
     */
    public <S extends Consumer<R>> S linkTo(S next)
    {
        this.next = Objects.requireNonNull(next, "next must not be null");
        return next;
    }

    /**
     * Returns the next stage in the chain, or {@code null} if none has been
     * linked yet.
     *
     * @return the linked next stage, or {@code null}
     */
    protected Consumer<R> getNext()
    {
        return next;
    }

    /**
     * Applies the configured function to the received message and forwards the
     * result to the linked {@code next} stage. If {@code next} is {@code null},
     * the result is silently discarded.
     *
     * @param m the message to transform
     */
    @Override
    protected void receive(T m)
    {
        R r = function.apply(m);
        Consumer<R> n = this.next;
        if(n != null)
        {
            n.accept(r);
        }
    }

    /**
     * {@inheritDoc}
     * Overridden to return the more specific {@code PipeBee<T,R>} type for
     * fluent chaining.
     */
    @Override
    public PipeBee<T,R> shutdown()
    {
        return (PipeBee<T,R>) super.shutdown();
    }

    /**
     * {@inheritDoc}
     * Overridden to return the more specific {@code PipeBee<T,R>} type for
     * fluent chaining.
     */
    @Override
    public PipeBee<T,R> shutdown(boolean onlyWhenEmpty)
    {
        return (PipeBee<T,R>) super.shutdown(onlyWhenEmpty);
    }

    @Override
    public Collection<Consumer<?>> getLinkedTargets()
    {
        return next != null ? Collections.singletonList(next) : Collections.emptyList();
    }
}
