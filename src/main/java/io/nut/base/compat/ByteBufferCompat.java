/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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
        this.byteBuffer.get(dst);
        return this;
    }
    
    public ByteBufferCompat get(byte[] dst, int offset, int length)
    {
        this.byteBuffer.get(dst, offset, length);
        return this;
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
