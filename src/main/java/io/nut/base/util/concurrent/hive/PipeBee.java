/*
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent.hive;

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
 * immediately calls {@link Consumer#send send()} on the linked {@code next}
 * stage. The next stage can be another {@code PipeBee<R,S>} (which keeps
 * transforming), a plain {@link Bee}{@code <R>} (which consumes the value),
 * or any other {@link Consumer}{@code <R>} (such as a {@link QueueBee} or a
 * {@link ListBee}).
 * <p>
 * Stages are wired together with {@link #linkTo}, which returns the next stage
 * so calls can be chained:
 * <pre>{@code
 * PipeBee<Integer, String> fmt  = hive.pipe(i -> "item " + i);
 * Bee<String>              sink = hive.bee(System.out::println);
 * fmt.linkTo(sink);
 * fmt.send(42);  // prints "item 42"
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
    public PipeBee(int threads, Hive hive, int queueSize, Function<T,R> function)
    {
        super(threads, hive, queueSize);
        this.function = Objects.requireNonNull(function, "function must not be null");
    }

    /**
     * Constructs a PipeBee with the given thread count and Hive, using the
     * default queue size.
     *
     * @param threads  the maximum number of concurrent worker threads
     * @param hive     the Hive thread pool, or {@code null} for synchronous mode
     * @param function the transformation applied to each message; must not be
     *                 {@code null}
     */
    public PipeBee(int threads, Hive hive, Function<T,R> function)
    {
        super(threads, hive);
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
        super(hive);
        this.function = Objects.requireNonNull(function, "function must not be null");
    }

    /**
     * Constructs a standalone PipeBee with the given thread count but no Hive.
     * A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param threads  the maximum number of concurrent worker threads
     * @param function the transformation applied to each message; must not be
     *                 {@code null}
     */
    public PipeBee(int threads, Function<T,R> function)
    {
        super(threads);
        this.function = Objects.requireNonNull(function, "function must not be null");
    }

    /**
     * Constructs a standalone PipeBee with the default thread count and no
     * Hive. A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param function the transformation applied to each message; must not be
     *                 {@code null}
     */
    public PipeBee(Function<T,R> function)
    {
        super();
        this.function = Objects.requireNonNull(function, "function must not be null");
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
     *             {@code Sendable<R>})
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
     * linked yet. Used by {@link Hive#shutdown(Sendable, boolean, boolean)}
     * to traverse the chain.
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
}
