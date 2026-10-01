/*
 *  PipesTest.java
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;


class PipesTest
{

    private static final String TEST_TEXT = "Hello Pipes!\nSecond line\n";

    /* ---------------------------------------------------------
       BYTE PIPE
       --------------------------------------------------------- */
    @Test
    void testBytePipeTransfer() throws Exception
    {

        Pipes.BytePipe pipe = Pipes.bytePipe();

        Thread writer = new Thread(() ->
        {
            try (OutputStream out = pipe.output)
            {
                out.write(TEST_TEXT.getBytes(StandardCharsets.UTF_8));
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        Thread reader = new Thread(() ->
        {
            try (InputStream in = pipe.input)
            {
                int b;
                while ((b = in.read()) != -1)
                {
                    result.write(b);
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });

        writer.start();
        reader.start();

        writer.join();
        reader.join();

        assertEquals(TEST_TEXT, result.toString("UTF-8"));
    }

    /* ---------------------------------------------------------
       CHAR PIPE
       --------------------------------------------------------- */
    @Test
    void testCharPipeTransfer() throws Exception
    {

        Pipes.CharPipe pipe = Pipes.charPipe();

        Thread writer = new Thread(() ->
        {
            try (Writer w = pipe.writer)
            {
                w.write(TEST_TEXT);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });

        StringWriter result = new StringWriter();

        Thread reader = new Thread(() ->
        {
            try (Reader r = pipe.reader)
            {
                char[] buf = new char[256];
                int n;
                while ((n = r.read(buf)) != -1)
                {
                    result.write(buf, 0, n);
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });

        writer.start();
        reader.start();

        writer.join();
        reader.join();

        assertEquals(TEST_TEXT, result.toString());
    }

    /* ---------------------------------------------------------
       BYTE DUPLEX
       --------------------------------------------------------- */
    @Test
    void testByteDuplexBidirectional() throws Exception
    {

        Pipes.BytePipe[] d = Pipes.byteDuplex();

        String msgA = "from A";
        String msgB = "from B";

        ByteArrayOutputStream receivedByA = new ByteArrayOutputStream();
        ByteArrayOutputStream receivedByB = new ByteArrayOutputStream();

        Thread sideA = new Thread(() ->
        {
            try
            {
                d[0].output.write(msgA.getBytes());
                d[0].output.close();

                int b;
                while ((b = d[1].input.read()) != -1)
                {
                    receivedByA.write(b);
                }

            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });

        Thread sideB = new Thread(() ->
        {
            try
            {
                d[1].output.write(msgB.getBytes());
                d[1].output.close();

                int b;
                while ((b = d[0].input.read()) != -1)
                {
                    receivedByB.write(b);
                }

            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });

        sideA.start();
        sideB.start();

        sideA.join();
        sideB.join();

        assertEquals(msgB, receivedByA.toString());
        assertEquals(msgA, receivedByB.toString());
    }

    /* ---------------------------------------------------------
       CHAR DUPLEX
       --------------------------------------------------------- */
    @Test
    void testCharDuplexBidirectional() throws Exception
    {

        Pipes.CharPipe[] d = Pipes.charDuplex();

        String msgA = "hello from A";
        String msgB = "hello from B";

        StringWriter receivedByA = new StringWriter();
        StringWriter receivedByB = new StringWriter();

        Thread sideA = new Thread(() ->
        {
            try
            {
                d[0].writer.write(msgA);
                d[0].writer.close();

                char[] buf = new char[64];
                int n;
                while ((n = d[1].reader.read(buf)) != -1)
                {
                    receivedByA.write(buf, 0, n);
                }

            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });

        Thread sideB = new Thread(() ->
        {
            try
            {
                d[1].writer.write(msgB);
                d[1].writer.close();

                char[] buf = new char[64];
                int n;
                while ((n = d[0].reader.read(buf)) != -1)
                {
                    receivedByB.write(buf, 0, n);
                }

            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });

        sideA.start();
        sideB.start();

        sideA.join();
        sideB.join();

        assertEquals(msgB, receivedByA.toString());
        assertEquals(msgA, receivedByB.toString());
    }

    /* ---------------------------------------------------------
       CONNECT
       --------------------------------------------------------- */
    @Test
    void testConnectReaderWriter() throws Exception
    {

        StringReader in = new StringReader(TEST_TEXT);
        StringWriter out = new StringWriter();

        Thread t = Pipes.connect(in, out);
        t.join();

        assertEquals(TEST_TEXT, out.toString());
    }

    /* ---------------------------------------------------------
       FAN OUT
       --------------------------------------------------------- */
    @Test
    void testFanOutBytes() throws Exception
    {

        ByteArrayInputStream in
                = new ByteArrayInputStream(TEST_TEXT.getBytes());

        ByteArrayOutputStream a = new ByteArrayOutputStream();
        ByteArrayOutputStream b = new ByteArrayOutputStream();

        Thread t = Pipes.fanOut(in, a, b);
        t.join();

        assertEquals(TEST_TEXT, a.toString());
        assertEquals(TEST_TEXT, b.toString());
    }

    /* ---------------------------------------------------------
       FAN IN
       --------------------------------------------------------- */
    @Test
    void testFanInBytes() throws Exception
    {

        ByteArrayInputStream a
                = new ByteArrayInputStream("A".getBytes());

        ByteArrayInputStream b
                = new ByteArrayInputStream("B".getBytes());

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Thread t = Pipes.fanIn(out, a, b);
        t.join();

        assertEquals("AB", out.toString());
    }

}
