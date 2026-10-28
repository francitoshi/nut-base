/*
 * Copyright (C) 2019-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class MeteredOutputStreamTest
{
    @Test
    public void testAll() throws Exception
    {
        byte[] buf = new byte[1024];
        for(int i=0;i<buf.length;i++)
        {
            buf[i] = (byte)i;
        }
        
        MeteredOutputStream instance = new MeteredOutputStream(new ByteArrayOutputStream());
        assertEquals(0, instance.getWrittenCount());
        
        instance.write(1);
        assertEquals(1, instance.getWrittenCount());
        
        instance.write(buf);
        assertEquals(1025, instance.getWrittenCount());
        
        instance.write(buf,10,1);
        assertEquals(1026, instance.getWrittenCount());
    }
    
}
