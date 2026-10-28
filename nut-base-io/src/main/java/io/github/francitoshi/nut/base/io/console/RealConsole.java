/*
 * Copyright (C) 2010-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io.console;

import java.io.Console;
import java.io.PrintWriter;
import java.io.Reader;

/**
 *
 * @author franci
 */
public class RealConsole extends AbstractConsole implements VirtualConsole
{
    private final Object lock = new Object();

    private final Console console;

    public RealConsole(Console console)
    {
        this.console = console;
    }  

    @Override
    public void flush()
    {
        console.flush();
    }

    @Override
    public VirtualConsole format(String fmt, Object... args)
    {
        synchronized(lock)
        {
            console.format(fmt, args);
            return this;
        }
    }

    @Override
    public VirtualConsole printf(String format, Object... args)
    {
        synchronized(lock)
        {
            console.printf(format, args);
            return this;
        }
    }

    @Override
    public String readLine(String fmt, Object... args)
    {
        synchronized(lock)
        {
            String s = super.readLine(fmt, args);
            if(s!=null)
            {
                return s;
            }
            return console.readLine(fmt, args);
        }
    }

    @Override
    public String readLine()
    {
        synchronized(lock)
        {
            String s = super.readLine();
            if(s!=null)
            {
                return s;
            }
            return console.readLine(); 
        }
    }

    @Override
    public char[] readPassword(String fmt, Object... args)
    {
        synchronized(lock)
        {
            char[] c = super.readPassword(fmt, args);
            if(c!=null)
            {
                return c;
            }
            return console.readPassword(fmt, args);
        }
    }

    @Override
    public char[] readPassword()
    {
        synchronized(lock)
        {
             char[] c = super.readPassword();
            if(c!=null)
            {
                return c;
            }
            return console.readPassword();
        }
    }

    @Override
    public Reader reader()
    {
        return console.reader();
    }

    @Override
    public PrintWriter writer()
    {
        return console.writer();
    }

}
