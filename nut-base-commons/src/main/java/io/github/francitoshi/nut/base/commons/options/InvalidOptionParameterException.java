/*
 * Copyright (C) 2010-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.options;

/**
 *
 * @author franci
 */
public class InvalidOptionParameterException extends OptionException
{
    private final String name;
    private final String value;
    public InvalidOptionParameterException(String message)
    {
        super(message);
        this.name = null;
        this.value = null;
    }

    InvalidOptionParameterException(String name, String value)
    {
        super("invalid argument '"+value+"' to '"+name+"'");
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }
    
}
