/*
 *  VirtualConsole.java
 *
 *  Copyright (C) 2010-2023 francitoshi@gmail.com
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
 *
 */
package io.nut.base.io.console;

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

    String readLine(String fmt, Object... args);

    String readLine();

    char[] readPassword(String fmt, Object... args);

    char[] readPassword();

    Reader reader();

    PrintWriter writer();

}
