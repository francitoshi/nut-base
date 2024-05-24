/*
 *  Debug.java
 *
 *  Copyright (C) 2015-2024 francitoshi@gmail.com
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
package io.nut.base.os;

import java.util.logging.Level;

/**
 * Created by franci on 24/03/15.
 */
public class Debug
{
    protected static final Level DEFAULT_LEVEL = Level.INFO;

    private static volatile Debug instance = new Debug(false, Level.INFO);

    protected static void setup(Debug debug)
    {
        Debug.instance = debug;
    }
    public static void setup(boolean value)
    {
        Debug.instance = new Debug(value, Level.INFO);
    }
    public static void setup(boolean value, Level level)
    {
        Debug.instance = new Debug(value, level);
    }

    private final boolean debug;
    private final Level level;

    protected Debug(boolean debug, Level level)
    {
        this.debug = debug;
        this.level = level;
    }

    protected boolean isDebug()
    {
        return debug;
    }
    protected final boolean isDebug(Level level)
    {
        return this.isDebug() && level.intValue() >= this.level.intValue();
    }

    public static boolean isDebuggable()
    {
        return Debug.instance.isDebug();
    }
    public static boolean isDebuggable(Level level)
    {
        return Debug.instance.isDebug(level);
    }
}
