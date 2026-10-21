/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.varint;

import io.nut.base.commons.encoding.Hex;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for encoding and decoding variable-length integers
 * using the **SmallInt** format.
 * <p>
 * The two most significant bits of the first byte are a prefix that tells how
 * many additional bytes the number takes and how the value is distributed:
 * <ul>
 *   <li><b>00</b> → 0 additional bytes: the value is the 6 least significant bits of the first byte (0..63)</li>
 *   <li><b>01</b> → 1 additional byte: the value is the 6 least significant bits of the first byte (the least
 *       significant ones) plus the 8 bits of the second byte (the most significant ones) (0..16383)</li>
 *   <li><b>10</b> → 2 additional bytes: the value is the 6 least significant bits of the first byte (the least
 *       significant ones), plus the 8 bits of the second byte, plus the 8 bits of the third byte
 *       (the most significant ones) (0..4194303)</li>
 *   <li><b>11</b> → -1: the 6 remaining bits of the first byte must be 0 (the byte is 0xC0). Any other
 *       value is invalid</li>
 * </ul>
 * <p>
 * The value -1 is the way to represent a null: whoever encodes a null writes -1 and
 * whoever decodes it gets -1. The other values are unsigned, so they range from 0
 * to {@value #MAX_VALUE}.
 * <p>
 * Values that do not fit in the format (less than -1 or greater than
 * {@value #MAX_VALUE}) raise an {@link IllegalArgumentException}.
 * <p>
 * Fully compatible with Java 8. No external dependencies.
 */
public final class SmallInt
{
    /**
     * The greatest value that can be encoded (22 bits).
     */
    public static final int MAX_VALUE = 0x3FFFFF;

    /**
     * The value used to represent null.
     */
    public static final int NULL_VALUE = -1;

    private static final int BYTE_PREFIX = 0x00;
    private static final int SHORT_PREFIX = 0x40;
    private static final int INT_PREFIX = 0x80;
    private static final int NULL_PREFIX = 0xC0;

    private static final int VALUE_MASK = 0x3F;

    private SmallInt()
    {
        // utility class - not instantiable
    }

    /**
     * Writes a SmallInt to an OutputStream.
     *
     * @param out   The output stream to write to.
     * @param value The value to encode, -1 for null.
     * @throws IOException If an I/O error occurs.
     */
    public static void write(OutputStream out, int value) throws IOException
    {
        switch (sizeOf(value))
        {
            case 1:
                out.write(value == NULL_VALUE ? NULL_PREFIX : (BYTE_PREFIX | value));
                break;
            case 2:
                out.write(SHORT_PREFIX | (value & VALUE_MASK));
                out.write((value >>> 6) & 0xFF);
                break;
            default:
                out.write(INT_PREFIX | (value & VALUE_MASK));
                out.write((value >>> 6) & 0xFF);
                out.write((value >>> 14) & 0xFF);
                break;
        }
    }

    /**
     * Reads a SmallInt from an InputStream.
     *
     * @param in The input stream to read from.
     * @return The decoded value, -1 if the encoded value is null.
     * @throws IOException If an I/O error occurs, the stream ends prematurely or the
     *                     encoded value is not valid.
     */
    public static int read(InputStream in) throws IOException
    {
        int first = in.read();
        if (first == -1)
        {
            throw new EOFException("Unexpected end of stream when reading SmallInt");
        }

        switch (first & 0xC0)
        {
            case BYTE_PREFIX:
                return first & VALUE_MASK;
            case SHORT_PREFIX:
            {
                byte[] buf = readBytes(in, 1);
                return (first & VALUE_MASK) | ((buf[0] & 0xFF) << 6);
            }
            case INT_PREFIX:
            {
                byte[] buf = readBytes(in, 2);
                return (first & VALUE_MASK) | ((buf[0] & 0xFF) << 6) | ((buf[1] & 0xFF) << 14);
            }
            default:
                if ((first & VALUE_MASK) != 0)
                {
                    throw new IOException("Invalid SmallInt: the null prefix requires the remaining bits to be 0");
                }
                return NULL_VALUE;
        }
    }

    /**
     * Encodes a value into its SmallInt byte representation.
     *
     * @param value The value to encode, -1 for null.
     * @return A byte array containing the encoded value.
     * @throws IllegalArgumentException If the value does not fit in the format.
     */
    public static byte[] encode(int value)
    {
        byte[] bytes = new byte[sizeOf(value)];
        encode(value, bytes, 0);
        return bytes;
    }

    /**
     * Encodes a value into a byte array at the specified offset.
     *
     * @param value  The value to encode, -1 for null.
     * @param bytes  The destination byte array.
     * @param offset The starting position in the array.
     * @return The number of bytes written.
     * @throws IllegalArgumentException If the value does not fit in the format.
     */
    public static int encode(int value, byte[] bytes, int offset)
    {
        switch (sizeOf(value))
        {
            case 1:
                bytes[offset] = value == NULL_VALUE ? (byte) NULL_PREFIX : (byte) (BYTE_PREFIX | value);
                return 1;
            case 2:
                bytes[offset] = (byte) (SHORT_PREFIX | (value & VALUE_MASK));
                bytes[offset + 1] = (byte) ((value >>> 6) & 0xFF);
                return 2;
            default:
                bytes[offset] = (byte) (INT_PREFIX | (value & VALUE_MASK));
                bytes[offset + 1] = (byte) ((value >>> 6) & 0xFF);
                bytes[offset + 2] = (byte) ((value >>> 14) & 0xFF);
                return 3;
        }
    }

    /**
     * Decodes a SmallInt from a byte array starting at a specific offset.
     *
     * @param buf    The byte array containing the SmallInt.
     * @param offset The starting position in the array.
     * @return The decoded value, -1 if the encoded value is null.
     * @throws IllegalArgumentException If the array is too short or the encoded value is not valid.
     */
    public static int decode(byte[] buf, int offset)
    {
        int first = 0xFF & buf[offset];
        switch (first & 0xC0)
        {
            case BYTE_PREFIX:
                return first & VALUE_MASK;
            case SHORT_PREFIX:
            {
                checkLength(buf, offset, 2);
                return (first & VALUE_MASK) | ((0xFF & buf[offset + 1]) << 6);
            }
            case INT_PREFIX:
            {
                checkLength(buf, offset, 3);
                return (first & VALUE_MASK) | ((0xFF & buf[offset + 1]) << 6) | ((0xFF & buf[offset + 2]) << 14);
            }
            default:
                if ((first & VALUE_MASK) != 0)
                {
                    throw new IllegalArgumentException("Invalid SmallInt: the null prefix requires the remaining bits to be 0");
                }
                return NULL_VALUE;
        }
    }

    /**
     * Decodes a SmallInt from a byte array.
     *
     * @param buf The byte array containing the SmallInt.
     * @return The decoded value, -1 if the encoded value is null.
     * @throws IllegalArgumentException If the array is too short or the encoded value is not valid.
     */
    public static int decode(byte[] buf)
    {
        return decode(buf, 0);
    }

    /**
     * Returns the number of bytes required to encode the given value as a SmallInt.
     *
     * @param value The value to check, -1 for null.
     * @return The size in bytes (1, 2 or 3).
     * @throws IllegalArgumentException If the value does not fit in the format.
     */
    public static int sizeOf(int value)
    {
        if (value < NULL_VALUE)
        {
            throw new IllegalArgumentException("SmallInt only supports values >= -1");
        }
        if (value == NULL_VALUE)
        {
            return 1; // 1 byte with the null prefix
        }
        if (value <= VALUE_MASK)
        {
            return 1; // 1 prefix byte with 6 value bits
        }
        if (value <= 0x3FFF)
        {
            return 2; // 1 prefix byte + 1 byte, 14 value bits
        }
        if (value <= MAX_VALUE)
        {
            return 3; // 1 prefix byte + 2 bytes, 22 value bits
        }
        throw new IllegalArgumentException("SmallInt only supports values <= " + MAX_VALUE);
    }

    /**
     * Returns the hexadecimal representation of the encoded value (useful for debugging).
     *
     * @param value The value to encode and represent as hex.
     * @return A hex string.
     */
    public static String toHex(int value)
    {
        return Hex.encode(encode(value), true);
    }

    private static void checkLength(byte[] buf, int offset, int length)
    {
        if (offset < 0 || buf.length < offset + length)
        {
            throw new IllegalArgumentException("Buffer too short to decode a SmallInt of " + length + " bytes");
        }
    }

    private static byte[] readBytes(InputStream in, int len) throws IOException
    {
        byte[] b = new byte[len];
        int read = in.read(b);
        if (read != len)
        {
            throw new EOFException("Could not read " + len + " bytes");
        }
        return b;
    }
}
