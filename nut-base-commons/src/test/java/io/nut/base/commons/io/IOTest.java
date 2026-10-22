/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.io;

import io.nut.base.commons.io.IO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class IOTest
{
    
    @Test
    public void testReadAllBytes_InputStream_int() throws Exception
    {
        Random random = new Random(0);
        byte[] data = new byte[48000];
        random.nextBytes(data);
        
        InputStream in = new ByteArrayInputStream(data);
        byte[] result = IO.readAllBytes(in, 64000);
        assertArrayEquals(data, result);

        in = new ByteArrayInputStream(data);
        result = IO.readAllBytes(in, 8000);
        assertEquals(8000, result.length);

    }

    @Test
    public void testReadLineBytes() throws Exception
    {
        {
            ByteArrayInputStream bais = new ByteArrayInputStream("hello\nworld".getBytes());

            assertArrayEquals("hello\n".getBytes(), IO.readLineBytes(bais,1000, true));

            assertArrayEquals("world".getBytes(), IO.readLineBytes(bais, 1000, true));
        }
        {
            ByteArrayInputStream bais = new ByteArrayInputStream("hello\nworld".getBytes());

            assertArrayEquals("hello".getBytes(), IO.readLineBytes(bais,1000, false));

            assertArrayEquals("world".getBytes(), IO.readLineBytes(bais, 1000, false));
        }
    }

    @Test
    public void testReadLine() throws Exception
    {
        ByteArrayInputStream bais = new ByteArrayInputStream("hello\nworld".getBytes());
        
        assertEquals("hello", IO.readLine(bais));
        
        assertEquals("world", IO.readLine(bais));
    }

    @Test
    public void testAsPrintStream()
    {
        OutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);

        assertNull(IO.asPrintStream(null));

        PrintStream resultOther = IO.asPrintStream(out);
        assertNotSame(out, resultOther);

        PrintStream resultSame = IO.asPrintStream(ps);
        assertSame(ps, resultSame);
    }

    @Test
    public void testWritePrivate(@TempDir Path dir) throws Exception
    {
        Path file = dir.resolve("secret.bin");
        byte[] data = new byte[]{1, 2, 3};
        IO.writePrivate(file, data);
        assertArrayEquals(data, Files.readAllBytes(file));

        byte[] other = new byte[]{4, 5};
        IO.writePrivate(file, other);
        assertArrayEquals(other, Files.readAllBytes(file));
    }

    @Test
    public void testWritePrivateIsNotWorldReadable(@TempDir Path dir) throws Exception
    {
        Path file = dir.resolve("secret.bin");
        IO.writePrivate(file, new byte[]{1});
        try
        {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
            assertEquals(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                    permissions);
        }
        catch (UnsupportedOperationException e)
        {
            // not a POSIX filesystem
        }
    }

    @Test
    public void testSetPrivate(@TempDir Path dir) throws Exception
    {
        Path file = dir.resolve("secret.bin");
        Files.write(file, new byte[]{1});
        IO.setPrivate(file);
        try
        {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
            assertEquals(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                    permissions);
        }
        catch (UnsupportedOperationException e)
        {
            // not a POSIX filesystem
        }
    }

    @Test
    public void testReplaceAtomically(@TempDir Path dir) throws Exception
    {
        Path source = dir.resolve("source.tmp");
        Path target = dir.resolve("target.bin");
        Files.write(target, new byte[]{9, 9});
        Files.write(source, new byte[]{1, 2, 3});

        IO.replaceAtomically(source, target);

        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(target));
        assertFalse(Files.exists(source));
    }

}
