/*
 *  InOutBee.java
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

/**
 * An abstract intermediate Bee that connects two stages in a processing pipeline
 * by adding a typed output channel to the base {@link Bee} actor.
 *
 * <p>{@code InOutBee} extends {@link Bee}{@code <I>} with a configurable downstream
 * {@link Bee}{@code <O>}, allowing processed results to be forwarded automatically
 * to the next stage. Subclasses define the processing logic by implementing
 * {@link Bee#receive(Object)} and forwarding their results by calling
 * {@link #sendOut(Object)}.
 *
 * <p><strong>Pipeline example:</strong>
 * <pre>{@code
 * Hive hive = new Hive(4);
 *
 * // Terminal sink: prints integers
 * Bee<Integer> printer = Bee.bee(hive, System.out::println);
 *
 * // Intermediate stage: parses strings into integers and forwards them
 * InOutBee<String, Integer> parser = new InOutBee<String, Integer>(1, hive) {
 *     @Override
 *     protected void receive(String s) {
 *         sendOut(Integer.parseInt(s));
 *     }
 * };
 * parser.setOut(printer);
 *
 * parser.send("42");
 * parser.send("7");
 * Bee.shutdownAndAwaitTermination(parser, printer);
 * }</pre>
 *
 * @param <I> the type of messages this Bee receives and processes
 * @param <O> the type of messages forwarded to the downstream Bee
 *
 * @see Bee
 * @see PipeBee
 */
public abstract class InOutBee<I,O> extends Bee<I>
{
    /**
     * The downstream Bee that receives the output of this stage.
     * Declared {@code volatile} to ensure visibility across threads when set
     * after construction via {@link #setOut(Bee)}.
     */
    protected volatile Bee<O> out;

    /**
     * Creates an {@code InOutBee} with the specified concurrency, hive, and queue capacity.
     *
     * @param threads   the maximum number of concurrent worker threads; {@code 0} defaults
     *                  to {@link Runtime#availableProcessors()}
     * @param hive      the {@link Hive} that schedules work; {@code null} for synchronous processing
     * @param queueSize the maximum number of queued inbound messages; {@code 0} uses the default
     */
    public InOutBee(int threads, Hive hive, int queueSize)
    {
        super(threads, hive, queueSize);
    }

    /**
     * Creates an {@code InOutBee} with the specified concurrency and hive, using the default queue size.
     *
     * @param threads the maximum number of concurrent worker threads; {@code 0} defaults
     *                to {@link Runtime#availableProcessors()}
     * @param hive    the {@link Hive} that schedules work; {@code null} for synchronous processing
     */
    public InOutBee(int threads, Hive hive)
    {
        super(threads, hive);
    }

    /**
     * Creates an {@code InOutBee} with the specified concurrency, no hive (synchronous),
     * and the default queue size.
     *
     * @param threads the maximum number of concurrent worker threads; {@code 0} defaults
     *                to {@link Runtime#availableProcessors()}
     */
    public InOutBee(int threads)
    {
        super(threads);
    }

    /**
     * Creates an {@code InOutBee} with all defaults: processor-count threads,
     * no hive, and default queue size.
     */
    public InOutBee()
    {
    }

    /**
     * Sets the downstream {@link Bee} that will receive output messages produced by this stage.
     *
     * <p>This method returns {@code this} to support fluent-style pipeline construction:
     * <pre>{@code
     * parser.setOut(formatter).setOut(sink); // reassign as needed
     * }</pre>
     *
     * @param out the downstream Bee; must not be {@code null} before {@link #sendOut(Object)} is called
     * @return this instance, for method chaining
     */
    public InOutBee<I,O> setOut(Bee<O> out)
    {
        this.out = out;
        return this;
    }

    /**
     * Forwards an output message to the downstream {@link Bee} set via {@link #setOut(Bee)}.
     *
     * <p>This method is intended to be called from within {@link Bee#receive(Object)} to
     * propagate a result to the next pipeline stage.
     *
     * @param o the output message to forward
     * @return {@code true} if the message was accepted by the downstream Bee,
     *         {@code false} if the downstream Bee rejected it (e.g. it has been shut down)
     * @throws IllegalStateException if no downstream Bee has been set via {@link #setOut(Bee)}
     */
    public final boolean sendOut(O o)
    {
        Bee<O> target = this.out;
        if (target == null)
        {
            throw new IllegalStateException("out Bee not set");
        }
        return this.out.send(o);
    }
    
}
