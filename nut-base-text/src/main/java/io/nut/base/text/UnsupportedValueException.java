/*
 * Copyright (C) 2014-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.text;

import java.util.Locale;

/**
 *
 * @author franci
 */
public class UnsupportedValueException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    final Locale locale;
    final long value;

    UnsupportedValueException(Locale locale, long value)
    {
        super("Unsupported value "+value+" for locale "+locale);
        this.locale = locale;
        this.value = value;
    }
}
