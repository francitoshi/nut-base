/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.debug;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class DebuggableByteBufferTest
{

    /**
     * Test of slice method, of class DebugByteBuffer.
     */
    @Test
    public void testDebug()
    {
        DebuggableByteBuffer debug = DebuggableByteBuffer.wrap(ByteBuffer.allocate(40), true);
        byte[] hw;
        debug.put((byte)123);
        debug.putChar((char)12345);
        debug.putShort((short)-12345);
        debug.putInt(1234567890);
        debug.putLong(1234567890123456789L);
        debug.putFloat(1.23456f);
        debug.putDouble(1.23456789);
        debug.put(hw = "hello world".getBytes());
        byte[][] data = debug.debug();
        
        assertEquals(data.length, 8);
        assertArrayEquals(hw, data[7]);
        
    }
    
}
