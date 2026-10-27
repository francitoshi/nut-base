/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class CommandOptionTest
{
    @Test
    public void testConstructors()
    {
        CommandOption longOnly = new CommandOption("help");
        assertEquals("help", longOnly.getLongName());
        assertEquals(0, longOnly.getShortName());
        assertTrue(longOnly.isCommand());

        CommandOption withShort = new CommandOption('h', "help");
        assertEquals('h', withShort.getShortName());
        assertTrue(withShort.isCommand());
    }

    @Test
    public void testParseLong()
    {
        CommandOption instance = new CommandOption('h', "help");

        assertEquals(1, instance.parseLong(0, new String[]{"help"}));
        assertTrue(instance.isUsed());
        assertEquals("help", instance.getUsedName());

        assertEquals(0, instance.parseLong(0, new String[]{"--help"}));
        assertEquals(0, instance.parseLong(0, new String[]{"-help"}));

        assertEquals(1, instance.parseLong(0, new String[]{"h"}));
        assertEquals(2, instance.getCount());
        assertEquals("h", instance.getUsedName());
    }

    @Test
    public void testParseShort()
    {
        CommandOption instance = new CommandOption('h', "help");

        assertEquals(1, instance.parseShort(0, 1, new String[]{"-h"}));
        assertTrue(instance.isUsed());
        assertEquals("h", instance.getUsedName());
    }

    @Test
    public void testIsUsed()
    {
        CommandOption unused = new CommandOption("help");
        CommandOption used = new CommandOption("version");
        used.addCount();

        assertFalse(CommandOption.isUsed(new CommandOption[0]));
        assertFalse(CommandOption.isUsed(unused));
        assertTrue(CommandOption.isUsed(unused, used));
        assertTrue(CommandOption.isUsed(used));
    }
}
