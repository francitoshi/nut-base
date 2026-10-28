/*
 * Copyright (C) 2015-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.debug;

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
