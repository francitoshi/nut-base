/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.compat;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ByteBufferCompatTest
{
    @Test
    public void testArray()
    {
        ByteBufferCompat instance = new ByteBufferCompat(ByteBuffer.allocate(100));
        byte[] result = instance.array();
        assertEquals(100, result.length);
    }

    @Test
    public void testGetAndPutSingleByte()
    {
        ByteBufferCompat instance = new ByteBufferCompat(ByteBuffer.allocate(10));
        instance.put((byte) 5);
        instance.put((byte) 10);
        
        // Absolute get
        assertEquals(5, instance.get(0));
        assertEquals(10, instance.get(1));
        
        // Relative get (need to flip/reset underlying position or read from start)
        ByteBuffer raw = ByteBuffer.allocate(10);
        ByteBufferCompat compat = new ByteBufferCompat(raw);
        compat.put((byte) 42);
        raw.flip();
        assertEquals(42, compat.get());
    }

    @Test
    public void testPutAndGetRelativeByteArray()
    {
        byte[] data = {1, 2, 3, 4, 5};
        ByteBuffer raw = ByteBuffer.allocate(10);
        ByteBufferCompat instance = new ByteBufferCompat(raw);
        
        instance.put(data);
        raw.flip();
        
        byte[] readData = new byte[5];
        instance.get(readData);
        assertArrayEquals(data, readData);
        
        // Verify relative get with offset and length
        raw.clear();
        instance.put(data);
        raw.flip();
        
        byte[] readDataOffset = new byte[7];
        instance.get(readDataOffset, 1, 5);
        assertArrayEquals(new byte[]{0, 1, 2, 3, 4, 5, 0}, readDataOffset);
    }

    @Test
    public void testPutAndGetAbsoluteByteArray()
    {
        byte[] data = {10, 20, 30};
        ByteBuffer raw = ByteBuffer.allocate(10);
        ByteBufferCompat instance = new ByteBufferCompat(raw);
        
        instance.put(2, data);
        
        byte[] readData = new byte[3];
        instance.get(2, readData);
        assertArrayEquals(data, readData);
        
        byte[] readDataOffset = new byte[5];
        instance.get(2, readDataOffset, 1, 3);
        assertArrayEquals(new byte[]{0, 10, 20, 30, 0}, readDataOffset);
    }

    @Test
    public void testPutSingleByteAbsolute()
    {
        ByteBuffer raw = ByteBuffer.allocate(5);
        ByteBufferCompat instance = new ByteBufferCompat(raw);
        instance.put(3, (byte) 99);
        assertEquals(99, instance.get(3));
    }

    @Test
    public void testPutByteBufferAndByteBufferCompat()
    {
        // put(ByteBuffer)
        ByteBuffer srcRaw = ByteBuffer.wrap(new byte[]{1, 2, 3});
        ByteBuffer destRaw = ByteBuffer.allocate(5);
        ByteBufferCompat dest = new ByteBufferCompat(destRaw);
        dest.put(srcRaw);
        destRaw.flip();
        byte[] res = new byte[3];
        destRaw.get(res);
        assertArrayEquals(new byte[]{1, 2, 3}, res);

        // put(ByteBufferCompat)
        ByteBuffer srcRaw2 = ByteBuffer.wrap(new byte[]{4, 5, 6});
        ByteBufferCompat srcCompat = new ByteBufferCompat(srcRaw2);
        ByteBuffer destRaw2 = ByteBuffer.allocate(5);
        ByteBufferCompat dest2 = new ByteBufferCompat(destRaw2);
        dest2.put(srcCompat);
        destRaw2.flip();
        byte[] res2 = new byte[3];
        destRaw2.get(res2);
        assertArrayEquals(new byte[]{4, 5, 6}, res2);
    }

    @Test
    public void testPutByteArrayWithOffsetAndLength()
    {
        byte[] src = {100, 101, 102, 103, 104};
        ByteBuffer destRaw = ByteBuffer.allocate(5);
        ByteBufferCompat dest = new ByteBufferCompat(destRaw);
        dest.put(src, 1, 3);
        destRaw.flip();
        byte[] res = new byte[3];
        destRaw.get(res);
        assertArrayEquals(new byte[]{101, 102, 103}, res);
    }

    @Test
    public void testPutAbsoluteByteBufferAndByteArray()
    {
        ByteBuffer srcRaw = ByteBuffer.wrap(new byte[]{7, 8});
        ByteBuffer destRaw = ByteBuffer.allocate(6);
        ByteBufferCompat dest = new ByteBufferCompat(destRaw);
        
        dest.put(2, srcRaw, 0, 2);
        assertEquals(7, dest.get(2));
        assertEquals(8, dest.get(3));

        byte[] srcBytes = {11, 12};
        dest.put(4, srcBytes, 0, 2);
        assertEquals(11, dest.get(4));
        assertEquals(12, dest.get(5));
    }
}
