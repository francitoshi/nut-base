/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThrottledOutputStreamTest
{

    @Test
    void testThrottlingPerByte() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bytesToWrite = 10;
        int millisPerByte = 50;

        ThrottledOutputStream throttledOut = new ThrottledOutputStream(baos, millisPerByte, 0, TimeUnit.MILLISECONDS, true);

        long start = System.nanoTime();

        for (int i = 0; i < bytesToWrite; i++)
        {
            throttledOut.write('a');
        }
        throttledOut.flush();

        long end = System.nanoTime();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(end - start);

        long expectedMillis = bytesToWrite * millisPerByte;

        System.out.printf("Elapsed: %d ms, Expected >= %d ms%n", elapsedMillis, expectedMillis);

        assertTrue(elapsedMillis >= expectedMillis, "OutputStream did not respect throttling (per byte)");
    }

    @Test
    void testThrottlingPerLine() throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int linesToWrite = 5;
        int millisPerLine = 100;

        ThrottledOutputStream throttledOut = new ThrottledOutputStream(baos, 0, millisPerLine, TimeUnit.MILLISECONDS, true);

        long start = System.nanoTime();

        for (int i = 0; i < linesToWrite; i++)
        {
            throttledOut.write("line\n".getBytes());
        }
        throttledOut.flush();

        long end = System.nanoTime();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(end - start);

        long expectedMillis = linesToWrite * millisPerLine;

        System.out.printf("Elapsed: %d ms, Expected >= %d ms%n", elapsedMillis, expectedMillis);

        assertTrue(elapsedMillis >= expectedMillis, "OutputStream did not respect throttling (per line)");
    }
}
