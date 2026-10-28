/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io;

import io.github.francitoshi.nut.base.commons.lang.Strings;
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
        int msOnClose = 200;

        try (InputStream in = new ThrottledInputStream(new ByteArrayInputStream(bytes), msPerByte, msPerLine, msOnClose, TimeUnit.MILLISECONDS, true))
        {

            long start = System.nanoTime();

            while (in.read() != -1)
            {
                // consume
            }

            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            long lines = data.chars().filter(x -> x == '\n').count();
            
            long expected = Math.max(bytes.length*msPerByte, lines*msPerLine)+msOnClose;
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
