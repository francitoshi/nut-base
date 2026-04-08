/*
 *  CompactSize.java
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
import io.nut.base.math.Integers;
import java.io.*;

/**
 * Utility class for encoding and decoding variable-length integers 
 * using the **CompactSize** format (the Bitcoin protocol standard).
 * <p>
 * Supports the full **uint64** range (0 to 2⁶⁴-1).
 * In Java, values &gt;= 2⁶³ are represented as negative numbers (long with the sign bit set).
 * For example: 0xCAFEBABEDEADBEEFL is handled correctly.
 * <p>
 * Encoding rules:
 * <ul>
 *   <li>0..252          → 1 byte</li>
 *   <li>253..65535      → 3 bytes (0xFD + 2 bytes little-endian)</li>
 *   <li>65536..2³²-1    → 5 bytes (0xFE + 4 bytes little-endian)</li>
 *   <li>2³²..2⁶⁴-1      → 9 bytes (0xFF + 8 bytes little-endian)</li>
 * </ul>
 * 
 * Fully compatible with Java 8. No external dependencies.
 */
public final class CompactSize
{

    private CompactSize()
    {
        // utility class - not instantiable
    }

    /**
     * Writes a CompactSize to an OutputStream. Supports any uint64 value
     * (including negative numbers representing values &gt; 2⁶³-1).
     *
     * @param out   The output stream to write to.
     * @param value The value to encode.
     * @throws IOException If an I/O error occurs.
     */
    public static void write(OutputStream out, long value) throws IOException
    {
        if (value < 0 || value > 0xFFFFFFFFL)
        {
            // 9-byte case: any value >= 2³² (including negatives)
            out.write(0xFF);
            for (int i = 0; i < 8; i++)
            {
                // >>> used to handle negatives correctly
                out.write((int) ((value >>> (8 * i)) & 0xFF)); 
            }
        }
        else if (value < 0xFD)
        {
            out.write((int) value);
        }
        else if (value <= 0xFFFFL)
        {
            out.write(0xFD);
            out.write((int) value & 0xFF);
            out.write((int) (value >> 8) & 0xFF);
        }
        else
        {
            out.write(0xFE);
            for (int i = 0; i < 4; i++)
            {
                out.write((int) (value >> (8 * i)) & 0xFF);
            }
        }        
    }

    /**
     * Reads a CompactSize from an InputStream.
     *
     * @param in The input stream to read from.
     * @return The decoded value as a long.
     * @throws IOException If an I/O error occurs or the stream ends prematurely.
     */
    public static long read(InputStream in) throws IOException
    {
        int first = in.read();
        if (first == -1)
        {
            throw new EOFException("Unexpected end of stream when reading CompactSize");
        }

        if (first < 0xFD)
        {
            return first;
        }
        if (first == 0xFD)
        {
            byte[] buf = readBytes(in, 2);
            return ((buf[1] & 0xFFL) << 8) | (buf[0] & 0xFFL);
        }
        if (first == 0xFE)
        {
            byte[] buf = readBytes(in, 4);
            long v = 0;
            for (int i = 0; i < 4; i++)
            {
                v |= (buf[i] & 0xFFL) << (8 * i);
            }
            return v;
        }
        if (first == 0xFF)
        {
            byte[] buf = readBytes(in, 8);
            long v = 0;
            for (int i = 0; i < 8; i++)
            {
                v |= (buf[i] & 0xFFL) << (8 * i);
            }
            return v;
        }
        throw new IOException("Invalid CompactSize: first byte 0x" + Integer.toHexString(first));
    }

    /**
     * Encodes a long value into its CompactSize byte representation.
     *
     * @param value The value to encode.
     * @return A byte array containing the encoded value.
     */
    public static byte[] encode(long value)
    {
        byte[] bytes;
        switch(sizeOf(value))
        {
            case 1:
                return new byte[]{ (byte) value };
            case 3:
                bytes = new byte[3];
                bytes[0] = (byte) 253;
                Integers.uint16ToByteArrayLE((int) value, bytes, 1);
                return bytes;
            case 5:
                bytes = new byte[5];
                bytes[0] = (byte) 254;
                Integers.uint32ToByteArrayLE(value, bytes, 1);
                return bytes;
            default:
                bytes = new byte[9];
                bytes[0] = (byte) 255;
                Integers.int64ToByteArrayLE(value, bytes, 1);
                return bytes;
        }
    }
    
    /**
     * Encodes a long value into a byte array at the specified offset.
     *
     * @param value  The value to encode.
     * @param bytes  The destination byte array.
     * @param offset The starting position in the array.
     * @return The number of bytes written.
     */
    public static int encode(long value, byte[] bytes, int offset)
    {
        switch(sizeOf(value))
        {
            case 1:
                bytes[offset] = (byte) value;
                return 1;
            case 3:
                bytes[offset] = (byte) 253;
                Integers.uint16ToByteArrayLE((int) value, bytes, offset+1);
                return 3;
            case 5:
                bytes[offset] = (byte) 254;
                Integers.uint32ToByteArrayLE(value, bytes, offset+1);
                return 5;
            default:
                bytes[offset] = (byte) 255;
                Integers.int64ToByteArrayLE(value, bytes, offset+1);
                return 9;
        }
    }
    
    /**
     * Decodes a CompactSize from a byte array starting at a specific offset.
     *
     * @param buf    The byte array containing the CompactSize.
     * @param offset The starting position in the array.
     * @return The decoded value as a long.
     */
    public static long decode(byte[] buf, int offset)
    {
        int first = 0xFF & buf[offset];
        if (first < 253)
        {
            return first;
        }
        else if (first == 253)
        {
            return Integers.readUint16(buf, offset + 1);
        }
        else if (first == 254)
        {
            return Integers.readUint32(buf, offset + 1);
        }
        else
        {
            return Integers.readInt64(buf, offset + 1);
        }
    }
    
    /**
     * Decodes a CompactSize from a byte array.
     *
     * @param buf The byte array containing the CompactSize.
     * @return The decoded value as a long.
     */
    public static long decode(byte[] buf)
    {
        return decode(buf, 0);
    }

    /**
     * Returns the number of bytes required to encode the given value as a CompactSize.
     * Supports the full uint64 range (negative values = &gt; 2⁶³-1).
     *
     * @param value The value to check.
     * @return The size in bytes (1, 3, 5, or 9).
     */
    public static int sizeOf(long value)
    {
        // If negative, it's actually a very large unsigned long value
        if (value < 0)
        {
            return 9; // 1 marker + 8 data bytes
        }
        if (value < 253)
        {
            return 1; // 1 data byte
        }
        if (value <= 0xFFFFL)
        {
            return 3; // 1 marker + 2 data bytes
        }
        if (value <= 0xFFFFFFFFL)
        {
            return 5; // 1 marker + 4 data bytes
        }
        return 9; // 1 marker + 8 data bytes
    }

    /**
     * Returns the hexadecimal representation of the encoded value (useful for debugging).
     *
     * @param value The value to encode and represent as hex.
     * @return A hex string.
     */
    public static String toHex(long value)
    {
        return Hex.encode(encode(value), true);
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
