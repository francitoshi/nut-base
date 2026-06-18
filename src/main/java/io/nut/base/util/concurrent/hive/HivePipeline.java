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
 * A fluent, type-safe builder for linear chains of {@link PipeBee} stages
 * all attached to the same {@link Hive}, created via {@link Hive#pipeline}.
 * It hides the manual {@link PipeBee#linkTo} wiring that would otherwise
 * be needed to build a long chain by hand, while still type-checking
 * every stage at compile time -- something a flat, heterogeneous varargs
 * call like {@code pipeline(f1, f2, f3, sink)} cannot do in Java, since
 * each function in such a chain has a different, incompatible
 * {@code Function<?,?>} type.
 * <p>
 * Usage:
 * <pre>{@code
 * Bee<Integer> head = hive.pipeline((Integer i) -> i * 2)
 *                         .then(i -> "value=" + i)
 *                         .then(String::toUpperCase)
 *                         .sink(System.out::println);
 * head.send(21);
 * }</pre>
 * Each {@code then(...)} call appends one more {@code PipeBee} stage and
 * returns a new {@code HivePipeline} view with the same head but an
 * updated "current output type", so further {@code then}/{@code sink}/
 * {@code to} calls keep being type-checked against it. The chain is not
 * usable for {@link #send(Object)} until it has been closed with
 * {@link #sink(Consumer)} or {@link #to(Sendable)}.
 *
 * @param <T> the type of message accepted by the first stage of the chain
 * @param <R> the type currently produced by the last stage added so far
 */
public final class HivePipeline<T,R> implements Sendable<T>
{
    private final Hive hive;
    private final Bee<T> head;
    private final PipeBee<?,R> tail;

    HivePipeline(Hive hive, Bee<T> head, PipeBee<?,R> tail)
    {
        this.hive = hive;
        this.head = head;
        this.tail = tail;
    }

    /**
     * Appends a new transformation stage to the chain, wired to the
     * previous stage's output, using the Hive's default thread count
     * and queue size for the new stage.
     *
     * @param function transforms the values produced by the current
     *                 last stage into the next stage's output type
     * @return a new pipeline view with the same head but an updated
     *         current output type
     */
    public <S> HivePipeline<T,S> then(Function<R,S> function)
    {
        PipeBee<R,S> next = hive.pipe(function);
        tail.linkTo(next);
        return new HivePipeline<>(hive, head, next);
    }

    public <S> HivePipeline<T,S> then(int threads, Function<R,S> function)
    {
        PipeBee<R,S> next = hive.pipe(threads, function);
        tail.linkTo(next);
        return new HivePipeline<>(hive, head, next);
    }

    public <S> HivePipeline<T,S> then(int threads, int queueSize, Function<R,S> function)
    {
        PipeBee<R,S> next = hive.pipe(threads, queueSize, function);
        tail.linkTo(next);
        return new HivePipeline<>(hive, head, next);
    }

    /**
     * Closes the chain with a terminal {@code Consumer<R>}, e.g.
     * {@code .sink(System.out::println)}.
     *
     * @param consumer consumes the final, fully-transformed values
     * @return the head Bee&lt;T&gt; of the chain, the entry point to
     *         {@link Sendable#send} messages into it and to
     *         {@link Bee#shutdown} the whole chain
     */
    public Bee<T> sink(Consumer<R> consumer)
    {
        Bee<R> terminal = hive.bee(consumer);
        tail.linkTo(terminal);
        return head;
    }

    /**
     * Closes the chain by linking it to an already-built
     * {@code Sendable<R>} (e.g. another chain's head, a {@link QueueBee},
     * a {@link ListBee}, a {@link BroadcastBee}, ...).
     *
     * @param next the next stage/sink that will receive the final values
     * @return the head Bee&lt;T&gt; of the chain
     */
    public Bee<T> to(Sendable<R> next)
    {
        tail.linkTo(Objects.requireNonNull(next, "next must not be null"));
        return head;
    }

    /**
     * @return the head Bee&lt;T&gt; of the chain built so far, the same
     *         instance that {@link #send(Object)} delegates to
     */
    public Bee<T> head()
    {
        return head;
    }

    /**
     * Sends a message into the head of the chain. Note this only has an
     * effect once the chain has actually been wired all the way to a
     * sink with {@link #sink(Consumer)} or {@link #to(Sendable)};
     * messages sent earlier reach the last-built stage but go nowhere
     * since it isn't linked to anything yet.
     */
    @Override
    public boolean send(T message)
    {
        return head.send(message);
    }
}
