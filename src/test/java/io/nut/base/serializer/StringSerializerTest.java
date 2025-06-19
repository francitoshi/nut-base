/*
 *  StringSerializerTest.java
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
public class StringSerializerTest
{
    @Test
    public void testSome()
    {
        StringSerializer instance = new StringSerializer();
        
        assertNull(instance.toBytes(null));
        
        String s0 = "";
        String r0 = instance.fromBytes(instance.toBytes(s0));
        assertEquals(s0, r0);
        
        String s1 = "Hello World";
        String r1 = instance.fromBytes(instance.toBytes(s1));
        assertEquals(s1, r1);
                
        for(int i=0;i<100;i++)
        {
            String s = Strings.repeat('a', i);
            String r = instance.fromBytes(instance.toBytes(s));
            assertEquals(s, r);
        }
    }
}
