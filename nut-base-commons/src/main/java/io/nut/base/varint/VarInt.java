/*
 *  VarInt.java
 *
 *  Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.varint;

import io.nut.base.encoding.Hex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for encoding and decoding variable-length integers 
 * using the **VarInt** format (the most compact format used internally by Bitcoin Core).
 * <p>
 * This format is NOT the same as the Bitcoin protocol **CompactSize** (which uses 0xFD/0xFE/0xFF prefixes). 
 * This is a Base-128 encoding with a continuation bit, used specifically for disk storage 
 * (LevelDB, chainstate, etc.).
 * <p>
 * Rules:
 * <ul>
 *   <li>Each byte uses 7 bits for the value and 1 bit (MSB) as a "more bytes" indicator.</li>
 *   <li>Supports values from 0 to 2⁶⁴-1 (equivalent to uint64_t).</li>
 *   <li>Maximum of 10 bytes per number.</li>
 * </ul>
 * 
 * Fully compatible with Java 8. No external dependencies.
 * 
 * @author Grok (based on Bitcoin Core serialize.h implementation)
 */
public final class VarInt
{

    private VarInt()
    {
        // utility class - not instantiable
    }

    /**
     * Writes a VarInt to an OutputStream.
     *
     * @param out   The output stream to write to.
     * @param value The value to encode.
     * @throws IOException If an I/O error occurs.
     */
    public static void write(OutputStream out, long value) throws IOException
    {
        while (true)
        {
            byte b = (byte) (value & 0x7F);
            value >>>= 7;
            if (value == 0)
            {
                out.write(b);
                return;
            }
            out.write(b | 0x80);
        }
    }

    /**
     * Reads a VarInt from an InputStream.
     *
     * @param in The input stream to read from.
     * @return The decoded value as a long.
     * @throws IOException If the end of stream is reached unexpectedly or 
     *                     the VarInt is too large (&gt; 2⁶⁴-1).
     */
    public static long read(InputStream in) throws IOException
    {
        long result = 0;
        int shift = 0;
        while (true)
        {
            int b = in.read();
            if (b == -1)
            {
                throw new EOFException("Unexpected end of stream while reading VarInt");
            }
            result |= ((long) (b & 0x7F)) << shift;
            if ((b & 0x80) == 0)
            {
                break;
            }
            shift += 7;
            if (shift > 63)
            {
                throw new IOException("VarInt too large (more than 64 bits)");
            }
        }
        return result;
    }

    /**
     * Encodes a long value into its VarInt byte representation.
     *
     * @param value The value to encode.
     * @return A byte array containing the encoded value.
     */
    public static byte[] encode(long value)
    {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(10))
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
     * Decodes a VarInt from a byte array starting at a specific offset. 
     * Only reads the necessary bytes (until a byte without the continuation bit is found).
     *
     * @param data   The byte array containing the VarInt.
     * @param offset The starting position in the array.
     * @param length Maximum number of bytes to read.
     * @return The decoded value as a long.
     * @throws IllegalArgumentException If the array does not contain a valid VarInt or it is too large.
     */
    public static long decode(byte[] data, int offset, int length)
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
     * Decodes a VarInt from a byte array. 
     * Only reads the necessary bytes (until a byte without the continuation bit is found).
     *
     * @param data The byte array containing the VarInt.
     * @return The decoded value as a long.
     * @throws IllegalArgumentException If the array does not contain a valid VarInt or it is too large.
     */
    public static long decode(byte[] data)
    {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data))
        {
            return read(bais);
        }
        catch (IOException ex)
        {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Returns the number of bytes required to encode the given value as a VarInt.
     *
     * @param value The value to check.
     * @return The size in bytes (up to 10).
     * @throws IllegalArgumentException If value is negative (and not -1L as UINT64_MAX).
     */
    public static int sizeOf(long value)
    {
        if (value == -1L)
        {
            return 10; // UINT64_MAX → 10 bytes
        }
        if (value < 0)
        {
            throw new IllegalArgumentException("VarInt only supports values >= 0");
        }
        int size = 1;
        while (value >= 0x80)
        {
            size++;
            value >>>= 7;
        }
        return size;
    }

    /**
     * Returns the hexadecimal representation of the VarInt (useful for debugging).
     *
     * @param value The value to encode and represent as hex.
     * @return A hex string.
     */
    public static String toHex(long value)
    {
        return Hex.encode(encode(value), true);
    }
}
