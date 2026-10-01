/*
 *  MeteredOutputStream.java
 *
 *  Copyright (c) 2019-2024 francitoshi@gmail.com
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
package io.nut.base.io;

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
