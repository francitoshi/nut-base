/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.logging;

import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class JulLog extends Log
{
    public static final class Builder implements Log.Builder
    {
        final boolean global;
        final Level julLevel;
        final Handler[] handlers;
        public Builder(boolean global, int level, Handler... handlers)
        {
            this.global = global;
            this.julLevel = JulLog.toJulLevel(level);
            this.handlers = handlers;
        }
        
        @Override
        public Log build(String name)
        {
            return global ? new JulLog(name, julLevel, null) : new JulLog(name, julLevel, handlers);
        }

        @Override
        public void register()
        {
            if(global)
            {
                Logger root = Logger.getLogger("");
                root.setLevel(julLevel);
                for(Handler item : root.getHandlers())
                {
                    root.removeHandler(item);
                }       
                for(Handler item : handlers)
                {
                    root.removeHandler(item);
                    root.addHandler(item);
                }       
            }
        }

        @Override
        public void unregister()
        {
            if(global)
            {
                Logger root = Logger.getLogger("");
                for(Handler item : handlers)
                {
                    root.removeHandler(item);
                }       
            }
        }
    }
    
    public static Formatter getJulFormatter(FormatType type)
    {
        switch (type)
        {
            case LEV_MSG:
            case DT_LEV_MSG:
            case DTZ_LEV_MSG:
            case DT_LEV_NAME_MSG:
            case DTZ_LEV_NAME_MSG:
                return new PlainFormatter(type);
            case Simple:
            default:
                return new SimpleFormatter();
        }
    }
    
    public static final Level OFF   = Level.OFF;
    public static final Level ERROR = Level.SEVERE;
    public static final Level WARN  = Level.WARNING;
    public static final Level INFO  = Level.INFO;
    public static final Level DEBUG = Level.FINE;
    public static final Level TRACE = Level.FINER;
    public static final Level ALL   = Level.ALL;
    
    private static final Level[] JUL_LEVELS = {TRACE, DEBUG, INFO, WARN, ERROR};
    
    public static Level toJulLevel(int level)
    {
        if(level>=0 && level<JUL_LEVELS.length)
        {
            return JUL_LEVELS[level];
        }
        if(level>0)
        {
            return OFF;
        }
        return ALL;
    }

    private static final int[] JUL_VALUE_LEVELS = {TRACE.intValue(), DEBUG.intValue(), INFO.intValue(), WARN.intValue(), ERROR.intValue()};
    private static final int FINEST_INT_VALUE = Level.FINEST.intValue();
    
    public static int fromJulLevel(Level level)
    {
        int value = level.intValue();
        if(value<=FINEST_INT_VALUE)
        {
            return Log.ALL;
        }
        for(int i=0;i<JUL_VALUE_LEVELS.length;i++)
        {
            if(value<=JUL_VALUE_LEVELS[i])
            {
                return i;
            }
        }
        return Log.OFF;
    }
    
    private final Logger logger;
    
    protected JulLog(String name, Level level, Handler[] handlers)
    {
        this.logger = Logger.getLogger(name);
        if(level!=null)
        {
            this.logger.setLevel(level);
        }
        this.logger.setUseParentHandlers(handlers==null);
        if(handlers!=null)
        {
            for(Handler item : handlers)
            {
                this.logger.removeHandler(item);
                this.logger.addHandler(item);
            }       
        }
    }

    @Override
    public void setLevel(int level)
    {
        logger.setLevel(toJulLevel(level));
    }
    
    // ==================== Any Level ====================
    protected void log(Level level, String msg)
    {
        if (logger.isLoggable(level))
        {
            logger.log(level, msg);
        }
    }

    protected void log(Level level, String msg, Throwable tr)
    {
        if (logger.isLoggable(level))
        {
            logger.log(level, msg, tr);
        }
    }

    protected void log(Level level, Supplier<String> msg)
    {
        if (logger.isLoggable(level))
        {
            logger.log(level, msg.get());
        }
    }

    protected void log(Level level, Supplier<String> msg, Throwable tr)
    {
        if (logger.isLoggable(level))
        {
            logger.log(level, msg.get(), tr);
        }
    }
    
    protected void log(Level level, String format, Object[] args)
    {
        if (logger.isLoggable(level))
        {
            String msg;
            try
            {
                msg = String.format(format, args);
            }
            catch (Exception ex)
            {
                // Evita que un '%' mal escrito o un mismatch de argumentos
                // tumbe la llamada que originó el log.
                msg = format + " [log format error: " + ex.getMessage() + "]";
            }
            logger.log(level, msg);            
        }
    }

    // ==================== Error Level ====================
    @Override
    public void error(String msg)
    {
        log(ERROR, msg);
    }

    @Override
    public void error(String msg, Throwable tr)
    {
        log(ERROR, msg, tr);
    }

    @Override
    public void error(Supplier<String> msg)
    {
        log(ERROR, msg);
    }

    @Override   
    public void error(Supplier<String> msg, Throwable tr)
    {
        log(ERROR, msg, tr);
    }
        
    @Override
    public void error(String format, Object... args)
    {
        log(ERROR, format, args);
    }

    // ==================== MÉTODOS SIMPLES WARN ====================

    @Override
    public void warn(String msg)
    {
        log(WARN, msg);
    }

    @Override
    public void warn(String msg, Throwable tr)
    {
        log(WARN, msg, tr);
    }

    @Override
    public void warn(Supplier<String> msg)
    {
        log(WARN, msg);
    }

    @Override
    public void warn(Supplier<String> msg, Throwable tr)
    {
        log(WARN, msg, tr);
    }

    @Override
    public void warn(String format, Object... args)
    {
        log(WARN, format, args);
    }

    // ==================== MÉTODOS SIMPLES INFO ====================
    @Override
    public void info(String msg)
    {
        log(INFO, msg);
    }

    @Override
    public void info(String msg, Throwable tr)
    {
        log(INFO, msg, tr);
    }

    @Override
    public void info(Supplier<String> msg)
    {
        log(INFO, msg);
    }

    @Override
    public void info(Supplier<String> msg, Throwable tr)
    {
        log(INFO, msg, tr);
    }

    @Override
    public void info(String format, Object... args)
    {
        log(INFO, format, args);
    }

    // ==================== MÉTODOS SIMPLES DEBUG ====================
    @Override
    public void debug(String msg)
    {
        log(DEBUG, msg);
    }

    @Override
    public void debug(String msg, Throwable tr)
    {
        log(DEBUG, msg, tr);
    }

    @Override
    public void debug(Supplier<String> msg)
    {
        log(DEBUG, msg);
    }

    @Override
    public void debug(Supplier<String> msg, Throwable tr)
    {
        log(DEBUG, msg, tr);
    }

    @Override
    public void debug(String format, Object... args)
    {
        log(DEBUG, format, args);
    }

    // ==================== MÉTODOS SIMPLES TRACE ====================
    @Override
    public void trace(String msg)
    {
        log(TRACE, msg);
    }

    @Override
    public void trace(String msg, Throwable tr)
    {
        log(TRACE, msg, tr);
    }

    @Override
    public void trace(Supplier<String> msg)
    {
        log(TRACE, msg);
    }

    @Override
    public void trace(Supplier<String> msg, Throwable tr)
    {
        log(TRACE, msg, tr);
    }

    @Override
    public void trace(String format, Object... args)
    {
        log(TRACE, format, args);
    }

    // ==================== Chequeos ====================
    @Override
    public boolean isDebugEnabled()
    {
        return logger.isLoggable(DEBUG);
    }

    @Override
    public boolean isTraceEnabled()
    {
        return logger.isLoggable(TRACE);
    }
}
