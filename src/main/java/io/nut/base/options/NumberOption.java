/*
 *  NumberOption.java
 *
 *  Copyright (C) 2010-2024 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 *
 */
package io.nut.base.options;

import io.nut.base.util.Parsers;
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
            return Parsers.safeParseBigDecimal(value);
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
            return Parsers.safeParseBigDecimal(value, def);
        }
        catch (NumberFormatException ex)
        {
            throw new InvalidOptionParameterException(getUsedName(),value);
        }
    }
}
