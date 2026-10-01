/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class MultiOutputStreamTest
{
    private static class Tracked extends ByteArrayOutputStream
    {
        final AtomicInteger flushes = new AtomicInteger();
        int closes = 0;

        @Override
        public void flush() throws IOException
        {
            flushes.incrementAndGet();
            super.flush();
        }

        @Override
        public void close() throws IOException
        {
            closes++;
            super.close();
        }
    }

    private static class Failing extends OutputStream
    {
        final boolean onWrite;
        Failing(boolean onWrite)
        {
            this.onWrite = onWrite;
        }

        @Override
        public void write(int b) throws IOException
        {
            if (onWrite)
            {
                throw new IOException("write failed");
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            if (onWrite)
            {
                throw new IOException("write failed");
            }
        }

        @Override
        public void flush() throws IOException
        {
            if (!onWrite)
            {
                throw new IOException("flush failed");
            }
        }

        @Override
        public void close() throws IOException
        {
            if (!onWrite)
            {
                throw new IOException("close failed");
            }
        }
    }

    @Test
    public void testWriteReachesEveryDelegate() throws Exception
    {
        ByteArrayOutputStream a = new ByteArrayOutputStream();
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ByteArrayOutputStream c = new ByteArrayOutputStream();

        try (OutputStream out = new MultiOutputStream(a, b, c))
        {
            out.write('h');
            out.write("ello".getBytes());
            out.write(" world".getBytes(), 1, 4);
        }

        assertArrayEquals("helloworl".getBytes(), a.toByteArray());
        assertArrayEquals(a.toByteArray(), b.toByteArray());
        assertArrayEquals(a.toByteArray(), c.toByteArray());
    }

    @Test
    public void testEmptyFanOutIsNoOpButStillValidates() throws Exception
    {
        OutputStream out = new MultiOutputStream();

        out.write('x');
        out.write(new byte[3]);
        out.flush();
        out.close();

        assertThrows(IndexOutOfBoundsException.class, () -> out.write(new byte[10], 5, 100));
        assertThrows(NullPointerException.class, () -> out.write(null));
    }

    @Test
    public void testWriteValidatesArguments() throws Exception
    {
        try (OutputStream out = new MultiOutputStream(new ByteArrayOutputStream()))
        {
            assertThrows(NullPointerException.class, () -> out.write(null, 0, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> out.write(new byte[10], -1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> out.write(new byte[10], 0, 11));
            assertThrows(IndexOutOfBoundsException.class, () -> out.write(new byte[10], 6, 5));
            assertThrows(IndexOutOfBoundsException.class, () -> out.write(new byte[10], 0, -1));
            out.write(new byte[10], 10, 0);
        }
    }

    @Test
    public void testDefensiveCopyOfDelegates() throws Exception
    {
        ByteArrayOutputStream original = new ByteArrayOutputStream();
        ByteArrayOutputStream replacement = new ByteArrayOutputStream();
        OutputStream[] items = new OutputStream[] { original };

        try (OutputStream out = new MultiOutputStream(items))
        {
            items[0] = replacement;
            out.write('z');
        }

        assertEquals(1, original.size());
        assertEquals(0, replacement.size());
    }

    @Test
    public void testRejectsNullDelegates() throws Exception
    {
        assertThrows(NullPointerException.class, () -> new MultiOutputStream((OutputStream[]) null));
        assertThrows(NullPointerException.class, () -> new MultiOutputStream(new ByteArrayOutputStream(), null));
    }

    @Test
    public void testCloseClosesEveryDelegateEvenOnFailure() throws Exception
    {
        Tracked first = new Tracked();
        Tracked last = new Tracked();

        OutputStream out = new MultiOutputStream(first, new Failing(false), last);
        IOException ex = assertThrows(IOException.class, out::close);
        assertEquals("close failed", ex.getMessage());
        assertEquals(0, ex.getSuppressed().length);

        assertEquals(1, first.closes);
        assertEquals(1, last.closes);
    }

    @Test
    public void testCloseCollectsEveryFailure() throws Exception
    {
        OutputStream out = new MultiOutputStream(new Failing(false), new Failing(false), new Failing(false));
        IOException ex = assertThrows(IOException.class, out::close);
        assertEquals("close failed", ex.getMessage());
        assertEquals(2, ex.getSuppressed().length);
    }

    @Test
    public void testFlushReachesEveryDelegateEvenOnFailure() throws Exception
    {
        Tracked first = new Tracked();
        Tracked last = new Tracked();

        OutputStream out = new MultiOutputStream(first, new Failing(false), last);
        IOException ex = assertThrows(IOException.class, out::flush);
        assertEquals("flush failed", ex.getMessage());
        assertEquals(1, first.flushes.get());
        assertEquals(1, last.flushes.get());
    }

    @Test
    public void testCloseIsIdempotent() throws Exception
    {
        Tracked a = new Tracked();
        Tracked b = new Tracked();

        OutputStream out = new MultiOutputStream(a, b);
        out.close();
        out.close();
        out.close();

        assertEquals(1, a.closes);
        assertEquals(1, b.closes);
    }

    @Test
    public void testFailsAfterClose() throws Exception
    {
        ByteArrayOutputStream a = new ByteArrayOutputStream();
        OutputStream out = new MultiOutputStream(a);
        out.close();

        assertThrows(IOException.class, () -> out.write('x'));
        assertThrows(IOException.class, () -> out.write(new byte[1]));
        assertThrows(IOException.class, () -> out.write(new byte[1], 0, 1));
        assertThrows(IOException.class, out::flush);
        assertDoesNotThrow(out::close);
    }

    @Test
    public void testWriteFailureStillHitsTheOtherDelegates() throws Exception
    {
        ByteArrayOutputStream a = new ByteArrayOutputStream();
        ByteArrayOutputStream c = new ByteArrayOutputStream();

        OutputStream out = new MultiOutputStream(a, new Failing(true), c);
        assertThrows(IOException.class, () -> out.write("hi".getBytes()));
        assertThrows(IOException.class, () -> out.write('!'));

        assertArrayEquals("hi!".getBytes(), a.toByteArray());
        assertArrayEquals("hi!".getBytes(), c.toByteArray());
    }
}
