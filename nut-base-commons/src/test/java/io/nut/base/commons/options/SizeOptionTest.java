/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class SizeOptionTest
{
    private static final long K = 1024L;
    private static final long M = K * K;
    private static final long G = M * K;

    private static SizeOption option(String value)
    {
        SizeOption instance = new SizeOption('s', "size");
        instance.parseLong(0, new String[]{"--size", value});
        return instance;
    }

    @Test
    public void testConstructors()
    {
        SizeOption longOnly = new SizeOption("size");
        assertEquals("size", longOnly.getLongName());

        SizeOption withShort = new SizeOption('s', "size");
        assertEquals('s', withShort.getShortName());
    }

    @Test
    public void testLongValue() throws Exception
    {
        assertEquals(1L, option("1").longValue());
        assertEquals(K, option("1k").longValue());
        assertEquals(M, option("1M").longValue());
        assertEquals(G, option("1G").longValue());
        assertEquals(2 * K, option("2k").longValue());
    }

    @Test
    public void testLongValueDefault() throws Exception
    {
        SizeOption instance = new SizeOption('s', "size");
        assertEquals(123L, instance.longValue(123L));

        instance.parseLong(0, new String[]{"--size", "1k"});
        assertEquals(K, instance.longValue(123L));
    }

    @Test
    public void testLongValueMissing() throws Exception
    {
        SizeOption instance = new SizeOption('s', "size");

        assertThrows(MissingOptionParameterException.class, instance::longValue);
    }

    @Test
    public void testLongValueInvalid() throws Exception
    {
        SizeOption instance = option("abc");

        InvalidOptionParameterException ex = assertThrows(InvalidOptionParameterException.class, instance::longValue);
        assertEquals("--size", ex.getName());
        assertEquals("abc", ex.getValue());
    }

    @Test
    public void testLongValueBinaryFlag() throws Exception
    {
        SizeOption decimal = new SizeOption('s', "size", true);
        decimal.parseLong(0, new String[]{"--size", "1k"});
        assertEquals(1000L, decimal.longValue());

        SizeOption binary = new SizeOption('s', "size", false);
        binary.parseLong(0, new String[]{"--size", "1k"});
        assertEquals(K, binary.longValue());
    }
}
