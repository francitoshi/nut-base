/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.encoding;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Benchmarks {@link io.nut.base.encoding.Base64} against {@link java.util.Base64}.
 *
 * @author franci
 */
@Tag("benchmark")
public class Base64BenchmarkTest
{
    public static void main(String[] args) throws Exception
    {
        new Base64BenchmarkTest().benchmarkBase64();
    }
    private static final int WARMUP_NANOS = 1_000_000_000;
    private static final int RUN_NANOS = 2_000_000_000;

    private static byte[] randomBytes(int size)
    {
        byte[] data = new byte[size];
        for (int i = 0; i < data.length; i++)
        {
            data[i] = (byte) (i * 31 + size);
        }
        return data;
    }

    private interface Op
    {
        void run();
    }

    private static long opsPerSecond(Op op)
    {
        long warmupUntil = System.nanoTime() + WARMUP_NANOS;
        long count = 0;
        while (System.nanoTime() < warmupUntil)
        {
            op.run();
            count++;
        }

        long until = System.nanoTime() + RUN_NANOS;
        count = 0;
        while (System.nanoTime() < until)
        {
            op.run();
            count++;
        }
        long elapsed = System.nanoTime() - until + RUN_NANOS;
        return count * 1_000_000_000L / Math.max(1, elapsed);
    }

    @Test
    public void benchmarkBase64() throws Exception
    {
        int[] sizes = { 16, 64, 256, 1024, 4096, 65536 };

        System.out.printf("\n%-14s %14s %14s %10s %14s %14s %10s\n",
                "size(bytes)", "nut ops/s", "jdk ops/s", "ratio", "nut ops/s", "jdk ops/s", "ratio");
        System.out.printf("%-14s %14s %14s %10s %14s %14s %10s\n",
                "", "-----encode-----", "", "", "-----decode-----", "", "");

        for (int size : sizes)
        {
            byte[] raw = randomBytes(size);
            final String encoded = java.util.Base64.getEncoder().encodeToString(raw);

            // sanity: both implementations must agree
            assertDecodeEquals(raw, encoded);

            long nutEncode = opsPerSecond(() -> Base64.encode(raw));
            long jdkEncode = opsPerSecond(() -> java.util.Base64.getEncoder().encodeToString(raw));

            long nutDecode = opsPerSecond(() -> {
                try
                {
                    Base64.decode(encoded);
                }
                catch (Base64DecoderException ex)
                {
                    throw new RuntimeException(ex);
                }
            });
            long jdkDecode = opsPerSecond(() -> java.util.Base64.getDecoder().decode(encoded));

            System.out.printf("%-14d %14d %14d %9.2fx %14d %14d %9.2fx\n",
                    size, nutEncode, jdkEncode, ratio(nutEncode, jdkEncode),
                    nutDecode, jdkDecode, ratio(nutDecode, jdkDecode));
        }
        System.out.println();
    }

    private static double ratio(long nut, long jdk)
    {
        return nut / (double) Math.max(1, jdk);
    }

    private static void assertDecodeEquals(byte[] raw, String encoded) throws Exception
    {
        byte[] nut = Base64.decode(encoded);
        byte[] jdk = java.util.Base64.getDecoder().decode(encoded);

        if (nut.length != jdk.length || nut.length != raw.length)
        {
            throw new AssertionError("length mismatch");
        }
        for (int i = 0; i < raw.length; i++)
        {
            if (nut[i] != raw[i] || jdk[i] != raw[i])
            {
                throw new AssertionError("decode mismatch at " + i);
            }
        }
        if (!Base64.encode(raw).equals(encoded))
        {
            throw new AssertionError("encode mismatch (nut)");
        }
    }
}