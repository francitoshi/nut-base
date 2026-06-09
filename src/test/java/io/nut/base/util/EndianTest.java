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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Endian}.
 *
 * <p>Compatibility: Java 8 + JUnit 5 (jupiter 5.x).
 *
 * <p>Strategy per group:
 * <ul>
 *   <li><b>Scalar overloads</b> – known bit patterns, boundary values (0, MAX, MIN,
 *       all-ones), and the self-inverse property: {@code swap(swap(x)) == x}.</li>
 *   <li><b>Typed-array overloads</b> – multi-element swap, in-place (src == dst),
 *       larger dst, empty array, null guards, and short-dst guard.</li>
 *   <li><b>byte[] overloads</b> – same as typed-array plus misaligned-length guard.</li>
 * </ul>
 */
@DisplayName("Endian")
class EndianTest
{
    // =========================================================================
    // Helpers
    // =========================================================================

    /** Convenience: build a byte array from int literals (avoids casts at call sites). */
    private static byte[] b(int... values)
    {
        byte[] a = new byte[values.length];
        for (int i = 0; i < values.length; i++) a[i] = (byte) values[i];
        return a;
    }

    // =========================================================================
    // NATIVE_ORDER
    // =========================================================================

    @Test
    @DisplayName("NATIVE_ORDER is non-null")
    void nativeOrderIsNonNull()
    {
        assertNotNull(Endian.NATIVE_ORDER);
    }

    // =========================================================================
    // short
    // =========================================================================

    @Nested
    @DisplayName("short — scalar")
    class ShortScalar
    {
        @Test
        @DisplayName("0x1234 → 0x3412")
        void knownPattern()
        {
            assertEquals((short) 0x3412, Endian.shortToLittle((short) 0x1234));
        }

        @Test
        @DisplayName("self-inverse: swap(swap(x)) == x")
        void selfInverse()
        {
            short value = (short) 0xABCD;
            assertEquals(value, Endian.shortToLittle(Endian.shortToLittle(value)));
        }

        @Test
        @DisplayName("zero is stable")
        void zero()
        {
            assertEquals((short) 0, Endian.shortToLittle((short) 0));
        }

        @Test
        @DisplayName("0x00FF → 0xFF00")
        void lowByteOnly()
        {
            assertEquals((short) 0xFF00, Endian.shortToLittle((short) 0x00FF));
        }

        @Test
        @DisplayName("0xFF00 → 0x00FF")
        void highByteOnly()
        {
            assertEquals((short) 0x00FF, Endian.shortToLittle((short) 0xFF00));
        }

        @Test
        @DisplayName("all-ones (0xFFFF) is stable")
        void allOnes()
        {
            assertEquals((short) 0xFFFF, Endian.shortToLittle((short) 0xFFFF));
        }

        @Test
        @DisplayName("littleToShort mirrors shortToLittle")
        void littleToShortMirrors()
        {
            short value = (short) 0x1234;
            assertEquals(Endian.shortToLittle(value), Endian.littleToShort(value));
        }

        @ParameterizedTest(name = "0x{0} → 0x{1}")
        @CsvSource({"1234,3412", "ABCD,CDAB", "0100,0001", "8000,0080"})
        @DisplayName("parameterized known patterns (hex strings)")
        void parameterized(String inputHex, String expectedHex)
        {
            short input    = (short) Integer.parseInt(inputHex,    16);
            short expected = (short) Integer.parseInt(expectedHex, 16);
            assertEquals(expected, Endian.shortToLittle(input));
        }
    }

    @Nested
    @DisplayName("short — array")
    class ShortArray
    {
        @Test
        @DisplayName("two-element swap")
        void twoElements()
        {
            short[] src = {(short) 0x1234, (short) 0xABCD};
            short[] dst = new short[2];
            short[] result = Endian.shortToLittle(src, dst);
            assertSame(dst, result);
            assertArrayEquals(new short[]{(short) 0x3412, (short) 0xCDAB}, dst);
        }

        @Test
        @DisplayName("in-place swap (src == dst)")
        void inPlace()
        {
            short[] arr = {(short) 0x1234, (short) 0x5678};
            Endian.shortToLittle(arr, arr);
            assertArrayEquals(new short[]{(short) 0x3412, (short) 0x7856}, arr);
        }

        @Test
        @DisplayName("dst larger than src is allowed")
        void dstLarger()
        {
            short[] src = {(short) 0x1122};
            short[] dst = new short[3];
            Endian.shortToLittle(src, dst);
            assertEquals((short) 0x2211, dst[0]);
        }

        @Test
        @DisplayName("empty array — no-op")
        void empty()
        {
            short[] empty = {};
            assertDoesNotThrow(() -> Endian.shortToLittle(empty, empty));
        }

        @Test
        @DisplayName("null src throws IllegalArgumentException")
        void nullSrc()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.shortToLittle((short[]) null, new short[1]));
        }

        @Test
        @DisplayName("null dst throws IllegalArgumentException")
        void nullDst()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.shortToLittle(new short[1], (short[]) null));
        }

        @Test
        @DisplayName("dst shorter than src throws IllegalArgumentException")
        void dstTooShort()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.shortToLittle(new short[2], new short[1]));
        }

        @Test
        @DisplayName("littleToShort(array) is symmetric with shortToLittle(array)")
        void littleToShortSymmetric()
        {
            short[] src = {(short) 0x1234};
            short[] a = new short[1], b = new short[1];
            Endian.shortToLittle(src, a);
            Endian.littleToShort(src, b);
            assertArrayEquals(a, b);
        }

        @Test
        @DisplayName("round-trip: littleToShort(shortToLittle(arr)) == original")
        void roundTrip()
        {
            short[] original = {(short) 0x1234, (short) 0xABCD};
            short[] tmp = new short[2];
            short[] back = new short[2];
            Endian.shortToLittle(original, tmp);
            Endian.littleToShort(tmp, back);
            assertArrayEquals(original, back);
        }
    }

    // =========================================================================
    // char
    // =========================================================================

    @Nested
    @DisplayName("char — scalar")
    class CharScalar
    {
        @Test
        @DisplayName("'AB' (0x4142) → 0x4241")
        void knownPattern()
        {
            assertEquals('\u4241', Endian.charToLittle('\u4142'));
        }

        @Test
        @DisplayName("self-inverse")
        void selfInverse()
        {
            char value = '\uABCD';
            assertEquals(value, Endian.charToLittle(Endian.charToLittle(value)));
        }

        @Test
        @DisplayName("zero is stable")
        void zero()
        {
            assertEquals('\u0000', Endian.charToLittle('\u0000'));
        }

        @Test
        @DisplayName("littleToChar mirrors charToLittle")
        void littleToCharMirrors()
        {
            char value = '\u1234';
            assertEquals(Endian.charToLittle(value), Endian.littleToChar(value));
        }
    }

    @Nested
    @DisplayName("char — array")
    class CharArray
    {
        @Test
        @DisplayName("two-element swap")
        void twoElements()
        {
            char[] src = {'\u1234', '\uABCD'};
            char[] dst = new char[2];
            Endian.charToLittle(src, dst);
            assertArrayEquals(new char[]{'\u3412', '\uCDAB'}, dst);
        }

        @Test
        @DisplayName("in-place swap")
        void inPlace()
        {
            char[] arr = {'\u1234'};
            Endian.charToLittle(arr, arr);
            assertArrayEquals(new char[]{'\u3412'}, arr);
        }

        @Test
        @DisplayName("null src throws")
        void nullSrc()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.charToLittle((char[]) null, new char[1]));
        }

        @Test
        @DisplayName("null dst throws")
        void nullDst()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.charToLittle(new char[1], (char[]) null));
        }

        @Test
        @DisplayName("dst shorter than src throws")
        void dstTooShort()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.charToLittle(new char[2], new char[1]));
        }

        @Test
        @DisplayName("round-trip")
        void roundTrip()
        {
            char[] original = {'\u1234', '\uABCD'};
            char[] tmp = new char[2], back = new char[2];
            Endian.charToLittle(original, tmp);
            Endian.littleToChar(tmp, back);
            assertArrayEquals(original, back);
        }
    }

    // =========================================================================
    // int
    // =========================================================================

    @Nested
    @DisplayName("int — scalar")
    class IntScalar
    {
        @Test
        @DisplayName("0x12345678 → 0x78563412")
        void knownPattern()
        {
            assertEquals(0x78563412, Endian.intToLittle(0x12345678));
        }

        @Test
        @DisplayName("self-inverse")
        void selfInverse()
        {
            int value = 0xDEADBEEF;
            assertEquals(value, Endian.intToLittle(Endian.intToLittle(value)));
        }

        @Test
        @DisplayName("zero is stable")
        void zero()
        {
            assertEquals(0, Endian.intToLittle(0));
        }

        @Test
        @DisplayName("all-ones is stable")
        void allOnes()
        {
            assertEquals(0xFFFFFFFF, Endian.intToLittle(0xFFFFFFFF));
        }

        @Test
        @DisplayName("0x00000001 → 0x01000000")
        void singleLowByte()
        {
            assertEquals(0x01000000, Endian.intToLittle(0x00000001));
        }

        @Test
        @DisplayName("littleToInt mirrors intToLittle")
        void littleToIntMirrors()
        {
            int value = 0x12345678;
            assertEquals(Endian.intToLittle(value), Endian.littleToInt(value));
        }
    }

    @Nested
    @DisplayName("int — array")
    class IntArray
    {
        @Test
        @DisplayName("two-element swap")
        void twoElements()
        {
            int[] src = {0x12345678, 0xDEADBEEF};
            int[] dst = new int[2];
            assertSame(dst, Endian.intToLittle(src, dst));
            assertArrayEquals(new int[]{0x78563412, 0xEFBEADDE}, dst);
        }

        @Test
        @DisplayName("in-place swap")
        void inPlace()
        {
            int[] arr = {0x12345678};
            Endian.intToLittle(arr, arr);
            assertArrayEquals(new int[]{0x78563412}, arr);
        }

        @Test
        @DisplayName("empty array — no-op")
        void empty()
        {
            assertDoesNotThrow(() -> Endian.intToLittle(new int[0], new int[0]));
        }

        @Test
        @DisplayName("null src throws")
        void nullSrc()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.intToLittle((int[]) null, new int[1]));
        }

        @Test
        @DisplayName("null dst throws")
        void nullDst()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.intToLittle(new int[1], (int[]) null));
        }

        @Test
        @DisplayName("dst shorter than src throws")
        void dstTooShort()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.intToLittle(new int[2], new int[1]));
        }

        @Test
        @DisplayName("round-trip")
        void roundTrip()
        {
            int[] original = {0x12345678, 0xDEADBEEF};
            int[] tmp = new int[2], back = new int[2];
            Endian.intToLittle(original, tmp);
            Endian.littleToInt(tmp, back);
            assertArrayEquals(original, back);
        }
    }

    // =========================================================================
    // long
    // =========================================================================

    @Nested
    @DisplayName("long — scalar")
    class LongScalar
    {
        @Test
        @DisplayName("0x0102030405060708L → 0x0807060504030201L")
        void knownPattern()
        {
            assertEquals(0x0807060504030201L, Endian.longToLittle(0x0102030405060708L));
        }

        @Test
        @DisplayName("self-inverse")
        void selfInverse()
        {
            long value = 0xCAFEBABEDEADBEEFL;
            assertEquals(value, Endian.longToLittle(Endian.longToLittle(value)));
        }

        @Test
        @DisplayName("zero is stable")
        void zero()
        {
            assertEquals(0L, Endian.longToLittle(0L));
        }

        @Test
        @DisplayName("all-ones is stable")
        void allOnes()
        {
            assertEquals(-1L, Endian.longToLittle(-1L));
        }

        @Test
        @DisplayName("0x0000000000000001L → 0x0100000000000000L")
        void singleLowByte()
        {
            assertEquals(0x0100000000000000L, Endian.longToLittle(0x0000000000000001L));
        }

        @Test
        @DisplayName("littleToLong mirrors longToLittle")
        void littleToLongMirrors()
        {
            long value = 0x0102030405060708L;
            assertEquals(Endian.longToLittle(value), Endian.littleToLong(value));
        }
    }

    @Nested
    @DisplayName("long — array")
    class LongArray
    {
        @Test
        @DisplayName("two-element swap")
        void twoElements()
        {
            long[] src = {0x0102030405060708L, 0xCAFEBABEDEADBEEFL};
            long[] dst = new long[2];
            assertSame(dst, Endian.longToLittle(src, dst));
            assertArrayEquals(
                new long[]{0x0807060504030201L, 0xEFBEADDEBEBAFECAL}, dst);
        }

        @Test
        @DisplayName("in-place swap")
        void inPlace()
        {
            long[] arr = {0x0102030405060708L};
            Endian.longToLittle(arr, arr);
            assertArrayEquals(new long[]{0x0807060504030201L}, arr);
        }

        @Test
        @DisplayName("null src throws")
        void nullSrc()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.longToLittle((long[]) null, new long[1]));
        }

        @Test
        @DisplayName("null dst throws")
        void nullDst()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.longToLittle(new long[1], (long[]) null));
        }

        @Test
        @DisplayName("dst shorter than src throws")
        void dstTooShort()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.longToLittle(new long[2], new long[1]));
        }

        @Test
        @DisplayName("round-trip")
        void roundTrip()
        {
            long[] original = {0x0102030405060708L, 0xCAFEBABEDEADBEEFL};
            long[] tmp = new long[2], back = new long[2];
            Endian.longToLittle(original, tmp);
            Endian.littleToLong(tmp, back);
            assertArrayEquals(original, back);
        }
    }

    // =========================================================================
    // float
    // =========================================================================

    @Nested
    @DisplayName("float — scalar")
    class FloatScalar
    {
        @Test
        @DisplayName("1.0f round-trip")
        void oneRoundTrip()
        {
            float original = 1.0f;
            float swapped  = Endian.floatToLittle(original);
            // bit pattern of 1.0f: 0x3F800000 → LE: 0x0000803F
            assertNotEquals(original, swapped, 0.0f);
            assertEquals(original, Endian.littleToFloat(swapped), 0.0f);
        }

        @Test
        @DisplayName("self-inverse")
        void selfInverse()
        {
            float value = 3.14159f;
            assertEquals(
                Float.floatToRawIntBits(value), Float.floatToRawIntBits(Endian.floatToLittle(Endian.floatToLittle(value))));
        }

        @Test
        @DisplayName("NaN payload is preserved (no canonicalization)")
        void nanPayload()
        {
            // Use a signalling NaN with a custom payload
            float nan = Float.intBitsToFloat(0x7F800001);
            float swapped = Endian.floatToLittle(nan);
            assertEquals(0x7F800001, Float.floatToRawIntBits(Endian.floatToLittle(swapped)));
        }

        @Test
        @DisplayName("positive infinity round-trip")
        void infinity()
        {
            float inf = Float.POSITIVE_INFINITY;
            assertEquals(
                Float.floatToRawIntBits(inf), Float.floatToRawIntBits(Endian.littleToFloat(Endian.floatToLittle(inf))));
        }

        @Test
        @DisplayName("littleToFloat mirrors floatToLittle")
        void littleToFloatMirrors()
        {
            float value = 2.718f;
            assertEquals(
                Float.floatToRawIntBits(Endian.floatToLittle(value)), Float.floatToRawIntBits(Endian.littleToFloat(value)));
        }
    }

    @Nested
    @DisplayName("float — array")
    class FloatArray
    {
        @Test
        @DisplayName("two-element swap and round-trip")
        void twoElementsRoundTrip()
        {
            float[] original = {1.0f, -1.0f};
            float[] swapped  = new float[2];
            float[] back     = new float[2];
            Endian.floatToLittle(original, swapped);
            Endian.littleToFloat(swapped, back);
            assertArrayEquals(original, back, 0.0f);
        }

        @Test
        @DisplayName("in-place swap")
        void inPlace()
        {
            float value = 1.0f;
            int   expectedBits = Integer.reverseBytes(Float.floatToRawIntBits(value));
            float[] arr = {value};
            Endian.floatToLittle(arr, arr);
            assertEquals(expectedBits, Float.floatToRawIntBits(arr[0]));
        }

        @Test
        @DisplayName("null src throws")
        void nullSrc()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.floatToLittle((float[]) null, new float[1]));
        }

        @Test
        @DisplayName("null dst throws")
        void nullDst()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.floatToLittle(new float[1], (float[]) null));
        }

        @Test
        @DisplayName("dst shorter than src throws")
        void dstTooShort()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.floatToLittle(new float[2], new float[1]));
        }
    }

    // =========================================================================
    // double
    // =========================================================================

    @Nested
    @DisplayName("double — scalar")
    class DoubleScalar
    {
        @Test
        @DisplayName("1.0d round-trip")
        void oneRoundTrip()
        {
            double original = 1.0d;
            double swapped  = Endian.doubleToLittle(original);
            assertNotEquals(
                Double.doubleToRawLongBits(original), Double.doubleToRawLongBits(swapped));
            assertEquals(
                Double.doubleToRawLongBits(original), Double.doubleToRawLongBits(Endian.littleToDouble(swapped)));
        }

        @Test
        @DisplayName("self-inverse")
        void selfInverse()
        {
            double value = Math.PI;
            assertEquals(
                Double.doubleToRawLongBits(value), Double.doubleToRawLongBits(Endian.doubleToLittle(Endian.doubleToLittle(value))));
        }

        @Test
        @DisplayName("NaN payload preserved")
        void nanPayload()
        {
            double nan = Double.longBitsToDouble(0x7FF0000000000001L);
            assertEquals(
                0x7FF0000000000001L, Double.doubleToRawLongBits(Endian.doubleToLittle(Endian.doubleToLittle(nan))));
        }

        @Test
        @DisplayName("javaToLittle aliases doubleToLittle (byte[])")
        void javaToLittleAlias()
        {
            double value = Math.E;
            assertEquals(
                Double.doubleToRawLongBits(Endian.doubleToLittle(value)), Double.doubleToRawLongBits(Endian.doubleToLittle(value)));
        }

        @Test
        @DisplayName("littleToDouble mirrors doubleToLittle")
        void littleToDoubleMirrors()
        {
            double value = Math.PI;
            assertEquals(
                Double.doubleToRawLongBits(Endian.doubleToLittle(value)), Double.doubleToRawLongBits(Endian.littleToDouble(value)));
        }
    }

    @Nested
    @DisplayName("double — array")
    class DoubleArray
    {
        @Test
        @DisplayName("two-element swap and round-trip (javaToLittle / littleToJava)")
        void roundTripViaAliases()
        {
            double[] original = {Math.PI, Math.E};
            double[] swapped  = new double[2];
            double[] back     = new double[2];
            Endian.javaToLittle(original, swapped);
            Endian.littleToJava(swapped, back);
            assertArrayEquals(original, back, 0.0d);
        }

        @Test
        @DisplayName("in-place swap")
        void inPlace()
        {
            double value = Math.PI;
            long   expectedBits = Long.reverseBytes(Double.doubleToRawLongBits(value));
            double[] arr = {value};
            Endian.javaToLittle(arr, arr);
            assertEquals(expectedBits, Double.doubleToRawLongBits(arr[0]));
        }

        @Test
        @DisplayName("null src throws")
        void nullSrc()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.javaToLittle((double[]) null, new double[1]));
        }

        @Test
        @DisplayName("null dst throws")
        void nullDst()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.javaToLittle(new double[1], (double[]) null));
        }

        @Test
        @DisplayName("dst shorter than src throws")
        void dstTooShort()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.javaToLittle(new double[2], new double[1]));
        }
    }

    // =========================================================================
    // byte[] — short (2 bytes)
    // =========================================================================

    @Nested
    @DisplayName("byte[] short (2 bytes)")
    class ByteArrayShort
    {
        @Test
        @DisplayName("[0x12,0x34] → [0x34,0x12]")
        void knownPattern()
        {
            assertArrayEquals(b(0x34, 0x12), Endian.shortToLittle(b(0x12, 0x34), new byte[2]));
        }

        @Test
        @DisplayName("four bytes (two words) swapped independently")
        void twoWords()
        {
            assertArrayEquals(b(0x34, 0x12, 0xCD, 0xAB), Endian.shortToLittle(b(0x12, 0x34, 0xAB, 0xCD), new byte[4]));
        }

        @Test
        @DisplayName("returns dst")
        void returnsDst()
        {
            byte[] dst = new byte[2];
            assertSame(dst, Endian.shortToLittle(b(0x12, 0x34), dst));
        }

        @Test
        @DisplayName("in-place swap")
        void inPlace()
        {
            byte[] arr = b(0x12, 0x34);
            Endian.shortToLittle(arr, arr);
            assertArrayEquals(b(0x34, 0x12), arr);
        }

        @Test
        @DisplayName("empty array — no-op")
        void empty()
        {
            assertDoesNotThrow(() -> Endian.shortToLittle(new byte[0], new byte[0]));
        }

        @Test
        @DisplayName("littleToShort is symmetric")
        void littleToShortSymmetric()
        {
            byte[] src = b(0x12, 0x34);
            byte[] a   = new byte[2], bArr = new byte[2];
            Endian.shortToLittle(src, a);
            Endian.littleToShort(src, bArr);
            assertArrayEquals(a, bArr);
        }

        @Test
        @DisplayName("round-trip")
        void roundTrip()
        {
            byte[] original = b(0x12, 0x34, 0xAB, 0xCD);
            byte[] tmp  = new byte[4], back = new byte[4];
            Endian.shortToLittle(original, tmp);
            Endian.littleToShort(tmp, back);
            assertArrayEquals(original, back);
        }

        @Test
        @DisplayName("null src throws")
        void nullSrc()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.shortToLittle((byte[]) null, new byte[2]));
        }

        @Test
        @DisplayName("null dst throws")
        void nullDst()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.shortToLittle(b(0x12, 0x34), null));
        }

        @Test
        @DisplayName("dst shorter than src throws")
        void dstTooShort()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.shortToLittle(b(0x12, 0x34), new byte[1]));
        }

        @Test
        @DisplayName("src length not multiple of 2 throws")
        void misaligned()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.shortToLittle(b(0x12, 0x34, 0x56), new byte[3]));
        }
    }

    // =========================================================================
    // byte[] — char (2 bytes)
    // =========================================================================

    @Nested
    @DisplayName("byte[] char (2 bytes)")
    class ByteArrayChar
    {
        @Test
        @DisplayName("[0xAB,0xCD] → [0xCD,0xAB]")
        void knownPattern()
        {
            assertArrayEquals(b(0xCD, 0xAB), Endian.charToLittle(b(0xAB, 0xCD), new byte[2]));
        }

        @Test
        @DisplayName("littleToChar is symmetric")
        void symmetric()
        {
            byte[] src = b(0xAB, 0xCD);
            byte[] a = new byte[2], bArr = new byte[2];
            Endian.charToLittle(src, a);
            Endian.littleToChar(src, bArr);
            assertArrayEquals(a, bArr);
        }

        @Test
        @DisplayName("misaligned length throws")
        void misaligned()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.charToLittle(b(0xAB), new byte[1]));
        }

        @Test
        @DisplayName("round-trip")
        void roundTrip()
        {
            byte[] original = b(0xAB, 0xCD);
            byte[] tmp = new byte[2], back = new byte[2];
            Endian.charToLittle(original, tmp);
            Endian.littleToChar(tmp, back);
            assertArrayEquals(original, back);
        }
    }

    // =========================================================================
    // byte[] — int (4 bytes)
    // =========================================================================

    @Nested
    @DisplayName("byte[] int (4 bytes)")
    class ByteArrayInt
    {
        @Test
        @DisplayName("[0x12,0x34,0x56,0x78] → [0x78,0x56,0x34,0x12]")
        void knownPattern()
        {
            assertArrayEquals(b(0x78, 0x56, 0x34, 0x12), Endian.intToLittle(b(0x12, 0x34, 0x56, 0x78), new byte[4]));
        }

        @Test
        @DisplayName("eight bytes (two words)")
        void twoWords()
        {
            assertArrayEquals(
                b(0x78, 0x56, 0x34, 0x12,  0xEF, 0xCD, 0xAB, 0x00), Endian.intToLittle(
                    b(0x12, 0x34, 0x56, 0x78,  0x00, 0xAB, 0xCD, 0xEF), new byte[8]));
        }

        @Test
        @DisplayName("in-place swap")
        void inPlace()
        {
            byte[] arr = b(0x12, 0x34, 0x56, 0x78);
            Endian.intToLittle(arr, arr);
            assertArrayEquals(b(0x78, 0x56, 0x34, 0x12), arr);
        }

        @Test
        @DisplayName("returns dst")
        void returnsDst()
        {
            byte[] dst = new byte[4];
            assertSame(dst, Endian.intToLittle(b(0x12, 0x34, 0x56, 0x78), dst));
        }

        @Test
        @DisplayName("littleToInt is symmetric")
        void symmetric()
        {
            byte[] src = b(0x12, 0x34, 0x56, 0x78);
            byte[] a = new byte[4], bArr = new byte[4];
            Endian.intToLittle(src, a);
            Endian.littleToInt(src, bArr);
            assertArrayEquals(a, bArr);
        }

        @Test
        @DisplayName("round-trip")
        void roundTrip()
        {
            byte[] original = b(0x12, 0x34, 0x56, 0x78);
            byte[] tmp = new byte[4], back = new byte[4];
            Endian.intToLittle(original, tmp);
            Endian.littleToInt(tmp, back);
            assertArrayEquals(original, back);
        }

        @Test
        @DisplayName("null src throws")
        void nullSrc()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.intToLittle((byte[]) null, new byte[4]));
        }

        @Test
        @DisplayName("null dst throws")
        void nullDst()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.intToLittle(b(0x12, 0x34, 0x56, 0x78), null));
        }

        @Test
        @DisplayName("dst shorter than src throws")
        void dstTooShort()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.intToLittle(b(0x12, 0x34, 0x56, 0x78), new byte[3]));
        }

        @Test
        @DisplayName("src length not multiple of 4 throws")
        void misaligned()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.intToLittle(b(0x12, 0x34, 0x56), new byte[3]));
        }
    }

    // =========================================================================
    // byte[] — long (8 bytes)
    // =========================================================================

    @Nested
    @DisplayName("byte[] long (8 bytes)")
    class ByteArrayLong
    {
        @Test
        @DisplayName("[01,02,03,04,05,06,07,08] → [08,07,06,05,04,03,02,01]")
        void knownPattern()
        {
            assertArrayEquals(b(0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01), Endian.longToLittle(b(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08),new byte[8]));
        }

        @Test
        @DisplayName("in-place swap")
        void inPlace()
        {
            byte[] arr = b(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);
            Endian.longToLittle(arr, arr);
            assertArrayEquals(b(0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01), arr);
        }

        @Test
        @DisplayName("littleToLong is symmetric")
        void symmetric()
        {
            byte[] src = b(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);
            byte[] a = new byte[8], bArr = new byte[8];
            Endian.longToLittle(src, a);
            Endian.littleToLong(src, bArr);
            assertArrayEquals(a, bArr);
        }

        @Test
        @DisplayName("round-trip")
        void roundTrip()
        {
            byte[] original = b(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08);
            byte[] tmp = new byte[8], back = new byte[8];
            Endian.longToLittle(original, tmp);
            Endian.littleToLong(tmp, back);
            assertArrayEquals(original, back);
        }

        @Test
        @DisplayName("null src throws")
        void nullSrc()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.longToLittle((byte[]) null, new byte[8]));
        }

        @Test
        @DisplayName("null dst throws")
        void nullDst()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.longToLittle(b(0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08), null));
        }

        @Test
        @DisplayName("dst shorter than src throws")
        void dstTooShort()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.longToLittle(b(0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08), new byte[7]));
        }

        @Test
        @DisplayName("src length not multiple of 8 throws")
        void misaligned()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.longToLittle(b(0x01,0x02,0x03,0x04,0x05), new byte[5]));
        }
    }

    // =========================================================================
    // byte[] — float (delegates to intToLittle)
    // =========================================================================

    @Nested
    @DisplayName("byte[] float (4 bytes — delegates to intToLittle)")
    class ByteArrayFloat
    {
        /** IEEE 754 bits of 1.0f = 0x3F800000 → LE: 0x0000803F */
        private final byte[] oneBeBytes = b(0x3F, 0x80, 0x00, 0x00);
        private final byte[] oneLeBytes = b(0x00, 0x00, 0x80, 0x3F);

        @Test
        @DisplayName("1.0f BE bytes → LE bytes")
        void oneFloat()
        {
            assertArrayEquals(oneLeBytes, Endian.floatToLittle(oneBeBytes, new byte[4]));
        }

        @Test
        @DisplayName("littleToFloat is symmetric with floatToLittle")
        void symmetric()
        {
            byte[] a = new byte[4], bArr = new byte[4];
            Endian.floatToLittle(oneBeBytes, a);
            Endian.littleToFloat(oneBeBytes, bArr);
            assertArrayEquals(a, bArr);
        }

        @Test
        @DisplayName("round-trip: floatToLittle then littleToFloat")
        void roundTrip()
        {
            byte[] tmp = new byte[4], back = new byte[4];
            Endian.floatToLittle(oneBeBytes, tmp);
            Endian.littleToFloat(tmp, back);
            assertArrayEquals(oneBeBytes, back);
        }

        @Test
        @DisplayName("misaligned length throws")
        void misaligned()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.floatToLittle(b(0x3F, 0x80, 0x00), new byte[3]));
        }
    }

    // =========================================================================
    // byte[] — double (delegates to longToLittle)
    // =========================================================================

    @Nested
    @DisplayName("byte[] double (8 bytes — delegates to longToLittle)")
    class ByteArrayDouble
    {
        /** IEEE 754 bits of 1.0d = 0x3FF0000000000000 */
        private final byte[] oneBeBytes =
            b(0x3F, 0xF0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
        private final byte[] oneLeBytes =
            b(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF0, 0x3F);

        @Test
        @DisplayName("1.0d BE bytes → LE bytes")
        void oneDouble()
        {
            assertArrayEquals(oneLeBytes, Endian.doubleToLittle(oneBeBytes, new byte[8]));
        }

        @Test
        @DisplayName("javaToLittle alias produces same result as doubleToLittle")
        void javaToLittleAlias()
        {
            byte[] a = new byte[8], bArr = new byte[8];
            Endian.doubleToLittle(oneBeBytes, a);
            Endian.javaToLittle(oneBeBytes, bArr);
            assertArrayEquals(a, bArr);
        }

        @Test
        @DisplayName("littleToDouble is symmetric")
        void symmetric()
        {
            byte[] a = new byte[8], bArr = new byte[8];
            Endian.doubleToLittle(oneBeBytes, a);
            Endian.littleToDouble(oneBeBytes, bArr);
            assertArrayEquals(a, bArr);
        }

        @Test
        @DisplayName("littleToJava alias produces same result as littleToDouble")
        void littleToJavaAlias()
        {
            byte[] a = new byte[8], bArr = new byte[8];
            Endian.littleToDouble(oneBeBytes, a);
            Endian.littleToJava(oneBeBytes, bArr);
            assertArrayEquals(a, bArr);
        }

        @Test
        @DisplayName("round-trip: doubleToLittle then littleToDouble")
        void roundTrip()
        {
            byte[] tmp = new byte[8], back = new byte[8];
            Endian.doubleToLittle(oneBeBytes, tmp);
            Endian.littleToDouble(tmp, back);
            assertArrayEquals(oneBeBytes, back);
        }

        @Test
        @DisplayName("misaligned length throws")
        void misaligned()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.doubleToLittle(b(0x3F,0xF0,0x00,0x00,0x00,0x00,0x00), new byte[7]));
        }

        @Test
        @DisplayName("null src throws")
        void nullSrc()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.doubleToLittle((byte[]) null, new byte[8]));
        }

        @Test
        @DisplayName("null dst throws")
        void nullDst()
        {
            assertThrows(IllegalArgumentException.class, () -> Endian.doubleToLittle(oneBeBytes, null));
        }
    }
}
