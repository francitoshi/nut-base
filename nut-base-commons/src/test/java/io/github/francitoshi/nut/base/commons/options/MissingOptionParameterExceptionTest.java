/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class MissingOptionParameterExceptionTest
{
    @Test
    public void testConstructor_String()
    {
        MissingOptionParameterException instance = new MissingOptionParameterException("boom");

        assertEquals("boom", instance.getMessage());
        assertTrue(instance instanceof OptionException);
    }

    @Test
    public void testConstructor_Option()
    {
        BooleanOption option = new BooleanOption('v', "verbose");
        option.parseLong(0, new String[]{"--verbose"});

        MissingOptionParameterException instance = new MissingOptionParameterException(option);

        assertEquals("missing argument to '--verbose'", instance.getMessage());
    }
}
