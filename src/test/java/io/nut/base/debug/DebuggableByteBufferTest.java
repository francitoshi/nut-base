/*
 *  DebuggableByteBufferTest.java
 *
 *  Copyright (c) 2023-2024 francitoshi@gmail.com
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
package io.nut.base.debug;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
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
