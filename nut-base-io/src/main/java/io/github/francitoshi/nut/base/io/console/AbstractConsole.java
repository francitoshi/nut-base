/*
 * Copyright (C) 2015-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io.console;

import java.io.Console;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author franci
 */
public abstract class AbstractConsole implements VirtualConsole
{
    private final Queue<String> queue = new ConcurrentLinkedQueue<>();

    @Override
    public VirtualConsole println(String s)
    {
        return format("%s\n", s);
    }
    
    @Override
    public void addLine(String e)
    {
        queue.add(e);
    }
    protected final String getLine()
    {
        return queue.poll();
    }

    @Override
    public String readLine(String fmt, Object... args)
    {
        String s = getLine();
        if(s!=null)
        {
            printf(fmt,args);
            printf("%s\n",s);
        }
        return s;
    }

    @Override
    public String readLine()
    {
        String s = getLine();
        if(s!=null)
        {
            printf("%s\n",s);
        }
        return s;
    }

    @Override
    public char[] readPassword(String fmt, Object... args)
    {
        String s = getLine();
        if(s!=null)
        {
            printf(fmt,args);
            return s.toCharArray();
        }
        return null;
    }

    @Override
    public char[] readPassword()
    {
        String s = getLine();
        if(s!=null)
        {
            return s.toCharArray();
        }
        return null;
    }
    
    public static VirtualConsole getInstance()
    {
        Console console = System.console();
        return getInstance(console==null);
    }

    public static VirtualConsole getInstance(boolean allowMock)
    {
        Console console = System.console();
        return (console==null && allowMock) ? new MockConsole(System.in, System.out) : new RealConsole(console);
    }

    public static VirtualConsole getMockInstance()
    {
        return new MockConsole(System.in, System.out);
    }

    public static VirtualConsole getRealInstance()
    {
        Console console = System.console();
        return (console==null) ? null : new RealConsole(console);
    }
    
}
