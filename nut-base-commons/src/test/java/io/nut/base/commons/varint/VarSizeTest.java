/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.varint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link VarSize}.
 *
 * <p>The format is a base-128 (LEB128) variable length integer limited to 4 bytes,
 * where the byte sequence {@code FF FF FF FF} represents the null value {@code -1}.
 *
 * <p>Strategy per group:
 * <ul>
 *   <li><b>encode/write</b> – expected byte sequences for every supported size,
 *       the null marker, and the rejected ranges.</li>
 *   <li><b>decode/read</b> – decoding from arrays and streams, offsets, null marker,
 *       truncated and oversized inputs.</li>
 *   <li><b>sizeOf</b> – number of bytes for a value and for an encoded buffer.</li>
 *   <li><b>round trip</b> – encode/decode consistency over the supported range.</li>
 * </ul>
 */
@DisplayName("VarSize")
public class VarSizeTest
{

    private static byte[] b(int... values)
    {
        byte[] a = new byte[values.length];
        for (int i = 0; i < values.length; i++)
        {
            a[i] = (byte) values[i];
        }
        return a;
    }

    @Nested
    @DisplayName("encode")
    class Encode
    {

        @Test
        @DisplayName("0 is encoded as a single zero byte")
        void zeroIsASingleZeroByte()
        {
            assertArrayEquals(b(0x00), VarSize.encode(0));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 42, 126, 127})
        @DisplayName("values below 0x80 use one byte")
        void oneByteValues(int value)
        {
            byte[] encoded = VarSize.encode(value);
            assertEquals(1, encoded.length);
            assertEquals(value, encoded[0] & 0xFF);
            assertEquals(1, VarSize.sizeOf(value));
        }

        @ParameterizedTest
        @ValueSource(ints = {128, 255, 256, 16383})
        @DisplayName("values below 0x4000 use two bytes")
        void twoByteValues(int value)
        {
            byte[] encoded = VarSize.encode(value);
            assertEquals(2, encoded.length);
            assertEquals(0x80, encoded[0] & 0x80);
            assertEquals(0x00, encoded[1] & 0x80);
            assertEquals(2, VarSize.sizeOf(value));
        }

        @ParameterizedTest
        @ValueSource(ints = {16384, 65536, 2097151})
        @DisplayName("values below 0x200000 use three bytes")
        void threeByteValues(int value)
        {
            byte[] encoded = VarSize.encode(value);
            assertEquals(3, encoded.length);
            assertEquals(0x80, encoded[0] & 0x80);
            assertEquals(0x80, encoded[1] & 0x80);
            assertEquals(0x00, encoded[2] & 0x80);
            assertEquals(3, VarSize.sizeOf(value));
        }

        @ParameterizedTest
        @ValueSource(ints = {2097152, 16777216, 268435455})
        @DisplayName("values below 0x10000000 use four bytes")
        void fourByteValues(int value)
        {
            byte[] encoded = VarSize.encode(value);
            assertEquals(4, encoded.length);
            assertEquals(0x80, encoded[0] & 0x80);
            assertEquals(0x80, encoded[1] & 0x80);
            assertEquals(0x80, encoded[2] & 0x80);
            assertEquals(0x00, encoded[3] & 0x80);
            assertEquals(4, VarSize.sizeOf(value));
        }

        @Test
        @DisplayName("known values produce the expected byte sequences")
        void knownByteSequences()
        {
            assertArrayEquals(b(0x00), VarSize.encode(0));
            assertArrayEquals(b(0x01), VarSize.encode(1));
            assertArrayEquals(b(0x7F), VarSize.encode(127));
            assertArrayEquals(b(0x80, 0x01), VarSize.encode(128));
            assertArrayEquals(b(0xFF, 0x01), VarSize.encode(255));
            assertArrayEquals(b(0xFF, 0x7F), VarSize.encode(16383));
            assertArrayEquals(b(0x80, 0x80, 0x01), VarSize.encode(16384));
            assertArrayEquals(b(0xFF, 0xFF, 0x7F), VarSize.encode(2097151));
            assertArrayEquals(b(0x80, 0x80, 0x80, 0x01), VarSize.encode(2097152));
            assertArrayEquals(b(0xFF, 0xFF, 0xFF, 0x7F), VarSize.encode(268435455));
        }

        @Test
        @DisplayName("the null value is encoded as four 0xFF bytes")
        void nullValueIsFourFFBytes()
        {
            assertEquals(-1, VarSize.NULL_VALUE);
            byte[] encoded = VarSize.encode(VarSize.NULL_VALUE);
            assertArrayEquals(b(0xFF, 0xFF, 0xFF, 0xFF), encoded);
            assertEquals(4, VarSize.sizeOf(VarSize.NULL_VALUE));
        }

        @ParameterizedTest
        @ValueSource(ints = {-2, -100, Integer.MIN_VALUE})
        @DisplayName("negative values other than -1 are rejected")
        void negativeValuesAreRejected(int value)
        {
            assertThrows(IllegalArgumentException.class, () -> VarSize.encode(value));
            assertThrows(IllegalArgumentException.class, () -> VarSize.write(new ByteArrayOutputStream(), value));
        }

        @ParameterizedTest
        @ValueSource(ints = {0x20000000, 0x40000000, Integer.MAX_VALUE})
        @DisplayName("values that do not fit in 4 bytes are rejected")
        void tooLargeValuesAreRejected(int value)
        {
            assertThrows(IOException.class, () -> VarSize.write(new ByteArrayOutputStream(), value));
            assertThrows(AssertionError.class, () -> VarSize.encode(value));
        }

        @Test
        @DisplayName("write appends the encoding to the stream")
        void writeAppendsToTheStream() throws IOException
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            VarSize.write(out, 300);
            VarSize.write(out, VarSize.NULL_VALUE);
            assertArrayEquals(b(0xAC, 0x02, 0xFF, 0xFF, 0xFF, 0xFF), out.toByteArray());
        }
    }

    @Nested
    @DisplayName("decode")
    class Decode
    {

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 127, 128, 16383, 16384, 2097151, 2097152, 268435455})
        @DisplayName("decodes the values of every size")
        void decodesEverySize(int value)
        {
            assertEquals(value, VarSize.decode(VarSize.encode(value)));
        }

        @Test
        @DisplayName("decodes the null marker")
        void decodesNullMarker()
        {
            assertEquals(VarSize.NULL_VALUE, VarSize.decode(b(0xFF, 0xFF, 0xFF, 0xFF)));
        }

        @Test
        @DisplayName("decodes at a given offset")
        void decodesAtOffset()
        {
            byte[] data = b(0xAA, 0x80, 0x01, 0xBB);
            assertEquals(128, VarSize.decode(data, 1));
        }

        @Test
        @DisplayName("decodes at a given offset with a maximum length")
        void decodesAtOffsetWithLength()
        {
            byte[] data = b(0xAA, 0x80, 0x01, 0xBB);
            assertEquals(128, VarSize.decode(data, 1, 2));
        }

        @Test
        @DisplayName("decodes only the first value of a buffer")
        void decodesOnlyTheFirstValue()
        {
            byte[] data = b(0x7F, 0x42);
            assertEquals(127, VarSize.decode(data));
        }

        @Test
        @DisplayName("an empty array is rejected")
        void emptyArrayIsRejected()
        {
            assertThrows(IllegalArgumentException.class, () -> VarSize.decode(new byte[0]));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3})
        @DisplayName("a truncated encoding is rejected")
        void truncatedEncodingIsRejected(int length)
        {
            byte[] data = VarSize.encode(0x200000);
            byte[] truncated = new byte[length];
            System.arraycopy(data, 0, truncated, 0, length);
            assertThrows(IllegalArgumentException.class, () -> VarSize.decode(truncated));
        }

        @Test
        @DisplayName("an encoding with more than 4 bytes is rejected")
        void tooLargeEncodingIsRejected()
        {
            assertThrows(IllegalArgumentException.class, () -> VarSize.decode(b(0x80, 0x80, 0x80, 0x80, 0x81)));
        }
    }

    @Nested
    @DisplayName("read")
    class Read
    {

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 127, 128, 16383, 16384, 2097151, 2097152, 268435455})
        @DisplayName("reads the values of every size from a stream")
        void readsEverySize(int value) throws IOException
        {
            try (ByteArrayInputStream in = new ByteArrayInputStream(VarSize.encode(value)))
            {
                assertEquals(value, VarSize.read(in));
            }
        }

        @Test
        @DisplayName("reads the null marker from a stream")
        void readsNullMarker() throws IOException
        {
            try (ByteArrayInputStream in = new ByteArrayInputStream(b(0xFF, 0xFF, 0xFF, 0xFF)))
            {
                assertEquals(VarSize.NULL_VALUE, VarSize.read(in));
            }
        }

        @Test
        @DisplayName("reads consecutive values from the same stream")
        void readsConsecutiveValues() throws IOException
        {
            byte[] data = b(0x7F, 0x80, 0x01, 0xFF, 0xFF, 0xFF, 0xFF, 0x42);
            try (ByteArrayInputStream in = new ByteArrayInputStream(data))
            {
                assertEquals(127, VarSize.read(in));
                assertEquals(128, VarSize.read(in));
                assertEquals(VarSize.NULL_VALUE, VarSize.read(in));
                assertEquals(0x42, in.read());
            }
        }

        @Test
        @DisplayName("an empty stream is rejected")
        void emptyStreamIsRejected()
        {
            assertThrows(EOFException.class, () -> VarSize.read(new ByteArrayInputStream(new byte[0])));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3})
        @DisplayName("a truncated stream is rejected")
        void truncatedStreamIsRejected(int length) throws IOException
        {
            byte[] data = VarSize.encode(0x200000);
            byte[] truncated = new byte[length];
            System.arraycopy(data, 0, truncated, 0, length);
            assertThrows(EOFException.class, () -> VarSize.read(new ByteArrayInputStream(truncated)));
        }

        @Test
        @DisplayName("an encoding with more than 4 bytes is rejected")
        void tooLargeEncodingIsRejected()
        {
            assertThrows(IOException.class, () -> VarSize.read(new ByteArrayInputStream(b(0x80, 0x80, 0x80, 0x80, 0x81))));
        }
    }

    @Nested
    @DisplayName("sizeOf(int)")
    class SizeOfValue
    {

        @ParameterizedTest
        @CsvSource({
            "0, 1",
            "127, 1",
            "128, 2",
            "16383, 2",
            "16384, 3",
            "2097151, 3",
            "2097152, 4",
            "268435455, 4"
        })
        @DisplayName("returns the number of bytes required by the value")
        void returnsTheExpectedSize(int value, int expected)
        {
            assertEquals(expected, VarSize.sizeOf(value));
            assertEquals(expected, VarSize.encode(value).length);
        }

        @Test
        @DisplayName("the null value uses 4 bytes")
        void nullValueUsesFourBytes()
        {
            assertEquals(4, VarSize.sizeOf(VarSize.NULL_VALUE));
            assertEquals(4, VarSize.encode(VarSize.NULL_VALUE).length);
        }

        @ParameterizedTest
        @ValueSource(ints = {-2, -100, Integer.MIN_VALUE})
        @DisplayName("negative values other than -1 are rejected")
        void negativeValuesAreRejected(int value)
        {
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(value));
        }

        @ParameterizedTest
        @ValueSource(ints = {0x20000000, 0x40000000, Integer.MAX_VALUE})
        @DisplayName("values that do not fit in 4 bytes are rejected")
        void tooLargeValuesAreRejected(int value)
        {
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(value));
        }
    }

    @Nested
    @DisplayName("sizeOf(byte[], int)")
    class SizeOfBuffer
    {

        @ParameterizedTest
        @CsvSource({
            "0x7F, 1",
            "0x80, 2",
            "0x4000, 3",
            "0x200000, 4"
        })
        @DisplayName("returns the number of bytes occupied by the encoded value")
        void returnsTheExpectedSize(String firstByte, int expected)
        {
            byte[] data = VarSize.encode(Integer.decode(firstByte));
            assertEquals(expected, VarSize.sizeOf(data, 0));
        }

        @Test
        @DisplayName("returns 4 for the null marker")
        void returnsFourForTheNullMarker()
        {
            assertEquals(4, VarSize.sizeOf(b(0xFF, 0xFF, 0xFF, 0xFF), 0));
        }

        @Test
        @DisplayName("returns the size of the value at the given offset")
        void returnsTheSizeAtTheGivenOffset()
        {
            byte[] data = b(0xAA, 0x80, 0x01, 0xBB);
            assertEquals(2, VarSize.sizeOf(data, 1));
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 1, 2, 5})
        @DisplayName("an invalid offset is rejected")
        void invalidOffsetIsRejected(int offset)
        {
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(b(0x01), offset));
        }

        @Test
        @DisplayName("an empty buffer is rejected")
        void emptyBufferIsRejected()
        {
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(new byte[0], 0));
        }

        @Test
        @DisplayName("a truncated buffer is rejected")
        void truncatedBufferIsRejected()
        {
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(b(0x80), 0));
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(b(0x80, 0x80), 0));
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(b(0x80, 0x80, 0x80), 0));
        }

        @Test
        @DisplayName("a truncated null marker is rejected")
        void truncatedNullMarkerIsRejected()
        {
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(b(0xFF), 0));
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(b(0xFF, 0xFF), 0));
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(b(0xFF, 0xFF, 0xFF), 0));
        }

        @Test
        @DisplayName("a buffer starting with 0xFF that is not a null marker is rejected")
        void invalidNullMarkerIsRejected()
        {
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(b(0xFF, 0x01), 0));
        }

        @Test
        @DisplayName("an encoding with more than 4 bytes is rejected")
        void tooLargeEncodingIsRejected()
        {
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(b(0x80, 0x80, 0x80, 0x80, 0x01), 0));
        }
    }

    @Nested
    @DisplayName("toHex")
    class ToHex
    {

        @ParameterizedTest
        @CsvSource({
            "0, 00",
            "1, 01",
            "127, 7F",
            "128, 8001",
            "16383, FF7F",
            "16384, 808001",
            "2097151, FFFF7F",
            "2097152, 80808001",
            "268435455, FFFFFF7F"
        })
        @DisplayName("returns the uppercase hexadecimal representation")
        void returnsTheHexRepresentation(int value, String expected)
        {
            assertEquals(expected, VarSize.toHex(value));
        }

        @Test
        @DisplayName("the null value is FFFFFFFF")
        void nullValueIsFFFFFFFF()
        {
            assertEquals("FFFFFFFF", VarSize.toHex(VarSize.NULL_VALUE));
        }
    }

    static Stream<Arguments> values()
    {
        return Stream.of(
            Arguments.of(0),
            Arguments.of(1),
            Arguments.of(127),
            Arguments.of(128),
            Arguments.of(255),
            Arguments.of(256),
            Arguments.of(16383),
            Arguments.of(16384),
            Arguments.of(2097151),
            Arguments.of(2097152),
            Arguments.of(16777216),
            Arguments.of(268435455),
            Arguments.of(VarSize.NULL_VALUE));
    }

    @Nested
    @DisplayName("round trip")
    class RoundTrip
    {

        @ParameterizedTest
        @MethodSource("io.nut.base.commons.varint.VarSizeTest#values")
        @DisplayName("encode and decode return the original value")
        void encodeAndDecodeReturnTheOriginalValue(int value)
        {
            byte[] encoded = VarSize.encode(value);
            assertEquals(encoded.length, VarSize.sizeOf(value));
            assertEquals(value, VarSize.decode(encoded));
        }

        @ParameterizedTest
        @MethodSource("io.nut.base.commons.varint.VarSizeTest#values")
        @DisplayName("write and read return the original value")
        void writeAndReadReturnTheOriginalValue(int value) throws IOException
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            VarSize.write(out, value);
            try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray()))
            {
                assertEquals(value, VarSize.read(in));
            }
        }

        @ParameterizedTest
        @MethodSource("io.nut.base.commons.varint.VarSizeTest#values")
        @DisplayName("the number of bytes of the encoding is the size of the value")
        void theSizeOfTheValueMatchesTheEncoding(int value)
        {
            assertEquals(VarSize.sizeOf(value), VarSize.encode(value).length);
        }
    }

    @Nested
    @DisplayName("current limitations")
    class Limitations
    {

        @Test
        @DisplayName("values above 0x0FFFFFFF are encoded in 5 bytes")
        void valuesAbove28BitsUseFiveBytes()
        {
            assertEquals(5, VarSize.encode(0x10000000).length);
            assertEquals(5, VarSize.encode(0x1FFFFFFF).length);
        }

        @Test
        @DisplayName("sizeOf reports 4 bytes for values that are encoded in 5 bytes")
        void sizeOfDisagreesWithTheEncoding()
        {
            assertEquals(4, VarSize.sizeOf(0x10000000));
            assertEquals(5, VarSize.encode(0x10000000).length);
        }

        @Test
        @DisplayName("values above 0x0FFFFFFF are not decoded back to the original value")
        void valuesAbove28BitsAreNotRoundTrippable()
        {
            assertEquals(0, VarSize.decode(VarSize.encode(0x10000000)));
            assertEquals(VarSize.NULL_VALUE, VarSize.decode(VarSize.encode(0x1FFFFFFF)));
        }

        @Test
        @DisplayName("sizeOf rejects a valid 4 byte encoding starting with 0xFF")
        void sizeOfRejectsFourByteValuesStartingWithFF()
        {
            byte[] encoded = VarSize.encode(0x0FFFFFFF);
            assertArrayEquals(b(0xFF, 0xFF, 0xFF, 0x7F), encoded);
            assertThrows(IllegalArgumentException.class, () -> VarSize.sizeOf(encoded, 0));
        }

        @Test
        @DisplayName("read accepts 4 bytes with the continuation bit set")
        void readAcceptsFourBytesWithContinuationBit() throws IOException
        {
            assertEquals(0, VarSize.read(new ByteArrayInputStream(b(0x80, 0x80, 0x80, 0x80))));
            assertEquals(0, VarSize.read(new ByteArrayInputStream(b(0x80, 0x80, 0x80, 0x80, 0x01))));
        }
    }
}
