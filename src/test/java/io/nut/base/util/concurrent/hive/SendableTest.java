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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Sendable} is a one-method functional interface that's exercised
 * end-to-end by virtually every other test in this suite, since every
 * Bee implements it. These two tests just pin down its minimal,
 * direct contract.
 */
class SendableTest
{
    @Test
    void isAFunctionalInterfaceImplementableWithALambda()
    {
        List<String> received = new ArrayList<>();
        Sendable<String> sendable = received::add;

        assertTrue(sendable.send("hi"));
        assertEquals("hi", received.get(0));
    }

    @Test
    void implementationsCanSignalRejectionByReturningFalse()
    {
        Sendable<String> alwaysRejects = message -> false;
        assertFalse(alwaysRejects.send("anything"));
    }
}
