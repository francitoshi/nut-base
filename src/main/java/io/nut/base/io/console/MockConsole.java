/*
 * Copyright (C) 2010-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.io.console;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import io.nut.base.util.Exceptions;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class MockConsole extends AbstractConsole implements VirtualConsole
{
    private static final Logger LOG = Logger.getLogger(MockConsole.class.getName());

    private final OutputStream out;
    private final Scanner sc;
    private final Object lock = new Object();
    private final PrintWriter pw;
    private final Reader reader;

    public MockConsole(InputStream in, OutputStream out)
    {
        this.out = out;
        this.sc  = new Scanner(in);
        pw = new PrintWriter(out, true)
        {
            @Override
            public void close() {}
        };//do not close the output stream

        reader = new InputStreamReader(in);

    }
    public static VirtualConsole getConsole(boolean debug)
    {
        if(System.console()!=null)
            return new RealConsole(System.console());
        if(debug)
            return new MockConsole(System.in,System.out);
        throw new IOError(new Exception("There is no console"));
    }

    @Override
    public void flush()
    {
        try
        {
            out.flush();
        }
        catch (IOException ex)
        {
            Exceptions.severe(LOG, ex);
        }
    }

    @Override
    public VirtualConsole format(String fmt, Object... args)
    {
        pw.format(fmt, args).flush();
        return this;
    }

    @Override
    public VirtualConsole printf(String format, Object... args)
    {
        return format(format, args);
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
            if (fmt.length() != 0)
            {
                pw.format(fmt, args);
            }
            return readLine();
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
            if(sc.hasNextLine())
            {
                return sc.nextLine();
            }
            return null;
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
            return readLine(fmt, args).toCharArray();
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
            return readLine().toCharArray();
        }
    }

    @Override
    public Reader reader()
    {
        return reader;
    }

    @Override
    public PrintWriter writer()
    {
        return pw;
    }

}
