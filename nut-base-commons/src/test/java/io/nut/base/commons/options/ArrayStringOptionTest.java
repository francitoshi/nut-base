/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.options;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class ArrayStringOptionTest
{
    @Test
    public void testConstructors()
    {
        ArrayStringOption longOnly = new ArrayStringOption("list");
        assertEquals("list", longOnly.getLongName());
        assertEquals(0, longOnly.getShortName());

        ArrayStringOption withShort = new ArrayStringOption('l', "list");
        assertEquals('l', withShort.getShortName());

        ArrayStringOption withSeparator = new ArrayStringOption("list", ',');
        assertEquals("list", withSeparator.getLongName());

        ArrayStringOption withShortAndSeparator = new ArrayStringOption('l', "list", ',');
        assertEquals('l', withShortAndSeparator.getShortName());
    }

    @Test
    public void testSetValueWithoutSeparator()
    {
        ArrayStringOption instance = new ArrayStringOption('l', "list");

        instance.setValue("one");
        instance.setValue("two");

        assertArrayEquals(new String[]{"one", "two"}, instance.getValues());
    }

    @Test
    public void testSetValueWithSeparator()
    {
        ArrayStringOption instance = new ArrayStringOption('l', "list", ',');

        instance.setValue("one,two,three");
        instance.setValue("four,,five,");

        assertArrayEquals(new String[]{"one", "two", "three", "four", "five"}, instance.getValues());
    }

    @Test
    public void testSetValueWithSeparatorEmptyValue()
    {
        ArrayStringOption instance = new ArrayStringOption("list", ',');

        instance.setValue("");

        assertArrayEquals(new String[0], instance.getValues());
    }

    @Test
    public void testGetValuesReturnsCopy()
    {
        ArrayStringOption instance = new ArrayStringOption("list");
        instance.setValue("one");

        String[] values = instance.getValues();
        values[0] = "changed";

        assertArrayEquals(new String[]{"one"}, instance.getValues());
    }

    @Test
    public void testToString()
    {
        ArrayStringOption instance = new ArrayStringOption("list");
        instance.setValue("one");
        instance.setValue("two");

        assertEquals("--list=[one, two])", instance.toString());
    }
}
