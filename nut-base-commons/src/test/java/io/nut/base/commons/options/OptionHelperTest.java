/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.options;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class OptionHelperTest
{
    private static OptionParser newParser()
    {
        OptionParser parser = new OptionParser("prog");
        parser.add(new CommandOption("help"));
        parser.add(new BooleanOption('v', "verbose"));
        parser.add(new StringOption('o', "output"));
        return parser;
    }

    private static String help(OptionHelper helper, boolean usage, boolean brief, boolean full, Locale locale) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8.name()))
        {
            helper.help(out, usage, brief, full, locale);
        }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    public void testHelpUsageOnly() throws IOException
    {
        OptionHelper instance = new OptionHelper(newParser(), HelpTarget.class);

        String text = help(instance, true, false, false, Locale.ENGLISH);

        assertEquals("usage: prog", text);
    }

    @Test
    public void testHelpBrief() throws IOException
    {
        OptionHelper instance = new OptionHelper(newParser(), HelpTarget.class);

        String text = help(instance, true, true, false, Locale.ENGLISH);

        assertTrue(text.startsWith("usage: prog"));
        assertTrue(text.contains("[-v|--verbose]"));
        assertTrue(text.contains("[-o|--output]"));
        assertTrue(text.contains("command [args]"));
        assertFalse(text.contains("--help"));
        assertFalse(text.contains("commands:"));
        assertFalse(text.contains("options:"));
    }

    @Test
    public void testHelpFull() throws IOException
    {
        OptionHelper instance = new OptionHelper(newParser(), HelpTarget.class);

        String text = help(instance, true, false, true, Locale.ENGLISH);

        assertTrue(text.startsWith("usage: prog"));
        assertTrue(text.contains("commands:"));
        assertTrue(text.contains("help"));
        assertTrue(text.contains("show the help"));
        assertTrue(text.contains("options:"));
        assertTrue(text.contains("--verbose"));
        assertTrue(text.contains("enable verbose output"));
        assertTrue(text.contains("--output"));
        assertTrue(text.contains("write the result to a file"));
    }

    @Test
    public void testHelpBriefAndFull() throws IOException
    {
        OptionHelper instance = new OptionHelper(newParser(), HelpTarget.class);

        String text = help(instance, true, true, true, Locale.ENGLISH);

        assertTrue(text.contains("[-v|--verbose]"));
        assertTrue(text.contains("command [args]"));
        assertTrue(text.contains("commands:"));
        assertTrue(text.contains("options:"));
    }

    @Test
    public void testHelpDefaultLocale() throws IOException
    {
        OptionHelper instance = new OptionHelper(newParser(), HelpTarget.class);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8.name()))
        {
            instance.help(out, true, false, false);
        }

        assertEquals("usage: prog", new String(bytes.toByteArray(), StandardCharsets.UTF_8));
    }
}
