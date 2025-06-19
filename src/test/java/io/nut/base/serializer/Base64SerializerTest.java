/*
 *  Base64SerializerTest.java
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
package io.nut.base.serializer;

import io.nut.base.util.Strings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class Base64SerializerTest
{
    @Test
    public void testSome()
    {
        Base64Serializer instance = new Base64Serializer();

        assertNull(instance.toBytes(null));
        
        byte[] s0 = "".getBytes();
        byte[] r0 = instance.toBytes(instance.fromBytes(s0));
        assertArrayEquals(s0, r0);
        
        byte[] s1 = "Hello World".getBytes();
        byte[] r1 = instance.toBytes(instance.fromBytes(s1));
        assertArrayEquals(s1, r1);
        
        for(int i=0;i<100;i++)
        {
            byte[] s = Strings.repeat('a', i).getBytes();
            byte[] r = instance.toBytes(instance.fromBytes(s));
            assertArrayEquals(s, r);
        }   
    }
}
