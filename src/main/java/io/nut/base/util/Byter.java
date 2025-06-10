/*
 *  Byter.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author franci
 */
public class Byter
{

    public static byte[] bytes(short[] src, ByteOrder byteOrder)
    {
        if (src == null)
        {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(src.length * Short.BYTES);
        buffer = (byteOrder==null) ? buffer : buffer.order(byteOrder);
        for (short item : src)
        {
            buffer.putShort(item);
        }
        return buffer.array();
    }

    public static byte[] bytes(char[] src, ByteOrder byteOrder)
    {
        if (src == null)
        {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(src.length * Character.BYTES);
        buffer = (byteOrder==null) ? buffer : buffer.order(byteOrder);
        for (char item : src)
        {
            buffer.putChar(item);
        }
        return buffer.array();
    }
    
    public static byte[] bytes(int[] src, ByteOrder byteOrder)
    {
        if (src == null)
        {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(src.length * Integer.BYTES);
        buffer = (byteOrder==null) ? buffer : buffer.order(byteOrder);
        for (int item : src)
        {
            buffer.putInt(item);
        }
        return buffer.array();
    }
    
    public static byte[] bytes(long[] src, ByteOrder byteOrder)
    {
        if (src == null)
        {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(src.length * Long.BYTES);
        buffer = (byteOrder==null) ? buffer : buffer.order(byteOrder);
        for (long item : src)
        {
            buffer.putLong(item);
        }
        return buffer.array();
    }
    
    public static byte[] bytes(short[] src)
    {
        return bytes(src, null);
    }
    public static byte[] bytes(char[] src)
    {
        return bytes(src, null);
    }
    public static byte[] bytes(int[] src)
    {
        return bytes(src, null);
    }
    public static byte[] bytes(long[] src)
    {
        return bytes(src, null);
    }
    
    public static byte[] bytesLE(short[] src)
    {
        return bytes(src, ByteOrder.LITTLE_ENDIAN);
    }
    public static byte[] bytesLE(char[] src)
    {
        return bytes(src, ByteOrder.LITTLE_ENDIAN);
    }
    public static byte[] bytesLE(int[] src)
    {
        return bytes(src, ByteOrder.LITTLE_ENDIAN);
    }
    public static byte[] bytesLE(long[] src)
    {
        return bytes(src, ByteOrder.LITTLE_ENDIAN);
    }

    public static short[] shorts(byte[] src, ByteOrder byteOrder)
    {
        if (src == null)
        {
            return null;
        }
        if(src.length % Short.BYTES != 0)
        {
            throw new IllegalArgumentException("byte[] size is not multiple of " + Short.BYTES);
        }
        int nshorts = src.length / Short.BYTES;
        ByteBuffer buffer = ByteBuffer.wrap(src);
        buffer = (byteOrder==null) ? buffer : buffer.order(byteOrder);
        ShortBuffer dst = ShortBuffer.wrap(new short[nshorts]);
        for (int i = 0; i < nshorts ; i++)
        {
            dst.put(buffer.getShort());
        }
        return dst.array();
    }

    public static char[] chars(byte[] src, ByteOrder byteOrder)
    {
        if (src == null)
        {
            return null;
        }
        if(src.length % Character.BYTES != 0)
        {
            throw new IllegalArgumentException("byte[] size is not multiple of " + Character.BYTES);
        }
        int nchars = src.length / Character.BYTES;
        ByteBuffer buffer = ByteBuffer.wrap(src);
        buffer = (byteOrder==null) ? buffer : buffer.order(byteOrder);
        CharBuffer dst = CharBuffer.wrap(new char[nchars]);
        for (int i = 0; i < nchars ; i++)
        {
            dst.put(buffer.getChar());
        }
        return dst.array();
    }
    
    public static int[] ints(byte[] src, ByteOrder byteOrder)
    {
        if (src == null)
        {
            return null;
        }
        if(src.length % Integer.BYTES != 0)
        {
            throw new IllegalArgumentException("byte[] size is not multiple of " + Integer.BYTES);
        }
        int nints=  src.length / Integer.BYTES;
        ByteBuffer buffer = ByteBuffer.wrap(src);
        buffer = (byteOrder==null) ? buffer : buffer.order(byteOrder);
        IntBuffer dst = IntBuffer.wrap(new int[nints]);
        for (int i = 0; i < nints ; i++)
        {
            dst.put(buffer.getInt());
        }
        return dst.array();
    }
    
    public static long[] longs(byte[] src, ByteOrder byteOrder)
    {
        if (src == null)
        {
            return null;
        }
        if(src.length % Long.BYTES != 0)
        {
            throw new IllegalArgumentException("byte[] size is not multiple of " + Long.BYTES);
        }
        int nlongs=  src.length / Long.BYTES;
        ByteBuffer buffer = ByteBuffer.wrap(src);
        buffer = (byteOrder==null) ? buffer : buffer.order(byteOrder);
        LongBuffer dst = LongBuffer.wrap(new long[nlongs]);
        for (int i = 0; i < nlongs ; i++)
        {
            dst.put(buffer.getLong());
        }
        return dst.array();
    }
    
    public static short[] shorts(byte[] src)
    {
        return shorts(src, null);
    }

    public static char[] chars(byte[] src)
    {
        return chars(src, null);
    }
    
    public static int[] ints(byte[] src)
    {
        return ints(src, null);
    }
    
    public static long[] longs(byte[] src)
    {
        return longs(src, null);
    }
    
    public static short[] shortsLE(byte[] src)
    {
        return shorts(src, ByteOrder.LITTLE_ENDIAN);
    }

    public static char[] charsLE(byte[] src)
    {
        return chars(src, ByteOrder.LITTLE_ENDIAN);
    }
    
    public static int[] intsLE(byte[] src)
    {
        return ints(src, ByteOrder.LITTLE_ENDIAN);
    }
    
    public static long[] longsLE(byte[] src)
    {
        return longs(src, ByteOrder.LITTLE_ENDIAN);
    }
    
    public static byte[] bytesUTF8(char[] src)
    {
        if (src == null)
        {
            return null;
        }
        Charset charset = StandardCharsets.UTF_8;
        ByteBuffer byteBuffer = charset.encode(CharBuffer.wrap(src));
        byte[] byteArray = new byte[byteBuffer.remaining()];
        byteBuffer.get(byteArray);
        return byteArray;
    }

    public static char[] charsUTF8(byte[] src)
    {
        if (src == null)
        {
            return null;
        }
        Charset charset = StandardCharsets.UTF_8;
        CharBuffer charBuffer = charset.decode(ByteBuffer.wrap(src));
        char[] charArray = new char[charBuffer.remaining()];
        charBuffer.get(charArray);
        return charArray;
    }

}
