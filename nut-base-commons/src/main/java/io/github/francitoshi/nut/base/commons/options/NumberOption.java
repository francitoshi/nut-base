/*
 * Copyright (C) 2010-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.options;

import io.github.francitoshi.nut.base.commons.lang.Parsers;
import java.math.BigDecimal;

/**
 *
 * @author franci
 */
public class NumberOption extends StringOption
{
    public NumberOption(String longName)
    {
        super(longName);
    }
    public NumberOption(char shortName, String longName)
    {
        super(shortName, longName);
    }

    public int intValue() throws MissingOptionParameterException, InvalidOptionParameterException
    {
        String value = getValue();
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ex)
        {
            throw new InvalidOptionParameterException(getUsedName(),value);
        }
    }
    public int intValue(int def) throws InvalidOptionParameterException
    {
        try
        {
            return isUsed() ? intValue() : def;
        }
        catch (MissingOptionParameterException ex)
        {
            return def;
        }
    }  
    public long longValue() throws MissingOptionParameterException, InvalidOptionParameterException
    {
        String value = getValue();
        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException ex)
        {
            throw new InvalidOptionParameterException(getUsedName(),value);
        }
    }
    public long longValue(long def) throws InvalidOptionParameterException
    {
        try
        {
            return isUsed() ? intValue() : def;
        }
        catch (MissingOptionParameterException ex)
        {
            return def;
        }
    }
    public double doubleValue() throws MissingOptionParameterException, InvalidOptionParameterException
    {
        String value = getValue();
        try
        {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException ex)
        {
            throw new InvalidOptionParameterException(getUsedName(),value);
        }
    }
    public double doubleValue(double def) throws InvalidOptionParameterException
    {
        try
        {
            return isUsed() ? doubleValue() : def;
        }
        catch (MissingOptionParameterException ex)
        {
            return def;
        }
    }
    public BigDecimal bigDecimalValue() throws MissingOptionParameterException, InvalidOptionParameterException
    {
        String value = getValue();
        try
        {
            return Parsers.parseBigDecimal(value);
        }
        catch (NumberFormatException ex)
        {
            throw new InvalidOptionParameterException(getUsedName(),value);
        }
    }
    public BigDecimal bigDecimalValue(BigDecimal def) throws InvalidOptionParameterException, MissingOptionParameterException
    {
        String value = getValue();
        try
        {
            return Parsers.parseBigDecimal(value, def);
        }
        catch (NumberFormatException ex)
        {
            throw new InvalidOptionParameterException(getUsedName(),value);
        }
    }
}
