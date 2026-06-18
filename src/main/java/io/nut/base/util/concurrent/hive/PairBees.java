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
 * A Bee that forwards every received message of type {@code F} to a
 * "forward" companion Bee, and that can produce an inverse Bee which
 * forwards messages of type {@code B} back to a "backward" companion Bee.
 * Pair instances are typically used to link two independent Bee chains
 * so that messages can flow in opposite directions between them.
 *
 * @param <F> the type of messages this Pair receives and forwards
 * @param <B> the type of messages handled by the inverse Pair
 */
public class PairBees<F, B> implements Sendable<F>
{

    protected final Bee<F> fw;
    protected final Bee<B> bw;
    protected volatile PairBees<B, F> inv;

    /**
     * Creates an Pair with a pre-existing inverse instance, used
     * internally to link a pair of mutually-inverse Pairs.
     *
     * @param fw the forward Bee that received messages are sent to
     * @param bw the backward Bee, used to construct the inverse Pair
     * @param inv the already-created inverse Pair, or null
     */
    protected PairBees(Bee<F> fw, Bee<B> bw, PairBees<B, F> inv)
    {
        this.fw = fw;
        this.bw = bw;
        this.inv = inv;
    }

    /**
     * Creates a new Pair linking the given forward and backward Bees.
     * The inverse Pair is lazily created on the first call to {@link #inverse()}.
     *
     * @param fw the forward Bee that received messages are sent to
     * @param bw the backward Bee used by the inverse Pair
     */
    public PairBees(Bee<F> fw, Bee<B> bw)
    {
        this.fw = fw;
        this.bw = bw;
        this.inv = null;
    }

    @Override
    public boolean send(F f)
    {
        return this.fw.send(f);
    }

    /**
     * Returns the inverse of this Pair, lazily creating it on first
     * invocation. The inverse forwards messages of type {@code B} to the
     * backward Bee.
     *
     * @return the inverse Pair, linked back to this instance
     */
    public PairBees<B, F> inverse()
    {
        return inv != null ? inv : (inv = new PairBees<>(bw, fw, this));
    }
    
}
