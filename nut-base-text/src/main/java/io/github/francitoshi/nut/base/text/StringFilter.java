/*
 * Copyright (C) 2014-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.text;

import java.util.Locale;

/**
 *
 * @author franci
 */
public interface StringFilter
{
    String filter(String s);
    String filter(String s, Locale locale);
}
