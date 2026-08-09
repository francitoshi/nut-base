/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Peer}.
 */
public class PeerTest
{
    /**
     * Verifies that the constructor stores the given key and value,
     * both of the same type, and that they are returned unchanged by
     * the inherited {@link Pair#getKey()} and {@link Pair#getVal()}.
     */
    @Test
    public void testConstructorAndGetters()
    {
        Peer<String> peer = new Peer<>("alice", "bob");

        assertEquals("alice", peer.getKey());
        assertEquals("bob", peer.getVal());
    }

    /**
     * Verifies that {@link Peer} correctly inherits the alias getters
     * from {@link Pair} (e.g. {@code get1st}/{@code get2nd}).
     */
    @Test
    public void testInheritedAliasGetters()
    {
        Peer<Integer> peer = new Peer<>(10, 20);

        assertEquals(10, peer.get1st());
        assertEquals(20, peer.get2nd());
        assertEquals(10, peer.getLeft());
        assertEquals(20, peer.getRight());
    }

    /**
     * Verifies that {@link Peer#inverse()} returns a new {@code Peer}
     * (not just a {@code Pair}) with key and value swapped, and that the
     * original peer is left unmodified.
     */
    @Test
    public void testInverseReturnsPeerWithSwappedValues()
    {
        Peer<String> peer = new Peer<>("first", "second");

        Peer<String> inverted = peer.inverse();

        assertTrue(inverted instanceof Peer);
        assertEquals("second", inverted.getKey());
        assertEquals("first", inverted.getVal());

        // original peer must remain unchanged
        assertEquals("first", peer.getKey());
        assertEquals("second", peer.getVal());
    }

    /**
     * Verifies that applying {@link Peer#inverse()} twice returns to a
     * peer equal to the original.
     */
    @Test
    public void testDoubleInverseRestoresOriginal()
    {
        Peer<String> peer = new Peer<>("first", "second");

        Peer<String> twiceInverted = peer.inverse().inverse();

        assertEquals(peer, twiceInverted);
    }

    /**
     * Verifies that two peers with the same key and value, of the same
     * type, are equal and share the same hash code, using the
     * {@code equals}/{@code hashCode} inherited from {@link Pair}.
     */
    @Test
    public void testEqualsAndHashCodeInheritedFromPair()
    {
        Peer<String> p1 = new Peer<>("x", "y");
        Peer<String> p2 = new Peer<>("x", "y");
        Peer<String> p3 = new Peer<>("y", "x");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
    }

    /**
     * Verifies that a {@code Peer} can hold equal key and value (i.e. a
     * self-loop / identical pair), which is a valid use case for a
     * homogeneous pair.
     */
    @Test
    public void testPeerWithEqualKeyAndValue()
    {
        Peer<String> peer = new Peer<>("same", "same");

        assertEquals("same", peer.getKey());
        assertEquals("same", peer.getVal());
        assertEquals(peer, peer.inverse());
    }

    /**
     * Verifies that the inherited {@link Pair#toString()} format is
     * preserved for {@code Peer} instances.
     */
    @Test
    public void testToString()
    {
        Peer<String> peer = new Peer<>("a", "b");

        assertEquals("a=b", peer.toString());
    }
}
