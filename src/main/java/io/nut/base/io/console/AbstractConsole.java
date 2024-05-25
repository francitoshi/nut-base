/*
 *  AbstractConsole.java
 *
 *  Copyright (C) 2015-2023 francitoshi@gmail.com
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
 */
package io.nut.base.io.console;

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
    public static VirtualConsole getInstance(boolean fake)
    {
        Console console = System.console();
        return (console==null || fake) ? new FakeConsole(System.in, System.out) : new RealConsole(console);
    }
    
}
