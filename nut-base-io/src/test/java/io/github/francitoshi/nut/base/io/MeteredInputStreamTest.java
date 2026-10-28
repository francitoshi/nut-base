/*
 * Copyright (C) 2019-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class MeteredInputStreamTest
{
    @Test
    public void testAll() throws Exception
    {
        byte[] buf = new byte[1024];
        for(int i=0;i<buf.length;i++)
        {
            buf[i] = (byte)i;
        }
        byte[] data = new byte[10];
        
        MeteredInputStream instance = new MeteredInputStream(new ByteArrayInputStream(buf));
        assertEquals(0, instance.getReadCount());
        
        instance.read();
        assertEquals(1, instance.getReadCount());
        
        instance.read(data);
        assertEquals(11, instance.getReadCount());
        
        instance.skip(20);
        assertEquals(20, instance.getSkipCount());
        assertEquals(11, instance.getReadCount());
        
        instance.read(data,5,1);
        assertEquals(12, instance.getReadCount());
    }
    
}
