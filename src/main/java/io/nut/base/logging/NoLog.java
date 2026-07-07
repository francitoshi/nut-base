/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.logging;

import java.util.function.Supplier;

public final class NoLog extends Log
{
    public static final class Builder implements Log.Builder
    {
        @Override
        public Log build(String name)
        {
            return new NoLog();
        }
        @Override
        public void register()
        {
        }
        @Override
        public void unregister()
        {
        }
    }
    
    public NoLog()
    {
    }

    @Override
    public boolean isTraceEnabled()
    {
        return false;
    }

    @Override
    public boolean isDebugEnabled()
    {
        return false;
    }

    @Override
    public void setLevel(int level)
    {
    }

    @Override
    public void trace(String format, Object... args)
    {
    }

    @Override
    public void trace(Supplier<String> msg, Throwable tr)
    {
    }

    @Override
    public void trace(Supplier<String> msg)
    {
    }

    @Override
    public void trace(String msg, Throwable tr)
    {
    }

    @Override
    public void trace(String msg)
    {
    }

    @Override
    public void debug(String format, Object... args)
    {
    }

    @Override
    public void debug(Supplier<String> msg, Throwable tr)
    {
    }

    @Override
    public void debug(Supplier<String> msg)
    {
    }

    @Override
    public void debug(String msg, Throwable tr)
    {
    }

    @Override
    public void debug(String msg)
    {
    }

    @Override
    public void info(String format, Object... args)
    {
    }

    @Override
    public void info(Supplier<String> msg, Throwable tr)
    {
    }

    @Override
    public void info(Supplier<String> msg)
    {
    }

    @Override
    public void info(String msg, Throwable tr)
    {
    }

    @Override
    public void info(String msg)
    {
    }

    @Override
    public void warn(String format, Object... args)
    {
    }

    @Override
    public void warn(Supplier<String> msg, Throwable tr)
    {
    }

    @Override
    public void warn(Supplier<String> msg)
    {
    }

    @Override
    public void warn(String msg, Throwable tr)
    {
    }

    @Override
    public void warn(String msg)
    {
    }

    @Override
    public void error(String format, Object... args)
    {
    }

    @Override
    public void error(Supplier<String> msg, Throwable tr)
    {
    }

    @Override
    public void error(Supplier<String> msg)
    {
    }

    @Override
    public void error(String msg, Throwable tr)
    {
    }

    @Override
    public void error(String msg)
    {
    }

}
