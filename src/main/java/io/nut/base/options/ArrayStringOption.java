/*
 * Copyright (C) 2009-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.options;

import io.nut.base.util.Empty;
import java.text.MessageFormat;
import java.util.ArrayList;

/**
 *
 * @author franci
 */
public class ArrayStringOption extends StringOption
{

    private final ArrayList<String> values = new ArrayList<>();
    private final String separator;

    public ArrayStringOption(String longName)
    {
        super(longName);
        separator = null;
    }

    public ArrayStringOption(String longName, char separator)
    {
        super(longName);
        this.separator = Character.toString(separator);
    }

    public ArrayStringOption(char shortName, String longName)
    {
        super(shortName, longName);
        separator = null;
    }

    public ArrayStringOption(char shortName, String longName, char separator)
    {
        super(shortName, longName);
        this.separator = Character.toString(separator);
    }

    public String[] getValues()
    {
        return values.toArray(Empty.STRINGS);
    }

    @Override
    public void setValue(String value)
    {
        if (separator == null)
        {
            values.add(value);
        }
        else
        {
            String[] tokens = value.split(separator);
            for (String item : tokens)
            {
                if (item.length()!=0)
                {
                    values.add(item);
                }
            }
        }
    }
    @Override
    public String toString()
    {
        return MessageFormat.format("--{0}={1})", longName,values);
    }
    
}
