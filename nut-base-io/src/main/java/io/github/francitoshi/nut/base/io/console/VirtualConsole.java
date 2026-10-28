/*
 * Copyright (C) 2018-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io.console;

import java.io.PrintWriter;
import java.io.Reader;

/**
 *
 * @author franci
 */
public interface VirtualConsole
{
    void addLine(String line);
    
    void flush();

    VirtualConsole format(String fmt, Object... args);

    VirtualConsole printf(String format, Object... args);

    VirtualConsole println(String s);

    String readLine(String fmt, Object... args);

    String readLine();

    char[] readPassword(String fmt, Object... args);

    char[] readPassword();

    Reader reader();

    PrintWriter writer();

}
