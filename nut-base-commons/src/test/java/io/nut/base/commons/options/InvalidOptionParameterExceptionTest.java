/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class InvalidOptionParameterExceptionTest
{
    @Test
    public void testConstructor_String()
    {
        InvalidOptionParameterException instance = new InvalidOptionParameterException("boom");

        assertEquals("boom", instance.getMessage());
        assertNull(instance.getName());
        assertNull(instance.getValue());
        assertTrue(instance instanceof OptionException);
    }

    @Test
    public void testConstructor_String_String()
    {
        InvalidOptionParameterException instance = new InvalidOptionParameterException("--count", "abc");

        assertEquals("invalid argument 'abc' to '--count'", instance.getMessage());
        assertEquals("--count", instance.getName());
        assertEquals("abc", instance.getValue());
    }
}
