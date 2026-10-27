/*
 * Copyright (C) 2009-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.options;

import io.github.francitoshi.nut.base.commons.units.SizeUnits;

/**
 *
 * @author franci
 */
public class SizeOption extends StringOption
{
    private static final SizeUnits stdSizeParser = SizeUnits.getBinaryInstance();
    private static final SizeUnits binSizeParser = SizeUnits.getStandardInstance();

    private final SizeUnits sizeParser;
    
    public SizeOption(String longName)
    {
        super(longName);
        this.sizeParser = SizeOption.stdSizeParser;
    }
    public SizeOption(char shortName, String longName)
    {
        super(shortName, longName);
        this.sizeParser = SizeOption.stdSizeParser;
    }
    public SizeOption(char shortName, String longName, boolean binary)
    {
        super(shortName, longName);
        this.sizeParser = binary ? SizeOption.binSizeParser : SizeOption.stdSizeParser;
    }

    public long longValue() throws MissingOptionParameterException, InvalidOptionParameterException
    {
        String value = getValue();
        try
        {
            return sizeParser.parse(value);
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
            return isUsed() ? longValue() : def;
        }
        catch (MissingOptionParameterException ex)
        {
            return def;
        }
    }     
}
