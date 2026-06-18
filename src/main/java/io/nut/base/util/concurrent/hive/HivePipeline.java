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
 * A fluent, type-safe builder for linear chains of {@link PipeBee} stages all
 * attached to the same {@link Hive}, created via {@link Hive#pipeline}.
 * <p>
 * It hides the manual {@link PipeBee#linkTo} wiring that would otherwise be
 * needed to build a long chain by hand, while still type-checking every stage
 * at compile time — something a flat, heterogeneous varargs call like
 * {@code pipeline(f1, f2, f3, sink)} cannot do in Java, since each function in
 * such a chain has a different, incompatible {@code Function<?,?>} type.
 * <p>
 * <strong>Usage:</strong>
 * <pre>{@code
 * Bee<Integer> head = hive.pipeline((Integer i) -> i * 2)
 *                         .then(i -> "value=" + i)
 *                         .then(String::toUpperCase)
 *                         .sink(System.out::println);
 * head.send(21);  // prints "VALUE=42"
 * }</pre>
 * Each {@link #then(Function)} call appends one more {@link PipeBee} stage to
 * the chain and returns a new {@code HivePipeline} view with the same head but
 * an updated "current output type", so further {@code then}/{@link #sink}/
 * {@link #to} calls are type-checked against it. The chain is not ready for
 * {@link #send(Object)} until it is closed with {@link #sink(Consumer)} or
 * {@link #to(Sendable)}.
 *
 * @param <T> the type of message accepted by the first stage of the chain (the head)
 * @param <R> the type currently produced by the last stage added so far (the tail)
 */
public final class HivePipeline<T,R> implements Sendable<T>
{
    private final Hive hive;
    private final Bee<T> head;
    private final PipeBee<?,R> tail;

    /**
     * Package-private constructor used by {@link Hive#pipeline} and by
     * {@link #then} to build successive views of the same chain.
     *
     * @param hive the Hive to which all stages in this chain are attached
     * @param head the first stage; messages are sent to it via {@link #send}
     * @param tail the last stage added so far; new stages are linked to it
     */
    HivePipeline(Hive hive, Bee<T> head, PipeBee<?,R> tail)
    {
        this.hive = hive;
        this.head = head;
        this.tail = tail;
    }

    /**
     * Appends a new transformation stage to the chain, wired to the previous
     * stage's output, using the Hive's default thread count and queue size for
     * the new stage.
     *
     * @param <S>      the output type of the new stage
     * @param function the transformation applied by the new stage; must not be
     *                 {@code null}
     * @return a new {@code HivePipeline} view with the same head but an updated
     *         current output type {@code S}
     */
    public <S> HivePipeline<T,S> then(Function<R,S> function)
    {
        PipeBee<R,S> next = hive.pipe(function);
        tail.linkTo(next);
        return new HivePipeline<>(hive, head, next);
    }

    /**
     * Appends a new transformation stage with the specified thread count.
     *
     * @param <S>      the output type of the new stage
     * @param threads  the maximum number of concurrent worker threads for the
     *                 new stage
     * @param function the transformation applied by the new stage; must not be
     *                 {@code null}
     * @return a new {@code HivePipeline} view with the updated output type
     */
    public <S> HivePipeline<T,S> then(int threads, Function<R,S> function)
    {
        PipeBee<R,S> next = hive.pipe(threads, function);
        tail.linkTo(next);
        return new HivePipeline<>(hive, head, next);
    }

    /**
     * Appends a new transformation stage with the specified thread count and
     * internal queue size.
     *
     * @param <S>       the output type of the new stage
     * @param threads   the maximum number of concurrent worker threads for the
     *                  new stage
     * @param queueSize the internal queue capacity for the new stage
     * @param function  the transformation applied by the new stage; must not be
     *                  {@code null}
     * @return a new {@code HivePipeline} view with the updated output type
     */
    public <S> HivePipeline<T,S> then(int threads, int queueSize, Function<R,S> function)
    {
        PipeBee<R,S> next = hive.pipe(threads, queueSize, function);
        tail.linkTo(next);
        return new HivePipeline<>(hive, head, next);
    }

    /**
     * Closes the chain with a terminal {@link Consumer}{@code <R>} and returns
     * the head of the fully-wired chain, ready for use.
     * <p>
     * After this call, the chain is complete: messages sent to the returned
     * {@link Bee} travel through every intermediate stage and are ultimately
     * consumed by {@code consumer}.
     *
     * @param consumer the terminal action applied to each fully-transformed
     *                 value; must not be {@code null}
     * @return the head {@link Bee}{@code <T>} of the chain — the entry point
     *         for {@link Sendable#send} and {@link Bee#shutdown}
     */
    public Bee<T> sink(Consumer<R> consumer)
    {
        Bee<R> terminal = hive.bee(consumer);
        tail.linkTo(terminal);
        return head;
    }

    /**
     * Closes the chain by linking it to an already-built
     * {@link Sendable}{@code <R>} and returns the head of the fully-wired
     * chain. The {@code next} argument can be any {@code Sendable<R>} —
     * another pipeline's head, a {@link QueueBee}, a {@link ListBee},
     * a {@link BroadcastBee}, and so on.
     *
     * @param next the downstream stage that will receive the final values;
     *             must not be {@code null}
     * @return the head {@link Bee}{@code <T>} of the chain
     */
    public Bee<T> to(Sendable<R> next)
    {
        tail.linkTo(Objects.requireNonNull(next, "next must not be null"));
        return head;
    }

    /**
     * Returns the head {@link Bee}{@code <T>} of the chain built so far — the
     * same instance that {@link #send(Object)} delegates to. The head can be
     * used to initiate shutdown of the whole chain via {@link Bee#shutdown()}.
     *
     * @return the head Bee of the chain
     */
    public Bee<T> head()
    {
        return head;
    }

    /**
     * Sends a message into the head of the chain.
     * <p>
     * Note that messages sent before the chain has been fully wired (i.e.
     * before {@link #sink(Consumer)} or {@link #to(Sendable)} is called) will
     * reach the last-built stage but go no further, since that stage is not yet
     * linked to a downstream consumer. Finish building the chain before sending.
     *
     * @param message the message to deliver to the head stage
     * @return {@code true} if the head stage accepted the message
     */
    @Override
    public boolean send(T message)
    {
        return head.send(message);
    }
}
