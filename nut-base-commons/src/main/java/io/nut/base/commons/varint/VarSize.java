/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.varint;

import io.nut.base.commons.encoding.Hex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for encoding and decoding variable-length integers using the
 * **VarInt** format, limited to at most 4 bytes.
 * <p>
 * The value {@code 0xFFFFFFFF} represents {@code -1} and is used to encode a
 * null value.
 * <p>
 * The value is split into groups of 7 bits,
 * from the least significant to the most significant one, and every group but
 * the last is stored with its most significant bit ({@code 0x80}) set.
 *
 * <table border="1">
 *   <caption>VarSize encoding: value ranges and byte patterns</caption>
 *   <tr><th>Bytes</th><th>Value range</th><th>Byte pattern</th></tr>
 *   <tr><td>1</td><td>0x00 .. 0x7F (0 .. 127)</td><td>{@code 0xxxxxxx}</td></tr>
 *   <tr><td>2</td><td>0x80 .. 0x3FFF (128 .. 16 383)</td><td>{@code 1xxxxxxx 0xxxxxxx}</td></tr>
 *   <tr><td>3</td><td>0x4000 .. 0x1FFFFF (16 384 .. 2 097 151)</td><td>{@code 1xxxxxxx 1xxxxxxx 0xxxxxxx}</td></tr>
 *   <tr><td>4</td><td>0x200000 .. 0x0FFFFFFF (2 097 152 .. 268 435 455)</td><td>{@code 1xxxxxxx 1xxxxxxx 1xxxxxxx 0xxxxxxx}</td></tr>
 *   <tr><td>4</td><td>{@code -1} (null)</td><td>{@code 11111111 11111111 11111111 11111111}</td></tr>
 * </table>
 *
 * <p>
 * The 4 byte range holds the 28 bits that fit in the format; the null marker
 * is the only 4 byte sequence with all its bits set, so it cannot collide
 * with a valid value. Values above {@code 0x0FFFFFFF} do not fit in 4 bytes:
 * they are currently encoded in 5 bytes, outside this format, and cannot be
 * decoded back.
 */
public final class VarSize
{

    /**
     * The value used to represent null.
     */
    public static final int NULL_VALUE = -1;

    private VarSize()
    {
        // utility class - not instantiable
    }

    /**
     * Writes a SmallSize to an OutputStream.
     *
     * @param out   The output stream to write to.
     * @param value The value to encode, -1 for null.
     * @throws IOException If an I/O error occurs.
     */
    public static void write(OutputStream out, int value) throws IOException
    {
        if (value == NULL_VALUE)
        {
            out.write(0xFF);
            out.write(0xFF);
            out.write(0xFF);
            out.write(0xFF);
            return;
        }
        if (value < 0)
        {
            throw new IllegalArgumentException("SmallSize only supports values >= -1");
        }
        if (value >= 0x20000000)
        {
            throw new IOException("SmallSize too large (more than 4 bytes)");
        }
        long v = value & 0xFFFFFFFFL;
        while (true)
        {
            byte b = (byte) (v & 0x7F);
            v >>>= 7;
            if (v == 0)
            {
                out.write(b);
                return;
            }
            out.write(b | 0x80);
        }
    }

    /**
     * Reads a SmallSize from an InputStream.
     *
     * @param in The input stream to read from.
     * @return The decoded value as an int, -1 if the encoded value is null.
     * @throws IOException If the end of stream is reached unexpectedly or the
     *                     SmallSize is too large (&gt; 4 bytes).
     */
    public static int read(InputStream in) throws IOException
    {
        // Check for null marker
        in.mark(5);
        byte[] peek = new byte[4];
        int read = in.read(peek);
        if (read == -1)
        {
            throw new EOFException("Unexpected end of stream while reading SmallSize");
        }
        if (read == 4 && (peek[0] & 0xFF) == 0xFF && (peek[1] & 0xFF) == 0xFF && (peek[2] & 0xFF) == 0xFF && (peek[3] & 0xFF) == 0xFF)
        {
            return NULL_VALUE;
        }
        in.reset();
        
        byte[] bytes = new byte[4];
        int count = 0;
        while (count < 4)
        {
            int b = in.read();
            if (b == -1)
            {
                if (count == 0)
                {
                    throw new EOFException("Unexpected end of stream while reading SmallSize");
                }
                throw new EOFException("Unexpected end of stream while reading SmallSize");
            }
            bytes[count] = (byte) b;
            count++;
            if ((b & 0x80) == 0)
            {
                break;
            }
        }
        if ((bytes[0] & 0xFF) == 0xFF && count == 4 && (bytes[1] & 0xFF) == 0xFF && (bytes[2] & 0xFF) == 0xFF && (bytes[3] & 0xFF) == 0xFF)
        {
            return NULL_VALUE;
        }
        // Check if we need to read more (if count == 4 and last has continuation bit)
        if (count == 4 && (bytes[3] & 0x80) != 0)
        {
            // Try to read 5th
            in.mark(1);
            int fifth = in.read();
            if (fifth == -1)
            {
                in.reset();
            }
            else if ((fifth & 0x80) != 0)
            {
                throw new IOException("SmallSize too large (more than 4 bytes)");
            }
            else
            {
                in.reset();
            }
        }
        // Decode
        long result = 0;
        for (int i = 0; i < count; i++)
        {
            result |= ((long) (bytes[i] & 0x7F)) << (7 * i);
        }
        // Check for null
        if (count == 4 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFF && (bytes[2] & 0xFF) == 0xFF && (bytes[3] & 0xFF) == 0xFF)
        {
            return NULL_VALUE;
        }
        if (count == 4 && result >= 0x20000000)
        {
            throw new IOException("SmallSize too large (more than 4 bytes)");
        }
        return (int) result;
    }

    /**
     * Encodes an int value into its SmallSize byte representation.
     *
     * @param value The value to encode, -1 for null.
     * @return A byte array containing the encoded value.
     */
    public static byte[] encode(int value)
    {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(4))
        {
            write(baos, value);
            return baos.toByteArray();
        }
        catch (IOException ex)
        {
            throw new AssertionError("It should not happen", ex);
        }
    }

    /**
     * Decodes a SmallSize from a byte array starting at a specific offset.
     *
     * @param data   The byte array containing the SmallSize.
     * @param offset The starting position in the array.
     * @param length Maximum number of bytes to read.
     * @return The decoded value as an int, -1 if the encoded value is null.
     * @throws IllegalArgumentException If the array does not contain a valid SmallSize or it is too large.
     */
    public static int decode(byte[] data, int offset, int length)
    {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, length))
        {
            return read(bais);
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Decodes a SmallSize from a byte array.
     *
     * @param data The byte array containing the SmallSize.
     * @return The decoded value as an int, -1 if the encoded value is null.
     * @throws IllegalArgumentException If the array does not contain a valid SmallSize or it is too large.
     */
    public static int decode(byte[] data)
    {
        return decode(data, 0, data.length);
    }

    /**
     * Decodes a SmallSize from a byte array starting at a specific offset.
     *
     * @param data   The byte array containing the SmallSize.
     * @param offset The starting position in the array.
     * @return The decoded value as an int, -1 if the encoded value is null.
     * @throws IllegalArgumentException If the array does not contain a valid SmallSize or it is too large.
     */
    public static int decode(byte[] data, int offset)
    {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data, offset, data.length - offset))
        {
            return read(bais);
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Returns the number of bytes required to encode the given value as a SmallSize.
     *
     * @param value The value to check, -1 for null.
     * @return The size in bytes.
     * @throws IllegalArgumentException If value is negative and not -1.
     */
    public static int sizeOf(int value)
    {
        if (value == NULL_VALUE)
        {
            return 4;
        }
        if (value < 0)
        {
            throw new IllegalArgumentException("SmallSize only supports values >= 0");
        }
        if (value < 0x80)
        {
            return 1;
        }
        if (value < 0x4000)
        {
            return 2;
        }
        if (value < 0x200000)
        {
            return 3;
        }
        if (value < 0x20000000)
        {
            return 4;
        }
        throw new IllegalArgumentException("SmallSize only supports values <= 0xFFFFFFFF");
    }

    /**
     * Returns the number of bytes occupied by the SmallSize encoded at the given
     * offset, without decoding it.
     *
     * @param buf    The byte array containing the SmallSize.
     * @param offset The starting position of the SmallSize in the array.
     * @return The number of bytes of the encoded value.
     * @throws IllegalArgumentException If the array is too short or invalid.
     */
    public static int sizeOf(byte[] buf, int offset)
    {
        if (offset < 0 || offset >= buf.length)
        {
            throw new IllegalArgumentException("Buffer too short to decode a SmallSize");
        }
        // Check for null marker
        if ((buf[offset] & 0xFF) == 0xFF)
        {
            if (offset + 4 > buf.length)
            {
                throw new IllegalArgumentException("Buffer too short to decode a SmallSize");
            }
            if ((buf[offset + 1] & 0xFF) == 0xFF && (buf[offset + 2] & 0xFF) == 0xFF && (buf[offset + 3] & 0xFF) == 0xFF)
            {
                return 4;
            }
            throw new IllegalArgumentException("Invalid SmallSize");
        }
        if ((buf[offset] & 0x80) == 0)
        {
            return 1;
        }
        if (offset + 1 >= buf.length)
        {
            throw new IllegalArgumentException("Buffer too short to decode a SmallSize");
        }
        if ((buf[offset + 1] & 0x80) == 0)
        {
            return 2;
        }
        if (offset + 2 >= buf.length)
        {
            throw new IllegalArgumentException("Buffer too short to decode a SmallSize");
        }
        if ((buf[offset + 2] & 0x80) == 0)
        {
            return 3;
        }
        if (offset + 3 >= buf.length)
        {
            throw new IllegalArgumentException("Buffer too short to decode a SmallSize");
        }
        if ((buf[offset + 3] & 0x80) == 0)
        {
            return 4;
        }
        throw new IllegalArgumentException("SmallSize too large (more than 4 bytes)");
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
}
