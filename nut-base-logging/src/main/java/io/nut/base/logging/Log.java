/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.logging;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;

public abstract class Log
{
    public enum FormatType
    {
        Simple,
        LEV_MSG, 
        DT_LEV_MSG, 
        DTZ_LEV_MSG,
        DT_LEV_NAME_MSG,
        DTZ_LEV_NAME_MSG
    }
            
    public static final int OFF   = Integer.MAX_VALUE;
    public static final int ERROR = 4;//la operación falla
    public static final int WARN  = 3;//comportamiento inesperado
    public static final int INFO  = 2;//funcionamiento normal
    public static final int DEBUG = 1;//depuración
    public static final int TRACE = 0;//seguimiento detallado
    public static final int ALL   = Integer.MIN_VALUE;
    
    private static final char[] LEVEL_CHARS = "TDIWE".toCharArray();
    
    public static char levelToChar(int level)
    {
        if(level>=0 && level<LEVEL_CHARS.length)
        {
            return LEVEL_CHARS[level];
        }
        return '?';
    }
    
    public interface Builder
    {
        void register();
        void unregister();
        Log build(String name);
    }
    
    private static volatile Builder BUILDER = new NoLog.Builder();
    
    // Factory methods
    public static Log of(String name)
    {
        return BUILDER.build(name);
    }

    public static Log of(Class<?> clazz)
    {
        return BUILDER.build(clazz.getName());
    }

    public static Log of(Object instance)
    {
        return of(instance.getClass().getName());
    }

    public static void setBuilder(Builder builder)
    {
        if(BUILDER!=null)
        {
            BUILDER.unregister();
        }
        BUILDER = builder;
        BUILDER.register();
    }
        
    public static void setNopBuilder()
    {
        Log.setBuilder(new NoLog.Builder());
    }
        
    public static void setJulBuilder(boolean global, int level, Handler... handlers)
    {
        Log.setBuilder(new JulLog.Builder(global, level, handlers));
    }
    
    public static ConsoleHandler getConsoleHandler(int level, FormatType fmt)
    {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        Level julLevel = JulLog.toJulLevel(level);
        if(julLevel!=null)
        {
            consoleHandler.setLevel(JulLog.toJulLevel(level));
        }
        consoleHandler.setFormatter(new PlainFormatter(fmt));
        return consoleHandler;
    }

    public static FileHandler getFileHandler(int level, FormatType fmt, String pattern, boolean append) throws IOException
    {
        FileHandler fh = new FileHandler(pattern, append);        
        Level julLevel = JulLog.toJulLevel(level);
        if(julLevel!=null)
        {
            fh.setLevel(JulLog.toJulLevel(level));
        }
        fh.setFormatter(new PlainFormatter(fmt));
        return fh;
    }
    
    public static FileHandler getFileHandler(int level, FormatType fmt, String pattern, int limit, int count) throws IOException
    {
        FileHandler fh = new FileHandler(pattern, limit, count);  
        Level julLevel = JulLog.toJulLevel(level);
        if(julLevel!=null)
        {
            fh.setLevel(JulLog.toJulLevel(level));
        }
        fh.setFormatter(new PlainFormatter(fmt));
        return fh;
    }
    
    public static FileHandler getFileHandler(int level, FormatType fmt, String pattern, int limit, int count, boolean append) throws IOException
    {
        FileHandler fh = new FileHandler(pattern, limit, count, append);
        Level julLevel = JulLog.toJulLevel(level);
        if(julLevel!=null)
        {
            fh.setLevel(julLevel);
        }
        fh.setFormatter(new PlainFormatter(fmt));
        return fh;
    }    
    
    // En Log.java
    public static void loadJulConfig(String filePath) throws IOException
    {
        try (InputStream is = new FileInputStream(filePath))
        {
            LogManager.getLogManager().readConfiguration(is);
        }
    }

    public abstract void setLevel(int level);
    
    // ==================== Error Level ====================
    public abstract void error(String msg);

    public abstract void error(String msg, Throwable tr);

    public abstract void error(Supplier<String> msg);

    public abstract void error(Supplier<String> msg, Throwable tr);
        
    public abstract void error(String format, Object... args);

    // ==================== MÉTODOS SIMPLES WARN ====================

    public abstract void warn(String msg);

    public abstract void warn(String msg, Throwable tr);

    public abstract void warn(Supplier<String> msg);

    public abstract void warn(Supplier<String> msg, Throwable tr);

    public abstract void warn(String format, Object... args);

    // ==================== MÉTODOS SIMPLES INFO ====================

    public abstract void info(String msg);

    public abstract void info(String msg, Throwable tr);

    public abstract void info(Supplier<String> msg);

    public abstract void info(Supplier<String> msg, Throwable tr);

    public abstract void info(String format, Object... args);

    // ==================== MÉTODOS SIMPLES DEBUG ====================

    public abstract void debug(String msg);

    public abstract void debug(String msg, Throwable tr);

    public abstract void debug(Supplier<String> msg);

    public abstract void debug(Supplier<String> msg, Throwable tr);

    public abstract void debug(String format, Object... args);

    // ==================== MÉTODOS SIMPLES TRACE ====================

    public abstract void trace(String msg);

    public abstract void trace(String msg, Throwable tr);

    public abstract void trace(Supplier<String> msg);

    public abstract void trace(Supplier<String> msg, Throwable tr);

    public abstract void trace(String format, Object... args);

    // ==================== Chequeos ====================
    public abstract boolean isDebugEnabled();

    public abstract boolean isTraceEnabled();
}
