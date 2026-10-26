/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.options;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class OptionParserTest
{
    @Test
    public void testConstructorsAndAdd()
    {
        OptionParser unnamed = new OptionParser();
        assertNull(unnamed.name);

        OptionParser named = new OptionParser("prog");
        assertEquals("prog", named.name);

        BooleanOption option = new BooleanOption('v', "verbose");
        assertSame(option, named.add(option));
    }

    @Test
    public void testParseLongOption() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        BooleanOption verbose = new BooleanOption('v', "verbose");
        StringOption output = new StringOption('o', "output");
        instance.add(verbose);
        instance.add(output);

        String[] remainder = instance.parse(new String[]{"--verbose", "--output", "file.txt"});

        assertArrayEquals(new String[0], remainder);
        assertTrue(verbose.isUsed());
        assertEquals("--verbose", verbose.getUsedName());
        assertEquals("file.txt", output.getValue());
    }

    @Test
    public void testParseLongOptionWithEquals() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        StringOption output = instance.add(new StringOption('o', "output"));

        String[] remainder = instance.parse(new String[]{"--output=file.txt"});

        assertArrayEquals(new String[0], remainder);
        assertTrue(output.isUsed());
        assertEquals("file.txt", output.getValue());
    }

    @Test
    public void testParseLongOptionPrefersLongerName() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        StringOption out = instance.add(new StringOption('o', "out"));
        StringOption output = instance.add(new StringOption('O', "output"));

        instance.parse(new String[]{"--output=file.txt"});

        assertFalse(out.isUsed());
        assertTrue(output.isUsed());
        assertEquals("file.txt", output.getValue());
    }

    @Test
    public void testParseGroupedShortOptions() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        BooleanOption alpha = instance.add(new BooleanOption('a', "alpha"));
        BooleanOption beta = instance.add(new BooleanOption('b', "beta"));
        BooleanOption gamma = instance.add(new BooleanOption('c', "gamma"));

        String[] remainder = instance.parse(new String[]{"-abc"});

        assertArrayEquals(new String[0], remainder);
        assertTrue(alpha.isUsed());
        assertTrue(beta.isUsed());
        assertTrue(gamma.isUsed());
    }

    @Test
    public void testParseShortOptionWithAttachedValue() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        StringOption output = instance.add(new StringOption('o', "output"));

        String[] remainder = instance.parse(new String[]{"-ofile.txt"});

        assertArrayEquals(new String[0], remainder);
        assertEquals("file.txt", output.getValue());
    }

    @Test
    public void testParseShortOptionWithSeparateValue() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        StringOption output = instance.add(new StringOption('o', "output"));

        String[] remainder = instance.parse(new String[]{"-o", "file.txt"});

        assertArrayEquals(new String[0], remainder);
        assertEquals("file.txt", output.getValue());
    }

    @Test
    public void testParseCommandOption() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        CommandOption run = instance.add(new CommandOption('r', "run"));

        String[] remainder = instance.parse(new String[]{"run", "arg"});

        assertArrayEquals(new String[]{"arg"}, remainder);
        assertTrue(run.isUsed());
        assertEquals("run", run.getUsedName());
    }

    @Test
    public void testParseShortCommandOption() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        CommandOption run = instance.add(new CommandOption('r', "run"));

        String[] remainder = instance.parse(new String[]{"-r"});

        assertArrayEquals(new String[0], remainder);
        assertTrue(run.isUsed());
        assertEquals("r", run.getUsedName());
    }

    @Test
    public void testParseNonOptionArguments() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        BooleanOption verbose = instance.add(new BooleanOption('v', "verbose"));

        String[] remainder = instance.parse(new String[]{"first", "-v", "second"});

        assertArrayEquals(new String[]{"first", "second"}, remainder);
        assertTrue(verbose.isUsed());
    }

    @Test
    public void testParseRepeatedOption() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        BooleanOption verbose = instance.add(new BooleanOption('v', "verbose"));

        String[] remainder = instance.parse(new String[]{"-v", "--verbose"});

        assertArrayEquals(new String[0], remainder);
        assertEquals(2, verbose.getCount());
        assertEquals(1, verbose.getLastUsed());
        assertEquals("--verbose", verbose.getUsedName());
    }

    @Test
    public void testParseInvalidShortOption()
    {
        OptionParser instance = new OptionParser("prog");
        instance.add(new BooleanOption('v', "verbose"));

        assertThrows(InvalidOptionException.class, () -> instance.parse(new String[]{"-z"}));
    }

    @Test
    public void testIgnoreUnknownShort() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        instance.add(new BooleanOption('v', "verbose"));
        instance.setIgnoreUnknownShort(true);

        assertTrue(instance.isIgnoreUnknownShort());
        String[] remainder = instance.parse(new String[]{"-z"});

        assertArrayEquals(new String[]{"-z"}, remainder);
    }

    @Test
    public void testIgnoreShort() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        BooleanOption verbose = instance.add(new BooleanOption('v', "verbose"));
        instance.setIgnoreShort(true);

        assertTrue(instance.isIgnoreShort());
        String[] remainder = instance.parse(new String[]{"-v"});

        assertArrayEquals(new String[]{"-v"}, remainder);
        assertFalse(verbose.isUsed());

        instance = new OptionParser("prog");
        verbose = instance.add(new BooleanOption('v', "verbose"));
        instance.setIgnoreShort(true);
        remainder = instance.parse(new String[]{"--verbose"});

        assertArrayEquals(new String[0], remainder);
        assertTrue(verbose.isUsed());
    }

    @Test
    public void testPosixly() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        BooleanOption verbose = instance.add(new BooleanOption('v', "verbose"));
        instance.setPosixly(true);

        assertTrue(instance.isPosixly());
        String[] remainder = instance.parse(new String[]{"file", "-v"});

        assertArrayEquals(new String[]{"file", "-v"}, remainder);
        assertFalse(verbose.isUsed());
    }

    @Test
    public void testSingleHyphen() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        BooleanOption verbose = instance.add(new BooleanOption('v', "verbose"));

        String[] remainder = instance.parse(new String[]{"-", "-v"});

        assertArrayEquals(new String[0], remainder);
        assertTrue(instance.getOneHyphenOption().isUsed());
        assertEquals(1, instance.getOneHyphenOption().getCount());
        assertTrue(verbose.isUsed());
    }

    @Test
    public void testDoubleHyphenStopsOptionParsing() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        BooleanOption verbose = instance.add(new BooleanOption('v', "verbose"));

        String[] remainder = instance.parse(new String[]{"--", "-v", "file"});

        assertArrayEquals(new String[]{"-v", "file"}, remainder);
        assertFalse(verbose.isUsed());
    }

    @Test
    public void testOneHyphenDisabled() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        BooleanOption verbose = instance.add(new BooleanOption('v', "verbose"));
        instance.setOneHyphen(false);

        assertFalse(instance.isOneHyphen());
        String[] remainder = instance.parse(new String[]{"-", "-v"});

        assertArrayEquals(new String[]{"-"}, remainder);
        assertFalse(instance.getOneHyphenOption().isUsed());
        assertTrue(verbose.isUsed());
    }

    @Test
    public void testLog() throws Exception
    {
        OptionParser instance = new OptionParser("prog");
        instance.add(new BooleanOption('v', "verbose"));
        instance.parse(new String[]{"--verbose"});

        assertDoesNotThrow(instance::log);
    }
}
