/*
 * Copyright (c) 2026 francitoshi@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.util;

import java.nio.ByteOrder;

public class Endian
{
    /** Reports the native byte order of the current CPU at runtime. */
    public static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();
 
    // =========================================================================
    // short  (2 bytes — reverseBytes intrinsic → single XCHG / BSWAP on x86)
    // =========================================================================
 
    /**
     * Converts a {@code short} from Java (Big Endian) to Little Endian by
     * swapping its two bytes.
     *
     * <p>Uses {@link Short#reverseBytes}, which HotSpot compiles to a single
     * {@code XCHG} or {@code BSWAP} instruction on x86.
     *
     * @param value value in Java/Big Endian byte order
     * @return same value in Little Endian byte order
     */
    public static short shortToLittle(short value) 
    {
        return Short.reverseBytes(value);
    }
 
    /**
     * Converts a {@code short} from Little Endian to Java (Big Endian) byte order.
     * Identical to {@link #shortToLittle(short)} because byte-swap is self-inverse.
     *
     * @param value value in Little Endian byte order
     * @return same value in Java/Big Endian byte order
     */
    public static short littleToShort(short value) 
    {
        return Short.reverseBytes(value);
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     * {@code src} and {@code dst} may be the same array (in-place swap).
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static short[] shortToLittle(short[] src, short[] dst) 
    {
        checkArrayArgs(src, dst);
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = Short.reverseBytes(src[i]);
        }
        return dst;
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static short[] littleToShort(short[] src, short[] dst)
    {
        return shortToLittle(src, dst);
    }
 
    // =========================================================================
    // char  (2 bytes — same mechanics as short)
    // =========================================================================
 
    /**
     * Converts a {@code char} from Java (Big Endian) to Little Endian.
     *
     * @param value char in Java/Big Endian byte order
     * @return same char in Little Endian byte order
     */
    public static char charToLittle(char value) 
    {
        return (char) Short.reverseBytes((short) value);
    }
 
    /**
     * Converts a {@code char} from Little Endian to Java (Big Endian) byte order.
     *
     * @param value char in Little Endian byte order
     * @return same char in Java/Big Endian byte order
     */
    public static char littleToChar(char value) 
    {
        return (char) Short.reverseBytes((short) value);
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static char[] charToLittle(char[] src, char[] dst)
    {
        checkArrayArgs(src, dst);
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = (char) Short.reverseBytes((short) src[i]);
        }
        return dst;
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static char[] littleToChar(char[] src, char[] dst)
    {
        return charToLittle(src, dst);
    }
 
    // =========================================================================
    // int  (4 bytes — BSWAP intrinsic)
    // =========================================================================
 
    /**
     * Converts an {@code int} from Java (Big Endian) to Little Endian by
     * reversing all four bytes.
     *
     * <p>Example: {@code 0x12345678} → {@code 0x78563412}
     *
     * <p>Uses {@link Integer#reverseBytes}, a HotSpot intrinsic compiled to
     * a single {@code BSWAP} instruction on x86/x64.
     *
     * @param value value in Java/Big Endian byte order
     * @return same value in Little Endian byte order
     */
    public static int intToLittle(int value) 
    {
        return Integer.reverseBytes(value);
    }
 
    /**
     * Converts an {@code int} from Little Endian to Java (Big Endian) byte order.
     * Identical to {@link #intToLittle(int)} because byte-swap is self-inverse.
     *
     * @param value value in Little Endian byte order
     * @return same value in Java/Big Endian byte order
     */
    public static int littleToInt(int value)
    {
        return Integer.reverseBytes(value);
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     * {@code src} and {@code dst} may be the same array (in-place).
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static int[] intToLittle(int[] src, int[] dst)
    {
        checkArrayArgs(src, dst);
        for (int i = 0; i < src.length; i++) dst[i] = Integer.reverseBytes(src[i]);
        return dst;
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static int[] littleToInt(int[] src, int[] dst)
    {
        return intToLittle(src, dst);
    }
 
    // =========================================================================
    // long  (8 bytes — BSWAP intrinsic on 64-bit)
    // =========================================================================
 
    /**
     * Converts a {@code long} from Java (Big Endian) to Little Endian by
     * reversing all eight bytes.
     *
     * <p>Uses {@link Long#reverseBytes}, a HotSpot intrinsic compiled to a
     * single {@code BSWAP} on 64-bit x86.
     *
     * @param value value in Java/Big Endian byte order
     * @return same value in Little Endian byte order
     */
    public static long longToLittle(long value)
    {
        return Long.reverseBytes(value);
    }
 
    /**
     * Converts a {@code long} from Little Endian to Java (Big Endian) byte order.
     *
     * @param value value in Little Endian byte order
     * @return same value in Java/Big Endian byte order
     */
    public static long littleToLong(long value)
    {
        return Long.reverseBytes(value);
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static long[] longToLittle(long[] src, long[] dst)
    {
        checkArrayArgs(src, dst);
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = Long.reverseBytes(src[i]);
        }
        return dst;
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static long[] littleToLong(long[] src, long[] dst)
    {
        return longToLittle(src, dst);
    }
 
    // =========================================================================
    // float  (4 bytes — bit-cast to int, BSWAP, bit-cast back)
    // =========================================================================
 
    /**
     * Converts a {@code float} from Java (Big Endian) to Little Endian.
     *
     * <p>The float is reinterpreted as its raw IEEE 754 {@code int} bit pattern
     * via {@link Float#floatToRawIntBits} (no NaN canonicalization), the four
     * bytes are reversed with a {@code BSWAP} intrinsic, then the result is
     * reinterpreted back as a {@code float} via {@link Float#intBitsToFloat}.
     *
     * @param value value in Java/Big Endian byte order
     * @return same bit pattern in Little Endian byte order
     */
    public static float floatToLittle(float value)
    {
        return Float.intBitsToFloat(Integer.reverseBytes(Float.floatToRawIntBits(value)));
    }
 
    /**
     * Converts a {@code float} from Little Endian to Java (Big Endian) byte order.
     *
     * @param value value in Little Endian byte order
     * @return same bit pattern in Java/Big Endian byte order
     */
    public static float littleToFloat(float value)
    {
        return Float.intBitsToFloat(Integer.reverseBytes(Float.floatToRawIntBits(value)));
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static float[] floatToLittle(float[] src, float[] dst)
    {
        checkArrayArgs(src, dst);
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = Float.intBitsToFloat(Integer.reverseBytes(Float.floatToRawIntBits(src[i])));
        }
        return dst;
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static float[] littleToFloat(float[] src, float[] dst) 
    {
        return floatToLittle(src, dst);
    }
 
    // =========================================================================
    // double  (8 bytes — bit-cast to long, BSWAP, bit-cast back)
    // =========================================================================
 
    /**
     * Converts a {@code double} from Java (Big Endian) to Little Endian.
     *
     * <p>Uses {@link Double#doubleToRawLongBits} (preserves NaN payloads),
     * {@link Long#reverseBytes}, and {@link Double#longBitsToDouble}.
     *
     * @param value value in Java/Big Endian byte order
     * @return same bit pattern in Little Endian byte order
     */
    public static double doubleToLittle(double value)
    {
        return Double.longBitsToDouble(Long.reverseBytes(Double.doubleToRawLongBits(value)));
    }
 
    /**
     * Converts a {@code double} from Little Endian to Java (Big Endian) byte order.
     *
     * @param value value in Little Endian byte order
     * @return same bit pattern in Java/Big Endian byte order
     */
    public static double littleToDouble(double value)
    {
        return Double.longBitsToDouble(Long.reverseBytes(Double.doubleToRawLongBits(value)));
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static double[] javaToLittle(double[] src, double[] dst) 
    {
        checkArrayArgs(src, dst);
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = Double.longBitsToDouble(Long.reverseBytes(Double.doubleToRawLongBits(src[i])));
        }
        return dst;
    }
 
    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static double[] littleToJava(double[] src, double[] dst) 
    {
        return javaToLittle(src, dst);
    }
 
    // =========================================================================
    // byte[] ↔ typed conversions  (Big Endian ↔ Little Endian via raw bytes)
    // =========================================================================
    //
    // Naming convention mirrors the typed overloads:
    //   shortToLittle / littleToShort
    //   charToLittle  / littleToChar
    //   intToLittle   / littleToInt
    //   longToLittle  / littleToLong
    //   floatToLittle / littleToFloat
    //   doubleToLittle / littleToDouble  (alias: javaToLittle / littleToJava)
    //
    // Every method that reads N-byte words requires src.length % N == 0.
    // Every method that writes to dst requires dst.length >= src.length.
    //
    // "ToLittle" interprets src bytes as Big Endian words and writes them
    // reversed into dst (Java BE → LE).  "ToJava"/"ToShort"/etc. does the
    // same operation in the opposite semantic direction; because byte-swap
    // is self-inverse both directions share the same implementation.
    // =========================================================================

    // ── short (2 bytes) ──────────────────────────────────────────────────────

    /**
     * Reinterprets {@code src} as a sequence of Big Endian {@code short} words
     * and writes each word byte-swapped (Little Endian) into {@code dst}.
     *
     * <p>{@code src} and {@code dst} may be the same array (in-place swap).
     *
     * @param src source byte array; length must be a multiple of 2
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if lengths are invalid
     */
    public static byte[] shortToLittle(byte[] src, byte[] dst)
    {
        checkByteArrayArgs(src, dst, Short.BYTES);
        for (int i = 0; i < src.length; i += Short.BYTES)
        {
            // Read all bytes before any write so in-place (src == dst) is safe.
            byte b0 = src[i    ];
            byte b1 = src[i + 1];
            dst[i    ] = b1;
            dst[i + 1] = b0;
        }
        return dst;
    }

    /**
     * Reinterprets {@code src} as a sequence of Little Endian {@code short} words
     * and writes each word byte-swapped (Big Endian / Java order) into {@code dst}.
     * Identical to {@link #shortToLittle(byte[], byte[])} because byte-swap is
     * self-inverse.
     *
     * @param src source byte array; length must be a multiple of 2
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] littleToShort(byte[] src, byte[] dst)
    {
        return shortToLittle(src, dst);
    }

    // ── char (2 bytes) ───────────────────────────────────────────────────────

    /**
     * Reinterprets {@code src} as a sequence of Big Endian {@code char} values
     * and writes each value byte-swapped (Little Endian) into {@code dst}.
     *
     * @param src source byte array; length must be a multiple of 2
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] charToLittle(byte[] src, byte[] dst)
    {
        return shortToLittle(src, dst);   // char and short share the same 2-byte layout
    }

    /**
     * Reinterprets {@code src} as a sequence of Little Endian {@code char} values
     * and writes each value byte-swapped (Big Endian / Java order) into {@code dst}.
     *
     * @param src source byte array; length must be a multiple of 2
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] littleToChar(byte[] src, byte[] dst)
    {
        return shortToLittle(src, dst);
    }

    // ── int (4 bytes) ────────────────────────────────────────────────────────

    /**
     * Reinterprets {@code src} as a sequence of Big Endian {@code int} words
     * and writes each word byte-swapped (Little Endian) into {@code dst}.
     *
     * <p>Example for a single word:
     * {@code [0x12, 0x34, 0x56, 0x78]} → {@code [0x78, 0x56, 0x34, 0x12]}
     *
     * @param src source byte array; length must be a multiple of 4
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] intToLittle(byte[] src, byte[] dst)
    {
        checkByteArrayArgs(src, dst, Integer.BYTES);
        for (int i = 0; i < src.length; i += Integer.BYTES)
        {
            // Read all bytes before any write so in-place (src == dst) is safe.
            byte b0 = src[i    ];
            byte b1 = src[i + 1];
            byte b2 = src[i + 2];
            byte b3 = src[i + 3];
            dst[i    ] = b3;
            dst[i + 1] = b2;
            dst[i + 2] = b1;
            dst[i + 3] = b0;
        }
        return dst;
    }

    /**
     * Reinterprets {@code src} as a sequence of Little Endian {@code int} words
     * and writes each word byte-swapped (Big Endian / Java order) into {@code dst}.
     *
     * @param src source byte array; length must be a multiple of 4
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] littleToInt(byte[] src, byte[] dst)
    {
        return intToLittle(src, dst);
    }

    // ── long (8 bytes) ───────────────────────────────────────────────────────

    /**
     * Reinterprets {@code src} as a sequence of Big Endian {@code long} words
     * and writes each word byte-swapped (Little Endian) into {@code dst}.
     *
     * @param src source byte array; length must be a multiple of 8
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] longToLittle(byte[] src, byte[] dst)
    {
        checkByteArrayArgs(src, dst, Long.BYTES);
        for (int i = 0; i < src.length; i += Long.BYTES)
        {
            // Read all bytes before any write so in-place (src == dst) is safe.
            byte b0 = src[i    ];
            byte b1 = src[i + 1];
            byte b2 = src[i + 2];
            byte b3 = src[i + 3];
            byte b4 = src[i + 4];
            byte b5 = src[i + 5];
            byte b6 = src[i + 6];
            byte b7 = src[i + 7];
            dst[i    ] = b7;
            dst[i + 1] = b6;
            dst[i + 2] = b5;
            dst[i + 3] = b4;
            dst[i + 4] = b3;
            dst[i + 5] = b2;
            dst[i + 6] = b1;
            dst[i + 7] = b0;
        }
        return dst;
    }

    /**
     * Reinterprets {@code src} as a sequence of Little Endian {@code long} words
     * and writes each word byte-swapped (Big Endian / Java order) into {@code dst}.
     *
     * @param src source byte array; length must be a multiple of 8
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] littleToLong(byte[] src, byte[] dst)
    {
        return longToLittle(src, dst);
    }

    // ── float (4 bytes) ──────────────────────────────────────────────────────

    /**
     * Reinterprets {@code src} as a sequence of Big Endian IEEE 754 {@code float}
     * bit patterns and writes each pattern byte-swapped (Little Endian) into
     * {@code dst}.  The byte-level operation is identical to
     * {@link #intToLittle(byte[], byte[])}.
     *
     * @param src source byte array; length must be a multiple of 4
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] floatToLittle(byte[] src, byte[] dst)
    {
        return intToLittle(src, dst);     // float and int share the same 4-byte layout
    }

    /**
     * Reinterprets {@code src} as a sequence of Little Endian IEEE 754
     * {@code float} bit patterns and writes each pattern byte-swapped
     * (Big Endian / Java order) into {@code dst}.
     *
     * @param src source byte array; length must be a multiple of 4
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] littleToFloat(byte[] src, byte[] dst)
    {
        return intToLittle(src, dst);
    }

    // ── double (8 bytes) ─────────────────────────────────────────────────────

    /**
     * Reinterprets {@code src} as a sequence of Big Endian IEEE 754
     * {@code double} bit patterns and writes each pattern byte-swapped
     * (Little Endian) into {@code dst}.  The byte-level operation is identical
     * to {@link #longToLittle(byte[], byte[])}.
     *
     * @param src source byte array; length must be a multiple of 8
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] doubleToLittle(byte[] src, byte[] dst)
    {
        return longToLittle(src, dst);    // double and long share the same 8-byte layout
    }

    /**
     * Alias for {@link #doubleToLittle(byte[], byte[])} following the
     * {@code javaToLittle} / {@code littleToJava} naming used by the
     * {@code double[]} overloads.
     *
     * @param src source byte array; length must be a multiple of 8
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] javaToLittle(byte[] src, byte[] dst)
    {
        return longToLittle(src, dst);
    }

    /**
     * Reinterprets {@code src} as a sequence of Little Endian IEEE 754
     * {@code double} bit patterns and writes each pattern byte-swapped
     * (Big Endian / Java order) into {@code dst}.
     *
     * @param src source byte array; length must be a multiple of 8
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] littleToDouble(byte[] src, byte[] dst)
    {
        return longToLittle(src, dst);
    }

    /**
     * Alias for {@link #littleToDouble(byte[], byte[])} following the
     * {@code javaToLittle} / {@code littleToJava} naming used by the
     * {@code double[]} overloads.
     *
     * @param src source byte array; length must be a multiple of 8
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     */
    public static byte[] littleToJava(byte[] src, byte[] dst)
    {
        return longToLittle(src, dst);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================
 
    /**
     * Validates that both arrays are non-null and that {@code dst} is at least
     * as long as {@code src}. Uses {@code Object} to serve all array types from
     * a single method; actual length extraction uses reflection-free casts via
     * overloaded length helpers (avoided here by accessing the length field
     * directly on the typed variants).
     */
    private static void checkArrayArgs(Object src, Object dst) 
    {
        if (src == null) throw new IllegalArgumentException("src must not be null");
        if (dst == null) throw new IllegalArgumentException("dst must not be null");
    }
 
    // Typed length guards — called before the loop in every array overload.
    // Kept private; public API already validates via the generic checkArrayArgs.
    private static void checkArrayArgs(byte[]    src, byte[]    dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(boolean[] src, boolean[] dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(short[]   src, short[]   dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(char[]    src, char[]    dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(int[]     src, int[]     dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(long[]    src, long[]    dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(float[]   src, float[]   dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(double[]  src, double[]  dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
 
    private static void checkNotNull(Object src, Object dst) 
    {
        if (src == null) throw new IllegalArgumentException("src must not be null");
        if (dst == null) throw new IllegalArgumentException("dst must not be null");
    }
 
    private static void checkLen(int srcLen, int dstLen) 
    {
        if (dstLen < srcLen)
        {
            throw new IllegalArgumentException("dst length (" + dstLen + ") must be >= src length (" + srcLen + ")");
        }
    }

    /**
     * Validates byte-array arguments for a typed swap of {@code wordSize} bytes.
     * Checks non-null, dst length ≥ src length, and that src length is an exact
     * multiple of {@code wordSize}.
     *
     * @param src      source byte array
     * @param dst      destination byte array
     * @param wordSize size of each logical word in bytes (2, 4, or 8)
     * @throws IllegalArgumentException if any constraint is violated
     */
    private static void checkByteArrayArgs(byte[] src, byte[] dst, int wordSize)
    {
        checkNotNull(src, dst);
        checkLen(src.length, dst.length);
        if (src.length % wordSize != 0)
        {
            throw new IllegalArgumentException(
                "src length (" + src.length + ") must be a multiple of " + wordSize);
        }
    }

}
