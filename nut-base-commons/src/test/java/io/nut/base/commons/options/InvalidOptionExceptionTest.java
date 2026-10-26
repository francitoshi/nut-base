/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class InvalidOptionExceptionTest
{
    @Test
    public void testConstructor()
    {
        InvalidOptionException instance = new InvalidOptionException("invalid option");

        assertEquals("invalid option", instance.getMessage());
        assertTrue(instance instanceof OptionException);
    }
}
