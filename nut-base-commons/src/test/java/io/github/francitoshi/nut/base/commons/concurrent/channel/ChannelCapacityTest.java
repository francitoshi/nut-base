/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.concurrent.channel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A negative capacity (from configuration, input or an arithmetic overflow)
 * used to silently create an unbounded channel, i.e. a memory-exhaustion
 * vector. Unbounded channels now have to be requested explicitly with
 * {@link Channel#UNLIMITED}.
 */
class ChannelCapacityTest
{
    @Test
    void negativeCapacitiesAreRejected()
    {
        for (int capacity : new int[]{-2, -100, Integer.MIN_VALUE})
        {
            assertThrows(IllegalArgumentException.class, () -> Channel.of(capacity), "of(" + capacity + ")");
            assertThrows(IllegalArgumentException.class, () -> Channel.closeableOf(capacity), "closeableOf(" + capacity + ")");
        }
    }

    @Test
    void unlimitedMustBeRequestedExplicitly()
    {
        assertEquals(-1, Channel.UNLIMITED);
        assertTrue(Channel.of(Channel.UNLIMITED) instanceof UnlimitedChannel);
        assertTrue(Channel.closeableOf(Channel.UNLIMITED) instanceof CloseableUnlimitedChannel);
    }

    @Test
    void nonNegativeCapacitiesKeepTheirMeaning()
    {
        assertTrue(Channel.of(0) instanceof UnbufferedChannel);
        assertTrue(Channel.of(3) instanceof BufferedChannel);
        assertTrue(Channel.closeableOf(0) instanceof CloseableUnbufferedChannel);
        assertTrue(Channel.closeableOf(3) instanceof CloseableBufferedChannel);
    }
}
