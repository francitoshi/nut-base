/*
 *  RealConsole.java
 *
 *  Copyright (C) 2010-2024 francitoshi@gmail.com
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
