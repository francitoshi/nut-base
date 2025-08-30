/*
 *  ThrottledOutputStreamTest.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
