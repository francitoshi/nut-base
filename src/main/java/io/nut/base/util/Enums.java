/*
 * Copyright (C) 2014-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.EnumSet;

public class Enums
{
    public static <T extends Enum<T>> T safeValueOf(Class<T> enumType, CharSequence name, T defaultValue)
    {
        if (enumType == null || name == null)
        {
            return defaultValue;
        }
        for (T value : enumType.getEnumConstants())
        {
            if(value.name().equalsIgnoreCase(name.toString()))
            {
                return value;
            }
        }
        return defaultValue;
    }
    public static <T extends Enum<T>> T safeValueOf(Class<T> enumType, int ordinal, T defaultValue)
    {
        if (enumType == null)
        {
            return defaultValue;
        }
        for (T value : enumType.getEnumConstants())
        {
            if(ordinal==value.ordinal())
            {
                return value;
            }
        }
        return defaultValue;
    }
    
    public static <T extends Enum<T>> EnumSet<T> toEnumSet(Class<T> elementType, T... items)
    {
        EnumSet<T> set = EnumSet.noneOf(elementType);
        for(T t : items)
        {
            set.add(t);
        }
        return set;
    }
}
