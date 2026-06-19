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

/**
 * A bidirectional bridge between two independent {@link Bee} chains, enabling
 * messages to flow in opposite directions between them.
 * <p>
 * A {@code PairBees<F,B>} acts as a {@link Sendable}{@code <F>}: calling
 * {@link #send(Object)} with a value of type {@code F} forwards it to the
 * <em>forward</em> Bee ({@code fw}). Calling {@link #inverse()} returns the
 * companion {@code PairBees<B,F>}, whose {@link #send(Object)} forwards values
 * of type {@code B} to the <em>backward</em> Bee ({@code bw}). The two
 * instances form a mutually-referencing pair that is lazily created on the
 * first call to {@link #inverse()}.
 * <p>
 * Typical use: linking two otherwise disconnected pipelines so that output
 * produced by one can be fed as input to the other, and vice-versa.
 * <pre>{@code
 * PairBees<Request, Response> pair = new PairBees<>(requestBee, responseBee);
 * // Sending a request:
 * pair.send(new Request(...));
 * // Sending a response back:
 * pair.inverse().send(new Response(...));
 * }</pre>
 *
 * @param <F> the type of messages forwarded in the "forward" direction
 * @param <B> the type of messages forwarded in the "backward" direction
 */
public class BeePair<F, B> implements Sendable<F>
{
    /** The Bee that receives forward ({@code F}) messages. */
    protected final Bee<F> fw;
    /** The Bee that receives backward ({@code B}) messages. */
    protected final Bee<B> bw;
    /** The inverse pair, lazily created on the first call to {@link #inverse()}. */
    protected volatile BeePair<B, F> inv;

    /**
     * Creates a {@code PairBees} with a pre-existing inverse, used internally
     * to link a pair of mutually-inverse instances.
     *
     * @param fw  the forward Bee that received messages are sent to
     * @param bw  the backward Bee, used to construct the inverse pair
     * @param inv the already-created inverse pair, or {@code null}
     */
    protected BeePair(Bee<F> fw, Bee<B> bw, BeePair<B, F> inv)
    {
        this.fw = fw;
        this.bw = bw;
        this.inv = inv;
    }

    /**
     * Creates a new {@code PairBees} linking the given forward and backward
     * Bees. The inverse pair is created lazily on the first call to
     * {@link #inverse()}.
     *
     * @param fw the forward Bee that received messages are sent to
     * @param bw the backward Bee used by the inverse pair
     */
    public BeePair(Bee<F> fw, Bee<B> bw)
    {
        this.fw = fw;
        this.bw = bw;
        this.inv = null;
    }

    /**
     * Sends a message of type {@code F} to the forward Bee.
     *
     * @param f the message to deliver to the forward Bee
     * @return {@code true} if the forward Bee accepted the message;
     *         {@code false} if it was rejected (e.g. the Bee is shut down)
     */
    @Override
    public boolean send(F f)
    {
        return this.fw.send(f);
    }

    /**
     * Returns the inverse of this pair, lazily creating it on the first
     * invocation. The inverse is a {@code PairBees<B,F>} whose
     * {@link #send(Object)} forwards messages of type {@code B} to the backward
     * Bee, and whose own {@link #inverse()} returns this instance.
     *
     * @return the inverse pair; never {@code null}
     */
    public BeePair<B, F> inverse()
    {
        return inv != null ? inv : (inv = new BeePair<>(bw, fw, this));
    }
}
