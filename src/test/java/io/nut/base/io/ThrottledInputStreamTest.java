/*
 *  ThrottledInputStreamTest.java
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

import io.nut.base.util.Strings;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThrottledInputStreamTest
{

    @Test
    void testDelayPerByteAndLine_singleByteReads() throws Exception
    {
        String data = Strings.repeat("Hello\nWorld\n", 10);
        byte[] bytes = data.getBytes("UTF-8");

        int msPerByte = 10;
        int msPerLine = 50;

        try (InputStream in = new ThrottledInputStream(new ByteArrayInputStream(bytes), msPerByte, msPerLine, TimeUnit.MILLISECONDS, true))
        {

            long start = System.currentTimeMillis();

            while (in.read() != -1)
            {
                // consume
            }

            long elapsed = System.currentTimeMillis() - start;

            long lines = data.chars().filter(x -> x == '\n').count();
            
            long expected = Math.max(bytes.length*msPerByte, lines*msPerLine);
            assertInRange(elapsed, expected, 0.8, 1.2);
        }
    }

    private static void assertInRange(long actual, long expected, double lowerFactor, double upperFactor)
    {
        long lower = (long) (expected * lowerFactor);
        long upper = (long) (expected * upperFactor);
        assertTrue(actual >= lower && actual <= upper, "Elapsed " + actual + " ms not in expected range [" + lower + "," + upper + "] (expected ~" + expected + ")");
    }
}
