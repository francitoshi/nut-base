/*
 * Copyright (C) 2009-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.options;

/**
 *
 * @author franci
 */
public interface Option
{
    int getCount();

    void addCount();

    boolean isUsed();
    
    boolean isCommand();

    int getLastUsed();

    String getLongName();

    char getShortName();

    int parseLong(int index, String[] args);

    int parseShort(int argIndex, int charIndex, String[] args);

    String getUsedName();
    
}
