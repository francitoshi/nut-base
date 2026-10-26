/*
 * Copyright (C) 2010-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.options;

/**
 *
 * @author franci
 */
public class MissingOptionParameterException extends OptionException
{
    public MissingOptionParameterException(String message)
    {
        super(message);
    }

    MissingOptionParameterException(Option opt)
    {
        super("missing argument to '"+opt.getUsedName()+"'");
    }
}
