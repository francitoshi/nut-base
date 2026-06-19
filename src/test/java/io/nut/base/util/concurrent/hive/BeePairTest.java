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
 * Unit tests for {@link BeePair}: forward delegation, lazy creation and
 * caching of the inverse, and the inverse-of-the-inverse identity.
 */
class BeePairTest
{
    @Test
    void sendDelegatesToTheForwardBee()
    {
        RecordingBee<String> fw = new RecordingBee<>();
        RecordingBee<Integer> bw = new RecordingBee<>();
        BeePair<String,Integer> pair = new BeePair<>(fw, bw);

        assertTrue(pair.send("hello"));

        assertEquals(Collections.singletonList("hello"), fw.received);
        assertTrue(bw.received.isEmpty());
    }

    @Test
    void inverseSendsThroughTheBackwardBee()
    {
        RecordingBee<String> fw = new RecordingBee<>();
        RecordingBee<Integer> bw = new RecordingBee<>();
        BeePair<String,Integer> pair = new BeePair<>(fw, bw);

        BeePair<Integer,String> inv = pair.inverse();
        assertTrue(inv.send(42));

        assertEquals(Collections.singletonList(42), bw.received);
        assertTrue(fw.received.isEmpty());
    }

    @Test
    void inverseIsLazilyCreatedAndCached()
    {
        BeePair<String,Integer> pair = new BeePair<>(new RecordingBee<>(), new RecordingBee<>());

        BeePair<Integer,String> inv1 = pair.inverse();
        BeePair<Integer,String> inv2 = pair.inverse();

        assertSame(inv1, inv2);
    }

    @Test
    void inverseOfInverseReturnsTheOriginalPair()
    {
        BeePair<String,Integer> pair = new BeePair<>(new RecordingBee<>(), new RecordingBee<>());

        BeePair<Integer,String> inv = pair.inverse();

        assertSame(pair, inv.inverse());
    }
}
