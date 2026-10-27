/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class BooleanOptionTest
{
    @Test
    public void testGettersAndConstructors()
    {
        BooleanOption longOnly = new BooleanOption("verbose");
        assertEquals("verbose", longOnly.getLongName());
        assertEquals(0, longOnly.getShortName());
        assertNull(longOnly.getUsedName());
        assertFalse(longOnly.isUsed());
        assertFalse(longOnly.isCommand());
        assertEquals(0, longOnly.getCount());
        assertEquals(0, longOnly.getLastUsed());

        BooleanOption withShort = new BooleanOption('v', "verbose");
        assertEquals('v', withShort.getShortName());

        BooleanOption withHelp = new BooleanOption("verbose", "brief", "full");
        assertEquals("verbose", withHelp.getLongName());

        BooleanOption withShortAndHelp = new BooleanOption('v', "verbose", "brief", "full");
        assertEquals('v', withShortAndHelp.getShortName());
    }

    @Test
    public void testAddCount()
    {
        BooleanOption instance = new BooleanOption('v', "verbose");

        instance.addCount();
        instance.addCount();

        assertEquals(2, instance.getCount());
        assertTrue(instance.isUsed());
    }

    @Test
    public void testParseLong()
    {
        BooleanOption instance = new BooleanOption('v', "verbose");

        assertEquals(0, instance.parseLong(0, new String[]{"--other"}));
        assertEquals(0, instance.parseLong(0, new String[]{"verbose"}));
        assertEquals(0, instance.parseLong(0, new String[]{"-v"}));

        assertEquals(1, instance.parseLong(0, new String[]{"--verbose"}));
        assertEquals(1, instance.getCount());
        assertEquals(0, instance.getLastUsed());
        assertEquals("--verbose", instance.getUsedName());

        assertEquals(1, instance.parseLong(1, new String[]{"other", "-verbose"}));
        assertEquals(2, instance.getCount());
        assertEquals(1, instance.getLastUsed());
        assertEquals("-verbose", instance.getUsedName());
    }

    @Test
    public void testParseShort()
    {
        BooleanOption instance = new BooleanOption('v', "verbose");

        assertEquals(0, instance.parseShort(0, 1, new String[]{"-x"}));

        assertEquals(1, instance.parseShort(1, 1, new String[]{"other", "-v"}));
        assertEquals(1, instance.getCount());
        assertEquals(1, instance.getLastUsed());
        assertEquals("v", instance.getUsedName());

        assertEquals(1, instance.parseShort(0, 2, new String[]{"-vv"}));
        assertEquals(2, instance.getCount());
    }

    @Test
    public void testToString()
    {
        BooleanOption instance = new BooleanOption('v', "verbose");
        assertEquals("--verbose=false (count=0)", instance.toString());

        instance.addCount();
        assertEquals("--verbose=true (count=1)", instance.toString());
    }
}
