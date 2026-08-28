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
import java.util.function.Predicate;

/**
 * A pipeline stage that forwards only those messages that satisfy a
 * {@link Predicate}, discarding the rest without further processing.
 * <p>
 * Like {@link PipeBee}, {@code FilterBee} follows the
 * <em>Continuation-Passing Style</em> (CPS) pattern: {@link #receive(Object)}
 * tests the message against the predicate and, only if the test passes, calls
 * {@link Consumer#accept accept()} on the linked {@code next} stage. The
 * message type is never changed, so {@code next} must be a
 * {@link Consumer}{@code <T>}.
 * <p>
 * Stages are wired together with {@link #linkTo}, which returns the next stage
 * for fluent chaining:
 * <pre>{@code
 * FilterBee<Integer> evens  = hive.filter(i -> i % 2 == 0);
 * Bee<Integer>       sink   = hive.bee(System.out::println);
 * evens.linkTo(sink);
 * for (int i = 0; i < 10; i++) evens.accept(i);  // prints 0, 2, 4, 6, 8
 * }</pre>
 * <p>
 * If {@code next} has not been set when a message passes the predicate, that
 * message is silently discarded.
 *
 * @param <T> the type of messages this stage receives, tests, and may forward
 *            unchanged
 */
public class FilterBee<T> extends Bee<T>
{
    private final Predicate<T> predicate;

    /**
     * The next stage in the chain that will receive messages passing the
     * predicate. Declared {@code volatile} so that a call to {@link #linkTo}
     * from one thread is immediately visible to worker threads invoking
     * {@link #receive(Object)}.
     */
    protected volatile Consumer<T> next;

    /**
     * Full constructor.
     *
     * @param threads   the maximum number of concurrent worker threads
     * @param hive      the Hive thread pool, or {@code null} for synchronous mode
     * @param queueSize the internal queue capacity (0 = default)
     * @param predicate the test applied to each message; must not be {@code null}
     */
    public FilterBee(Hive hive, int threads, int queueSize, Predicate<T> predicate)
    {
        super(hive, threads, queueSize);
        this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
    }

    /**
     * Constructs a FilterBee attached to the given Hive with the default thread
     * count and queue size.
     *
     * @param hive      the Hive thread pool, or {@code null} for synchronous mode
     * @param predicate the test applied to each message; must not be {@code null}
     */
    public FilterBee(Hive hive, Predicate<T> predicate)
    {
        this(hive, 0, 0, predicate);
    }

    /**
     * Constructs a standalone FilterBee with the given thread count but no
     * Hive. A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param threads   the maximum number of concurrent worker threads
     * @param predicate the test applied to each message; must not be {@code null}
     */
    public FilterBee(int threads, int queueSize, Predicate<T> predicate)
    {
        this(null, threads, queueSize, predicate);
    }

    /**
     * Constructs a standalone FilterBee with the default thread count and no
     * Hive. A Hive can be attached later with {@link Bee#setHive(Hive)}.
     *
     * @param predicate the test applied to each message; must not be {@code null}
     */
    public FilterBee(Predicate<T> predicate)
    {
        this(null, 0, 0, predicate);
    }

    /**
     * Links this filter to the next stage of the chain (the continuation),
     * which is invoked only for messages that pass the predicate. The returned
     * value is {@code next} itself, allowing fluent chaining:
     * <pre>{@code
     * filterA.linkTo(pipeB).linkTo(sink);
     * }</pre>
     *
     * @param <S>  the concrete type of the next stage (must extend
     *             {@link Consumer}{@code <T>})
     * @param next the stage that will receive the passing messages; must not be
     *             {@code null}
     * @return {@code next}, typed as {@code S}, enabling fluent chaining
     */
    public <S extends Consumer<T>> S linkTo(S next)
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
    protected Consumer<T> getNext()
    {
        return next;
    }

    /**
     * Tests the received message against the predicate. If the test passes,
     * forwards the message to the linked {@code next} stage unchanged. If the
     * test fails, or if {@code next} is {@code null}, the message is discarded.
     *
     * @param m the message to test and potentially forward
     */
    @Override
    protected void receive(T m)
    {
        if (predicate.test(m))
        {
            Consumer<T> n = this.next;
            if (n != null)
            {
                n.accept(m);
            }
        }
    }

    /**
     * {@inheritDoc}
     * Overridden to return the more specific {@code FilterBee<T>} type for
     * fluent chaining.
     */
    @Override
    public FilterBee<T> shutdown()
    {
        return (FilterBee<T>) super.shutdown();
    }

    /**
     * {@inheritDoc}
     * Overridden to return the more specific {@code FilterBee<T>} type for
     * fluent chaining.
     */
    @Override
    public FilterBee<T> shutdown(boolean onlyWhenEmpty)
    {
        return (FilterBee<T>) super.shutdown(onlyWhenEmpty);
    }

    @Override
    public Collection<Consumer<?>> getLinkedTargets()
    {
        return next != null ? Collections.singletonList(next) : Collections.emptyList();
    }
}
