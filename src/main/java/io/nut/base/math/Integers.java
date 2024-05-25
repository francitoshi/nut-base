/*
 *  Integers.java
 *
 *  Copyright (c) 2023 francitoshi@gmail.com
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
package io.nut.base.math;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author franci
 */
public class Integers
{
    public static int unsigned(short value)
    {
        return ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort(0,value).getInt(0);
    }
    
    /**
     * from BitcoinJ
     * Write 2 bytes to the byte array (starting at the offset) as unsigned
     * 16-bit integer in little endian format.
     */
    public static void uint16ToByteArrayLE(int val, byte[] out, int offset)
    {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
    }

    /**
     * from BitcoinJ
     * Write 4 bytes to the byte array (starting at the offset) as unsigned
     * 32-bit integer in little endian format.
     */
    public static void uint32ToByteArrayLE(long val, byte[] out, int offset)
    {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
    }

    /**
     * from BitcoinJ
     * Write 8 bytes to the byte array (starting at the offset) as signed 64-bit
     * integer in little endian format.
     */
    public static void int64ToByteArrayLE(long val, byte[] out, int offset)
    {
        out[offset] = (byte) (0xFF & val);
        out[offset + 1] = (byte) (0xFF & (val >> 8));
        out[offset + 2] = (byte) (0xFF & (val >> 16));
        out[offset + 3] = (byte) (0xFF & (val >> 24));
        out[offset + 4] = (byte) (0xFF & (val >> 32));
        out[offset + 5] = (byte) (0xFF & (val >> 40));
        out[offset + 6] = (byte) (0xFF & (val >> 48));
        out[offset + 7] = (byte) (0xFF & (val >> 56));
    }    

    /**
     * from BitcoinJ
     * Parse 2 bytes from the byte array (starting at the offset) as unsigned
     * 16-bit integer in little endian format.
     */
    public static int readUint16(byte[] bytes, int offset)
    {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8);
    }

    /**
     * from BitcoinJ
     * Parse 4 bytes from the byte array (starting at the offset) as unsigned
     * 32-bit integer in little endian format.
     */
    public static long readUint32(byte[] bytes, int offset)
    {
        return (bytes[offset] & 0xffL)
                | ((bytes[offset + 1] & 0xffL) << 8)
                | ((bytes[offset + 2] & 0xffL) << 16)
                | ((bytes[offset + 3] & 0xffL) << 24);
    }

    /**
     * from BitcoinJ
     * Parse 8 bytes from the byte array (starting at the offset) as signed
     * 64-bit integer in little endian format.
     */
    public static long readInt64(byte[] bytes, int offset)
    {
        return (bytes[offset] & 0xffL)
                | ((bytes[offset + 1] & 0xffL) << 8)
                | ((bytes[offset + 2] & 0xffL) << 16)
                | ((bytes[offset + 3] & 0xffL) << 24)
                | ((bytes[offset + 4] & 0xffL) << 32)
                | ((bytes[offset + 5] & 0xffL) << 40)
                | ((bytes[offset + 6] & 0xffL) << 48)
                | ((bytes[offset + 7] & 0xffL) << 56);
    }
    
    /**
     * from BitcoinJ
     * Parse 4 bytes from the byte array (starting at the offset) as unsigned
     * 32-bit integer in big endian format.
     */
    public static long readUint32BE(byte[] bytes, int offset)
    {
        return ((bytes[offset] & 0xffL) << 24)
                | ((bytes[offset + 1] & 0xffL) << 16)
                | ((bytes[offset + 2] & 0xffL) << 8)
                | (bytes[offset + 3] & 0xffL);
    }

    /**
     * from BitcoinJ
     * Parse 2 bytes from the byte array (starting at the offset) as unsigned
     * 16-bit integer in big endian format.
     */
    public static int readUint16BE(byte[] bytes, int offset)
    {
        return ((bytes[offset] & 0xff) << 8)
                | (bytes[offset + 1] & 0xff);
    }

    /**
     * from BitcoinJ
     * Parse 2 bytes from the stream as unsigned 16-bit integer in little endian
     * format.
     */
    public static int readUint16FromStream(InputStream is)
    {
        try
        {
            return (is.read() & 0xff) | ((is.read() & 0xff) << 8);
        }
        catch (IOException x)
        {
            throw new RuntimeException(x);
        }
    }
    
}
