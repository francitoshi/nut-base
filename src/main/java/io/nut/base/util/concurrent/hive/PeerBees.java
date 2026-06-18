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
 * A symmetric specialization of {@link PairBees} where both directions
 * carry the same message type {@code M}. A Peer forwards messages it
 * receives to its forward Bee, and its inverse forwards messages to its
 * backward Bee.
 *
 * @param <M> the type of messages exchanged in both directions
 */
public class PeerBees<M> extends PairBees<M, M>
{

    /**
     * Creates a Peer with a pre-existing inverse instance, used
     * internally to link a pair of mutually-inverse Peers.
     *
     * @param fw the forward Bee that received messages are sent to
     * @param bw the backward Bee, used to construct the inverse Peer
     * @param inv the already-created inverse Peer, or null
     */
    private PeerBees(Bee<M> fw, Bee<M> bw, PeerBees<M> inv)
    {
        super(fw, bw, inv);
    }

    /**
     * Creates a new Peer linking the given forward and backward Bees.
     * The inverse Peer is lazily created on the first call to {@link #inverse()}.
     *
     * @param fw the forward Bee that received messages are sent to
     * @param bw the backward Bee used by the inverse Peer
     */
    public PeerBees(Bee<M> fw, Bee<M> bw)
    {
        super(fw, bw);
    }

    /**
     * Returns the inverse of this Peer, lazily creating it on first
     * invocation. The inverse forwards messages to the backward Bee.
     *
     * @return the inverse Peer, linked back to this instance
     */
    @Override
    public PeerBees<M> inverse()
    {
        return inv != null ? (PeerBees<M>) inv : (PeerBees<M>) (inv = new PeerBees<>(bw, fw, this));
    }
    
}
