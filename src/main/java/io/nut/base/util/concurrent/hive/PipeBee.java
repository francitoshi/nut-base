/*
 *  PipeBee.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util.concurrent.hive;

import java.util.function.Function;

/**
 * A concrete intermediate Bee that transforms each inbound message into an outbound
 * message and automatically forwards the result to the downstream stage.
 *
 * <p>{@code PipeBee} extends {@link InOutBee} with a fixed receive/forward contract:
 * every message received is passed to {@link #process(Object)}, and the return value
 * is immediately forwarded via {@link InOutBee#sendOut(Object)}. Subclasses only need
 * to implement the pure transformation logic in {@code process}.
 *
 * <p>Instances can be created either by subclassing or via the static factory methods
 * {@link #pipe(Function)}, {@link #pipe(Hive, Function)}, and
 * {@link #pipe(int, Hive, Function)}, which allow inline definition using lambdas.
 *
 * <p><strong>Subclass example:</strong>
 * <pre>{@code
 * PipeBee<String, Integer> lengthPipe = new PipeBee<String, Integer>(2, hive) {
 *     @Override
 *     protected Integer process(String s) {
 *         return s.length();
 *     }
 * };
 * }</pre>
 *
 * <p><strong>Lambda factory example:</strong>
 * <pre>{@code
 * Hive hive = new Hive(4);
 * Bee<Integer> sink = Bee.bee(hive, n -> System.out.println("Length: " + n));
 *
 * PipeBee<String, Integer> pipe = PipeBee.pipe(hive, String::length);
 * pipe.setOut(sink);
 *
 * pipe.send("hello");
 * pipe.send("world");
 * Bee.shutdownAndAwaitTermination(pipe, sink);
 * }</pre>
 *
 * @param <I> the type of messages received by this Bee
 * @param <O> the type of messages produced and forwarded to the downstream Bee
 *
 * @see InOutBee
 * @see Bee
 */
public abstract class PipeBee<I,O> extends InOutBee<I,O>
{
    /**
     * Creates a {@code PipeBee} with the specified concurrency, hive, and queue capacity.
     *
     * @param threads   the maximum number of concurrent worker threads; {@code 0} defaults
     *                  to {@link Runtime#availableProcessors()}
     * @param hive      the {@link Hive} that schedules work; {@code null} for synchronous processing
     * @param queueSize the maximum number of queued inbound messages; {@code 0} uses the default
     */
    public PipeBee(int threads, Hive hive, int queueSize)
    {
        super(threads, hive, queueSize);
    }

    /**
     * Creates a {@code PipeBee} with the specified concurrency and hive, using the default queue size.
     *
     * @param threads the maximum number of concurrent worker threads; {@code 0} defaults
     *                to {@link Runtime#availableProcessors()}
     * @param hive    the {@link Hive} that schedules work; {@code null} for synchronous processing
     */
    public PipeBee(int threads, Hive hive)
    {
        super(threads, hive);
    }

    /**
     * Creates a {@code PipeBee} with the specified concurrency, no hive (synchronous),
     * and the default queue size.
     *
     * @param threads the maximum number of concurrent worker threads; {@code 0} defaults
     *                to {@link Runtime#availableProcessors()}
     */
    public PipeBee(int threads)
    {
        super(threads);
    }

    /**
     * Creates a {@code PipeBee} with all defaults: processor-count threads,
     * no hive, and default queue size.
     */
    public PipeBee()
    {
    }

    /**
     * Implements the {@link Bee#receive(Object)} contract by transforming the inbound
     * message via {@link #process(Object)} and forwarding the result downstream.
     *
     * <p>Subclasses should not override this method; override {@link #process(Object)} instead.
     *
     * @param i the inbound message to transform and forward
     */
    @Override
    protected void receive(I i)
    {
        sendOut(process(i));
    }

    /**
     * Transforms a single inbound message into an outbound message.
     *
     * <p>Implementations must be thread-safe when the Bee is configured with more than
     * one worker thread.
     *
     * @param i the inbound message
     * @return the transformed outbound message to be forwarded downstream; must not be {@code null}
     *         unless the downstream Bee explicitly handles null messages
     */
    protected abstract O process(I i);

    /**
     * Creates a synchronous single-threaded {@code PipeBee} from a {@link Function}.
     *
     * <p>This is a convenience factory for the common case where no {@link Hive} is needed
     * and the default thread count is acceptable.
     *
     * @param <I> the input type
     * @param <O> the output type
     * @param fn  the transformation function; must not be {@code null}
     * @return a new {@code PipeBee} that applies {@code fn} to each inbound message
     */
    public static <I,O> PipeBee<I, O> pipe(Function<I, O> fn)
    {
        return PipeBee.pipe(0, null, fn);
    }

    /**
     * Creates a {@code PipeBee} associated with the given {@link Hive}, using the default
     * thread count, from a {@link Function}.
     *
     * @param <I>  the input type
     * @param <O>  the output type
     * @param hive the {@link Hive} to schedule work on; {@code null} for synchronous processing
     * @param fn   the transformation function; must not be {@code null}
     * @return a new {@code PipeBee} that applies {@code fn} to each inbound message
     */
    public static <I,O> PipeBee<I, O> pipe(Hive hive, Function<I, O> fn)
    {
        return PipeBee.pipe(0, hive, fn);
    }

    /**
     * Creates a fully configured {@code PipeBee} from a {@link Function}.
     *
     * <p>This is the primary factory method; the other {@code pipe} overloads delegate here.
     *
     * @param <I>     the input type
     * @param <O>     the output type
     * @param threads the maximum number of concurrent worker threads; {@code 0} defaults
     *                to {@link Runtime#availableProcessors()}
     * @param hive    the {@link Hive} to schedule work on; {@code null} for synchronous processing
     * @param fn      the transformation function; must not be {@code null}
     * @return a new {@code PipeBee} that applies {@code fn} to each inbound message
     */
    public static <I,O> PipeBee<I, O> pipe(int threads, Hive hive, Function<I, O> fn)
    {
        return new PipeBee<I, O>(threads, hive)
        {
            @Override
            protected O process(I i)
            {
                return fn.apply(i);
            }
        };
    }
}
