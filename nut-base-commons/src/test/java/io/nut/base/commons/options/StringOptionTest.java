/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class StringOptionTest
{
    @Test
    public void testConstructors()
    {
        StringOption longOnly = new StringOption("output");
        assertEquals("output", longOnly.getLongName());
        assertEquals(0, longOnly.getShortName());

        StringOption withShort = new StringOption('o', "output");
        assertEquals('o', withShort.getShortName());
    }

    @Test
    public void testGetValue() throws MissingOptionParameterException
    {
        StringOption instance = new StringOption('o', "output");

        assertThrows(MissingOptionParameterException.class, instance::getValue);
        assertEquals("default", instance.getValue("default"));

        instance.setValue("file.txt");

        assertEquals("file.txt", instance.getValue());
        assertEquals("default", instance.getValue("default"));

        instance.addCount();

        assertEquals("file.txt", instance.getValue("default"));
    }

    @Test
    public void testParseLong_SeparateValue() throws MissingOptionParameterException
    {
        StringOption instance = new StringOption('o', "output");

        assertEquals(2, instance.parseLong(0, new String[]{"--output", "file.txt"}));
        assertTrue(instance.isUsed());
        assertEquals(1, instance.getCount());
        assertEquals("--output", instance.getUsedName());
        assertEquals("file.txt", instance.getValue());
    }

    @Test
    public void testParseLong_EqualsValue() throws MissingOptionParameterException
    {
        StringOption instance = new StringOption('o', "output");

        assertEquals(1, instance.parseLong(0, new String[]{"--output=file.txt"}));
        assertEquals("--output", instance.getUsedName());
        assertEquals("file.txt", instance.getValue());

        StringOption singleHyphen = new StringOption('o', "output");
        assertEquals(1, singleHyphen.parseLong(0, new String[]{"-output=file.txt"}));
        assertEquals("-output", singleHyphen.getUsedName());
        assertEquals("file.txt", singleHyphen.getValue());

        StringOption attached = new StringOption('o', "output");
        assertEquals(1, attached.parseLong(0, new String[]{"--outputfile.txt"}));
        assertEquals("file.txt", attached.getValue());
    }

    @Test
    public void testParseLong_MissingValue() throws MissingOptionParameterException
    {
        StringOption instance = new StringOption('o', "output");

        assertEquals(1, instance.parseLong(0, new String[]{"--output"}));
        assertEquals("", instance.getValue());

        StringOption empty = new StringOption('o', "output");
        assertEquals(1, empty.parseLong(0, new String[]{"--output="}));
        assertEquals("", empty.getValue());
    }

    @Test
    public void testParseLong_NoMatch()
    {
        StringOption instance = new StringOption('o', "output");

        assertEquals(0, instance.parseLong(0, new String[]{"--other"}));
        assertFalse(instance.isUsed());
    }

    @Test
    public void testParseShort() throws MissingOptionParameterException
    {
        StringOption attached = new StringOption('o', "output");
        assertEquals(2, attached.parseShort(0, 1, new String[]{"-ofile.txt"}));
        assertEquals("o", attached.getUsedName());
        assertEquals("file.txt", attached.getValue());

        StringOption separate = new StringOption('o', "output");
        assertEquals(3, separate.parseShort(0, 1, new String[]{"-o", "file.txt"}));
        assertEquals("file.txt", separate.getValue());

        StringOption last = new StringOption('o', "output");
        assertEquals(2, last.parseShort(0, 1, new String[]{"-o"}));
        assertEquals("", last.getValue());

        StringOption noMatch = new StringOption('o', "output");
        assertEquals(0, noMatch.parseShort(0, 1, new String[]{"-x"}));
        assertFalse(noMatch.isUsed());
    }

    @Test
    public void testToString()
    {
        StringOption instance = new StringOption('o', "output");
        assertEquals("--output=null", instance.toString());

        instance.setValue("file.txt");
        assertEquals("--output=file.txt", instance.toString());
    }
}
