/*
 *  ByteBufferCompat.java
 *
 *  Copyright (C) 2024 francitoshi@gmail.com
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
package io.nut.base.compat;

import java.nio.ByteBuffer;

/**
 *
 * @author franci
 */
public final class ByteBufferCompat
{
    private final ByteBuffer byteBuffer;

    public ByteBufferCompat(ByteBuffer byteBuffer)
    {
        this.byteBuffer = byteBuffer;
    }

    public byte get()
    {
        return this.byteBuffer.get();
    }

    public byte get(int index)
    {
        return this.byteBuffer.get(index);
    }

    public ByteBufferCompat get(int index, byte[] dst, int offset, int length)
    {
        for (int i = offset, j = index; i < offset + length; i++, j++)
        {
            dst[i] = this.get(j);
        }
        return this;
    }

    public ByteBufferCompat get(byte[] dst)
    {
        return this.get(0, dst, 0, dst.length);
    }
    
    public ByteBufferCompat get(byte[] dst, int offset, int length)
    {
        return this.get(0, dst, offset, dst.length);
    }

    public ByteBufferCompat get(int index, byte[] dst)
    {
        return this.get(index, dst, 0, dst.length);
    }

    public ByteBufferCompat put(byte b)
    {
        this.byteBuffer.put(b);
        return this;
    }

    public ByteBufferCompat put(int index, byte b)
    {
        this.byteBuffer.put(index, b);
        return this;
    }

    public ByteBufferCompat put(ByteBuffer src)
    {
        this.byteBuffer.put(src);
        return this;
    }
    public ByteBufferCompat put(ByteBufferCompat src)
    {
        this.byteBuffer.put(src.byteBuffer);
        return this;
    }

    public ByteBufferCompat put(byte[] src, int offset, int length)
    {
        this.byteBuffer.put(src, offset, length);
        return this;
    }

    public ByteBufferCompat put(byte[] src)
    {
        this.byteBuffer.put(src);
        return this;
    }

    public ByteBufferCompat put(int index, ByteBuffer src, int offset, int length)
    {
        for (int i = offset, j = index; i < offset + length; i++, j++)
        {
            this.put(j, src.get(i));
        }
        return this;
    }
    
    public ByteBufferCompat put(int index, byte[] src, int offset, int length)
    {
        for (int i = offset, j = index; i < offset + length; i++, j++)
        {
            this.put(j, src[i]);
        }
        return this;
    }
    
    public ByteBufferCompat put(int index, byte[] src)
    {
        return this.put(index, src, 0, src.length);
    }

    public final byte[] array()
    {
        return this.byteBuffer.array();
    }
    
}
