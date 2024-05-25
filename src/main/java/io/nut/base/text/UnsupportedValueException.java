/*
 * UnsupportedValueException.java
 *
 * Copyright (c) 2014-2023 francitoshi@gmail.com
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
 */
package io.nut.base.text;

import java.util.Locale;

/**
 *
 * @author franci
 */
public class UnsupportedValueException extends RuntimeException
{
    final Locale locale;
    final long value;

    UnsupportedValueException(Locale locale, long value)
    {
        super("Unsupported value "+value+" for locale "+locale);
        this.locale = locale;
        this.value = value;
    }
}
