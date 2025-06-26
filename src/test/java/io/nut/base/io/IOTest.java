/*
 *  IOTest.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
import java.io.InputStream;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class IOTest
{
    
    @Test
    public void testReadAllBytes_InputStream_int() throws Exception
    {
        Random random = new Random(0);
        byte[] data = new byte[48000];
        random.nextBytes(data);
        
        InputStream in = new ByteArrayInputStream(data);
        byte[] result = IO.readAllBytes(in, 64000);
        assertArrayEquals(data, result);

        in = new ByteArrayInputStream(data);
        result = IO.readAllBytes(in, 8000);
        assertEquals(8000, result.length);

    }
    
}
