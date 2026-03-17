/*
 *  Pipes.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
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
package io.nut.base.io;

import java.io.*;
import java.util.Objects;

public final class Pipes
{
    private Pipes()
    {
    }

    private static final int BUFFER = 8192;

    /* ---------------------------------------------------------
       CONNECT (1 → 1)
       --------------------------------------------------------- */
    public static Thread connect(InputStream in, OutputStream out)
    {
        return fanOut(in, out);
    }

    public static Thread connect(Reader in, Writer out)
    {
        return fanOut(in, out);
    }

    public static Thread connect(InputStream in, Writer out)
    {
        return fanOut(new InputStreamReader(in), out);
    }

    public static Thread connect(Reader in, OutputStream out)
    {
        return fanOut(in, new OutputStreamWriter(out));
    }

    /* ---------------------------------------------------------
       FAN OUT (1 → N)
       --------------------------------------------------------- */
    public static Thread fanOut(InputStream in, OutputStream... outs)
    {

        Objects.requireNonNull(outs);

        Thread thread = new Thread(() ->
        {

            byte[] buf = new byte[BUFFER];
            int n;

            try
            {

                while ((n = in.read(buf)) != -1)
                {

                    for (OutputStream os : outs)
                    {
                        os.write(buf, 0, n);
                        os.flush();
                    }

                }

                for (OutputStream os : outs)
                {
                    os.close();
                }

                in.close();

            }
            catch (IOException ex)
            {
                throw new UncheckedIOException(ex);
            }
        });

        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public static Thread fanOut(Reader in, Writer... outs)
    {
        Objects.requireNonNull(outs);

        Thread thread = new Thread(() ->
        {
            char[] buf = new char[BUFFER];
            int n;
            try
            {
                while ((n = in.read(buf)) != -1)
                {
                    for (Writer w : outs)
                    {
                        w.write(buf, 0, n);
                        w.flush();
                    }
                }

                for (Writer w : outs)
                {
                    w.close();
                }

                in.close();
            }
            catch (IOException ex)
            {
                throw new UncheckedIOException(ex);
            }

        });

        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /* ---------------------------------------------------------
       FAN IN (N → 1)
       --------------------------------------------------------- */
    public static Thread fanIn(OutputStream out, InputStream... ins)
    {
        Thread thread = new Thread(() ->
        {
            byte[] buf = new byte[BUFFER];

            try
            {

                for (InputStream is : ins)
                {

                    int n;

                    while ((n = is.read(buf)) != -1)
                    {
                        out.write(buf, 0, n);
                        out.flush();
                    }

                }

                out.close();

            }
            catch (IOException ex)
            {
                throw new UncheckedIOException(ex);
            }

        });

        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public static Thread fanIn(Writer out, Reader... ins)
    {

        Thread t = new Thread(() ->
        {

            char[] buf = new char[BUFFER];

            try
            {

                for (Reader in : ins)
                {

                    int n;

                    while ((n = in.read(buf)) != -1)
                    {
                        out.write(buf, 0, n);
                        out.flush();
                    }

                }

                out.close();

            }
            catch (IOException ex)
            {
                throw new UncheckedIOException(ex);
            }

        });

        t.setDaemon(true);
        t.start();
        return t;
    }

    /* ---------------------------------------------------------
       PIPES
       --------------------------------------------------------- */
    public static BytePipe bytePipe() throws IOException
    {
        return new BytePipe();
    }

    public static CharPipe charPipe() throws IOException
    {
        return new CharPipe();
    }

    /* ---------------------------------------------------------
       DUPLEX (2 pipes)
       --------------------------------------------------------- */
    public static BytePipe[] byteDuplex() throws IOException
    {
        return new BytePipe[]
        {
            new BytePipe(), // A → B
            new BytePipe()  // B → A
        };
    }

    public static CharPipe[] charDuplex() throws IOException
    {
        return new CharPipe[]
        {
            new CharPipe(), // A → B
            new CharPipe()  // B → A
        };
    }

    /* ---------------------------------------------------------
       INNER CLASSES
       --------------------------------------------------------- */
    public static final class BytePipe
    {

        public final PipedInputStream input;
        public final PipedOutputStream output;

        private BytePipe() throws IOException
        {
            output = new PipedOutputStream();
            input = new PipedInputStream(output);
        }
    }

    public static final class CharPipe
    {

        public final PipedReader reader;
        public final PipedWriter writer;

        private CharPipe() throws IOException
        {
            writer = new PipedWriter();
            reader = new PipedReader(writer);
        }
    }

}
