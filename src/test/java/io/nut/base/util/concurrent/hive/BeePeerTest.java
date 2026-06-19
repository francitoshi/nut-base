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

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BeePeer}, the symmetric (same message type in
 * both directions) specialization of {@link PairBees}.
 */
class BeePeerTest
{
    @Test
    void sendDelegatesToTheForwardBee()
    {
        RecordingBee<String> fw = new RecordingBee<>();
        RecordingBee<String> bw = new RecordingBee<>();
        BeePeer<String> peer = new BeePeer<>(fw, bw);

        assertTrue(peer.send("ping"));

        assertEquals(Collections.singletonList("ping"), fw.received);
        assertTrue(bw.received.isEmpty());
    }

    @Test
    void inverseSendsThroughTheBackwardBeeAndIsCached()
    {
        RecordingBee<String> fw = new RecordingBee<>();
        RecordingBee<String> bw = new RecordingBee<>();
        BeePeer<String> peer = new BeePeer<>(fw, bw);

        BeePeer<String> inv = peer.inverse();
        inv.send("pong");

        assertEquals(Collections.singletonList("pong"), bw.received);
        assertSame(inv, peer.inverse());
    }

    @Test
    void inverseOfInverseReturnsTheOriginalPeer()
    {
        BeePeer<String> peer = new BeePeer<>(new RecordingBee<>(), new RecordingBee<>());

        BeePeer<String> inv = peer.inverse();

        assertSame(peer, inv.inverse());
    }
}
