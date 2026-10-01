/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.lang;

import java.nio.ByteOrder;

/**
 * Conversions between Java (Big Endian) and Little Endian byte order.
 *
 * <p>Java is always Big Endian, so the conversions to Little Endian and back
 * are the same operation: reversing the bytes of each word. Both names are
 * provided for every type so that the intent is visible at the call site, and
 * they are guaranteed to be exact inverses of each other:
 * <pre>
 *     littleToShort(shortToLittle(x)) == x
 * </pre>
 *
 * <p>Every type comes in three forms:
 * <ul>
 *     <li>a single value &mdash; {@code shortToLittle(short)}</li>
 *     <li>an array of values &mdash; {@code shortToLittle(short[], short[])}</li>
 *     <li>an array of bytes &mdash; {@code shortToLittle(byte[], byte[])}</li>
 * </ul>
 *
 * <p>The {@code byte[]} forms reverse each word of {@code src} in place inside
 * {@code dst}, so {@code src.length} must be a multiple of the word size
 * (2, 4 or 8 bytes). The array forms require {@code dst.length >= src.length}
 * and leave any remaining {@code dst} elements untouched. {@code src} and
 * {@code dst} may be the same array, in which case the swap happens in place.
 *
 * <p>All methods are implemented on top of {@link Short#reverseBytes},
 * {@link Integer#reverseBytes} and {@link Long#reverseBytes}, which HotSpot
 * compiles to single {@code XCHG} / {@code BSWAP} instructions on x86.
 *
 * @author franci
 * @see #NATIVE_ORDER
 */
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
     * <p>Example: {@code 0x1234} → {@code 0x3412}
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
     * Identical to {@link #shortToLittle(short[], short[])}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static short[] littleToShort(short[] src, short[] dst)
    {
        return shortToLittle(src, dst);
    }

    /**
     * Reinterprets {@code src} as a sequence of Big Endian {@code short} words
     * and writes each word byte-swapped (Little Endian) into {@code dst}.
     *
     * <p>Example: {@code [0x12, 0x34]} → {@code [0x34, 0x12]}
     *
     * <p>{@code src} and {@code dst} may be the same array (in-place swap).
     *
     * @param src source byte array; length must be a multiple of 2
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] shortToLittle(byte[] src, byte[] dst)
    {
        return swap(src, dst, Short.BYTES);
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
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] littleToShort(byte[] src, byte[] dst)
    {
        return swap(src, dst, Short.BYTES);
    }

    // =========================================================================
    // char  (2 bytes — same mechanics as short)
    // =========================================================================

    /**
     * Converts a {@code char} from Java (Big Endian) to Little Endian.
     *
     * <p>{@link Character} has no {@code reverseBytes}, so the value goes
     * through {@link Short#reverseBytes}; the narrowing and widening casts are
     * lossless because both types are exactly 16 bits.
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
     * Identical to {@link #charToLittle(char)} because byte-swap is self-inverse.
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
     * {@code src} and {@code dst} may be the same array (in-place swap).
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
     * Identical to {@link #charToLittle(char[], char[])}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static char[] littleToChar(char[] src, char[] dst)
    {
        return charToLittle(src, dst);
    }

    /**
     * Reinterprets {@code src} as a sequence of Big Endian {@code char} values
     * and writes each value byte-swapped (Little Endian) into {@code dst}.
     * Identical to {@link #shortToLittle(byte[], byte[])} because {@code char}
     * and {@code short} share the same 2-byte layout.
     *
     * @param src source byte array; length must be a multiple of 2
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] charToLittle(byte[] src, byte[] dst)
    {
        return swap(src, dst, Short.BYTES);
    }

    /**
     * Reinterprets {@code src} as a sequence of Little Endian {@code char} values
     * and writes each value byte-swapped (Big Endian / Java order) into {@code dst}.
     * Identical to {@link #charToLittle(byte[], byte[])}.
     *
     * @param src source byte array; length must be a multiple of 2
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] littleToChar(byte[] src, byte[] dst)
    {
        return swap(src, dst, Short.BYTES);
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
     * {@code src} and {@code dst} may be the same array (in-place swap).
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static int[] intToLittle(int[] src, int[] dst)
    {
        checkArrayArgs(src, dst);
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = Integer.reverseBytes(src[i]);
        }
        return dst;
    }

    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     * Identical to {@link #intToLittle(int[], int[])}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static int[] littleToInt(int[] src, int[] dst)
    {
        return intToLittle(src, dst);
    }

    /**
     * Reinterprets {@code src} as a sequence of Big Endian {@code int} words
     * and writes each word byte-swapped (Little Endian) into {@code dst}.
     *
     * <p>Example for a single word:
     * {@code [0x12, 0x34, 0x56, 0x78]} → {@code [0x78, 0x56, 0x34, 0x12]}
     *
     * <p>{@code src} and {@code dst} may be the same array (in-place swap).
     *
     * @param src source byte array; length must be a multiple of 4
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] intToLittle(byte[] src, byte[] dst)
    {
        return swap(src, dst, Integer.BYTES);
    }

    /**
     * Reinterprets {@code src} as a sequence of Little Endian {@code int} words
     * and writes each word byte-swapped (Big Endian / Java order) into {@code dst}.
     * Identical to {@link #intToLittle(byte[], byte[])}.
     *
     * @param src source byte array; length must be a multiple of 4
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] littleToInt(byte[] src, byte[] dst)
    {
        return swap(src, dst, Integer.BYTES);
    }

    // =========================================================================
    // long  (8 bytes — BSWAP intrinsic on 64-bit)
    // =========================================================================

    /**
     * Converts a {@code long} from Java (Big Endian) to Little Endian by
     * reversing all eight bytes.
     *
     * <p>Example: {@code 0x0102030405060708L} → {@code 0x0807060504030201L}
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
     * Identical to {@link #longToLittle(long)} because byte-swap is self-inverse.
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
     * {@code src} and {@code dst} may be the same array (in-place swap).
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
     * Identical to {@link #longToLittle(long[], long[])}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static long[] littleToLong(long[] src, long[] dst)
    {
        return longToLittle(src, dst);
    }

    /**
     * Reinterprets {@code src} as a sequence of Big Endian {@code long} words
     * and writes each word byte-swapped (Little Endian) into {@code dst}.
     * <p>{@code src} and {@code dst} may be the same array (in-place swap).
     *
     * @param src source byte array; length must be a multiple of 8
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] longToLittle(byte[] src, byte[] dst)
    {
        return swap(src, dst, Long.BYTES);
    }

    /**
     * Reinterprets {@code src} as a sequence of Little Endian {@code long} words
     * and writes each word byte-swapped (Big Endian / Java order) into {@code dst}.
     * Identical to {@link #longToLittle(byte[], byte[])}.
     *
     * @param src source byte array; length must be a multiple of 8
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] littleToLong(byte[] src, byte[] dst)
    {
        return swap(src, dst, Long.BYTES);
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
        return reverseFloatBits(value);
    }

    /**
     * Converts a {@code float} from Little Endian to Java (Big Endian) byte order.
     * Identical to {@link #floatToLittle(float)} because byte-swap is self-inverse.
     *
     * @param value value in Little Endian byte order
     * @return same bit pattern in Java/Big Endian byte order
     */
    public static float littleToFloat(float value)
    {
        return reverseFloatBits(value);
    }

    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     * {@code src} and {@code dst} may be the same array (in-place swap).
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
            dst[i] = reverseFloatBits(src[i]);
        }
        return dst;
    }

    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     * Identical to {@link #floatToLittle(float[], float[])}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static float[] littleToFloat(float[] src, float[] dst)
    {
        return floatToLittle(src, dst);
    }

    /**
     * Reinterprets {@code src} as a sequence of Big Endian IEEE 754 {@code float}
     * bit patterns and writes each pattern byte-swapped (Little Endian) into
     * {@code dst}.  The byte-level operation is identical to
     * {@link #intToLittle(byte[], byte[])}.
     * <p>{@code src} and {@code dst} may be the same array (in-place swap).
     *
     * @param src source byte array; length must be a multiple of 4
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] floatToLittle(byte[] src, byte[] dst)
    {
        return swap(src, dst, Integer.BYTES);
    }

    /**
     * Reinterprets {@code src} as a sequence of Little Endian IEEE 754
     * {@code float} bit patterns and writes each pattern byte-swapped
     * (Big Endian / Java order) into {@code dst}.
     * Identical to {@link #floatToLittle(byte[], byte[])}.
     *
     * @param src source byte array; length must be a multiple of 4
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] littleToFloat(byte[] src, byte[] dst)
    {
        return swap(src, dst, Integer.BYTES);
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
        return reverseDoubleBits(value);
    }

    /**
     * Converts a {@code double} from Little Endian to Java (Big Endian) byte order.
     * Identical to {@link #doubleToLittle(double)} because byte-swap is self-inverse.
     *
     * @param value value in Little Endian byte order
     * @return same bit pattern in Java/Big Endian byte order
     */
    public static double littleToDouble(double value)
    {
        return reverseDoubleBits(value);
    }

    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     * {@code src} and {@code dst} may be the same array (in-place swap).
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static double[] doubleToLittle(double[] src, double[] dst)
    {
        checkArrayArgs(src, dst);
        for (int i = 0; i < src.length; i++)
        {
            dst[i] = reverseDoubleBits(src[i]);
        }
        return dst;
    }

    /**
     * Byte-swaps each element of {@code src} into {@code dst}.
     * Identical to {@link #doubleToLittle(double[], double[])}.
     *
     * @param src source array
     * @param dst destination array (length ≥ {@code src.length})
     * @return {@code dst}
     */
    public static double[] littleToDouble(double[] src, double[] dst)
    {
        return doubleToLittle(src, dst);
    }

    /**
     * Reinterprets {@code src} as a sequence of Big Endian IEEE 754
     * {@code double} bit patterns and writes each pattern byte-swapped
     * (Little Endian) into {@code dst}.  The byte-level operation is identical
     * to {@link #longToLittle(byte[], byte[])}.
     * <p>{@code src} and {@code dst} may be the same array (in-place swap).
     *
     * @param src source byte array; length must be a multiple of 8
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] doubleToLittle(byte[] src, byte[] dst)
    {
        return swap(src, dst, Long.BYTES);
    }

    /**
     * Reinterprets {@code src} as a sequence of Little Endian IEEE 754
     * {@code double} bit patterns and writes each pattern byte-swapped
     * (Big Endian / Java order) into {@code dst}.
     * Identical to {@link #doubleToLittle(byte[], byte[])}.
     *
     * @param src source byte array; length must be a multiple of 8
     * @param dst destination byte array; length ≥ {@code src.length}
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    public static byte[] littleToDouble(byte[] src, byte[] dst)
    {
        return swap(src, dst, Long.BYTES);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Reverses the bytes of each {@code wordSize}-byte word in {@code src},
     * writing into {@code dst}.  This is the single byte-level implementation
     * behind every {@code byte[]} method; the public methods pass
     * {@code Short.BYTES}, {@code Integer.BYTES} or {@code Long.BYTES} so the
     * {@code switch} folds to a single unrolled branch at JIT compile time.
     *
     * <p>Each word is fully read into locals before any write, so
     * {@code src} and {@code dst} may be the same array.
     *
     * @param src      source byte array; length must be a multiple of {@code wordSize}
     * @param dst      destination byte array; length ≥ {@code src.length}
     * @param wordSize size of each logical word in bytes: 2, 4 or 8
     * @return {@code dst}
     * @throws IllegalArgumentException if any argument is invalid
     */
    private static byte[] swap(byte[] src, byte[] dst, int wordSize)
    {
        switch (wordSize)
        {
            case Short.BYTES:
            {
                checkByteArrayArgs(src, dst, Short.BYTES);
                for (int i = 0; i < src.length; i += Short.BYTES)
                {
                    byte b0 = src[i    ];
                    byte b1 = src[i + 1];
                    dst[i    ] = b1;
                    dst[i + 1] = b0;
                }
                return dst;
            }
            case Integer.BYTES:
            {
                checkByteArrayArgs(src, dst, Integer.BYTES);
                for (int i = 0; i < src.length; i += Integer.BYTES)
                {
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
            case Long.BYTES:
            {
                checkByteArrayArgs(src, dst, Long.BYTES);
                for (int i = 0; i < src.length; i += Long.BYTES)
                {
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
            default:
                throw new IllegalArgumentException("wordSize must be 2, 4 or 8, was " + wordSize);
        }
    }

    /** Bit-swaps a {@code float} without canonicalizing NaN payloads. */
    private static float reverseFloatBits(float value)
    {
        return Float.intBitsToFloat(Integer.reverseBytes(Float.floatToRawIntBits(value)));
    }

    /** Bit-swaps a {@code double} without canonicalizing NaN payloads. */
    private static double reverseDoubleBits(double value)
    {
        return Double.longBitsToDouble(Long.reverseBytes(Double.doubleToRawLongBits(value)));
    }

    // Typed length guards — called before the loop in every array overload.
    private static void checkArrayArgs(short[]  src, short[]  dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(char[]   src, char[]   dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(int[]    src, int[]    dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(long[]   src, long[]   dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(float[]  src, float[]  dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }
    private static void checkArrayArgs(double[] src, double[] dst) { checkNotNull(src, dst); checkLen(src.length, dst.length); }

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
     * @param wordSize size of each logical word in bytes (2, 4 or 8)
     * @throws IllegalArgumentException if any constraint is violated
     */
    private static void checkByteArrayArgs(byte[] src, byte[] dst, int wordSize)
    {
        checkNotNull(src, dst);
        checkLen(src.length, dst.length);
        if (src.length % wordSize != 0)
        {
            throw new IllegalArgumentException("src length (" + src.length + ") must be a multiple of " + wordSize);
        }
    }
}
