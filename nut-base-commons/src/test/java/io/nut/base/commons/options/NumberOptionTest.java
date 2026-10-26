/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.options;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class NumberOptionTest
{
    private static NumberOption option(String value)
    {
        NumberOption instance = new NumberOption('n', "number");
        instance.parseLong(0, new String[]{"--number", value});
        return instance;
    }

    @Test
    public void testConstructors()
    {
        NumberOption longOnly = new NumberOption("number");
        assertEquals("number", longOnly.getLongName());

        NumberOption withShort = new NumberOption('n', "number");
        assertEquals('n', withShort.getShortName());
    }

    @Test
    public void testIntValue() throws Exception
    {
        assertEquals(42, option("42").intValue());
        assertEquals(-42, option("-42").intValue());
    }

    @Test
    public void testIntValueMissing() throws Exception
    {
        NumberOption instance = new NumberOption('n', "number");

        assertThrows(MissingOptionParameterException.class, instance::intValue);
        assertEquals(7, instance.intValue(7));
    }

    @Test
    public void testIntValueInvalid() throws Exception
    {
        NumberOption instance = option("abc");

        InvalidOptionParameterException ex = assertThrows(InvalidOptionParameterException.class, instance::intValue);
        assertEquals("--number", ex.getName());
        assertEquals("abc", ex.getValue());
    }

    @Test
    public void testLongValue() throws Exception
    {
        assertEquals(42L, option("42").longValue());
        assertEquals(-42L, option("-42").longValue());
        assertEquals(9L, new NumberOption('n', "number").longValue(9L));
    }

    @Test
    public void testLongValueInvalid() throws Exception
    {
        NumberOption instance = option("abc");

        InvalidOptionParameterException ex = assertThrows(InvalidOptionParameterException.class, instance::longValue);
        assertEquals("--number", ex.getName());
        assertEquals("abc", ex.getValue());
    }

    @Test
    public void testDoubleValue() throws Exception
    {
        assertEquals(3.14, option("3.14").doubleValue(), 0.0);
        assertEquals(1.5, new NumberOption('n', "number").doubleValue(1.5), 0.0);
    }

    @Test
    public void testDoubleValueInvalid() throws Exception
    {
        NumberOption instance = option("abc");

        InvalidOptionParameterException ex = assertThrows(InvalidOptionParameterException.class, instance::doubleValue);
        assertEquals("--number", ex.getName());
        assertEquals("abc", ex.getValue());
    }

    @Test
    public void testBigDecimalValue() throws Exception
    {
        assertEquals(new BigDecimal("123.45"), option("123.45").bigDecimalValue());
        assertEquals(BigDecimal.ZERO, option("abc").bigDecimalValue());
    }

    @Test
    public void testBigDecimalValueDefault() throws Exception
    {
        BigDecimal def = new BigDecimal("10");
        assertEquals(new BigDecimal("123.45"), option("123.45").bigDecimalValue(def));
        assertEquals(def, option("abc").bigDecimalValue(def));

        NumberOption instance = new NumberOption('n', "number");
        assertThrows(MissingOptionParameterException.class, () -> instance.bigDecimalValue(def));
    }
}
